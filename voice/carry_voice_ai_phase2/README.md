# Carry Voice Assistant 2차 AI 버전 초기 산출물

## 포함 파일

| 파일 | 용도 |
|---|---|
| `carry_voice_commands_seed.csv` | 7개 라벨 기반 초기 학습 데이터셋 |
| `command_mapping.json` | AI label → Carry Command 변환 규칙 |
| `train_carry_voice_classifier.py` | TF-IDF char n-gram + Logistic Regression 학습 스크립트 |
| `app.py` | Flask 기반 `/api/ai/voice-intent` 추론 API |
| `android_retrofit_integration.md` | Android Retrofit 연동 예시 |

## 현재 기준 라벨

| Label | intent | targetTrayId |
|---|---|---|
| CALL_TRAY_BABY | CALL_TRAY | TRAY_BABY |
| CALL_TRAY_MEDICINE | CALL_TRAY | TRAY_MEDICINE |
| CALL_TRAY_HOBBY | CALL_TRAY | TRAY_HOBBY |
| STOP | STOP | null |
| RETURN_HOME | RETURN_HOME | null |
| STATUS_CHECK | STATUS_CHECK | null |
| UNKNOWN | UNKNOWN | null |

## 실행 순서

```bash
cd carry_voice_ai_phase2
pip install pandas scikit-learn matplotlib joblib flask
python train_carry_voice_classifier.py --data carry_voice_commands_seed.csv
python app.py
```

## API 테스트

```bash
curl -X POST http://127.0.0.1:5000/api/ai/voice-intent \
  -H "Content-Type: application/json" \
  -d '{"text":"캐리야 기저귀 가져와"}'
```

## 다음 검수 포인트

1. Android SpeechRecognizer 실제 STT 결과를 CSV에 추가한다.
2. 라벨별 오인식 문장을 정답 라벨과 함께 저장한다.
3. `UNKNOWN`에는 실제로 실행하면 안 되는 문장을 충분히 넣는다.
4. 테스트셋 기준 classification report와 confusion matrix를 발표 자료에 넣는다.
5. confidence 0.70 기준이 너무 엄격하거나 느슨하면 0.60~0.80 범위에서 조정한다.
