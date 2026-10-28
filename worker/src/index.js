/**
 * ClawBack guardian push relay (Cloudflare Worker, free plan).
 *
 * Replaces Firebase Cloud Functions so the Firebase project can stay on the free Spark plan.
 *
 *   POST /alert       child phone -> push to its linked guardian phone (Firebase ID token required)
 *   cron (30 min)     missed check-in warnings, "back online" notices, stale pairing-code cleanup
 *
 * Talks to Firestore and FCM over their REST APIs using a Firebase service account
 * stored as the FIREBASE_SERVICE_ACCOUNT secret.
 */

// A phone in deep sleep (Doze) can delay its 15-minute check-ins by a few hours overnight,
// so a shorter threshold would cause false alarms.
const MISSED_CHECK_IN_HOURS = 6;
const BACK_ONLINE_WINDOW_MINUTES = 60;
const UNCLAIMED_LINK_TTL_HOURS = 24;
// Free Workers allow 50 outbound requests per invocation; keep each sweep well inside that.
const SWEEP_BATCH = 10;

const ALERT_TYPES = new Set([
  "SETUP_TEST", "UNINSTALL_ATTEMPT", "ADMIN_DISABLE_ATTEMPT", "ADMIN_DISABLED", "PAIRED",
]);
const URGENT_TYPES = new Set(["UNINSTALL_ATTEMPT", "ADMIN_DISABLED", "MISSED_CHECK_IN"]);
const CODE_PATTERN = /^[A-HJ-NP-Z2-9]{8}$/;

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.pathname !== "/alert") return json({ error: "not_found" }, 404);
    if (request.method !== "POST") return json({ error: "method_not_allowed" }, 405);

    let uid;
    try {
      uid = await verifyIdToken(bearerToken(request), env.FIREBASE_PROJECT_ID);
    } catch (err) {
      return json({ error: "unauthorized" }, 401);
    }

    const payload = await request.json().catch(() => null);
    const code = String(payload?.code ?? "");
    const type = String(payload?.type ?? "");
    if (!CODE_PATTERN.test(code) || !ALERT_TYPES.has(type)) {
      return json({ error: "bad_request" }, 400);
    }
    const title = String(payload.title ?? "Clawse Guardian Alert").slice(0, 120);
    const body = String(payload.body ?? "").slice(0, 500);

    const google = await googleAuth(env);
    const link = await getLink(env, google, code);
    if (!link || link.childUid !== uid) return json({ error: "forbidden" }, 403);

    const sent = await pushToGuardian(env, google, code, link, type, title, body);
    return json({ ok: true, delivered: sent });
  },

  async scheduled(event, env, ctx) {
    ctx.waitUntil(sweep(env));
  },
};

// ==========================================
// SCHEDULED SWEEP
// ==========================================

async function sweep(env) {
  const google = await googleAuth(env);
  const now = Date.now();

  // 1. Phones that stopped checking in.
  const silent = await queryLinks(env, google, [
    eq("offlineAlertSent", { booleanValue: false }),
    cmp("lastHeartbeat", "LESS_THAN", { timestampValue: iso(now - MISSED_CHECK_IN_HOURS * 3600e3) }),
  ]);
  for (const { code, link } of silent) {
    const device = link.childDeviceName || "Child phone";
    await pushToGuardian(env, google, code, link, "MISSED_CHECK_IN",
      "⚠️ Phone stopped checking in",
      `${device} hasn't checked in for over ${MISSED_CHECK_IN_HOURS} hours. ` +
      "It may be switched off or offline, or Clawse may have been removed.");
    await patchLink(env, google, code, {
      offlineAlertSent: { booleanValue: true },
      offlineSince: { timestampValue: iso(now) },
    });
  }

  // 2. Phones that were reported silent and have checked in again.
  const returned = await queryLinks(env, google, [
    eq("offlineAlertSent", { booleanValue: true }),
    cmp("lastHeartbeat", "GREATER_THAN", { timestampValue: iso(now - BACK_ONLINE_WINDOW_MINUTES * 60e3) }),
  ]);
  for (const { code, link } of returned) {
    const device = link.childDeviceName || "Child phone";
    await pushToGuardian(env, google, code, link, "BACK_ONLINE",
      "💚 Back online", `${device} is checking in again.`);
    await patchLink(env, google, code, { offlineAlertSent: { booleanValue: false } });
  }

  // 3. Pairing codes that were never used.
  const stale = await queryLinks(env, google, [
    { unaryFilter: { field: { fieldPath: "childUid" }, op: "IS_NULL" } },
    cmp("pairingExpiresAt", "LESS_THAN", { timestampValue: iso(now - UNCLAIMED_LINK_TTL_HOURS * 3600e3) }),
  ]);
  for (const { code } of stale) {
    await firestoreFetch(env, google, `links/${code}`, { method: "DELETE" });
  }

  console.log(JSON.stringify({ sweep: "done", missed: silent.length, backOnline: returned.length, staleRemoved: stale.length }));
}

// ==========================================
// FCM
// ==========================================

/** Sends a push to the link's guardian phone. Clears the token if FCM says it is dead. */
async function pushToGuardian(env, google, code, link, type, title, body) {
  if (!link.guardianToken) return false;
  const res = await fetch(
    `https://fcm.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/messages:send`,
    {
      method: "POST",
      headers: { Authorization: `Bearer ${google}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        message: {
          token: link.guardianToken,
          notification: { title, body },
          data: { type, code, title, body },
          android: {
            priority: "HIGH",
            notification: {
              channel_id: "guardian_alerts",
              notification_priority: URGENT_TYPES.has(type) ? "PRIORITY_MAX" : "PRIORITY_HIGH",
              default_sound: true,
            },
          },
        },
      }),
    },
  );
  if (res.ok) return true;

  const err = await res.json().catch(() => ({}));
  const codes = (err?.error?.details ?? []).map((d) => d.errorCode);
  if (codes.includes("UNREGISTERED") || codes.includes("INVALID_ARGUMENT")) {
    await patchLink(env, google, code, { guardianToken: { stringValue: "" } });
  }
  console.error(JSON.stringify({ fcm: "failed", status: res.status, codes }));
  return false;
}

// ==========================================
// FIRESTORE (REST)
// ==========================================

function firestoreFetch(env, google, path, init = {}) {
  const base = `https://firestore.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/databases/(default)/documents`;
  return fetch(path.startsWith(":") ? `${base}${path}` : `${base}/${path}`, {
    ...init,
    headers: { Authorization: `Bearer ${google}`, "Content-Type": "application/json", ...(init.headers ?? {}) },
  });
}

async function getLink(env, google, code) {
  const res = await firestoreFetch(env, google, `links/${code}`);
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`Firestore get failed: ${res.status}`);
  return decodeFields((await res.json()).fields);
}

async function patchLink(env, google, code, fields) {
  const mask = Object.keys(fields).map((f) => `updateMask.fieldPaths=${f}`).join("&");
  const res = await firestoreFetch(env, google, `links/${code}?${mask}&currentDocument.exists=true`, {
    method: "PATCH",
    body: JSON.stringify({ fields }),
  });
  if (!res.ok) console.error(JSON.stringify({ patch: "failed", code, status: res.status }));
}

async function queryLinks(env, google, filters) {
  const res = await firestoreFetch(env, google, ":runQuery", {
    method: "POST",
    body: JSON.stringify({
      structuredQuery: {
        from: [{ collectionId: "links" }],
        where: { compositeFilter: { op: "AND", filters } },
        limit: SWEEP_BATCH,
      },
    }),
  });
  if (!res.ok) {
    console.error(JSON.stringify({ query: "failed", status: res.status, body: await res.text() }));
    return [];
  }
  return (await res.json())
    .filter((row) => row.document)
    .map((row) => ({ code: row.document.name.split("/").pop(), link: decodeFields(row.document.fields) }));
}

const eq = (fieldPath, value) => ({ fieldFilter: { field: { fieldPath }, op: "EQUAL", value } });
const cmp = (fieldPath, op, value) => ({ fieldFilter: { field: { fieldPath }, op, value } });

function decodeFields(fields = {}) {
  const out = {};
  for (const [key, v] of Object.entries(fields)) {
    if ("stringValue" in v) out[key] = v.stringValue;
    else if ("booleanValue" in v) out[key] = v.booleanValue;
    else if ("timestampValue" in v) out[key] = v.timestampValue;
    else if ("integerValue" in v) out[key] = Number(v.integerValue);
    else if ("nullValue" in v) out[key] = null;
  }
  return out;
}

// ==========================================
// GOOGLE AUTH (service account -> access token)
// ==========================================

let cachedAccess = null;

async function googleAuth(env) {
  if (cachedAccess && cachedAccess.expires > Date.now() + 60e3) return cachedAccess.token;

  const sa = JSON.parse(env.FIREBASE_SERVICE_ACCOUNT);
  const now = Math.floor(Date.now() / 1000);
  const assertion = await signJwt(
    { alg: "RS256", typ: "JWT", kid: sa.private_key_id },
    {
      iss: sa.client_email,
      scope: "https://www.googleapis.com/auth/datastore https://www.googleapis.com/auth/firebase.messaging",
      aud: "https://oauth2.googleapis.com/token",
      iat: now,
      exp: now + 3600,
    },
    sa.private_key,
  );
  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion }),
  });
  if (!res.ok) throw new Error(`Google token exchange failed: ${res.status}`);
  const data = await res.json();
  cachedAccess = { token: data.access_token, expires: Date.now() + data.expires_in * 1000 };
  return cachedAccess.token;
}

async function signJwt(header, claims, privateKeyPem) {
  const der = base64ToBytes(privateKeyPem.replace(/-----[^-]+-----/g, "").replace(/\s+/g, ""));
  const key = await crypto.subtle.importKey(
    "pkcs8", der, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"]);
  const unsigned = `${b64urlJson(header)}.${b64urlJson(claims)}`;
  const sig = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(unsigned));
  return `${unsigned}.${bytesToB64url(new Uint8Array(sig))}`;
}

// ==========================================
// FIREBASE ID TOKEN VERIFICATION
// ==========================================

const JWKS_URL = "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";
let cachedJwks = null;

async function verifyIdToken(token, projectId) {
  const [h, p, s] = token.split(".");
  if (!h || !p || !s) throw new Error("malformed token");
  const header = JSON.parse(b64urlDecodeText(h));
  const claims = JSON.parse(b64urlDecodeText(p));
  if (header.alg !== "RS256") throw new Error("bad alg");

  const jwk = (await getJwks()).find((k) => k.kid === header.kid);
  if (!jwk) throw new Error("unknown key");
  const key = await crypto.subtle.importKey(
    "jwk", jwk, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["verify"]);
  const valid = await crypto.subtle.verify(
    "RSASSA-PKCS1-v1_5", key, b64urlToBytes(s), new TextEncoder().encode(`${h}.${p}`));
  if (!valid) throw new Error("bad signature");

  const now = Math.floor(Date.now() / 1000);
  if (claims.aud !== projectId) throw new Error("bad audience");
  if (claims.iss !== `https://securetoken.google.com/${projectId}`) throw new Error("bad issuer");
  if (!(claims.exp > now) || claims.iat > now + 300) throw new Error("expired");
  if (!claims.sub) throw new Error("no subject");
  return claims.sub;
}

async function getJwks() {
  if (cachedJwks && cachedJwks.expires > Date.now()) return cachedJwks.keys;
  const res = await fetch(JWKS_URL);
  if (!res.ok) throw new Error("jwks fetch failed");
  const maxAge = Number(/max-age=(\d+)/.exec(res.headers.get("cache-control") ?? "")?.[1] ?? 3600);
  cachedJwks = { keys: (await res.json()).keys, expires: Date.now() + maxAge * 1000 };
  return cachedJwks.keys;
}

// ==========================================
// HELPERS
// ==========================================

function bearerToken(request) {
  const header = request.headers.get("Authorization") ?? "";
  if (!header.startsWith("Bearer ")) throw new Error("missing token");
  return header.slice(7);
}

function json(body, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

const iso = (ms) => new Date(ms).toISOString();

function base64ToBytes(b64) {
  return Uint8Array.from(atob(b64), (c) => c.charCodeAt(0));
}
function b64urlToBytes(s) {
  return base64ToBytes(s.replace(/-/g, "+").replace(/_/g, "/").padEnd(Math.ceil(s.length / 4) * 4, "="));
}
function b64urlDecodeText(s) {
  return new TextDecoder().decode(b64urlToBytes(s));
}
function bytesToB64url(bytes) {
  let bin = "";
  for (const b of bytes) bin += String.fromCharCode(b);
  return btoa(bin).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}
function b64urlJson(obj) {
  return bytesToB64url(new TextEncoder().encode(JSON.stringify(obj)));
}
