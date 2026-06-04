package com.example.carry.voice

object VoiceCommandParser {

    fun parse(rawText: String): ParsedVoiceCommand {
        val text = normalize(rawText)

        return when {
            containsAny(text, listOf("멈춰", "정지", "그만", "중지")) -> {
                ParsedVoiceCommand(
                    label = "STOP",
                    intent = "STOP",
                    targetTrayId = null,
                    displayMessage = "Carry를 정지합니다."
                )
            }

            containsAny(text, listOf("돌아가", "복귀", "제자리", "충전기", "홈")) -> {
                ParsedVoiceCommand(
                    label = "RETURN_HOME",
                    intent = "RETURN_HOME",
                    targetTrayId = null,
                    displayMessage = "Carry를 복귀시킬까요?"
                )
            }

            containsAny(text, listOf("기저귀", "물티슈", "육아", "아기", "아이", "등원")) -> {
                ParsedVoiceCommand(
                    label = "CALL_TRAY_BABY",
                    intent = "CALL_TRAY",
                    targetTrayId = "TRAY_BABY",
                    displayMessage = "육아 트레이를 호출할까요?"
                )
            }

            containsAny(text, listOf("약", "복약", "비타민", "영양제", "알약")) -> {
                ParsedVoiceCommand(
                    label = "CALL_TRAY_MEDICINE",
                    intent = "CALL_TRAY",
                    targetTrayId = "TRAY_MEDICINE",
                    displayMessage = "복약 트레이를 호출할까요?"
                )
            }

            containsAny(text, listOf("취미", "리모컨", "공구", "게임", "책상")) -> {
                ParsedVoiceCommand(
                    label = "CALL_TRAY_HOBBY",
                    intent = "CALL_TRAY",
                    targetTrayId = "TRAY_HOBBY",
                    displayMessage = "취미 트레이를 호출할까요?"
                )
            }

            containsAny(text, listOf("상태", "배터리", "어디", "뭐해", "연결")) -> {
                ParsedVoiceCommand(
                    label = "STATUS_CHECK",
                    intent = "STATUS_CHECK",
                    targetTrayId = null,
                    displayMessage = "Carry 상태를 확인할까요?"
                )
            }

            else -> {
                ParsedVoiceCommand(
                    label = "UNKNOWN",
                    intent = "UNKNOWN",
                    targetTrayId = null,
                    displayMessage = "명령을 이해하지 못했습니다.",
                    confidence = 0.0f
                )
            }
        }
    }

    private fun normalize(text: String): String {
        return text
            .lowercase()
            .replace(" ", "")
            .replace(".", "")
            .replace(",", "")
            .replace("캐리야", "")
            .replace("캐리", "")
            .replace("carry", "")
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { keyword -> text.contains(keyword) }
    }
}
