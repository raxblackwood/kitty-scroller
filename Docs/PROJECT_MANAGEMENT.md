# Clawse - Project Management & System Architecture Roadmap

## 1. Executive Summary
This document tracks all features, architectural decisions, completed work, and future roadmaps for **Clawse**.
It serves as the unified project memory for all engineering and product specifications.

---

## 2. Daily Log: Completed Work (Version 1.0.4 - Release Code 5)

### 2.1 Character & Visual Identity Restoration
- Restored the original full-curtain vector illustrations for **Saturn** (the pull-up cat with wiggling tail) and **Rolex** (the mobster cat with shades and cigar).
- The custom pixel-art sleeping loaf cat sprite is now reserved exclusively for **Ginger** and **Tabby**.
- Enforced unified pole rendering across the curtain width to eliminate visual seams during animation playback.

### 2.2 Anti-Impulse Cooling-Off Architecture
- Eliminated impulsive cheating loops where users bypass the curtain by changing settings during active doomscrolling.
- **Tightening Limits**: Decreasing reel limits (e.g. from 25 to 15) is always allowed immediately.
- **Loosening Limits**: Increasing reel limits (e.g. from 25 to 50) is locked behind a 24-hour cooling-off period.
- Solo users can schedule limit increases, which automatically take effect the following day at midnight (00:00).
- Immediate increases are restricted to parents or accountability partners who unlock via the Guardian PIN.
- **App Protection Toggles**: Turning ON protection for Instagram or YouTube is immediate.
- Turning OFF protection during an active day is blocked with an anti-impulse dialog.
- Users can choose to schedule protection removal for the next day at midnight, preventing midday relapse.

### 2.3 Scroll Reset Security Override
- Completely removed the public scroll reset button from the general user interface.
- Normal users can no longer wipe their scroll count to bypass the screen curtain.
- Moved the reset action inside the Parent/Partner PIN Guard card as a PIN-authenticated override.

### 2.4 Google Play Store Listing & Compliance
- Updated store listing copy with an 80-character short description and comprehensive feature breakdown.
- Added explicit Google Play AccessibilityService API disclosures.
- Bumped application version code to 5 (`versionName 1.0.4`).

---

## 3. Guardian & Parental Control Architecture

### 3.1 Option A: Production Implementation (Current V1.0 - V1.0.4)
- **Model**: Single family subscription ($0.50/month) activated on the child device.
- **Zero Server Overhead**: Uses end-to-end encrypted topic pairing over high-performance message relays (e.g. `ntfy.sh`).
- **Guardian Setup**: The guardian phone installs Clawse for free without paying a second subscription.
- The guardian scans or enters the child device's unique topic key.
- **Instant Tamper Defense**: Dispatches real-time alerts if a child attempts to uninstall Clawse, clear app storage, or deactivate Device Administration.
- **Privacy First**: Zero user account databases, zero personally identifiable information (PII) stored, and 100% on-device operation.

---

## 4. Future Architectural Roadmap: Option B (Two-Way Remote Control)

### 4.1 Target Milestone: Version 1.1 / 2.0
Option B introduces live cloud synchronization and bidirectional remote management between parent and child devices.

### 4.2 Architecture & Data Model
- **Cloud Database**: Managed backend using Firebase Firestore or Supabase.
- **Device Pairing**: The child app generates a QR code containing an ephemeral pairing token and public device identifier.
- The parent scans the QR code from the Clawse Guardian Scanner to bind the records.
- **Device Record Schema**:
  - `childDeviceId`: Unique hardware/install UUID.
  - `guardianDeviceIds`: Array of authorized parent device UUIDs.
  - `dailyReelCap`: Remote configurable integer.
  - `currentReelCount`: Synced usage counter.
  - `curtainState`: Real-time drop percentage.
  - `isTamperLocked`: Security state flag.
  - `lastHeartbeat`: Periodic health timestamp.

### 4.3 Feature Capabilities in Option B
1. **Remote Limit Adjustments**: Parents can change daily limits directly from their own phone without touching the child's device.
2. **Instant Emergency Lockdown**: One-tap "Curtain Down Now" command pushed via Firebase Cloud Messaging (FCM).
3. **Weekly Analytics Dashboard**: Visual charts showing daily doomscroll time, peak scrolling hours, and habit progression.
4. **Scheduled Quiet Hours**: Automatic full curtain drops during school hours or bedtime.
