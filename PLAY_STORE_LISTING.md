# Google Play Store Listing & Submission Guide
**App Name:** AntiScroller: Kitty Curtain  
**Package ID:** `com.antiscroller.kitty`  
**Version:** 1.0.0 (Version Code: 1)  

---

## 1. Store Metadata

### Title (max 30 characters)
`AntiScroller: Kitty Curtain`

### Short Description (max 80 characters)
`Tame doomscrolling on Reels & Shorts with a cute, gradual kitty screen curtain!`

### Full Description (up to 4,000 characters)
```text
Tired of losing hours to endless scrolling on Instagram Reels and YouTube Shorts? 🐾

AntiScroller: Kitty Curtain is the gentle, playful, yet firmly effective digital wellbeing tool that keeps your screen time in check. Instead of jarring locks or frustrating timers, AntiScroller introduces a cute kitty that slowly drops down a screen curtain as you scroll!

🐾 HOW IT WORKS
1. Choose Your Daily Limit: Pick how many Reels or Shorts you want to allow yourself each day (e.g., 10, 20, 50, or custom).
2. Browse Mindfully: As you scroll through Instagram Reels or YouTube Shorts, AntiScroller accurately measures your scroll distance (1 screen height = 1 Reel).
3. The Curtain Descends: The more you scroll, the further your cute kitty curtain lowers from the top of your screen, progressively covering your view.
4. Take a Break: Once you reach 100% of your limit, the screen is covered—signaling it's time to put your phone down and get back to your life!
5. Daily Midnight Reset: Your counter resets automatically every night at midnight so you start fresh each morning.

✨ KEY FEATURES
• Accurate Scroll Distance Tracking: Specifically monitors vertical scroll depth in Instagram Reels and YouTube Shorts without false triggers when reading DMs or switching tabs.
• Multiple Kitty Themes: Choose between Ginger Cat 🐱, Midnight Black Tuxedo 🐈‍⬛, Calico, and Siamese!
• Guardian / Parental PIN Lock: Perfect for parents, friends, or accountability partners. Lock your limit settings and prevent unauthorized disabling or uninstallation without the PIN.
• 100% Private & Offline: AntiScroller runs completely on your device. Zero internet requests, zero tracking, zero data collection.

🔒 ACCESSIBILITY SERVICE API DISCLOSURE
AntiScroller requires Android's AccessibilityService API solely for its core functionality:
• To detect when Instagram or YouTube is open in the foreground.
• To calculate vertical scroll displacement within short-form video feeds.
The AccessibilityService is NOT used to read personal messages, keystrokes, passwords, or device content. All processing occurs strictly on your device in real-time, and no data is ever transmitted or stored remotely.

Take control of your attention today with AntiScroller: Kitty Curtain! 🐾
```

---

## 2. Google Play Console Form Answers

### Category & Tags
- **App Category:** Productivity / Health & Fitness / Digital Wellbeing
- **Tags:** Screen Time, Digital Wellbeing, Focus, App Blocker, Parental Controls, Habit Tracker

### Target Audience & Content Rating
- **Target Age:** 13 and up (or All Ages if certified)
- **Content Rating:** Everyone (no violence, no sexual content, no user-generated content, no tracking)
- **Contains Ads:** No
- **In-App Purchases:** No

---

## 3. Accessibility Service Declaration Form (CRITICAL)

Google strictly requires answers for apps using `AccessibilityService`. Copy & paste the following answers into the Google Play Console Accessibility Declaration form:

#### Question: Which core features of your app require the AccessibilityService API?
> **Answer:**  
> "AntiScroller is a digital wellbeing and screen-time management application designed to curb compulsive doomscrolling on short-form video platforms (Instagram Reels and YouTube Shorts). The AccessibilityService API is required to observe vertical scroll gestures and compute scroll distance on-screen in real time so the app can visually lower a screen-curtain overlay proportionately to the user's configured limits."

#### Question: Why is an alternative API not sufficient?
> **Answer:**  
> "Android does not provide public platform APIs or UsageStats callbacks that report granular scroll depth or individual video transitions inside third-party apps. AccessibilityService is the only supported Android framework API capable of observing accessibility scroll events to calculate screen-height scroll displacement."

#### Question: Does the app collect, share, or store sensitive user data using AccessibilityService?
> **Select:** `No, the app does not collect or store any personal data.`

#### Question: Instructions for App Reviewers / Google Play Review Team:
```text
Instructions for testing AntiScroller: Kitty Curtain:
1. Open AntiScroller.
2. Grant "Display Over Other Apps" permission and enable the "AntiScroller Kitty Service" in Accessibility Settings when prompted.
3. Set the daily Reel limit slider to a low number (e.g. 5 reels) for quick verification.
4. Open Instagram or YouTube and scroll through Reels/Shorts.
5. Notice the Kitty Curtain smoothly descend from the top of the display proportionally with each reel scrolled.
6. Optional Guardian Lock test: Set a 4-digit PIN in Settings to see security locking in action.
```

---

## 4. Privacy Policy URL
Host `PRIVACY_POLICY.html` on GitHub Pages or paste the raw GitHub link:
- GitHub Pages: `https://raxblackwood.github.io/kitty-scroller/PRIVACY_POLICY.html` (after enabling Pages in repo settings)
- Raw File: `https://raw.githubusercontent.com/raxblackwood/kitty-scroller/main/PRIVACY_POLICY.md`

---

## 5. Artifacts Checklist

| Asset | Location in Repo | Specifications |
|---|---|---|
| **Production App Bundle** | `dist/AntiScroller-v1.0.0-release.aab` | Signed AAB (Upload to Play Console Release track) |
| **Shareable Test APK** | `dist/AntiScroller-v1.0.0-release.apk` | Signed APK (Direct installation / Sideloading) |
| **App Icon** | `play_store_assets/icon_512.png` | 512 x 512 PNG, 32-bit color with alpha |
| **Feature Graphic** | `play_store_assets/feature_graphic_1024x500.png` | 1024 x 500 PNG |
| **Privacy Policy** | `PRIVACY_POLICY.md` & `PRIVACY_POLICY.html` | Compliant with Google Accessibility Disclosures |
| **Keystore File** | `keystore/release.jks` | Keystore credentials in `keystore/KEYSTORE_INFO.txt` |
