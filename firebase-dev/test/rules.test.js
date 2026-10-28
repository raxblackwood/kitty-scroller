// Firestore security rules tests for guardian pairing.
// Run from firebase-dev/: npm run test:rules   (starts the Firestore emulator; needs JDK 21+)
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const path = require("node:path");
const {
  initializeTestEnvironment, assertSucceeds, assertFails,
} = require("@firebase/rules-unit-testing");
const {
  doc, getDoc, getDocs, setDoc, updateDoc, deleteDoc, addDoc, collection,
  serverTimestamp, Timestamp,
} = require("firebase/firestore");

const CODE = "ABCD2345";
let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: "clawse-rules-test",
    firestore: { rules: fs.readFileSync(path.join(__dirname, "../../firestore.rules"), "utf8") },
  });
});
after(async () => env.cleanup());
beforeEach(async () => env.clearFirestore());

const db = (uid) => env.authenticatedContext(uid).firestore();
const minutesFromNow = (m) => Timestamp.fromMillis(Date.now() + m * 60000);

function newLink(guardianUid, overrides = {}) {
  return {
    guardianUid, guardianToken: "tok", guardianName: "Pixel 9", childUid: null,
    childDeviceName: "", createdAt: serverTimestamp(), pairingExpiresAt: minutesFromNow(15),
    offlineAlertSent: false, ...overrides,
  };
}

async function seedLink(data) {
  await env.withSecurityRulesDisabled((ctx) => setDoc(doc(ctx.firestore(), "links", CODE), data));
}

const claim = (uid) => updateDoc(doc(db(uid), "links", CODE), {
  childUid: uid, childDeviceName: "Sam", pairedAt: serverTimestamp(), lastHeartbeat: serverTimestamp(),
});

test("guardian can create a link for itself only", async () => {
  await assertSucceeds(setDoc(doc(db("g"), "links", CODE), newLink("g")));
  await assertFails(setDoc(doc(db("g"), "links", "ZZZZ2345"), newLink("someone-else")));
  await assertFails(setDoc(doc(db("g"), "links", "ZZZZ2346"), newLink("g", { childUid: "g" })));
  await assertFails(setDoc(doc(db("g"), "links", "ZZZZ2347"), newLink("g", { pairingExpiresAt: minutesFromNow(600) })));
  await assertFails(setDoc(doc(db("g"), "links", "bad"), newLink("g")));
  await assertFails(setDoc(doc(env.unauthenticatedContext().firestore(), "links", CODE), newLink("g")));
});

test("child can claim an unclaimed, unexpired link once", async () => {
  await seedLink(newLink("g", { createdAt: Timestamp.now() }));
  await assertSucceeds(getDoc(doc(db("c"), "links", CODE)));
  await assertSucceeds(claim("c"));
  // Another phone can no longer read or claim it.
  await assertFails(getDoc(doc(db("x"), "links", CODE)));
  await assertFails(claim("x"));
});

test("expired codes cannot be claimed", async () => {
  await seedLink(newLink("g", { createdAt: Timestamp.now(), pairingExpiresAt: minutesFromNow(-1) }));
  await assertFails(claim("c"));
});

test("linked child can check in but not touch server fields", async () => {
  await seedLink(newLink("g", { createdAt: Timestamp.now(), childUid: "c", lastHeartbeat: Timestamp.now() }));
  const ref = doc(db("c"), "links", CODE);
  await assertSucceeds(updateDoc(ref, { lastHeartbeat: serverTimestamp(), childDeviceName: "Sam" }));
  await assertFails(updateDoc(ref, { lastHeartbeat: Timestamp.fromMillis(Date.now() + 864e5) }));
  await assertFails(updateDoc(ref, { offlineAlertSent: true }));
  await assertFails(updateDoc(ref, { guardianToken: "hijack" }));
});

test("guardian can refresh token but not impersonate child", async () => {
  await seedLink(newLink("g", { createdAt: Timestamp.now(), childUid: "c" }));
  const ref = doc(db("g"), "links", CODE);
  await assertSucceeds(updateDoc(ref, { guardianToken: "new" }));
  await assertFails(updateDoc(ref, { childUid: "g" }));
  await assertFails(updateDoc(doc(db("x"), "links", CODE), { guardianToken: "x" }));
});

test("clients cannot list links or write anything else", async () => {
  await seedLink(newLink("g", { createdAt: Timestamp.now(), childUid: "c" }));
  await assertFails(getDocs(collection(db("g"), "links")));
  await assertFails(addDoc(collection(db("c"), "links", CODE, "alerts"), { type: "X" }));
  await assertFails(setDoc(doc(db("c"), "other", "x"), { a: 1 }));
});

test("either linked phone can unlink, others cannot", async () => {
  await seedLink(newLink("g", { createdAt: Timestamp.now(), childUid: "c" }));
  await assertFails(deleteDoc(doc(db("x"), "links", CODE)));
  await assertSucceeds(deleteDoc(doc(db("c"), "links", CODE)));
});
