package com.antiscroller.kitty.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.antiscroller.kitty.R
import com.antiscroller.kitty.data.ScrollDataManager

/**
 * Manages real acoustic cat vocalizations (purrs and growls) triggered in stages
 * as the doomscroll curtain descends:
 * - Stage 1 (>= 50% / halfway down): Real soothing cat purr (R.raw.cat_purr_normal)
 * - Stage 2 (>= 75% down): Agitated angry cat purr / low growl (R.raw.cat_purr_angry)
 *
 * User can enable/disable this feature in settings via ScrollDataManager.isPurrSoundEnabled.
 */
class CatPurrSoundManager private constructor(context: Context) {

    companion object {
        private const val TAG = "CatPurrSoundManager"

        @Volatile
        private var instance: CatPurrSoundManager? = null

        fun getInstance(context: Context): CatPurrSoundManager {
            return instance ?: synchronized(this) {
                instance ?: CatPurrSoundManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val appContext = context.applicationContext
    private val dataManager = ScrollDataManager.getInstance(appContext)

    private var soundPool: SoundPool? = null
    private var normalPurrId: Int = 0
    private var angryPurrId: Int = 0
    private var normalPurrLoaded = false
    private var angryPurrLoaded = false

    private var activeStreamId: Int = 0
    private var lastPlayedStage: Int = 0 // 0 = none, 1 = 50% normal purr, 2 = 75% angry purr

    init {
        initSoundPool()
    }

    private fun initSoundPool() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val pool = SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(audioAttributes)
                .build()

            pool.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) {
                    if (sampleId == normalPurrId) normalPurrLoaded = true
                    if (sampleId == angryPurrId) angryPurrLoaded = true
                }
            }

            normalPurrId = pool.load(appContext, R.raw.cat_purr_normal, 1)
            angryPurrId = pool.load(appContext, R.raw.cat_purr_angry, 1)
            soundPool = pool
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SoundPool: ${e.message}")
        }
    }

    /**
     * Evaluates curtain drop percentage and plays stage sounds if enabled.
     */
    fun onDropPercentageChanged(dropPercentage: Float) {
        if (!dataManager.isPurrSoundEnabled) {
            return
        }

        // Reset stage if user scrolls backwards or curtain retracts below 40%
        if (dropPercentage < 0.40f) {
            lastPlayedStage = 0
            return
        }

        // Stage 1: Halfway down (50% to 74%)
        if (dropPercentage in 0.50f..0.749f) {
            if (lastPlayedStage < 1) {
                lastPlayedStage = 1
                playNormalPurr()
            }
        }
        // Stage 2: 75% down and beyond
        else if (dropPercentage >= 0.75f) {
            if (lastPlayedStage < 2) {
                lastPlayedStage = 2
                playAngryPurr()
            }
        }
    }

    fun playNormalPurr(volume: Float = 0.9f) {
        stopCurrentSound()
        try {
            val pool = soundPool ?: return
            if (normalPurrLoaded && normalPurrId != 0) {
                activeStreamId = pool.play(normalPurrId, volume, volume, 1, 0, 1.0f)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed playing normal purr: ${e.message}")
        }
    }

    fun playAngryPurr(volume: Float = 0.95f) {
        stopCurrentSound()
        try {
            val pool = soundPool ?: return
            if (angryPurrLoaded && angryPurrId != 0) {
                activeStreamId = pool.play(angryPurrId, volume, volume, 1, 0, 1.0f)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed playing angry purr: ${e.message}")
        }
    }

    fun stopCurrentSound() {
        try {
            if (activeStreamId != 0) {
                soundPool?.stop(activeStreamId)
                activeStreamId = 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping sound: ${e.message}")
        }
    }

    fun resetStages() {
        lastPlayedStage = 0
        stopCurrentSound()
    }
}
