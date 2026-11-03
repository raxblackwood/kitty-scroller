package com.antiscroller.kitty

import com.antiscroller.kitty.ui.KittyCurtainView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Hard flings on the Instagram home feed arrive as a few coalesced jumps 40-500ms apart
 * (captured on device). The curtain must follow them as one continuous drop: no stop between
 * bursts, no moving backwards, no overshoot past the real scroll position.
 */
class CurtainSpringTest {

    @Test
    fun burstyTargetsProduceContinuousMonotonicDrop() {
        // (time ms, target) from a real hard-fling log: +2474, +2490, +2450, +178, then a second fling
        val events = listOf(0 to 0.04f, 130 to 0.08f, 200 to 0.12f, 240 to 0.123f, 750 to 0.137f, 860 to 0.146f, 920 to 0.153f, 1050 to 0.158f)
        var progress = 0f
        var velocity = 0f
        var target = 0f
        var eventIdx = 0
        var stoppedMidBurst = false

        for (t in 0..2500 step 16) {
            while (eventIdx < events.size && events[eventIdx].first <= t) target = events[eventIdx++].second
            val before = progress
            val (p, v) = KittyCurtainView.springStep(progress, velocity, target, 0.016f)
            progress = p
            velocity = v
            assertTrue("moved backwards at $t ms", progress >= before)
            assertTrue("overshot target at $t ms", progress <= target)
            if (t in 20..240 && velocity == 0f) stoppedMidBurst = true
        }

        assertTrue("curtain stalled between coalesced events of one fling", !stoppedMidBurst)
        assertEquals(0.158f, progress, 0f)
    }

    @Test
    fun retractsWhenCountIsReset() {
        var progress = 0.8f
        var velocity = 0f
        repeat(120) {
            val (p, v) = KittyCurtainView.springStep(progress, velocity, 0f, 0.016f)
            assertTrue(p <= progress && p >= 0f)
            progress = p
            velocity = v
        }
        assertEquals(0f, progress, 0f)
    }
}
