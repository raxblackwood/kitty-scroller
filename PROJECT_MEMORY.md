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
