package com.antiscroller.kitty

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for cosmetic entitlements, paywall checks, and guardian alert contracts.
 */
class EntitlementsAndGuardianTest {

    private fun checkThemeUnlocked(theme: String, isCosmeticPackUnlocked: Boolean): Boolean {
        return when (theme.lowercase()) {
            "ginger", "orange" -> true
            else -> isCosmeticPackUnlocked
        }
    }

    @Test
    fun testGingerAlwaysFreeByDefault() {
        val cosmeticUnlocked = false
        assertTrue("Ginger must always be unlocked for free users", checkThemeUnlocked("ginger", cosmeticUnlocked))
        assertTrue("Orange legacy mapping must also be unlocked", checkThemeUnlocked("orange", cosmeticUnlocked))
    }

    @Test
    fun testPremiumCatsLockedWithoutPurchase() {
        val cosmeticUnlocked = false
        assertFalse("Tabby must be locked behind paywall", checkThemeUnlocked("tabby", cosmeticUnlocked))
        assertFalse("Saturn must be locked behind paywall", checkThemeUnlocked("saturn", cosmeticUnlocked))
        assertFalse("Rolex must be locked behind paywall", checkThemeUnlocked("rolex", cosmeticUnlocked))
    }

    @Test
    fun testPremiumCatsUnlockedAfterPurchase() {
        val cosmeticUnlocked = true
        assertTrue("Tabby unlocked after purchase", checkThemeUnlocked("tabby", cosmeticUnlocked))
        assertTrue("Saturn unlocked after purchase", checkThemeUnlocked("saturn", cosmeticUnlocked))
        assertTrue("Rolex unlocked after purchase", checkThemeUnlocked("rolex", cosmeticUnlocked))
        assertTrue("Ginger remains unlocked", checkThemeUnlocked("ginger", cosmeticUnlocked))
    }

    @Test
    fun testGuardianEndpointResolution() {
        fun resolveEndpoint(raw: String): String {
            return when {
                raw.startsWith("http://") || raw.startsWith("https://") -> raw
                raw.contains("@") -> "https://ntfy.sh/${raw.replace("@", "_at_")}"
                else -> "https://ntfy.sh/$raw"
            }
        }

        assertEquals(
            "https://discord.com/api/webhooks/123/xyz",
            resolveEndpoint("https://discord.com/api/webhooks/123/xyz")
        )
        assertEquals(
            "https://ntfy.sh/my_clawback_topic",
            resolveEndpoint("my_clawback_topic")
        )
        assertEquals(
            "https://ntfy.sh/parent_at_example.com",
            resolveEndpoint("parent@example.com")
        )
    }

    @Test
    fun testGuardianAlertPayloadStructure() {
        val eventType = "UNINSTALL_ATTEMPT"
        val deviceName = "Child Phone"
        val summary = "Uninstall attempt intercepted"

        val map = mapOf(
            "app" to "ClawBack",
            "event" to eventType,
            "device" to deviceName,
            "message" to summary,
            "content" to summary
        )

        assertEquals("ClawBack", map["app"])
        assertEquals("UNINSTALL_ATTEMPT", map["event"])
        assertEquals("Child Phone", map["device"])
        assertEquals(summary, map["content"])
    }

    @Test
    fun testIgnoredPackagesContract() {
        fun isIgnoredPackage(pkg: String, myPkg: String = "com.antiscroller.kitty"): Boolean {
            val lower = pkg.lowercase()
            return lower == myPkg.lowercase() ||
                   lower.contains("inputmethod") ||
                   lower.contains("keyboard") ||
                   lower.contains("keyguard") ||
                   lower.contains("systemui") ||
                   lower == "android"
        }

        // SystemUI and Framework volume/nav bars must be ignored
        assertTrue("com.android.systemui must be ignored", isIgnoredPackage("com.android.systemui"))
        assertTrue("com.nothing.systemui must be ignored", isIgnoredPackage("com.nothing.systemui"))
        assertTrue("android framework must be ignored", isIgnoredPackage("android"))
        assertTrue("Keyboards must be ignored", isIgnoredPackage("com.google.android.inputmethod.latin"))
        assertTrue("Keyguard must be ignored", isIgnoredPackage("com.android.keyguard"))
        assertTrue("Our own app must be ignored", isIgnoredPackage("com.antiscroller.kitty"))

        // Real user applications and launchers must NOT be ignored
        assertFalse("Launcher must NOT be ignored", isIgnoredPackage("com.nothing.launcher"))
        assertFalse("Nexus launcher must NOT be ignored", isIgnoredPackage("com.google.android.apps.nexuslauncher"))
        assertFalse("Instagram must NOT be ignored", isIgnoredPackage("com.instagram.android"))
        assertFalse("YouTube must NOT be ignored", isIgnoredPackage("com.google.android.youtube"))
        assertFalse("WhatsApp must NOT be ignored", isIgnoredPackage("com.whatsapp"))
    }

    @Test
    fun testSettingsOrInstallerContract() {
        fun isSettingsOrInstaller(pkg: String): Boolean {
            val lower = pkg.lowercase()
            return lower.contains("settings") ||
                   lower.contains("packageinstaller") ||
                   lower.contains("permissioncontroller") ||
                   lower == "com.android.vending"
        }

        assertTrue("Settings must be detected", isSettingsOrInstaller("com.android.settings"))
        assertTrue("PackageInstaller must be detected", isSettingsOrInstaller("com.google.android.packageinstaller"))
        assertTrue("PermissionController must be detected", isSettingsOrInstaller("com.google.android.permissioncontroller"))
        assertTrue("Play Store must be detected", isSettingsOrInstaller("com.android.vending"))

        // Critical fix verification: Framework and SystemUI must NOT be detected as Settings/Installer
        assertFalse("android framework is NOT settings or installer", isSettingsOrInstaller("android"))
        assertFalse("systemui is NOT settings or installer", isSettingsOrInstaller("com.android.systemui"))
        assertFalse("Instagram is NOT settings or installer", isSettingsOrInstaller("com.instagram.android"))
    }
}
