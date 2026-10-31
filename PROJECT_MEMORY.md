# ClawBack (AntiScroller) - Project Memory

- **Project ID**: $uuid
- **Workspace**: E:\CODES\anti scroller
- **Target**: Android Anti-Doomscrolling Application

## 1. Executive Summary
ClawBack is an anti-doomscrolling Android application that drops an animated kitten and fabric curtain from the ceiling of the screen as the user scrolls through Instagram Reels or YouTube Shorts.
It scopes its overlay strictly to video doomscrolling feeds while leaving productive apps (WhatsApp, Chrome, Notion) 100% accessible.

## 2. Key Components
- Animated Kitty Curtain (KittyCurtainView.kt) with dual-sprite physics.
- Accessibility / overlay service detecting reels/shorts view hierarchies.
- Token and design system specified in Docs/UI_DESIGN_SPEC.md.

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
  - Fortified Instagram Reels refresh detection with `isInstagramReelsActive()` querying root window tab markers (`clips_tab`, `reels_viewer_root`).
  - Swiping down to refresh (`deltaY < -20`) immediately registers as a fresh reel scroll and drops the curtain with a 2500ms debounce window.
  - Assembled and stream-installed debug APK onto connected physical device via ADB without launching.

