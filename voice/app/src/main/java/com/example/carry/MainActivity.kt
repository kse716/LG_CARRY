package com.example.carry

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.carry.voice.VoiceCommandParser
import com.example.carry.voice.VoiceRecognizerManager
import com.example.ui.theme.MyApplicationTheme
import com.example.carry.network.RetrofitClient
import com.example.carry.network.VoiceIntentRequest
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var voiceRecognizerManager: VoiceRecognizerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    CarryVoiceAppScreen(
                        modifier = Modifier.padding(innerPadding),
                        onInitRecognizer = { onReady, onResult, onError ->
                            voiceRecognizerManager = VoiceRecognizerManager(
                                context = this,
                                onReady = onReady,
                                onResult = onResult,
                                onError = onError
                            )
                        },
                        onStartListening = {
                            voiceRecognizerManager.startListening()
                        },
                        onDestroyRecognizer = {
                            if (::voiceRecognizerManager.isInitialized) {
                                voiceRecognizerManager.destroy()
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarryVoiceAppScreen(
    modifier: Modifier = Modifier,
    onInitRecognizer: (onReady: () -> Unit, onResult: (String) -> Unit, onError: (String) -> Unit) -> Unit,
    onStartListening: () -> Unit,
    onDestroyRecognizer: () -> Unit
) {
    val context = LocalContext.current

    // State Variables
    var statusState by remember { mutableStateOf("대기 중") }
    var recognizedTextState by remember { mutableStateOf("-") }
    var intentState by remember { mutableStateOf("-") }
    var commandState by remember { mutableStateOf("-") }
    var isListening by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val requestVoiceIntent = { text: String ->
        statusState = "분석 중..."
        recognizedTextState = text
        coroutineScope.launch {
            try {
                val response = RetrofitClient.voiceIntentApi.predictIntent(
                    VoiceIntentRequest(text = text)
                )
                if (response.isSuccessful) {
                    val result = response.body()
                    if (result != null) {
                        intentState = "${result.label} / confidence=${String.format(java.util.Locale.US, "%.4f", result.confidence)}"
                        commandState = if (result.accepted) {
                            if (result.targetTrayId != null) {
                                "${result.intent}, target=${result.targetTrayId}"
                            } else {
                                result.intent
                            }
                        } else {
                            result.message ?: "명령이 불확실합니다. 다시 말씀해주세요."
                        }
                        statusState = "이해 완료"
                        Toast.makeText(context, result.message ?: "명령을 인식했습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        statusState = "AI 응답 오류"
                        intentState = "오류"
                        commandState = "AI 응답이 비어 있습니다."
                    }
                } else {
                    statusState = "AI 서버 오류"
                    intentState = "오류: ${response.code()}"
                    commandState = "AI API 오류: ${response.code()}"
                }
            } catch (e: Exception) {
                statusState = "AI 서버 연결 실패"
                intentState = "오류"
                commandState = "AI 서버 연결 실패: ${e.message}"
            }
        }
    }

    // Init speech recognizer
    DisposableEffect(Unit) {
        onInitRecognizer(
            {
                statusState = "듣는 중..."
                isListening = true
            },
            { text ->
                isListening = false
                requestVoiceIntent(text)
            },
            { errorMsg ->
                statusState = errorMsg
                isListening = false
            }
        )
        onDispose {
            onDestroyRecognizer()
        }
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            statusState = "음성 인식 시작"
            onStartListening()
        } else {
            Toast.makeText(context, "마이크 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper functions
    val checkAndStartSpeech = {
        val permissionCheck = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        )
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            statusState = "음성 인식 시작"
            onStartListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Quick Test simulator
    val simulateSpeech = { text: String ->
        requestVoiceIntent(text)
    }

    // Pulse animation logic for microphone pulse wave helper
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                )
            )
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "App Logo Mic",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Carry Voice Assistant",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "로봇 지원 음성 인식 및 분류 시스템",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tvStatusCard"),
            colors = CardDefaults.cardColors(
                containerColor = if (isListening) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "음성 제어 상태",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val statusIcon = when {
                        isListening -> Icons.Default.Refresh
                        statusState.contains("오류") -> Icons.Default.Warning
                        statusState == "완료" || statusState == "이해 완료" -> Icons.Default.CheckCircle
                        else -> Icons.Default.Info
                    }
                    val statusColor = when {
                        isListening -> MaterialTheme.colorScheme.secondary
                        statusState.contains("오류") -> MaterialTheme.colorScheme.error
                        statusState == "완료" || statusState == "이해 완료" -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Icon(
                        imageVector = statusIcon,
                        contentDescription = "Status Icon",
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "상태: $statusState",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor,
                        modifier = Modifier.testTag("tvStatus")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Large Accent Recording Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(140.dp)
                .scale(pulseScale)
        ) {
            // Outer glowing boundary rings
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                if (isListening) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // The main interactive button
            Button(
                onClick = { checkAndStartSpeech() },
                modifier = Modifier
                    .size(100.dp)
                    .testTag("btnVoice"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    contentColor = if (isListening) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Mic Voice Recognition Start",
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isListening) "듣는 중" else "말하기",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "마이크를 눌러 음성 명령을 시작하거나 아래 시뮬레이터를 사용해 주세요.",
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Recognized Results Section
        Text(
            text = "실시간 분석 리포트",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // STT Result
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "1. STT 결과 (인식 결과)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = recognizedTextState,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .testTag("tvRecognizedText")
                        )
                    }
                }

                // Intent Decision
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "2. 임시 분류 결과 (Intent)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = intentState,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .testTag("tvIntent")
                        )
                    }
                }

                // Command Result
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "3. 실행 명령 (Carry Command)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = commandState,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .testTag("tvCommand")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Simulator / Quick speech trigger section
        Text(
            text = "테스트 발화 시뮬레이터 (마이크 입력 우회)",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val testUtterances = listOf(
                    "캐리야 기저귀 가져와" to "CALL_TRAY_BABY",
                    "캐리야 약 가져와" to "CALL_TRAY_MEDICINE",
                    "캐리야 멈춰" to "STOP",
                    "캐리야 돌아가" to "RETURN_HOME",
                    "상태 알려줘" to "STATUS_CHECK"
                )

                testUtterances.forEach { (utterance, expectedLabel) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { simulateSpeech(utterance) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Simulate Play",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = utterance,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = expectedLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
