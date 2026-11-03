---

summary: "Anti-doomscrolling Android app dropping an animated kitten curtain over Instagram Reels and YouTube Shorts."

workspace: "E:\CODES\anti scroller"

stack: "Kotlin, Android AccessibilityService, WindowManager Overlay, Firebase (Auth, Firestore, FCM, Functions)"

type: project-hub

---



# ClawBack



> [!ABSTRACT] Quick Summary (Token-Saving Preview)

> **Scope**: Anti-doomscrolling Android app dropping an animated kitten curtain over Instagram Reels and YouTube Shorts.

> **Stack**: Kotlin, Android AccessibilityService, WindowManager Overlay

> **Contains**: Milestones, current focus, technical scope, invariants, and backlog.

> **Skip if**: Your task is unrelated to **ClawBack** .



- **Status**: #project #active

- **Vault Location**: [[02 Projects/ClawBack/|ClawBack]]

- **Sub-Documents**: [[ClawBack - Architecture|Architecture]] | [[ClawBack - Decisions|Decisions]] | [[ClawBack - Identity|Project Identity]]



---

- **Project ID**: $uuid

- **Workspace**: $ws

- **Status**: Active



## 1. Executive Summary



Anti-doomscrolling Android app dropping an animated kitten curtain over Instagram Reels and YouTube Shorts.



## 2. Technical Scope & Stack



- **Stack**: Kotlin, Android AccessibilityService, WindowManager Overlay, Custom Canvas View

- **Directory**: $ws



## 3. Milestones & Current Focus



- Project onboarded to persistent AI memory system.

- Initial identity marker generated and mapped.

- v1.0.20: Fixed Instagram Story/Gallery scroll leaks and eliminated app-resume curtain jitter.

  - Implemented strict Instagram target whitelist (Home Feed and Reels exclusively).

  - Filtered out all Story creation, Gallery media pickers, Camera, Drafts, and Filters.

  - Strengthened Direct Message (DM) and Explore/Profile rejection up to 10 ancestor levels.

  - Added 800ms resume grace period to discard layout restoration scrolls upon switching back to the app.

  - Fixed WindowManager overlay attachment to render statically at current drop level without re-animating downward on app resume.

- v1.0.21: Soothing UI redesign (Apple Design Principles), first-time walkthrough slider, permission floater notification, and YouTube-to-Home overlay fix.

  - Resolved YouTube-to-Home overlay persistence bug by adding dedicated launcher package detection and overriding Picture-in-Picture window ambiguity.

  - Designed and implemented interactive 3-slide onboarding walkthrough (`ViewPager2` carousel with dynamic pill dots) for first-time users.

  - Added prominent floating notification card on the home screen when Accessibility or Overlay permissions are missing, allowing 1-tap activation.

  - Refined dashboard typography and surfaces according to Apple Design Principles, removing shouting all-caps text, aggressive disclaimers, and clutter.

- v1.0.22: Replaced app launcher icon background with deep sapphire indigo blue.

  - Updated `@color/ic_launcher_background` to `#1E3A8A` in `colors.xml`.

  - Re-rendered all legacy adaptive and round launcher mipmaps and drawable assets with the new soothing blue background.

  - Clean uninstalled and reinstalled v1.0.22 on physical device via ADB.

- v1.0.23: Finalized black and gray icon design, companion preview mode toggle, and seamless wooden rod architecture.

  - Updated app icon to solid black background (`#000000`) with neutral gray kitty interior (`#808080`) and crisp white boundary/facial features (`#FFFFFF`).

  - Unified Kitty Companion avatar preview to display the descending cat by default for all skins (Ginger, Tabby, Saturn, Rolex).

  - Implemented interactive tap toggle to switch between curtain coming down and closed curtain modes with per-breed sleeping loaf artwork.

  - Removed baked-in wooden horizontal bar completely from all companion sprite assets across all 4 breeds (Rolex, Saturn, Ginger, Tabby).

  - Isolated paws and hands to hang directly over the continuous screen-wide wooden pole drawn in the background by `KittyCurtainView.drawExtendedStick`.

  - Updated stick ratios and early draw clipping in `KittyCurtainView.kt` to eliminate all seams, gaps, notches, and animation jitter.

  - Removed redundant developer "Preview Stretch" slider from the Kitty Companion card, streamlining the card UI.

  - Integrated the new Knight Cat companion breed "Salmon" (⚔️) with a 16-frame animation loop, matching loaf icon, and custom midnight navy / ice crystal palette.

  - Generated release APK and AAB bundle (versionCode 24) and installed to physical device via ADB.

- v1.0.24: Integrated new 8-frame Salmon animation, added new Katrina companion breed, and unified 8-frame animation engine.

  - Extracted new 8-frame Salmon animation loop from Row 0 of `Cats/salmon and katrina.png` and anchored paw at X=65, Y=22.

  - Removed previous 16-frame Salmon assets and pruned old frame drawables.

  - Implemented new cat companion breed Katrina (🌸) from Row 1 of `Cats/salmon and katrina.png` with 8-frame pull-up animation loop.

  - Centered Katrina's dual paws symmetrically at X=140, Y=22 resting seamlessly on top of the continuous wooden pole.

  - Generated custom pixel-art loaf icon `kitty_katrina_loaf.png` featuring cream fur, soft sakura blossom pink inner ears, coral ribbon, golden bells, and sapphire eyes.

  - Added Katrina to `ScrollDataManager.kt`, `MainActivity.kt`, `activity_main.xml`, `KittyCurtainView.kt`, `dialog_paywall.xml`, and `BillingManager.kt`.

  - Unified cartoon sprite animator across all animated breeds (Saturn, Rolex, Salmon, Katrina) to an 8-frame loop at 120ms per frame (960ms duration).

  - Bumped release version to 1.0.24 (versionCode 25).

  - Built production release APK and AAB bundle in `dist/`.

- v1.0.25: Replaced fast sprite-switching with calm ambient body sway and tail-wagging animation for Rolex, Salmon, and Katrina.

  - Removed rapid 8-frame sprite looping from Rolex, Salmon, and Katrina to eliminate fast, jarring animation flicker.

  - Implemented organic 2D mesh deformation (`drawBitmapMesh`) keeping paws 100% stationary and anchored to the wooden pole while the cat body gently sways with the breeze.

  - Integrated harmonic tail-wagging displacement at the bottom-right curled tail region driven by the 850ms tail animator.

  - Preserved Saturn's athletic pull-up sprite animation at a smooth, deliberate 180ms per frame pace (1440ms loop).

  - Reduced release APK download footprint from 7.9 MB to 4.98 MB by shedding redundant multi-frame flipbook drawables.

  - Bumped release version to 1.0.25 (versionCode 26).

  - Compiled release APK/AAB and installed directly to connected physical device via ADB.

- v1.0.26: Fixed bottom tail truncation on Salmon and Katrina by expanding canvas to 280x510.

  - Sliced and re-exported Salmon and Katrina to a full 280x510 canvas (leaving 23px and 18px of transparent bottom padding).

  - Preserved the full natural tapered tails for both breeds without any clipped or flat-cut rows.

  - Updated stick ratios (30f/510f and 47f/510f) and aspect ratio (0.549) in `KittyCurtainView.kt` so the continuous wooden rod aligns with millimeter accuracy.

  - Bumped release version to 1.0.26 (versionCode 27).

  - Compiled release APK/AAB and installed directly to connected physical device via ADB.

- v1.0.27: Fixed Settings App Info access, removed test overlay button, and prioritized Guardian in paywall.

  - Narrowed anti-tamper interception in Android Settings to only fire on actual confirmation dialogs rather than blocking regular viewing of App Info.

  - Removed premature `GLOBAL_ACTION_HOME` from anti-tamper interception that previously killed the cooling-off activity after 1 millisecond.

  - Removed "Test Floating Overlay (5s)" developer button from the main screen and cleaned up its listeners.

  - Prioritized Guardian Parental Control card on top of the paywall bottom sheet whenever triggered from the Guardian section.

  - Documented complete Guardian testing procedure using free, zero-login ntfy.sh topics and simulated tamper events.

  - Bumped release version to 1.0.27 (versionCode 28).

  - Built production release APK and AAB bundle in `dist/`.

- v1.0.28: Silenced local alert notifications, added dynamic Security onboarding slide, and bumped release.

  - Removed intrusive local status-bar notifications completely from `GuardianAlertManager.kt`.

  - Guardian alerts are now dispatched silently in the background strictly to the remote endpoint.

  - Added dedicated "Security & Anti-Tamper" slide to the first-time onboarding walkthrough carousel.

  - Replaced hardcoded XML dot indicators with dynamic programmatic dots adapting to any number of onboarding slides.

  - Bumped release version to 1.0.28 (versionCode 29).

  - Built production release APK and installed directly to connected physical device via ADB.

- v1.0.29: Resolved ViewPager2 initial half-width slide rendering bug in onboarding modal.

  - Explicitly initialized dialog view minimum width to full display bounds (`displayWidth * 0.92f`).

  - Added `setOnShowListener` configuring window width explicitly and triggering layout refresh on ViewPager2 adapter.

  - Enforced `ViewGroup.LayoutParams.MATCH_PARENT` on item slide view holders in `OnboardingAdapter`.

  - Applied the same structured layout sizing to the initial setup limit dialog.

  - Bumped release version to 1.0.29 (versionCode 30).

  - Clean uninstalled and reinstalled fresh on connected physical device via ADB.

- v1.0.30: Added bottom Ad banner placeholder and updated onboarding privacy copy.

  - Added structured bottom Ad container (`layoutAdContainer`) below Device Owner settings in `activity_main.xml`.

  - Updated onboarding Slide 4 to "Private & Secure" without the "zero ads" restriction.

  - Bumped release version to 1.0.30 (versionCode 31).

  - Installed updated release APK directly to connected physical device via ADB.

- v1.0.31 Firebase Guardian Push Alerts (2026-10-27 to 2026-10-28): Two-phone parental alerts over Firebase, replacing ntfy as the default channel.

  - Firebase project `clawse-app` on the free Spark plan, owned by the Google account raxblackwoodbackup@gmail.com (separate from the Play Console account, which is fine).

  - Firebase BoM 34.10.0, because newer BoMs ship Kotlin 2.3+ metadata that the project's Kotlin 2.1 compiler can't read.

  - `app/google-services.json` is gitignored because GitHub Pages publishes the repo root.

  - Both phones sign in with Firebase Anonymous Auth (no accounts). The guardian phone creates an 8-character pairing code (15-minute window); the child enters it under Guardian Alerts > Configure Alert.

  - Push relay is a free Cloudflare Worker (`worker/`), not Cloud Functions: the Blaze upgrade needed a ₹1,000 UPI prepayment in India, so Firebase stays on the free Spark plan (see ADR-006).

  - The child app POSTs alerts to `${GUARDIAN_RELAY_URL}/alert` with its Firebase ID token. The Worker checks `link.childUid == uid` and sends FCM HTTP v1 to the guardian. A "Phone linked" push is sent right after pairing. ntfy/webhook kept as optional advanced backup.

  - Missed check-in: child heartbeat (15-min WorkManager) writes `lastHeartbeat` to Firestore. A Worker cron (every 30 min) warns the guardian after 6h of silence, sends "back online", and deletes unused codes after 24h.

  - New: `GuardianLinkManager.kt`, `GuardianMessagingService.kt`, Guardian Phone card in `activity_main.xml`, `worker/`, `firestore.rules`, `firestore.indexes.json`, `firebase-dev/` (Firebase CLI plus rules tests).

  - Privacy policy (`privacy.html`, `Docs/PRIVACY_POLICY.md`) and Play listing copy updated to disclose the optional Firebase and Cloudflare relay; `Docs/PRIVACY_POLICY.html` now redirects to `privacy.html`.

  - Deployed 2026-10-28:

    - Firestore rules and indexes are live.

    - Worker `clawback-guardian` is live at https://clawback-guardian.clawback-guardian-worker.workers.dev, on Cloudflare account raxblackwoodbackup@gmail.com, with cron `*/30 * * * *`.

    - The `FIREBASE_SERVICE_ACCOUNT` secret is set. The key file lives outside the repo, in `C:\Users\raman\Documents\clawse-keys\`.

    - `GUARDIAN_RELAY_URL` is set in `app/build.gradle.kts`. Debug and release APKs rebuilt.

  - Verified 2026-10-28 on a physical phone (A142, Android 16), using one phone as both guardian and child:

    - Pairing code created, code claimed, and the "Phone linked" push arrived.

    - "Send Test Ping" push arrived.

    - The guardian card shows the last check-in time.

    - The 15:30 Worker cron sweep ran cleanly (indexes and service account OK).

  - UI polish from the test:

    - Pairing code is shown large, monospaced and tap-to-copy in a rounded box.

    - The guardian card refreshes after a claim.

    - The button reads "Link Another" once a phone is linked, so it no longer wraps.

    - The claim toast reads "Linked to the guardian's phone".

  - Pending:

    - Two-device test of missed check-in (needs child mode, which is behind the parental subscription).

    - Rules tests (need JDK 21).

    - Play data safety form update, then upload `dist/Clawse-v1.0.31-release.aab`.

  - Merged into main (fast-forward to `53a7844`) and pushed 2026-10-28. The updated privacy policy is live at https://raxblackwood.github.io/kitty-scroller/privacy.html.

  - Bumped release version to 1.0.31 (versionCode 32).

  - Built the release APK and AAB into `dist/` and installed the APK on the physical device via ADB.

- Protection section redesign (2026-10-28, commit `cc63701` on `feat/guardian-push-alerts`, part of v1.0.31; `dist/` APK and AAB rebuilt): Grouped the four security cards under one "Protection" section (see ADR-007).

  - Order: PIN Security, Guardian Alerts, Device Owner Mode, Guardian Phone.

  - The section has a 20sp "Protection" header and a one-line description. Cards inside it are 12dp apart.

  - Emoji headers are replaced by 36dp teal-tinted icon tiles with 24dp outline vector icons (`ic_sec_lock`, `ic_sec_bell`, `ic_sec_shield`, `ic_sec_phone`, 1.75 stroke) and 16sp `text_primary` titles.

  - Badges are rounded chips in title case (`bg_security_badge`, text `secondary_light` #4DB6AC for WCAG AA).

  - Paired buttons have a uniform 12dp gap, kept on the second button so a lone button fills the width.

  - The Guardian Phone status uses a green or red dot span instead of emoji.

  - Verified on the physical device.

- Guardian card merge (2026-10-28, commit `c0aeea2` on `feat/guardian-push-alerts`, part of v1.0.31): Guardian Alerts (child side) and Guardian Phone (parent side) were one feature split across two cards, which confused first-time users. They are now merged into a single Guardian Alerts card (see ADR-008).

  - The section is now three cards: PIN Security, Guardian Alerts, Device Owner Mode.

  - The card has two labelled halves split by a divider: "Protect this phone" (Premium) and "Get alerts on this phone" (Free).

  - Button labels:

    - Child side: "Enter Code" / "Change Code" and "Send Test".

    - Parent side: "Show Pairing Code" / "Link Another" and "Unlink".

  - Both sides use the colored status-dot line.

  - The child dialog is now "Enter Guardian Code". Fields: pairing code, a name for this phone, then the optional advanced webhook. Labels align with the dialog text.

  - The pairing dialog is now titled "Pairing Code" and refers to "Enter Code" on the other phone.

  - `ic_sec_phone` is no longer used by the layout and is kept for possible reuse.

- v1.0.32: Fixed startup permission prompt flow and added Daily Progress History modal.

  - Resolved missing permission setup dialog on initial launch by ensuring `checkAndPromptPermissions()` fires after onboarding and boundary lock-in.

  - Sized `dialog_permission_setup` with explicit window bounds and minimum width matching other glass dialogs.

  - Implemented persistent daily focus history in `ScrollDataManager` (`getDailyHistory`, `recordHistoryEntry`, `KEY_DAILY_HISTORY`).

  - Automatically records previous day counts upon midnight reset and upserts live daily scroll stats.

  - Created `ProgressHistoryBottomSheetDialog` displaying past 14 days breakdown, daily averages, budget adherence metrics, and per-day progress bars.

  - Added "📊 Progress" button in the "Today's Focus" header card in `activity_main.xml`.

  - Bumped release version to 1.0.32 (versionCode 33).

  - Built release APK and AAB bundle in `dist/`.

- v1.0.33: Native Android Digital Wellbeing style Vertical Bar Chart for Daily Progress.

  - Built custom hardware-accelerated `DailyBarChartView` featuring vertical animated columns, horizontal dashed limit baseline, and interactive day tap inspection.

  - Refactored `ProgressHistoryBottomSheetDialog` to replace list rows with the 7-day weekly vertical column chart.

  - Added dynamic Selected Day detail card showing exact reel counts, remaining budget, and status chips.

  - Bumped release version to 1.0.33 (versionCode 34).

  - Stream-installed release APK to connected phone via ADB and compiled release AAB bundle in `dist/`.

- v1.0.34: Themed Progress Button Highlight & Production Release Bundle.

  - Designed custom rounded vector icon `ic_chart_progress.xml` featuring ascending capsule bars.

  - Upgraded "Today's Focus" dashboard button to a prominent highlighted chip with 15% orange tint background and 1.2dp solid border.

  - Updated progress dialog header row with security icon tile matching the app-wide design system.

  - Bumped release version to 1.0.34 (versionCode 35).

  - Built production release APK and App Bundle (`dist/Clawse-v1.0.34-release.aab`).

  - Stream-installed release APK directly to connected physical device via ADB.

- v1.0.35: Dual Theme Architecture - Default "Sticker Pop" Light Theme & "Midnight OLED" Dark Theme.

  - Implemented the user-selected neobrutalist/pastel "Sticker Pop" design direction from Claude prototype as the default Light Theme.

  - Preserved the existing OLED glassmorphism aesthetic as the Dark Theme ("Midnight OLED").

  - Extracted color palette: Pastel Lilac background (`#EFE9FF`), plum dark ink borders (`#1A1325`), sky blue hero card (`#8EC5FF`), rod/accent yellow (`#FFD43B`), pink card (`#FF9AC9`), mint card (`#5FD9A6`), and paper white cards (`#FFFCF5`).

  - Integrated custom Bricolage Grotesque typography (`app/src/main/res/font/bricolage_grotesque.ttf`).

  - Generated white sticker-outlined cat avatar assets using morphological PIL dilation in `drawable-nodpi/` for Ginger, Tabby, Saturn, Rolex, Salmon, and Katrina.

  - Authored 20+ neobrutalist layer-list XML drawables with 3dp ink borders and hard bottom offset shadows (`bg_sticker_hero.xml`, `bg_sticker_rod.xml`, `bg_sticker_speech.xml`, `bg_sticker_progress_track.xml`, `bg_sticker_progress_fill.xml`, etc.).

  - Created dedicated layout `activity_main_light.xml` with hero card, hanging cat animation, speech bubble, custom progress bar, chart button, 3x2 cat selection grid, and 3 protection cards.

  - Added theme selection option to the main three-dot sheet menu (`sheet_main_menu.xml`) with instant activity recreation.

  - Synchronized theme preferences via `ScrollDataManager.getUiTheme()` and `setUiTheme()`.

  - Stream-installed debug build directly to connected physical device via ADB after clean uninstall.

- v1.0.36: Complete Dialogs & Progress Sheet Theming + Normalized Cat Sprite Alignment.

  - Normalized all 6 cat stickers (Ginger, Tabby, Saturn, Rolex, Salmon, Katrina) onto an identical 420x800 coordinate canvas with paws aligned tightly at top.

  - Adjusted hero hanging cat (`ivHeroCat`) in `activity_main_light.xml` to 120x228dp with `-6dp` top margin so paws physically hook over the wooden rod across all breeds without shrinking.

  - Created bespoke Sticker Pop layouts for all startup and modal flows:

    - `dialog_onboarding_light.xml` and `item_onboarding_slide_light.xml` (paper white card, 3dp ink border, 6dp shadow, Bricolage Grotesque typography, sticker dot pills, yellow CTA).

    - `dialog_initial_setup_light.xml` (boundary lock-in dialog with clean neobrutalist inputs and switches).

    - `dialog_permission_setup_light.xml` (step-by-step accessibility and overlay permission setup in Sticker Pop design).

    - `dialog_change_limit_light.xml` (daily limit editor modal in matching design).

    - `dialog_progress_history_light.xml` (past 7 days vertical bar chart sheet with paper summary bento, neobrutalist cards, and dark plum CTA).

  - Upgraded `DailyBarChartView.kt` with dynamic `isLightMode` awareness: lilac slot capsules, mint/yellow/pink bars, dark ink limit dashed lines, and Bricolage Grotesque values.

  - Upgraded `ProgressHistoryBottomSheetDialog.kt` to dynamically select the Light/Dark sheet layout and clear modal background clipping.

  - Stream-installed debug build directly to connected phone via ADB.

- v1.0.37: Scope Expansion to Snapchat Spotlight & Facebook Reels + Instagram Refresh Loophole Fix.

  - Expanded Clawse anti-doomscrolling protection to Snapchat Spotlight (`com.snapchat.android`) and Facebook Reels (`com.facebook.katana`, `com.facebook.lite`).

  - Implemented strict negative filtering for Snapchat to reject Camera viewfinder, chat messaging, friends feed, snap map, and memories up to 10 ancestor levels.

  - Implemented strict negative filtering for Facebook to reject Messenger, Marketplace, groups discussion comments, and user profiles.

  - Added positive view matching for Snapchat (`spotlight`, `opera_view_pager`, `snap_player`, `discover_feed`) and Facebook (`fb_shorts`, `short_form_video`, `reel_viewer`, `reels_tab`, `reel_feed`).

  - Resolved Snapchat detection bug: extracted bare resource entry name (`substringAfter(":id/")`) to prevent "chat" keyword from matching package prefix `com.snapchat.android`.

  - Added `isSnapchatSpotlightActive()` and `isFacebookReelsActive()` fallbacks querying active bottom tab state in `rootInActiveWindow`.

  - Resolved Instagram refresh loophole: detected pull-to-refresh downward gesture (`deltaY < -20`) at index 0 in Reels, added `TYPE_WINDOW_CONTENT_CHANGED` listener with child node text inspection for refresh announcements ("A new reel is loaded", "new release loaded"), and added Reels bottom tab click detection.

  - Upgraded `isViewPagerScroll` to support `RecyclerView` and increased traversal depth for discrete snap pagination.

  - Added Snapchat and Facebook monitoring toggles with 24-hour anti-impulse cooling-off dialogs to both Sticker Pop Light and Midnight OLED Dark themes.

  - Added Snapchat and Facebook switches to initial setup boundary lock-in dialogs.

  - Built debug APK ready for device installation.
- v1.0.38: ViewPager Settle Bounce Filtering & Instagram Reels Refresh Enforcement.
  - Eliminated upward scroll curtain drops caused by ViewPager/RecyclerView snap settle animations.
  - Swiping up (`deltaY < -10`) previously generated an opposite rebound event (`deltaY > 0`) 100-250ms later that falsely advanced the curtain.
  - Implemented `lastUpwardScrollTs` timestamp and a 1200ms upward scroll cooldown period (`UPWARD_SCROLL_COOLDOWN_MS`).
  - Discarded any positive scroll delta arriving within the cooldown period as an uncalculated settle rebound.
  - Added index regression validation (`fromIndex <= lastIndex`) to prevent upward navigation from advancing the reel counter.
- v1.0.39: Elimination of Accessibility Main-Thread Scroll Lag & Tab Navigation False Positives.
  - Completely eradicated main-thread UI lag during scrolls by removing all synchronous `rootInActiveWindow` IPC queries from event dispatch loops.
  - Replaced heavy tree-crawling in `checkInstagramReelRefreshEvent` with lightweight `checkInstagramRefreshAnnouncement` scoped strictly to `TYPE_ANNOUNCEMENT` and `TYPE_NOTIFICATION_STATE_CHANGED`.
  - Removed false curtain drop on Home-to-Reels navigation by eliminating `triggerInstagramReelRefresh` from `handleViewClicked`.
  - Clicking the Reels navigation tab now strictly updates the index marker to 0 without dropping the curtain.
  - Refined Reels pull-to-refresh: strictly requires `(lastInstagramIndex == 0 || fromIndex == 0) && isReels` or explicit refresh containers (`refresh`, `swipe`), preventing normal backward scroll navigation from triggering refreshes.
  - Allowed `refresh` and `swipe` container view IDs in `isInstagramTargetScroll`.
  - Stream-installed debug build to device via ADB and reset daily scroll count and accumulated pixels to zero.
- v1.0.35 Release Bundle (versionCode 36): Production Release App Bundle for Google Play Console.
  - Bumped release `versionCode` to 36 and `versionName` to "1.0.35" in `app/build.gradle.kts`.
  - Compiled signed release Android App Bundle (`dist/Clawse-v1.0.35-release.aab`) and release APK (`dist/Clawse-v1.0.35-release.apk`).
  - Bundled the Dual Theme architecture (Sticker Pop Light and Midnight OLED Dark), Snapchat Spotlight and Facebook Reels protection, ViewPager settle bounce suppression, and zero-lag accessibility architecture into production.
- v1.0.35 Update: Strict Snapchat Curtain Scoping to Spotlight Reels Exclusively.
  - Restricted the curtain overlay in Snapchat to show exclusively when viewing Spotlight (Snapchat Reels).
  - Kept camera viewfinder, chats, friends messages, snap map, and stories completely free of the curtain overlay.
  - In `handleWindowStateChanged`, checks `isSnapchatSpotlightActive()` and immediately hides the overlay if user is on camera or other screens upon switching to Snapchat.
  - In `handleViewClicked`, immediately hides the overlay when tapping Camera, Chat, Map, or Stories tabs, and only shows when tapping Spotlight.
  - In `handleViewScrolled`, detects horizontal tab swipes across main tabs to hide or show the overlay based on whether Spotlight is active, and immediately hides overlay if scrolling on non-spotlight screens.
  - Re-compiled signed release Android App Bundle (`dist/Clawse-v1.0.35-release.aab`) and release APK (`dist/Clawse-v1.0.35-release.apk`).
- v1.0.35 Update 2: Elimination of Friend Snap Viewing False Triggers in Snapchat.
  - Resolved personal snaps received in chat triggering the curtain drop when opened.
  - Root cause: Snapchat uses its internal `opera_viewer` and `snap_player` multimedia engine for all snaps, which previously matched generic keywords in `spotlightKeywords` and `isSnapchatSpotlightActive`.
  - Removed `opera`, `story_viewer`, `snap_player`, and `playback_view` from `spotlightKeywords` and `isViewPagerScroll`.
  - Restricted Spotlight detection exclusively to explicit `spotlight` identifiers and selected `ngs_spotlight_icon_container` navigation tab.
  - Added explicit rejection for direct snaps, snap previews, and friend stories.
  - Re-compiled signed release Android App Bundle (`dist/Clawse-v1.0.35-release.aab`) and release APK (`dist/Clawse-v1.0.35-release.apk`).



- v1.0.36 Release Bundle (versionCode 37): Production Release App Bundle for Google Play Console.
  - Bumped release `versionCode` to 37 and `versionName` to "1.0.36" in `app/build.gradle.kts`.
  - Bundled strict Snapchat Spotlight scoping (friend snaps, stories, and chats completely excluded).
  - Bundled zero-lag accessibility loop and settle rebound filtering.
  - Compiled signed release Android App Bundle (`dist/Clawse-v1.0.36-release.aab`) and release APK (`dist/Clawse-v1.0.36-release.apk`).
- v1.0.36 Update: Fix Instagram Home Feed Scroll Rate & Unblock Settings Uninstall Pop-up.
  - Resolved Instagram Home Feed dropping the curtain too fast.
  - Root cause: `cls.contains("recycler")` in `isViewPagerScroll` falsely treated Instagram Home Feed `RecyclerView` as a full-screen discrete ViewPager, adding full screen height units (~2000px) on every post instead of finger scroll delta.
  - Removed generic `recycler` from `isViewPagerScroll`.
  - Strictly differentiated continuous scrolling on Instagram Home Feed (`pixelsToAdd = deltaY.toFloat()`) from discrete pagination on Instagram Reels (`screenHeight * diff * 0.85f`).
  - Resolved screen freeze and missing pop-up when tapping "Uninstall" in Android Settings.
  - Root cause: `checkAndApplyUninstallProtection` ran unconditionally when Guardian Lock was not enabled, immediately clicking `Cancel` and sending `GLOBAL_ACTION_BACK` which killed the system uninstallation dialog and froze Settings.
  - Gated anti-tamper interception strictly behind `isGuardianLockEnabled`.
  - Users without Guardian Lock can now uninstall cleanly via the standard Android confirmation dialog.
  - Re-compiled signed release Android App Bundle (`dist/Clawse-v1.0.36-release.aab`) and release APK (`dist/Clawse-v1.0.36-release.apk`).
  - Stream-installed updated release APK to connected phone via ADB.
- v1.0.37 Release Bundle (versionCode 38): Production Release App Bundle for Google Play Console.
  - Bumped release `versionCode` to 38 and `versionName` to "1.0.37" in `app/build.gradle.kts`.
  - Bundles Instagram Home Feed continuous scroll rate normalization.
  - Bundles Settings uninstallation confirmation pop-up unblocking (anti-tamper strictly gated to Guardian Lock).
  - Bundles pure Snapchat Spotlight scoping (excluding friend snaps, chats, and stories).
  - Compiled signed release Android App Bundle (`dist/Clawse-v1.0.37-release.aab`) and release APK (`dist/Clawse-v1.0.37-release.apk`).

- v1.0.38 Release Bundle (versionCode 39): 8-Bit Arcade (Retro Pixel) Theme & Casper Breed Integration.
  - Implemented third app theme: "8-Bit Arcade (Retro Pixel)" alongside "Sticker Pop (Light)" and "Midnight OLED (Dark)".
  - Built custom `PixelAwningView` rendering a striped honey-gold awning canopy with semicircular scalloped hem and a dynamic peeking cat companion.
  - Built custom `SegmentedProgressBar` drawing an 18-tick chunky retro tally bar for daily reel limit tracking.
  - Integrated open-source Google Silkscreen pixel typography (`silkscreen_bold.ttf`, `silkscreen_regular.ttf`) for headings, badges, and big pixel counters.
  - Added the 7th companion cat breed: "Casper" (ghostly pixel cat), extracted and normalized from `Cats/casper.png`.
  - Processed transparent animation loop frames `kitty_casper_0.png` through `kitty_casper_11.png` for curtain sway, loaf icon `kitty_casper_loaf.png`, and sticker asset `kitty_casper_sticker.png`.
  - Integrated Casper across all 3 themes in `activity_main_pixel.xml`, `activity_main_light.xml`, and `activity_main.xml`.
  - Added theme selection options in `showThemeSelectionDialog()` and `showMainMenuSheet()`.
  - Bumped release `versionCode` to 39 and `versionName` to "1.0.38" in `app/build.gradle.kts`.
  - Compiled signed release Android App Bundle (`dist/Clawse-v1.0.38-release.aab`) and release APK (`dist/Clawse-v1.0.38-release.apk`).

- v1.0.38 Update: App Info Anti-Tamper & 60-Second Cognitive Friction Timer.
  - Closed the force stop and data-clearing bypass loophole by enforcing anti-tamper directly upon opening App Info or Storage settings for Clawse.
  - When a user without a Guardian PIN opens Clawse settings in Android Settings, the 60-second cooling-off period (`BreathingFrictionActivity`) launches immediately to break impulsive dopamine cravings.
  - Prevented bypassing the friction screen via gesture or button back navigation by intercepting back presses to safely return to the home screen.
  - When the 60-second timer completes, users can tap "Proceed to Settings (5m Access)" which grants 5 minutes of unrestricted maintenance mode.
  - During active maintenance mode, users can freely modify permissions, clear storage, force stop, or uninstall without friction.
  - If Guardian Lock is active with a PIN, opening Clawse settings routes directly to `GuardianUnlockActivity` for PIN verification.
  - Initial setup permission flows (Accessibility and Overlay granting) are explicitly whitelisted so first-time onboarding is never blocked.
  - Built and installed the updated build directly to the connected phone via ADB.


- v1.0.39 Release Bundle (versionCode 40): 8-Bit Arcade Official Dark Theme & Interactive Cat Preview.

  - Transformed the 8-Bit Arcade theme into the official Dark theme of Clawse, sunsetting the legacy OLED theme.

  - Clawse now presents two canonical app themes: "Sticker Pop (Light)" (default) and "8-Bit Arcade (Dark)".

  - Built a rich dark arcade color palette: deep background (`#12111A`), card surfaces (`#1C1A27`), stroke boundaries (`#37334D`), honey-gold awning stripes (`#F2B723`), and Silkscreen pixel typography.

  - Resolved hero card cat visibility by separating the canopy awning (`PixelAwningView` 64dp) and companion preview (`ImageView` `ivHeroCatPreview` 86x86dp with gentle idle rotation sway) into an overlapping layout, eliminating canvas stripe clipping.

  - Resolved cat card click behavior: tapping any cat card in the 3x2 grid or Casper row now immediately previews the cat on the hero card and updates speech bubble quotes without popping open paywall modals prematurely.

  - Added dynamic action button (`btnEquipOrUnlock`) supporting "✓ [Breed] on duty", "Equip [Breed]", and "✨ Unlock [Breed]" states (paywall is triggered only when tapping the explicit unlock button).

  - Repointed modal glass blur hardware GPU effect to target the active layout root (`bDark.root` or `bLight.root`) rather than unattached legacy bindings.

  - Bumped release `versionCode` to 40 and `versionName` to "1.0.39" in `app/build.gradle.kts`.

  - Compiled signed release Android App Bundle (`dist/Clawse-v1.0.39-release.aab`) and release APK (`dist/Clawse-v1.0.39-release.apk`).

  - Stream-installed updated debug APK to connected phone (`000693485000641`) via ADB.

- v1.0.40 Release Bundle (versionCode 41): Restored Clean 8-Bit Dark Design, Hanging Cat Companion & Retro Dialogs.
  - Reverted deviations from the reference design to restore clean 3x2 cat grid and Casper row without bottom equip button or card badge clutter.
  - Implemented direct tap-to-equip for unlocked cats and tap-to-preview for locked cats (tapping a previewed locked cat or "Unlock breeds" triggers cosmetic paywall).
  - Fixed hero card companion preview to display the hanging cat sticker (`kitty_{breed}_sticker`) peeking from the awning canopy rather than the sleeping loaf icon shown after curtain drop.
  - Redesigned all 5 dark mode dialogs (`dialog_initial_setup`, `dialog_permission_setup`, `dialog_onboarding`, `dialog_change_limit`, and `dialog_progress_history`) with Silkscreen headings, retro input boxes (`#12111A`), and honey-gold action buttons (`#F2B723`).
  - Bumped release `versionCode` to 41 and `versionName` to "1.0.40" in `app/build.gradle.kts`.
  - Compiled signed release Android App Bundle (`dist/Clawse-v1.0.40-release.aab`) and release APK (`dist/Clawse-v1.0.40-release.apk`).
  - Stream-installed updated debug APK to connected phone (`000693485000641`) via ADB.

- v1.0.41 Release Bundle (versionCode 42): Play Store Blue Launcher Icon & Natural Pendulum Curtain Cat Swing.
  - Upgraded app launcher icon across all Android densities (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`, `anydpi-v26`, `drawable-nodpi`) and Play Store 512x512 asset from `Cats/playstore_icon7.png`.
  - Replaced legacy black launcher background with brand teal/blue (`#2E9FA2`), centering the Clawse pixel cat in the 66% adaptive icon safe zone.
  - Replaced curtain overlay tail-wagging mesh deformation with smooth pendulum swinging anchored at the paws on the wooden pole (`stickTop` pivot).
  - Synchronized curtain swing physics with the hero companion card (1800ms period, +/-3.2 degree arc, `AccelerateDecelerateInterpolator`).
  - Added view lifecycle management in `KittyCurtainView` (`onAttachedToWindow`, `onDetachedFromWindow`) to pause and resume animators without memory leaks.
  - Bumped release `versionCode` to 42 and `versionName` to "1.0.41" in `app/build.gradle.kts`.
  - Compiled signed release Android App Bundle (`dist/Clawse-v1.0.41-release.aab`) and release APK (`dist/Clawse-v1.0.41-release.apk`).
  - Stream-installed updated debug APK to connected phone (`000693485000641`) via ADB.

- v1.0.42 Release Bundle (versionCode 43): 8-Bit Dark Protection Panels Parity & Bottom Card Cleanup.
  - Aligned the 8-Bit Arcade Dark theme with Sticker Pop Light by restoring all 3 core protection panels under the "PROTECTION" header.
  - Panel 1: PIN Security & Daily Limit (`cardPinSecurity`) with PIN status, Set/Unlock PIN actions, collapsible protected settings (`llGuardianProtectedSettings`), daily reel cap numeric input (`etCustomLimit`), and per-app monitored switches.
  - Panel 2: Guardian Alerts (`cardGuardianProtection`) with bell icon, partner ping summary, and endpoint link configuration button (`btnConfigGuardianEndpoint`).
  - Panel 3: Device Owner Protection (`cardDeviceOwner`) with shield icon, ADB tamper status, and setup command clipboard copy button (`btnDeviceOwnerAdb`).
  - Removed unwanted standalone "Uninstall Now" card (`btnSelfDestructUninstall`) from the bottom of `activity_main_pixel.xml`.
  - Bumped release `versionCode` to 43 and `versionName` to "1.0.42" in `app/build.gradle.kts`.
  - Compiled signed release Android App Bundle (`dist/Clawse-v1.0.42-release.aab`) and release APK (`dist/Clawse-v1.0.42-release.apk`).
  - Stream-installed updated debug APK to connected phone (`000693485000641`) via ADB.

- v1.0.43 Release Bundle (versionCode 44): Strict Settings Timer Reset on Dismiss & Backgrounding Prevention.
  - Resolved the friction timer background bypass loophole in `BreathingFrictionActivity`.
  - Closing the popup via Back button, "Keep My Focus" button, outside card tap, Home gesture, or app switcher now immediately terminates the timer and finishes the activity.
  - Any subsequent attempt to open Clawse settings starts fresh with the full 60-second cooling-off countdown.
  - Replaced `FLAG_ACTIVITY_SINGLE_TOP` with `FLAG_ACTIVITY_CLEAR_TASK` in `KittyAccessibilityService` for clean task recreation.
  - Bumped release `versionCode` to 44 and `versionName` to "1.0.43" in `app/build.gradle.kts`.
  - Compiled signed release Android App Bundle (`dist/Clawse-v1.0.43-release.aab`) and release APK (`dist/Clawse-v1.0.43-release.apk`).
  - Stream-installed updated debug APK to connected phone (`000693485000641`) via ADB.
- v1.0.44 Release Bundle (versionCode 45): Staged Cat Purr & Growl Audio Feedback with Settings Toggle.
  - Implemented authentic acoustic cat audio feedback in two distinct stages as the curtain descends.
  - Stage 1 (50% drop / halfway down): Gentle soothing cat purr (3.5s 44.1kHz stereo PCM WAV `cat_purr_normal.wav`).
  - Stage 2 (75% drop): Agitated angry cat purr / low guttural growl and hiss (3.0s 44.1kHz stereo PCM WAV `cat_purr_angry.wav`).
  - Built `CatPurrSoundManager.kt` using Android `SoundPool` with `USAGE_ASSISTANCE_SONIFICATION` for zero-lag in-memory sound effect playback.
  - Connected `KittyOverlayManager.showOrUpdateOverlay` to trigger stage sounds when crossing 50% and 75% thresholds.
  - Added debounce logic: stages reset if curtain retracts below 40% or when daily scrolls reset, and audio immediately halts on overlay dismissal.
  - Added "CAT PURR AUDIO" configuration card (`cardPurrSound`) with enable/disable switch (`switchPurrSound`) and preview test buttons (`btnTestNormalPurr`, `btnTestAngryPurr`) to both 8-Bit Arcade Dark (`activity_main_pixel.xml`) and Sticker Pop Light (`activity_main_light.xml`) themes.
  - Stored user audio preference in `ScrollDataManager.isPurrSoundEnabled` (default `true`).
  - Bumped release `versionCode` to 45 and `versionName` to "1.0.44" in `app/build.gradle.kts`.
  - Compiled signed release Android App Bundle (`dist/Clawse-v1.0.44-release.aab`) and release APK (`dist/Clawse-v1.0.44-release.apk`).
- Unreleased fix: Profile reels over-dropped the curtain.
  - Opening a reel from a profile grid jumps the pager index; `KittyAccessibilityService` charged up to 3 reels (`coerceAtMost(3)`) for that one jump.
  - Discrete pagers now charge exactly one reel (`screenHeight * 0.85f`) per forward page advance.
  - Not yet built or device-tested. Open: add a minimal visual cue to the curtain drop (scope to confirm with user).
- Unreleased: Curtain drop cue and device install.
  - `KittyCurtainView` draws a faint 3-step shadow under the stick only while the drop animator runs (`dropShadowPaint`).
  - Debug APK installed on phone `000693485000641`; the old release-signed `com.clawse.app` was uninstalled first (signature mismatch, local data wiped).
  - Accessibility service, overlay, and notification permissions re-enabled via ADB.
  - Handshake responses answered in `Schema/Handshake/Handshake - ClawBack Device Check.md`.
- Unreleased: Debug builds now signed with the release key (`app/build.gradle.kts` `debug { signingConfig = release }`), so `adb install -r` keeps PIN and data. Debug APK cert SHA-256 matches `keystore/release.jks`.
- v1.0.45 (versionCode 46): Smooth curtain on hard flings, bounded history writes, release log stripping.
  - Root cause (reproduced on device): Instagram coalesces a hard fling into a few ~2500px scroll events 40-500ms apart, and each event restarted a 320ms ease-out that stopped dead, so the curtain moved in a staircase. Rendering itself was clean (0 janky frames, no slow UI thread).
  - `KittyCurtainView` now follows the target with a critically damped spring (`TimeAnimator` + `springStep`, omega 12 rad/s) that retargets without losing velocity and snaps instead of overshooting.
  - Sway and Saturn sprite animators only invalidate while the hanging cat is visible (`isSwinging()`); a fully locked or hidden curtain no longer redraws the full-screen overlay every frame (540 frames per 5s down to 3).
  - `ScrollDataManager.recordHistoryEntry` prunes entries older than 60 days, since the whole JSON is rewritten every 500ms while scrolling.
  - `proguard-rules.pro` strips `Log.v/d/i` from release (the service logged every scroll event).
  - New `CurtainSpringTest` replays a captured hard-fling event pattern: monotonic, no overshoot, no stall between bursts, converges.
  - Built `dist/Clawse-v1.0.45-release.aab` and `.apk` (release key). The phone runs the same APK re-signed with the machine debug key, because the 19:34 install is debug-key signed and the active device admin blocks uninstall.
  - Not device-verified visually: E2E flings hit today's limit (curtain fully locked), so motion is proven by the unit test only.
  - The build currently on the phone is still debug-key signed. The next install needs one more uninstall, then updates are in-place.
  - Antigravity re-enabled accessibility, overlay and notifications via ADB. The PIN must be set in the app. Awaiting on-device confirmation of the profile-reels fix and the drop shadow cue.
- v1.0.46 (versionCode 47): Full-app recheck, curtain moved to its own render thread, 9 gap fixes.
  - Root cause of the remaining hard-fling stall (measured with logs on device): the service main thread is saturated by accessibility events during a fling, and the overlay drew on that same thread, so updates landed 600-800ms late. `KittyOverlayManager` now owns a `ClawseOverlay` `HandlerThread`; all overlay view work runs there and the service calls it directly (no main-thread hop), keeping show/hide ordering.
  - Already-attached overlays always spring to new targets; same-package window changes used to snap the curtain mid-fling.
  - `KittyCurtainView` no longer starts infinite animators in its constructor (the never-attached preview inflated by `MainActivity` leaked them forever).
  - `DailyResetWorker` calls the date-based `checkAndPerformMidnightReset()` instead of `resetDailyCount()`; the 15 min flex window could fire before midnight and wipe the day.
  - `BillingManager.queryActivePurchases` mirrors Play both ways, so refunds and lapsed subscriptions revoke access.
  - `verifyPin` locks out for 60s after 5 wrong tries (all PIN entry points), with the remaining time shown.
  - Settings tamper checks are coalesced to one trailing check per 150ms, immediate on window changes (verified: App Info still opens the 60s friction screen).
  - Android 8 crash fixed: `scrollDeltaX/Y` are API 28+, now guarded (pagers still count via `fromIndex`).
  - Lint errors 5 to 0: removed redundant `onBackPressed` override, unused malformed string, `compileSdk` 36. `MainActivity` clears its handler on destroy.
  - E2E on phone (clean uninstall + reinstall, permissions and device admin re-granted via adb): two hard flings now drop the curtain in one continuous motion (138 to 399px over ~600ms, no stalls), 1 janky frame of 1279.
  - Built `dist/Clawse-v1.0.46-release.aab/.apk`; phone runs the release-key APK with today's count reset to 0.
