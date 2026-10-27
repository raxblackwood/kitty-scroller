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
