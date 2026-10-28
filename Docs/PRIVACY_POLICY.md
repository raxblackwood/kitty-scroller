# Privacy Policy for ClawBack: Kitty Screen Curtain

**Last updated:** October 27, 2026

**ClawBack: Kitty Screen Curtain** ("ClawBack", "we", "our", or "the app") is committed to protecting your privacy. This Privacy Policy outlines our strict privacy practices, explains the permissions our Android application requests, and details how user information is handled.

---

### Summary: Local by Default
**ClawBack does NOT collect, store, transmit, or share any personal data, scrolling activity, or analytics.** Everything ClawBack does to limit doomscrolling happens locally on your device. The only data that leaves your phone is the small amount needed for the optional Guardian Alerts feature (see "Optional Guardian Alerts" below), and only if you choose to link two phones.

---

### Accessibility Service API Disclosure (Google Play Compliance)

ClawBack uses Android's `AccessibilityService` API solely to deliver its core digital wellbeing functionality.

#### 1. Why ClawBack Requires the Accessibility Service:
ClawBack is designed to curb excessive short-form video consumption ("doomscrolling") on targeted apps (specifically Instagram Reels and YouTube Shorts). The Accessibility Service allows the app to detect:
- When a monitored target app (Instagram or YouTube) is running in the foreground.
- When vertical scroll events occur within short-form feed containers (Instagram Reels and YouTube Shorts).

#### 2. How the Data is Used:
- Scroll events are processed **locally in real-time** to compute vertical scroll displacement (e.g., counting reels viewed, where 1 reel corresponds to 1 screen-height of scrolling).
- When scrolling exceeds your selected threshold, the app triggers a visual curtain overlay to remind you to take a break.
- Scrolling comments, long-form YouTube videos, the YouTube Home feed, direct messages, and profile pages are strictly ignored and not counted.

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

### Optional Guardian Alerts (Google Firebase)
Guardian Alerts notify a parent or accountability partner on their own phone when someone tries to remove ClawBack from a linked phone, or when that phone stops checking in. The feature is **off by default** and only works once two phones are linked with a pairing code. It uses Google Firebase (Authentication, Cloud Firestore, Cloud Messaging), operated by Google LLC, plus a small Cloudflare Workers relay (Cloudflare, Inc.) that forwards alerts to the guardian's phone without storing them.

When Guardian Alerts are in use, only this data is sent to Firebase:
- **Anonymous app ID** from Firebase Anonymous Authentication (no name, email, phone number or account).
- **Pairing code and device label** (the name you enter, or the phone model).
- **Push notification token** for the guardian's phone.
- **Check-in time** of the protected phone (about every 15 minutes), used to warn the guardian if it goes silent.
- **Alert events**: event type and a short message, forwarded through the relay to the guardian's phone and not stored.

ClawBack never sends scroll counts, app usage, screen content, location, contacts or accessibility data to Firebase. This data is used only to deliver Guardian Alerts; it is not used for advertising, analytics or profiling, and is not sold or shared. Data is encrypted in transit. Either phone can unlink at any time, which deletes the link from the server; unused pairing codes are deleted within 24 hours. See Google's Firebase privacy information: https://firebase.google.com/support/privacy

Alternatively, alerts can be sent to a webhook or ntfy.sh topic you configure yourself, in which case the alert message goes to the address you provide.

---

### Data Storage & Retention
- All user preferences (such as selected kitty theme, scroll limits, Guardian PIN hash, and daily counter) are stored locally on your device via Android's secure `SharedPreferences`.
- All daily counters reset automatically at midnight or can be manually reset within the app.
- When you uninstall the application, all local settings and data are permanently deleted by Android.
- Guardian Alert link data on Firebase is deleted when either phone unlinks, or on request via the contact below.

---

### Third-Party Services
- **Google Play Billing** for optional purchases.
- **Google Firebase** and **Cloudflare Workers**, only for the optional Guardian Alerts feature described above.

ClawBack does **not** include tracking libraries, cloud analytics (such as Google Analytics), or external telemetry frameworks.

---

### Children's Privacy
ClawBack does not knowingly collect personal information from children under 13. When a parent links a child's phone for Guardian Alerts, only the limited, non-identifying data listed above is processed, solely to notify the parent. All other settings and limits stay on the child's device under the parent's control.

---

### Changes to This Privacy Policy
If we update this Privacy Policy, the revised version will be published here with an updated "Last updated" date.

---

### Contact Us
If you have questions, feedback, or concerns regarding this Privacy Policy or ClawBack, you can contact us via our GitHub repository:  
[https://github.com/raxblackwood/kitty-scroller](https://github.com/raxblackwood/kitty-scroller)
