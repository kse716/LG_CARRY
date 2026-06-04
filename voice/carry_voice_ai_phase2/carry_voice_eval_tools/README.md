# Carry Voice AI 평가 코드

## 0. 전제

터미널 1에서 Flask API 서버가 실행 중이어야 한다.

```powershell
python app.py
```

## 1. 테스트셋으로 API 일괄 평가

새 PowerShell 터미널에서 실행한다.

```powershell
python evaluate_voice_api.py --cases carry_voice_test_cases.csv --threshold 0.70
```

생성 파일:

```text
voice_api_eval_results.csv
voice_api_failed_cases_for_training.csv
```

## 2. 실패 문장만 학습 CSV에 병합

```powershell
python merge_failed_cases.py --base carry_voice_commands_seed_expanded.csv --failures voice_api_failed_cases_for_training.csv --out carry_voice_commands_seed_next.csv
```

## 3. 모델 재학습

```powershell
python train_carry_voice_classifier.py --data carry_voice_commands_seed_next.csv
```

## 4. 서버 재시작

기존 `python app.py` 터미널에서 `Ctrl + C`로 종료 후 다시 실행한다.

```powershell
python app.py
```

## 5. 같은 테스트셋으로 재평가

```powershell
python evaluate_voice_api.py --cases carry_voice_test_cases.csv --threshold 0.70 --out voice_api_eval_results_round2.csv --fail-out voice_api_failed_cases_round2.csv
```
