# ClawBack (AntiScroller) - UI Design & Architecture Specification

## 1. Overview & Visual Identity

ClawBack is an anti-doomscrolling companion application for Android.
The design merges a modern dark cyberpunk and terminal aesthetic with cute retro pixel-art characters.
Instead of harsh lockouts or intrusive fullscreen alerts that prompt users to uninstall the app, ClawBack introduces physical and visual friction.
As the user doomscrolls through short-form video feeds, an adorable kitten slowly pulls down a fabric curtain from the ceiling of the screen.

The user interface is split into two distinct systems:
1. **In-App Material 3 Dashboard**: A dark-mode control center for configuring daily scroll caps, previewing kitty mechanics, choosing breeds, and locking settings with accountability PINs.
2. **Floating System Overlay Canvas**: A hardware-accelerated translucent system overlay that draws the curtain fabric, the full-width wooden pole, and the dual-sprite animated kitten directly over targeted short-form video feeds.

---

## 2. Design System & Style Tokens

### 2.1 Color Palette

The app uses a focused high-contrast dark theme optimized for OLED displays.

```
+-------------------+-----------------+----------------------------------------+
| Token Name        | Hex Code        | Purpose                                |
+-------------------+-----------------+----------------------------------------+
| background        | #121212         | Primary app background (deep black)    |
| surface           | #1E1E1E         | Card surfaces and dialog backgrounds   |
| surface_variant   | #2D2D2D         | Secondary containers, track colors     |
| primary           | #FF6E40         | Warm Kitty Orange (primary actions)    |
| primary_variant   | #E65100         | Deep orange accent                     |
| secondary         | #26A69A         | Sage teal (guardian & status accents)  |
| text_primary      | #F5F5F5         | High-contrast body & header text       |
| text_secondary    | #B0B0B0         | Subtitles, captions, helper text       |
| accent_green      | #00E676         | Success badges and unlocked states     |
| accent_red        | #FF5252         | Danger actions and locked warnings     |
| kitty_peach       | #FF9E80         | Soft orange accent for headings        |
| stroke_border     | #333333         | 1dp border stroke for Material cards   |
+-------------------+-----------------+----------------------------------------+
```

### 2.2 Kitty Breed Theme Tokens

ClawBack includes two selectable kitty breeds, each with distinct color mappings across the canvas and dashboard:

#### Breed A: Ginger Tabby (`orange`)
- Main fur tone: `#FFA726`
- Fur dark shading: `#FB8C00`
- Curtain fabric: `#FFE0B2` (cream fabric)
- Curtain vertical stripes: `#33FFA726` (semi-transparent orange)
- Wooden pole body: `#8F5E40` (classic warm cedar wood)
- Wooden pole border: `#120A05` (dark pixel outline)
- Sleeping eyes & smile: `#4E342E`
- Sleeping Zzz text: `#FF6E40`

#### Breed B: Midnight Black Cat (`black`)
- Main fur tone: `#262626`
- Fur dark shading: `#141414`
- Eyes: `#FFD600` (luminous golden yellow)
- Inner ear & paw pads: `#FF80AB` (soft pink)
- Curtain fabric: `#2A2D34` (slate midnight fabric)
- Curtain vertical stripes: `#33FFD600` (subtle gold pleats)
- Wooden pole body: `#8A5F4A` (dark walnut wood)
- Wooden pole border: `#120A05` (dark pixel outline)
- Sleeping eyes & smile: `#FFD600` (golden)
- Sleeping Zzz text: `#FFD600`

### 2.3 Typography

- **Headers**: System bold sans-serif, 22sp.
- **Card Section Tags**: Uppercase bold, 12sp, with letter-spacing `0.08`.
- **Hero Counters**: Bold display numbers, 40sp.
- **Body & Captions**: Clean readable sans-serif, 13sp to 14sp, line-spacing `2dp` to `3dp`.
- **Canvas Text**: System bold typeface, 26sp to 34sp, center-aligned.

### 2.4 Card Geometry & Elevation

- Card Corner Radius: `16dp` uniform across all dashboard cards.
- Dialog Corner Radius: `24dp` for floating modal sheets.
- Elevation: `2dp` subtle elevation with explicit 1dp outline stroke (`#333333`).
- Spacing: Standard `20dp` page padding, `16dp` inner card padding, `12dp` to `18dp` inter-card gaps.

---

## 3. Screen Layout Breakdown

### 3.1 Main Dashboard (`MainActivity.kt` & `activity_main.xml`)

The main dashboard is arranged vertically inside a `CoordinatorLayout` and `NestedScrollView`.

```
+-------------------------------------------------------------------+
|  [Launcher Icon 52dp]  ClawBack                                   |
|                        Claw back your focus with Kitty Curtain    |
+-------------------------------------------------------------------+
|                                                                   |
|  TODAY'S DOOMSCROLL METER                                         |
|  [ 42 ] / 100 scrolls                           42% covered       |
|  [======================----------------------------------------] |
|  🌙 Daily reset scheduled every midnight (00:00)                  |
|                                                                   |
+-------------------------------------------------------------------+
|                                                                   |
|  KITTY CURTAIN PREVIEW                                            |
|  +-------------------------------------------------------------+  |
|  |  [Live 220dp interactive KittyCurtainView canvas preview]   |  |
|  +-------------------------------------------------------------+  |
|  CHOOSE KITTY BREED:                                              |
|  ( * ) 🍊 Ginger Tabby        (   ) 🐈‍⬛ Midnight Black Cat        |
|  Drag slider to test kitty stretch:                               |
|  [---O---------------------------------------------------------]  |
|  [ Test Floating Overlay on Real Screen (5s) Button ]             |
|                                                                   |
+-------------------------------------------------------------------+
|                                                                   |
|  SETUP & PERMISSIONS                                              |
|  +-------------------------------------------------------------+  |
|  | 1. Accessibility Service                [ Enable / Active ] |  |
|  |    Detects scrolls in Instagram & YouTube Shorts            |  |
|  +-------------------------------------------------------------+  |
|  | 2. Draw Over Other Apps                 [ Enable / Active ] |  |
|  |    Allows the kitty curtain to slide down                   |  |
|  +-------------------------------------------------------------+  |
|  | 3. Smart Uninstall Friction             [ Activate / Active]|  |
|  |    Stops impulsive deletion while giving full app access    |  |
|  +-------------------------------------------------------------+  |
|                                                                   |
+-------------------------------------------------------------------+
|                                                                   |
|  🛡️ PARENT / PARTNER PIN GUARD                                    |
|  Lock settings and prevent app deletion with a Security PIN known |
|  only to the parent or accountability partner.                    |
|  Status: 🔒 Locked by Guardian PIN                                |
|  [ Change PIN ]                             [ Unlock (5m) ]       |
|                                                                   |
+-------------------------------------------------------------------+
|                                                                   |
|  DAILY SCROLL BUDGET                                              |
|  How many video scrolls before the kitty completely covers screen |
|  [ ⭐ 50 Reels (Recommended) ]                                    |
|  [ Type custom reel limit: [ 50 ] ]   [ Set Cap ]                 |
|  Active Cap: 50 Reels / day                                       |
|  ---------------------------------------------------------------  |
|  TARGETED DOOMSCROLL APPS                                         |
|  [x] Instagram (Reels & Feed)                                     |
|  [x] YouTube (Shorts)                                             |
|  [ Reset Today's Scrolls to 0 ]                                   |
|                                                                   |
+-------------------------------------------------------------------+
```

### 3.2 Guardian Unlock Dialog (`GuardianUnlockActivity.kt` & `activity_guardian_unlock.xml`)

When the user attempts to adjust caps, toggle monitored apps, or open Android Settings to uninstall ClawBack, this modal intercepts the action:
- Background: Dimmed translucent scrim (`#DD000000`).
- Card: `24dp` rounded card with 8dp elevation and `#444444` outline.
- Security Header: Shield icon (`🛡️`) with "Guardian Security PIN" title.
- Input: High-contrast centered PIN entry field (`letterSpacing="0.3"`, masked password type, 22sp).
- Validation: Shows red error text on incorrect entry; on success, unlocks maintenance mode for 5 minutes.

---

## 4. Floating Curtain Overlay Architecture (`KittyCurtainView.kt`)

The floating curtain is rendered via Android `WindowManager` with window type `TYPE_APPLICATION_OVERLAY`.
It is 100% pass-through during scrolling so the user can continue using their phone, until 100% doomscroll lock is reached.

### 4.1 Window Flags & Lifecycle
- **During Normal Usage (0% to 95% Coverage)**:
  - `FLAG_NOT_FOCUSABLE`
  - `FLAG_NOT_TOUCH_MODAL`
  - `FLAG_NOT_TOUCHABLE`
  - `FLAG_LAYOUT_IN_SCREEN`
  - `FLAG_LAYOUT_NO_LIMITS`
  - Touch passes directly through to Instagram or YouTube without any input lag.
- **At 100% Doomscroll Lock (96% to 100% Coverage)**:
  - `FLAG_NOT_TOUCHABLE` and `FLAG_NOT_TOUCH_MODAL` are cleared.
  - The overlay blocks all touch input on the video screen, halting the dopamine doomscroll loop until midnight or PIN release.

### 4.2 Multi-Layer Canvas Drawing Order

The canvas rendering inside `onDraw(Canvas)` follows a strict back-to-front layer order:

```
Ceiling (Y = 0)
=====================================================================
Layer 1: Solid Fabric Body (curtainPath with 6 scalloped bottom waves)
         Vertical pleat lines spaced evenly across width
---------------------------------------------------------------------
Layer 2: Full-Width Horizontal Wooden Bar (edge to edge, X: 0 -> W)
         Top pixel border (#120A05)
         Main wood tone (#8F5E40 or #8A5F4A)
         Bottom pixel border (#120A05)
---------------------------------------------------------------------
Layer 3: Kitty Tail Sprite (Back Layer behind body)
         Rotates around hip pivot point with lively wagging
---------------------------------------------------------------------
Layer 4: Kitty Body Sprite (Front Layer)
         Paws gripping pole, full torso, legs, and back feet intact
         Rotates with gentle pendulum sway around paws
=====================================================================
Bottom Hem Line (Y = hemY)
```

### 4.3 Sprite Anatomy & Dimensions
- The sprite is enlarged by 50% relative to standard phone UI, occupying approximately `21%` of screen width:
  - `catW = (screenWidth * 0.21f).coerceIn(200f, 260f)`
  - Aspect ratio: `628 / 1162` (cat height `catH = catW / 0.54f`)
- **Bar Alignment Formula**:
  - The wooden bar in the sprite sits centered at pixel `Y = 51` in a `1162` pixel tall canvas.
  - Cat sprite top is positioned at: `catTop = hemY - (catH * (51f / 1162f))`
  - This guarantees that Kitty's gripping paws wrap seamlessly over the edge-to-edge wooden pole.
- **Dual-Sprite Files**:
  - Black Cat Body: `kitty_black_body.png`
  - Black Cat Tail: `kitty_black_tail.png`
  - Orange Cat Body: `kitty_orange_body.png`
  - Orange Cat Tail: `kitty_orange_tail.png`

### 4.4 Animation Physics & Oscillations
1. **Lively Tail Wagging (`tailAnimator`)**:
   - Hip pivot location: `X = catRect.left + catW * 0.525f`, `Y = catRect.top + catH * 0.6885f`
   - Angle oscillation: `-12.0°` to `+12.0°`
   - Cycle period: `850ms` (infinite reverse repeat)
   - Interpolator: `AccelerateDecelerateInterpolator`
   - Renders behind the body sprite so the legs and crotch remain completely intact without any graphical clipping.
2. **Hanging Body Sway (`swayAnimator`)**:
   - Paws pivot location: `X = catRect.centerX()`, `Y = hemY`
   - Angle oscillation: `-2.2°` to `+2.2°`
   - Cycle period: `1750ms` (gentle pendulum swing)
   - Interpolator: `AccelerateDecelerateInterpolator`
3. **Curtain Drop Progress (`animator`)**:
   - Drop fraction transition: `200ms` duration with `DecelerateInterpolator(1.2f)`.
   - Guarantees immediate visual feedback as the user swipes.

### 4.5 Silent Scrolling Interaction Paradigm
- **Intermediate Scroll (0% to 95%)**:
  - Completely silent.
  - No chat bubbles, no popups, and no speech text appear while the user scrolls.
  - The cat wags its tail and sways silently as the curtain progressively covers the screen.
- **Full Lockout State (96% to 100%)**:
  - When the daily budget is exhausted, the curtain fully drops to the bottom of the display.
  - The hanging cat transitions into a sleeping loaf cat centered at `45%` screen height.
  - Features sleeping closed eyes, a gentle smile, and floating animated `Z z z` letters.
  - Displays a clean speech bubble badge: `"Daily scroll limit reached 💤"`.

---

## 5. Scoping, Detection & Trigger Rules

ClawBack strictly limits curtain drops to short-form addiction surfaces while leaving productive surfaces completely unaffected.

### 5.1 Permitted Scroll Targets (Only 3 Allowed)
1. **Instagram Home Feed**:
   - Monitored when package is `com.instagram.android`.
   - Only downward scrolling with vertical travel (`deltaY > 15`) counts towards the scroll budget.
   - Upward scrolling (`deltaY < -10`) is explicitly ignored.
2. **Instagram Reels Feed**:
   - Monitored when swiping vertically to advance reels.
   - Forward page advances (`fromIndex > lastIndex`) accumulate scroll distance.
3. **YouTube Shorts**:
   - Monitored when package is `com.google.android.youtube`.
   - Strictly scoped to `reel_recycler`, `reel_watch_fragment`, and `shorts_container`.
   - Forward video swipes or tapping the "Next" button accumulate scroll distance.

### 5.2 Explicitly Ignored Surfaces (Zero Curtain Movement)
- **Instagram Comments**: Comments bottom sheet and reply lists are completely ignored.
- **YouTube Comments**: `engagement_panel` and comment lists are completely ignored.
- **Instagram Direct Messages (DMs)**: Chat threads, inbox, and message recipient lists are ignored.
- **Instagram Explore & Profile**: Search grids and user profile grids are ignored.
- **YouTube Home Feed & Search**: `results_recycler_view`, `feed_filter_bar`, and regular video watch players are ignored.

### 5.3 Overlay Persistence Rule
- Once the user opens Instagram or YouTube, the curtain overlay remains visible across all internal tabs, profiles, and comment sheets (as long as `dropPercentage > 0`).
- The curtain does not disappear or flicker when opening comments or switching between tabs.
- The overlay only detaches and hides when the user switches out of Instagram/YouTube to another application (such as WhatsApp, Chrome, or Notion).

---

## 6. Daily Reset & Data Persistence

- **Data Store**: Android `SharedPreferences` managed through `ScrollDataManager.kt`.
- **Unit of Measure**: 1 Reel unit = 1 full screen height of scroll pixels (`screenHeightPx`).
- **Midnight Reset**:
  - `DailyResetWorker.kt` is scheduled using Android `WorkManager` for `00:00` midnight every night.
  - On midnight trigger, today's scroll count and accumulated pixel distance reset to zero.
  - The curtain smoothly retracts back to the ceiling.
- **Anti-Uninstall Device Admin**:
  - `KittyAdminReceiver.kt` uses Android Device Policy Manager to prevent impulsive app uninstallation during doomscroll sessions.
  - If a Guardian PIN is configured, changing limits or uninstalling requires entering the PIN.
