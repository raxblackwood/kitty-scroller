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
