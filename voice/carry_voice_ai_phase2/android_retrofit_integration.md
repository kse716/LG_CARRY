# Android 연동 메모

## Retrofit API Interface 예시

```kotlin
interface VoiceIntentApi {
    @POST("/api/ai/voice-intent")
    suspend fun classifyVoiceIntent(
        @Body request: VoiceIntentRequest
    ): VoiceIntentResponse
}

data class VoiceIntentRequest(
    val text: String
)

data class VoiceIntentResponse(
    val text: String,
    val label: String,
    val confidence: Double,
    val intent: String,
    val targetTrayId: String?,
    val requiresConfirm: Boolean?,
    val confirmText: String?,
    val accepted: Boolean,
    val message: String
)
```

## STT 결과 수신 후 처리 흐름

```kotlin
suspend fun onSpeechResult(sttText: String) {
    val response = voiceIntentApi.classifyVoiceIntent(
        VoiceIntentRequest(text = sttText)
    )

    // 1. STT 결과 영역
    sttResultTextView.text = response.text

    // 2. 임시 분류 결과 영역
    intentResultTextView.text =
        "${response.label} / confidence=${"%.2f".format(response.confidence)}"

    // 3. 실행 명령 영역
    commandResultTextView.text =
        "${response.intent} / ${response.targetTrayId ?: "null"}"

    if (!response.accepted) {
        showToast(response.message)
        return
    }

    when (response.intent) {
        "CALL_TRAY", "RETURN_HOME" -> {
            showConfirmDialog(
                message = response.confirmText ?: "명령을 실행할까요?",
                onConfirm = { sendVoiceCommandToBackend(response) }
            )
        }
        "STOP", "STATUS_CHECK" -> {
            sendVoiceCommandToBackend(response)
        }
        else -> {
            showToast("지원하지 않는 명령입니다.")
        }
    }
}
```

## Android 에뮬레이터/실기기 URL 주의

- Android Emulator에서 PC localhost Flask 호출: `http://10.0.2.2:5000`
- 실제 Android 기기에서 PC Flask 호출: 같은 Wi-Fi에 연결 후 `http://PC_IP:5000`
- 배포/시연 시에는 Backend 서버 주소로 변경
