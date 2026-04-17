package com.marconius.whackabraille.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class AndroidSpeechEngine(
    context: Context,
) : TextToSpeech.OnInitListener {

    private var isReady = false
    private var pendingText: String? = null
    private val textToSpeech = TextToSpeech(context.applicationContext, this)

    override fun onInit(status: Int) {
        isReady = status == TextToSpeech.SUCCESS
        if (!isReady) return

        textToSpeech.language = Locale.getDefault()
        textToSpeech.setSpeechRate(1.0f)
        textToSpeech.setPitch(1.0f)

        pendingText?.let {
            pendingText = null
            speak(it)
        }
    }

    fun speak(text: String, interrupt: Boolean = true) {
        val normalized = text.trim()
        if (normalized.isEmpty()) return

        if (!isReady) {
            pendingText = normalized
            return
        }

        if (interrupt) {
            textToSpeech.stop()
        }

        textToSpeech.speak(normalized, TextToSpeech.QUEUE_FLUSH, null, "wab_announcement")
    }

    fun stop() {
        if (isReady) {
            textToSpeech.stop()
        }
    }

    fun shutdown() {
        textToSpeech.stop()
        textToSpeech.shutdown()
    }
}
