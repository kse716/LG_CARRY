# Carry Voice AI 2차 모델 작업 정리 및 Antigravity 연동 지시서

작성일: 2026-06-03  
대상 기능: Carry Android 앱 음성 명령 AI 분류 기능  
현재 상태: 1차 MVP 음성 인식 완료 → 2차 AI 명령 분류 모델 학습 및 API 테스트 완료 → 기존 Android 앱 연동 준비

---

## 1. 현재 작업 목적

기존 Carry Voice Android 앱은 아래 기능까지 완료된 상태다.

```text
사용자 음성 입력
→ Android SpeechRecognizer
→ STT 텍스트 변환
→ 앱 화면에 STT 결과 표시
```

이번 2차 작업의 목표는 STT 결과 텍스트를 직접 학습한 AI 명령 분류 모델에 전달하여, 사용자의 발화가 어떤 Carry 명령인지 판별하는 것이다.

최종 목표 구조는 아래와 같다.

```text
Android SpeechRecognizer
→ STT 텍스트
→ Retrofit으로 Flask AI API 호출
→ Carry Voice Command Classifier
→ label / confidence / accepted 반환
→ 앱 화면에 AI 분류 결과 표시
→ 이후 Backend / Carry Device 명령 전송
```

---

## 2. 현재 Antigravity 작업 폴더 상태

현재 모델 작업 폴더를 Antigravity 작업 폴더에 복사해 둔 상태다.

```text
C:\Users\JangJiseok\antigravity\Carry-Voice-2026-06-03-38aa3\carry_voice_ai_phase2
```

이 폴더는 기존 Android 앱에 AI 모델을 붙이기 위한 로컬 Flask AI API 서버 및 학습 코드가 들어 있는 폴더다.

예상 구조는 다음과 같다.

```text
Carry-Voice-2026-06-03-38aa3/
  Android 앱 프로젝트 파일들
  carry_voice_ai_phase2/
    app.py
    train_carry_voice_classifier.py
    carry_voice_commands_seed.csv
    carry_voice_commands_seed_weighted.csv
    add_weights.py
    model_output/
      carry_voice_command_model.pkl
      classification_report.txt
      confusion_matrix.png
      sample_predictions.tsv
    carry_voice_eval_tools/
      evaluate_voice_api.py
      carry_voice_test_cases.csv
      merge_failed_cases.py
      voice_api_eval_results.csv
      voice_api_failed_cases_for_training.csv
```

---

## 3. 라벨 체계

현재 AI 모델은 STT 텍스트를 아래 7개 라벨 중 하나로 분류한다.

| 라벨 | 의미 | 실제 Carry Command |
|---|---|---|
| `CALL_TRAY_BABY` | 육아 관련 트레이 호출 | `CALL_TRAY / TRAY_BABY` |
| `CALL_TRAY_HOBBY` | 취미 관련 트레이 호출 | `CALL_TRAY / TRAY_HOBBY` |
| `CALL_TRAY_MEDICINE` | 노인/복약 관련 트레이 호출 | `CALL_TRAY / TRAY_MEDICINE` |
| `STOP` | Carry 정지 | `STOP` |
| `RETURN_HOME` | Carry 복귀 / 충전 위치 이동 | `RETURN_HOME` |
| `STATUS_CHECK` | Carry 상태 확인 | `STATUS_CHECK` |
| `UNKNOWN` | 지원하지 않는 명령 | 실행 안 함 |

---

## 4. 라벨별 핵심 키워드

초기 라벨링은 3개 트레이 중심으로 구성했다.

### 4.1 육아 관련

라벨: `CALL_TRAY_BABY`

키워드:

```text
기저귀, 젖병, 물티슈, 장난감, 손수건
```

예시 발화:

```text
캐리야 기저귀 가져와
물티슈 가져와
젖병 좀 챙겨줘
아이 장난감 불러줘
손수건 가져와
```

### 4.2 취미 관련

라벨: `CALL_TRAY_HOBBY`

키워드:

```text
게임패드, 충전기, 충전기 케이블, 액체괴물, 스케치북, 뜨개질
```

예시 발화:

```text
게임패드 가져와
충전 케이블 좀 갖다줘
액체 괴물 불러줘
스케치북 가져와
뜨개질 도구 가져와
```

### 4.3 노인/약 관련

라벨: `CALL_TRAY_MEDICINE`

키워드:

```text
약, 혈압약, 당뇨약, 통풍약, 영양제, 비타민, 한약, 안경, 돋보기
```

예시 발화:

```text
약 가져와
혈압 약 챙겨줘
당뇨 약 불러줘
비타민 챙겨줘
안경 가져와
돋보기 안경 갖다줘
```

---

## 5. 사용 모델 및 방법론

현재 사용 모델은 아래 구조다.

```text
TF-IDF char n-gram
+ Logistic Regression
```

### 5.1 TF-IDF char n-gram

한국어 짧은 명령어와 STT 오인식에 대응하기 위해 단어 단위가 아니라 문자 조각 기반으로 벡터화한다.

예:

```text
기저귀 가져와
→ 기저 / 저귀 / 가져 / 져와 / 기저귀 / 가져와 ...
```

이 방식은 아래와 같은 STT 오인식에 비교적 강하다.

```text
기저귀 → 기저기
젖병 → 젓병
게임패드 → 게임 패드 / 게임패트
액체괴물 → 액체 괴물
혈압약 → 혈압 약
```

### 5.2 Logistic Regression

로지스틱 회귀는 각 라벨에 속할 확률을 계산한다.

예:

```text
입력: 캐리야 기저귀 가져와

CALL_TRAY_BABY      0.9671
CALL_TRAY_HOBBY     0.0102
CALL_TRAY_MEDICINE  0.0081
UNKNOWN             0.0040
```

가장 높은 확률을 가진 라벨이 최종 예측 라벨이 된다.

---

## 6. 현재 학습 코드 핵심 설정

`train_carry_voice_classifier.py`는 다음 설정을 사용한다.

```python
TfidfVectorizer(
    analyzer="char_wb",
    ngram_range=(2, 5),
    min_df=1,
    sublinear_tf=True
)
```

```python
LogisticRegression(
    max_iter=3000,
    C=5.0,
    class_weight=None,
    solver="lbfgs"
)
```

핵심 포인트:

- `min_df=1`: 한 번 등장한 STT 오인식 문장도 학습에 반영
- `ngram_range=(2, 5)`: 짧은 한국어 명령어와 띄어쓰기 변형 대응
- `C=5.0`: 정규화를 완화해 hard case confidence 상승
- `sample_weight`: confidence가 낮은 문장에 가중치 부여 가능

---

## 7. 가중치 적용 작업

일부 문장은 라벨은 맞게 예측되지만 confidence가 threshold 0.70보다 낮게 나왔다.

이를 해결하기 위해 중복 데이터를 추가하는 대신 `sample_weight` 방식을 적용했다.

### 7.1 가중치 정책

| 데이터 유형 | weight |
|---|---:|
| 일반 학습 문장 | 1 |
| 실제 STT 오인식 문장 | 3 |
| confidence 낮은 hard case | 5 |
| 발표 시연 핵심 문장 | 5~7 |

현재 `add_weights.py` 실행 결과:

```text
saved: carry_voice_commands_seed_weighted.csv
weight
1    841
5      2
Name: count, dtype: int64
```

즉, 총 843개 문장 중 2개 문장에 `weight=5`가 적용된 상태다.

### 7.2 중요 수정 사항

기존 학습 코드에서는 `weight` 컬럼을 읽지 않았기 때문에 가중치가 실제 학습에 반영되지 않았다.

수정 후 학습 코드는 다음을 반영해야 한다.

```python
if "weight" not in df.columns:
    df["weight"] = 1.0

weights = df["weight"].fillna(1).astype(float)
```

그리고 Pipeline 구조이므로 학습 시 아래처럼 전달해야 한다.

```python
model.fit(X_train, y_train, clf__sample_weight=w_train)
```

최종 API 배포용 모델은 test split이 아니라 전체 데이터로 다시 학습한 뒤 저장해야 한다.

```python
final_model.fit(X, y, clf__sample_weight=weights)
joblib.dump(final_model, outdir / "carry_voice_command_model.pkl")
```

---

## 8. 모델 학습 및 평가 결과

### 8.1 학습 실행 명령

```powershell
cd "C:\Users\JangJiseok\antigravity\Carry-Voice-2026-06-03-38aa3\carry_voice_ai_phase2"
python train_carry_voice_classifier.py --data carry_voice_commands_seed_weighted.csv
```

실행 결과:

```text
Saved model and reports to: ...\carry_voice_ai_phase2\model_output
Accuracy: 0.9645
```

### 8.2 API 평가 실행 명령

Flask 서버 실행 상태에서 평가 도구 실행:

```powershell
cd "C:\Users\JangJiseok\antigravity\Carry-Voice-2026-06-03-38aa3\carry_voice_ai_phase2\carry_voice_eval_tools"
python evaluate_voice_api.py --cases carry_voice_test_cases.csv --threshold 0.70
```

### 8.3 평가 결과

```text
total=51
label_accuracy=51/51 = 100.00%
final_pass=49/51 = 96.08%
```

해석:

- 51개 테스트 문장 모두 라벨 분류는 정답
- threshold 0.70 기준 최종 통과는 49개
- 실패 2개도 라벨은 맞았으나 confidence가 0.70 미만

---

## 9. 테스트셋 주요 결과

| 문장 | 예측 라벨 | confidence | 통과 여부 |
|---|---|---:|---|
| 캐리야 기저귀 가져와 | `CALL_TRAY_BABY` | 0.9671 | 통과 |
| 캐리아 기저기 가져와 | `CALL_TRAY_BABY` | 0.8189 | 통과 |
| 케리야 물 티슈 갖다줘 | `CALL_TRAY_BABY` | 0.6989 | 실패 |
| 게임 패드 불러줘 | `CALL_TRAY_HOBBY` | 0.8680 | 통과 |
| 충전 케이블 좀 갖다줘 | `CALL_TRAY_HOBBY` | 0.8984 | 통과 |
| 혈압 약 챙겨줘 | `CALL_TRAY_MEDICINE` | 0.7803 | 통과 |
| 비타민 챙겨줘 | `CALL_TRAY_MEDICINE` | 0.6703 | 실패 |
| 그만해 | `STOP` | 0.7578 | 통과 |
| 스탑 | `STOP` | 0.7133 | 통과 |
| 충전 장소로 가 | `RETURN_HOME` | 0.7715 | 통과 |
| 캐리 상태 확인해줘 | `STATUS_CHECK` | 0.8222 | 통과 |
| 날씨 알려줘 | `UNKNOWN` | 0.8678 | 통과 |
| 커튼 닫아줘 | `UNKNOWN` | 0.6241 | 통과 |

---

## 10. 현재 AI 분류 단계 판정

현재 목표는 다음이었다.

```text
AI가 어떤 Carry 명령인지 제대로 분류하는지 확인
```

이 기준에서는 통과로 판단할 수 있다.

근거:

```text
라벨 분류 정확도: 100.00%
threshold 0.70 기준 최종 통과율: 96.08%
UNKNOWN 명령 차단 정상
CALL_TRAY_BABY / HOBBY / MEDICINE 분류 정상
STOP / RETURN_HOME / STATUS_CHECK 분류 정상
```

발표용 문장:

```text
대표 발화 및 STT 오인식 예상 문장 51개를 대상으로 Carry Voice Command Classifier를 평가한 결과, 라벨 분류 정확도는 100.00%, confidence threshold 0.70 기준 최종 실행 승인율은 96.08%로 확인되었다. 실패 2건은 모두 라벨 예측은 성공했으나 confidence가 기준값에 근소하게 미달한 사례였다.
```

---

## 11. Flask AI API 실행 방법

### 11.1 서버 실행

```powershell
cd "C:\Users\JangJiseok\antigravity\Carry-Voice-2026-06-03-38aa3\carry_voice_ai_phase2"
python app.py
```

### 11.2 app.py host 설정

기존 앱과 실제 Android 기기에서 테스트하려면 `app.py` 맨 아래는 아래처럼 되어 있어야 한다.

```python
app.run(host="0.0.0.0", port=5000, debug=True)
```

의미:

```text
127.0.0.1 → PC 내부에서만 접속 가능
0.0.0.0 → 같은 Wi-Fi의 Android 기기에서도 접속 가능
```

### 11.3 PC IP 확인

PowerShell에서:

```powershell
ipconfig
```

Wi-Fi IPv4 주소 확인:

```text
예: 192.168.0.15
```

Android 앱에서 사용할 BASE_URL:

```text
http://192.168.0.15:5000/
```

에뮬레이터라면:

```text
http://10.0.2.2:5000/
```

---

## 12. AI API 테스트 명령

PowerShell에서 단일 문장 테스트:

```powershell
Invoke-RestMethod -Uri "http://127.0.0.1:5000/api/ai/voice-intent" -Method POST -ContentType "application/json; charset=utf-8" -Body '{"text":"캐리야 기저귀 가져와"}'
```

실제 Android 기기에서 접근할 때는 `127.0.0.1`이 아니라 PC IP를 사용해야 한다.

```text
http://PC_IP:5000/api/ai/voice-intent
```

---

## 13. API 응답 형식

Flask AI API는 다음 형태의 JSON을 반환한다.

```json
{
  "text": "캐리야 기저귀 가져와",
  "label": "CALL_TRAY_BABY",
  "confidence": 0.9671,
  "accepted": true,
  "intent": "CALL_TRAY",
  "targetTrayId": "TRAY_BABY",
  "message": "명령을 인식했습니다."
}
```

필드 의미:

| 필드 | 의미 |
|---|---|
| `text` | 입력 STT 텍스트 |
| `label` | AI가 예측한 라벨 |
| `confidence` | 예측 확신도 |
| `accepted` | threshold 기준 명령 인정 여부 |
| `intent` | 실제 Carry 명령 |
| `targetTrayId` | 호출할 트레이 ID |
| `message` | 사용자 표시 메시지 |

---

## 14. Android 앱 연동 목표

기존 앱 화면에 다음 3개 영역이 있다고 가정한다.

```text
1. STT 결과
2. 임시 분류 결과(Intent)
3. 실행 명령(Carry Command)
```

연동 후 표시 예:

```text
STT 결과
캐리야 기저귀 가져와

임시 분류 결과
CALL_TRAY_BABY / confidence=0.9671

실행 명령
CALL_TRAY / TRAY_BABY
```

confidence가 낮은 경우:

```text
STT 결과
비타민 챙겨줘

임시 분류 결과
CALL_TRAY_MEDICINE / confidence=0.6703

실행 명령
명령이 불확실합니다. 다시 말씀해주세요.
```

---

## 15. Android Retrofit 추가 작업

### 15.1 Gradle dependency

`app/build.gradle` 또는 `build.gradle.kts`에 Retrofit 추가.

Groovy:

```gradle
dependencies {
    implementation 'com.squareup.retrofit2:retrofit:2.11.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.11.0'
}
```

Kotlin DSL:

```kotlin
dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
}
```

### 15.2 AndroidManifest 설정

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

`application` 태그에 추가:

```xml
<application
    android:usesCleartextTraffic="true"
    ... >
```

로컬 Flask 서버는 `http://`이므로 `usesCleartextTraffic="true"`가 필요하다.

---

## 16. Android API 모델 클래스

예시 파일명: `VoiceIntentModels.kt`

```kotlin
data class VoiceIntentRequest(
    val text: String
)

data class VoiceIntentResponse(
    val text: String,
    val label: String,
    val confidence: Double,
    val accepted: Boolean,
    val intent: String,
    val targetTrayId: String?,
    val message: String?
)
```

---

## 17. Retrofit API 인터페이스

예시 파일명: `VoiceIntentApi.kt`

```kotlin
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface VoiceIntentApi {
    @POST("api/ai/voice-intent")
    suspend fun predictIntent(
        @Body request: VoiceIntentRequest
    ): Response<VoiceIntentResponse>
}
```

---

## 18. Retrofit Client

예시 파일명: `RetrofitClient.kt`

실제 Android 기기 기준:

```kotlin
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://192.168.0.15:5000/"

    val voiceIntentApi: VoiceIntentApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VoiceIntentApi::class.java)
    }
}
```

에뮬레이터 기준:

```kotlin
private const val BASE_URL = "http://10.0.2.2:5000/"
```

주의:

```text
실제 기기에서는 127.0.0.1 사용 금지
127.0.0.1은 Android 기기 자기 자신을 의미함
```

---

## 19. STT 결과 후 AI API 호출 함수

`MainActivity.kt` 또는 기존 음성 인식 Activity에 추가.

```kotlin
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

private fun requestVoiceIntent(sttText: String) {
    lifecycleScope.launch {
        try {
            val response = RetrofitClient.voiceIntentApi.predictIntent(
                VoiceIntentRequest(text = sttText)
            )

            if (response.isSuccessful) {
                val result = response.body()

                if (result != null) {
                    showAiResult(result)
                } else {
                    showAiError("AI 응답이 비어 있습니다.")
                }
            } else {
                showAiError("AI API 오류: ${response.code()}")
            }
        } catch (e: Exception) {
            showAiError("AI 서버 연결 실패: ${e.message}")
        }
    }
}
```

---

## 20. AI 결과 표시 함수

TextView 이름은 기존 앱의 실제 변수명에 맞게 수정해야 한다.

```kotlin
private fun showAiResult(result: VoiceIntentResponse) {
    sttResultTextView.text = result.text

    intentResultTextView.text =
        "${result.label} / confidence=${String.format("%.4f", result.confidence)}"

    commandResultTextView.text = if (result.accepted) {
        if (result.targetTrayId != null) {
            "${result.intent} / ${result.targetTrayId}"
        } else {
            result.intent
        }
    } else {
        result.message ?: "명령이 불확실합니다. 다시 말씀해주세요."
    }
}

private fun showAiError(message: String) {
    intentResultTextView.text = "AI 연결 오류"
    commandResultTextView.text = message
}
```

---

## 21. 기존 SpeechRecognizer 콜백에 붙일 코드

기존 `onResults()` 내부에서 STT 결과를 얻은 뒤 아래 한 줄을 추가한다.

```kotlin
requestVoiceIntent(sttText)
```

예시:

```kotlin
override fun onResults(results: Bundle?) {
    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
    val sttText = matches?.firstOrNull() ?: return

    sttResultTextView.text = sttText

    // AI API 호출 추가
    requestVoiceIntent(sttText)
}
```

---

## 22. Antigravity 작업 지시 요약

Antigravity에서 기존 Android 앱에 다음 순서로 적용한다.

```text
1. carry_voice_ai_phase2 폴더는 Android 프로젝트 루트 옆에 유지
2. PC에서 carry_voice_ai_phase2/app.py 실행
3. app.py host를 0.0.0.0으로 설정
4. Android 프로젝트에 Retrofit dependency 추가
5. AndroidManifest에 INTERNET 권한 및 cleartext 허용 추가
6. VoiceIntentRequest / VoiceIntentResponse data class 추가
7. VoiceIntentApi 인터페이스 추가
8. RetrofitClient 추가
9. BASE_URL을 PC IPv4 주소로 설정
10. SpeechRecognizer onResults에서 STT 결과를 requestVoiceIntent(sttText)에 전달
11. 응답받은 label / confidence / accepted / intent / targetTrayId를 앱 화면에 표시
12. 앱으로 실제 음성 테스트 진행
```

---

## 23. 테스트 시나리오

앱에서 아래 문장을 실제 음성으로 발화해 테스트한다.

### 육아

```text
캐리야 기저귀 가져와
물티슈 가져와
젖병 가져와
장난감 가져와
손수건 가져와
```

기대 결과:

```text
CALL_TRAY_BABY / CALL_TRAY / TRAY_BABY
```

### 취미

```text
게임패드 가져와
충전기 가져와
충전 케이블 가져와
액체괴물 가져와
스케치북 가져와
뜨개질 가져와
```

기대 결과:

```text
CALL_TRAY_HOBBY / CALL_TRAY / TRAY_HOBBY
```

### 노인/약

```text
약 가져와
혈압약 가져와
비타민 챙겨줘
안경 가져와
돋보기 가져와
```

기대 결과:

```text
CALL_TRAY_MEDICINE / CALL_TRAY / TRAY_MEDICINE
```

### 제어

```text
멈춰
스탑
돌아가
충전 장소로 가
상태 알려줘
캐리 상태 확인해줘
```

기대 결과:

```text
STOP
RETURN_HOME
STATUS_CHECK
```

### UNKNOWN

```text
날씨 알려줘
음악 틀어줘
뉴스 알려줘
에어컨 켜줘
유튜브 틀어줘
커튼 닫아줘
```

기대 결과:

```text
UNKNOWN / accepted=false / 실행 안 함
```

---

## 24. 자주 발생할 수 있는 오류와 해결

### 24.1 Android에서 서버 연결 실패

원인:

```text
BASE_URL에 127.0.0.1 사용
Flask 서버 host가 127.0.0.1
PC와 Android가 같은 Wi-Fi가 아님
Windows 방화벽 차단
```

해결:

```text
BASE_URL을 PC IPv4 주소로 변경
app.py host=0.0.0.0 설정
PC와 Android 같은 Wi-Fi 연결
Windows Defender 방화벽에서 Python 허용
```

### 24.2 CLEARTEXT communication not permitted

해결:

```xml
<application
    android:usesCleartextTraffic="true">
```

### 24.3 404 Not Found

해결:

```kotlin
@POST("api/ai/voice-intent")
```

BASE_URL 끝에는 `/` 포함:

```kotlin
private const val BASE_URL = "http://192.168.0.15:5000/"
```

### 24.4 label은 맞는데 accepted=false

원인:

```text
confidence가 threshold보다 낮음
```

처리:

```text
AI 분류 결과는 화면에 표시
실행 명령은 차단
"명령이 불확실합니다. 다시 말씀해주세요." 표시
```

---

## 25. 다음 단계

현재는 AI 분류 결과를 앱에 붙여 테스트하는 단계다.

이 단계가 완료되면 다음으로 넘어간다.

```text
1. 앱에서 STT → AI API → label/confidence 표시 성공
2. accepted=True일 때 label을 Carry Command로 표시
3. CALL_TRAY 계열은 사용자 확인 팝업 추가
4. STOP은 즉시 실행 후보로 처리
5. Backend /api/voice-command 명세 확정
6. Android Retrofit으로 Backend 명령 전송
7. Backend → Carry Device / TurtleBot 제어 연결
```

---

## 26. 현재 결론

현재 모델은 대표 테스트셋 기준으로 다음 성능을 보였다.

```text
라벨 분류 정확도: 100.00%
threshold 0.70 기준 최종 통과율: 96.08%
```

따라서 AI 모델 자체 검증은 1차 통과로 보고, 이제는 기존 Android 앱과의 연결 테스트를 진행하는 것이 맞다.

Antigravity에서는 우선 Flask AI API를 로컬에서 실행한 뒤, 기존 앱의 SpeechRecognizer 결과를 Retrofit으로 `/api/ai/voice-intent`에 보내고, 반환된 label/confidence/accepted/intent/targetTrayId를 화면에 표시하는 작업을 수행한다.
