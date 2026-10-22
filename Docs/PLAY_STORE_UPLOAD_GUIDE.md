# Clawse - Google Play Store Upload & Configuration Guide

This document provides a complete, step-by-step roadmap to publish **Clawse** to the Google Play Store.
It covers building the production artifacts, setting up Google Play Console, configuring In-App Billing products, completing regulatory declarations, and navigating policy reviews.

---

## 1. Quick Release Summary

| Parameter | Configuration Value | Notes |
| :--- | :--- | :--- |
| **App Name** | `Clawse: Screen Time Curtain` (or `Clawse`) | Under 30 characters for the Play Store title. |
| **Package Name** | `com.antiscroller.kitty` | Unique application ID defined in Gradle. |
| **Version Code** | `1` | Increment with each new Play Store upload. |
| **Version Name** | `1.0.0` | User-visible version string. |
| **Default Language** | English (United States) | Expandable to other languages later. |
| **Category** | Tools / Productivity / Digital Wellbeing | Best matches user intent. |
| **Monetization Type** | Free with In-App Purchases (IAP) | One-time cosmetic pack + monthly subscription. |

---

## 2. Store Assets & Graphics Checklist

All prepared graphic assets are located in the `play_store_assets/` directory.

- **App Icon (512 x 512 px)**:
  - File: `play_store_assets/icon_512.png`
  - Format: 32-bit PNG, RGB, opaque black background (Google Play rejects transparent icons).
  - Maximum file size: 1024 KB.

- **Feature Graphic (1024 x 500 px)**:
  - File: `play_store_assets/feature_graphic_1024x500.png`
  - Format: JPEG or 24-bit PNG without alpha.
  - Placed on the top banner of the store listing.

- **Screenshots (Phone)**:
  - Minimum 4 screenshots showing:
    1. Home dashboard with scroll limits and permissions.
    2. Live Kitty Curtain dropping over Instagram Reels or YouTube Shorts.
    3. The 100% full-drop screen with the speech bubble: "It's time to pause Clawse."
    4. Cat breed selection and paywall options.
  - Dimensions: 1080 x 2400 (or standard 16:9 / 9:16 portrait).

---

## 3. Store Listing Copy

### App Title
`Clawse: Screen Time Curtain`

### Short Description (Max 80 characters)
`Gentle, playful screen curtain that pauses your doomscroll loops.`

### Full Description
```text
Tired of losing hours to endless doomscrolling on Reels and Shorts?

Clawse is your gentle, playful digital wellbeing companion designed to help you break compulsive scrolling loops without stressful timers or aggressive app blockers.

HOW IT WORKS:
🐾 1. Set Your Comfort Limit: Choose how many reels you want to watch before taking a pause (e.g., 5 to 50 reels).
🐾 2. Ambient Curtain: As you scroll through Instagram Reels or YouTube Shorts, a friendly pixel-art kitty slowly lowers a screen curtain.
🐾 3. Natural Pause: When you hit your limit, the curtain reaches full drop with the reminder: "It's time to pause Clawse."
🐾 4. Mindful Choice: Take a calm 60-second breathing pause, step away, or intentionally reset your scroll session.

WHY PAWSE WORKS:
Unlike aggressive blockers that cause resentment and prompt you to uninstall them, Clawse uses gradual visual feedback.
You remain fully aware of how much content you have consumed in real time.

KEY FEATURES:
• Zero Friction on Productive Apps: Clawse only observes Instagram Reels and YouTube Shorts. WhatsApp, Chrome, Notion, Phone, and all other apps remain 100% unrestricted.
• 100% Private & Offline: Clawse runs entirely on your device. Zero analytics, zero tracking, and zero personal data collection.
• Unlockable Kitty Companions: Choose your favorite feline guide - Ginger, Saturn, Tabby, or Rolex.
• Optional Parental / Guardian Shields: Real-time alerts to notify parents or accountability partners if uninstall or admin deactivation is attempted.

Take back control of your screen time today with Clawse!
```

---

## 4. Google Play Console Step-by-Step Setup

### Step 1: Create the App
1. Log in to [Google Play Console](https://play.google.com/console).
2. Click **Create app** in the top right.
3. Enter App name: `Clawse`.
4. Select **App** (not Game) and **Free**.
5. Accept the Declarations and click **Create app**.

### Step 2: Upload Initial Build to Internal Testing
> **Crucial Rule:** Google Play Console does not allow creating In-App Products until an APK or AAB with the billing permission (`com.android.vending.BILLING`) is uploaded to an active track.

1. Navigate to **Testing** -> **Internal testing**.
2. Click **Create new release**.
3. Upload `app/build/outputs/bundle/release/app-release.aab`.
4. Enter release notes: `Initial release build of Clawse with Google Play Billing and Guardian shield support.`
5. Click **Next**, review, and click **Save and publish to Internal testing**.

---

## 5. In-App Products & Subscriptions Setup

Once your internal release is processed, configure the two monetization items in the Google Play Console:

### Product 1: Cosmetic Pack (One-Time In-App Purchase)
- Navigate to **Monetize** -> **Products** -> **In-app products**.
- Click **Create product**.
- **Product ID**: `clawback_cosmetic_pack` *(Must match the constant in BillingManager.kt)*
- **Product Name**: `Clawse Cosmetic Kitty Pack`
- **Description**: `Unlocks all premium feline companions: Saturn, Rolex, and Tabby.`
- **Price**: `$2.50` (or local equivalent in INR/EUR/GBP).
- **Status**: Click **Save**, then click **Activate**.

### Product 2: Guardian Shield (Monthly Subscription)
- Navigate to **Monetize** -> **Products** -> **Subscriptions**.
- Click **Create subscription**.
- **Subscription ID**: `clawback_parental_monthly` *(Must match the constant in BillingManager.kt)*
- **Name**: `Clawse Guardian Shield Monthly`
- **Description**: `Real-time webhook alerts when uninstall or admin deactivation is attempted.`
- **Base Plan**:
  - Add a base plan with **Auto-renewing**.
  - Billing period: `1 month`.
  - Price: `$0.50 / month`.
- **Status**: Click **Save**, then click **Activate**.

### Step 3: Add License Testers (Free Testing)
1. Go to **Setup** -> **License testing**.
2. Add your Google email address and any tester emails under "License testers".
3. Set **License response** to `RESPOND_NORMALLY` or `LICENSED`.
4. Testers can now test the purchase and subscription flows without real credit card charges.

---

## 6. Critical Policy Declarations

Google Play enforces strict requirements for apps using system-level permissions.
Use the exact explanations provided below to pass policy review.

### A. Accessibility Service Declaration
Google Play requires a dedicated declaration form for the `AccessibilityService` API.

- **Why does the app use AccessibilityService?**
  Select: `Screen monitoring / Digital wellbeing / Behavioral intervention`.
- **Declaration Text**:
  ```text
  Clawse is a digital wellbeing and screen-time management application designed to curb compulsive doomscrolling on short-form video platforms (Instagram Reels and YouTube Shorts).

  The AccessibilityService API is required strictly for:
  1. Observing scroll gestures within Instagram Reels and YouTube Shorts to calculate cumulative scroll distance.
  2. Rendering the non-interactive screen curtain overlay progressively from the top hem.
  3. Detecting tampering or impulsive uninstallation attempts while inside device settings to notify guardians or prompt mindful breathing friction.

  Clawse does NOT read keystrokes, personal messages, account credentials, or user inputs.
  Clawse does not transmit any user interaction data off the device.
  ```
- **Review Video URL**:
  Record a 30 to 60-second video showing:
  1. The in-app disclosure dialog before granting permission.
  2. Enabling Clawse in Android Accessibility Settings.
  3. The Kitty Curtain dropping while scrolling Instagram Reels.
  Upload this video as unlisted on YouTube and paste the link into the declaration form.

### B. Device Administration API Declaration
- **Declaration Text**:
  ```text
  Clawse uses the Device Administration API (DevicePolicyManager) solely as an opt-in friction mechanism to protect against impulsive uninstallation during doomscroll cravings and to support parental oversight.
  It does not lock the device or erase data.
  ```

### C. Data Safety Questionnaire
Complete the Data Safety form with these responses:
- **Does your app collect or share any user data?** -> Select **No**.
- All calculations occur locally in device memory.
- No network requests are made, except user-configured optional outgoing webhook alerts to the guardian's selected endpoint.

### D. Target Audience and Content Rating
- **Target Age**: 13 and older (or All Ages if accompanied by parental consent).
- **IARC Content Rating**: Answer "No" to violence, offensive language, and gambling. Result will be PEGI 3 / Everyone.

---

## 7. How to Build the Release Artifacts

Whenever you prepare a new build for the Play Store, run the following Gradle commands in the project root:

### 1. Run All Verification Tests
```powershell
./gradlew testDebugUnitTest
```

### 2. Build the Production Android App Bundle (.aab)
```powershell
./gradlew bundleRelease
```
The output file will be generated at:
`app/build/outputs/bundle/release/app-release.aab`

### 3. Build a Sideloadable Release APK (For Direct Phone Testing)
```powershell
./gradlew assembleRelease
```
The output file will be generated at:
`app/build/outputs/apk/release/app-release.apk`

---

## 8. Release Roadmap

1. **Internal Testing**: Upload `app-release.aab` and verify billing on your own device.
2. **Closed Testing**: Invite 20 testers for 14 days (required by Google for new personal developer accounts).
3. **Production Track**: Submit for final review.
4. Google Play review typically takes between 24 and 72 hours for apps using AccessibilityService.
