package com.andvl1.engrade.platform

import android.content.Context
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class SoundManager(private val context: Context) {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 200)

    private val ringtone: Ringtone? by lazy {
        var alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        if (alertUri == null) {
            alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
        if (alertUri == null) {
            alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        }
        alertUri?.let { RingtoneManager.getRingtone(context, it) }
    }

    /**
     * Short vibration for timer start
     */
    fun vibrateStart() {
        vibrate(200)
    }

    /**
     * Long vibration for timer end
     */
    fun vibrateEnd() {
        vibrate(3000)
    }

    /**
     * Play short beep tone
     */
    fun playBeep() {
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_SIGNAL_OFF, 100)
    }

    /**
     * Play alarm ringtone
     */
    fun playAlarm() {
        ringtone?.play()
    }

    /**
     * Stop all sounds
     */
    fun stopAll() {
        toneGenerator.stopTone()
        ringtone?.stop()
        vibrator.cancel()
    }

    private fun vibrate(durationMs: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

    fun release() {
        stopAll()
        toneGenerator.release()
    }
}
