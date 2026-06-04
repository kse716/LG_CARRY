package com.example.carry.voice

data class ParsedVoiceCommand(
    val label: String,
    val intent: String,
    val targetTrayId: String?,
    val displayMessage: String,
    val confidence: Float = 1.0f
)
