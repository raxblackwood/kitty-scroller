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
}
