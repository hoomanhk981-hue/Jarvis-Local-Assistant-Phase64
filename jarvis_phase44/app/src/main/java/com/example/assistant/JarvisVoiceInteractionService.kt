package com.example.assistant

import android.content.Intent
import android.service.voice.VoiceInteractionService

/**
 * System-managed entry point for Android's default ASSISTANT role.
 * Keep this service lightweight: Android may keep it alive while Jarvis is selected.
 */
class JarvisVoiceInteractionService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
    }

    override fun onShutdown() {
        super.onShutdown()
    }

    override fun onLaunchVoiceAssistFromKeyguard() {
        super.onLaunchVoiceAssistFromKeyguard()
        launchJarvisVoiceCommand()
    }

    private fun launchJarvisVoiceCommand() {
        val intent = Intent(this, com.example.MainActivity::class.java).apply {
            action = Intent.ACTION_VOICE_COMMAND
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }
}
