# Privacy Policy for AntiScroller: Kitty Curtain

**Last updated:** October 17, 2026

**AntiScroller: Kitty Curtain** ("AntiScroller", "we", "our", or "the app") is committed to protecting your privacy. This Privacy Policy outlines our strict privacy practices, explains the permissions our Android application requests, and details how user information is handled.

---

### Summary: Zero Data Collection
**AntiScroller does NOT collect, store, transmit, or share any personal data, usage data, device identifiers, or analytics with any third party or remote server.** The application runs 100% locally and offline on your device.

---

### Accessibility Service API Disclosure (Google Play Compliance)

AntiScroller uses Android's `AccessibilityService` API solely to deliver its core digital wellbeing functionality.

#### 1. Why AntiScroller Requires the Accessibility Service:
AntiScroller is designed to curb excessive short-form video consumption ("doomscrolling") on targeted apps (specifically Instagram Reels and YouTube Shorts). The Accessibility Service allows the app to detect:
- When a monitored target app (Instagram or YouTube) is running in the foreground.
- When vertical scroll events occur within short-form feed containers.

#### 2. How the Data is Used:
- Scroll events are processed **locally in real-time** to compute vertical scroll displacement (e.g., counting reels viewed, where 1 reel corresponds to 1 screen-height of scrolling).
- When scrolling exceeds your selected threshold, the app triggers a visual curtain overlay to remind you to take a break.

#### 3. What We Do NOT Do:
- **No text, messages, or keystrokes are read, monitored, or saved.**
- **No user profile data, photos, audio, or video content is accessed or analyzed.**
- **No interactions outside of scroll displacement within the specified target apps are captured.**
- **No accessibility data is ever saved to disk or transmitted over the internet.**

---

### Other Permissions Used

1. **`SYSTEM_ALERT_WINDOW` ("Display over other apps"):**
   - **Purpose:** Used strictly to render the visual "Kitty Curtain" overlay descending from the top of the screen when scroll thresholds are reached.

2. **`RECEIVE_BOOT_COMPLETED`:**
   - **Purpose:** Automatically restores daily scroll limits and schedules midnight reset alarms when the device reboots.

3. **`REQUEST_DELETE_PACKAGES` / `DEVICE_ADMIN` (Guardian Lock):**
   - **Purpose:** Enforces the Guardian / Parental PIN lock to prevent unauthorized uninstallation or bypassing of screen limits by children or accountability partners.

---

### Data Storage & Retention
- All user preferences (such as selected kitty theme, scroll limits, Guardian PIN hash, and daily counter) are stored locally on your device via Android's secure `SharedPreferences`.
- All daily counters reset automatically at midnight or can be manually reset within the app.
- When you uninstall the application, all local settings and data are permanently deleted by Android.

---

### Third-Party Services
AntiScroller does **not** include any third-party advertising SDKs, tracking libraries, cloud analytics (such as Google Analytics or Firebase), or external telemetry frameworks.

---

### Children's Privacy
AntiScroller does not collect information from anyone, including children under the age of 13. When used with the Guardian PIN feature, parents have full control over the application settings and limits on their child's device.

---

### Changes to This Privacy Policy
If we update this Privacy Policy, the revised version will be published here with an updated "Last updated" date.

---

### Contact Us
If you have questions, feedback, or concerns regarding this Privacy Policy or AntiScroller, you can contact us via our GitHub repository:  
[https://github.com/raxblackwood/kitty-scroller](https://github.com/raxblackwood/kitty-scroller)
