# Carry Voice AI Integration 프로젝트 ('voice' 폴더)

이 폴더는 Carry 로봇의 음성 인식(STT) 및 2차 AI 명령 분류 모델(TF-IDF + Logistic Regression) 연동 코드가 포함된 독립된 안드로이드 프로젝트와 로컬 Flask AI API 서버 폴더입니다.

기존 레포지토리의 파일들과 완전히 분리되어 `voice/` 하위 폴더에만 존재하므로, 기존 작업 내용을 엉키게 하지 않고 정상적으로 빌드 및 테스트를 수행할 수 있습니다.

---

## 타 팀원의 Pull 및 로컬 테스트 방법 가이드

다른 팀원분이 이 내용을 Pull 받아 테스트하려면 다음 순서대로 로컬 환경을 세팅해야 합니다.

### 1. 최신 코드 Pull 받기
로컬 저장소에서 최신 변경 사항을 Pull 받습니다.
```bash
git pull origin main
```
모든 코드와 모델 설정은 `voice` 폴더 밑에 다운로드됩니다.

### 2. 안드로이드 서명 키 복원
보안상 실제 키스토어(`debug.keystore`)는 깃허브 업로드에서 제외(Git Ignore)되었으며, 대신 Base64 인코딩된 파일이 제공됩니다. 빌드 전 반드시 아래 명령어로 디코딩하여 복원해야 빌드가 작동합니다.

* **Windows (PowerShell)에서 실행:**
  ```powershell
  # 'voice' 폴더 내에서 실행
  $base64 = Get-Content -Path debug.keystore.base64 -Raw
  $bytes = [System.Convert]::FromBase64String($base64.Trim())
  [System.IO.File]::WriteAllBytes("debug.keystore", $bytes)
  ```
* **Mac / Linux (Terminal)에서 실행:**
  ```bash
  # 'voice' 폴더 내에서 실행
  base64 -d debug.keystore.base64 > debug.keystore
  ```

### 3. 로컬 Flask AI API 서버 가상환경 세팅 및 기동
서버 구동을 위해 Python이 설치되어 있어야 합니다.

1. **AI 폴더로 이동:**
   ```bash
   cd voice/carry_voice_ai_phase2
   ```
2. **가상환경 생성:**
   ```bash
   python -m venv .venv
   ```
3. **가상환경 활성화 및 패키지 설치:**
   * **Windows:**
     ```powershell
     .venv\Scripts\pip install flask joblib scikit-learn pandas matplotlib
     ```
   * **Mac / Linux:**
     ```bash
     source .venv/bin/activate
     pip install flask joblib scikit-learn pandas matplotlib
     ```
4. **Flask 서버 실행:**
   ```bash
   # Windows
   .venv\Scripts\python app.py
   # Mac/Linux
   python app.py
   ```
   * 서버가 기동되면 `http://0.0.0.0:5000/`에서 대기합니다.

### 4. 접속 IP 주소 및 API Key 설정
로컬 PC에서 작동하는 Flask 서버와 스마트폰/에뮬레이터 간의 주소를 맞춰주어야 합니다.

1. **접속 주소 설정 수정:**
   * **[RetrofitClient.kt](app/src/main/java/com/example/carry/network/RetrofitClient.kt)** 파일을 엽니다.
   * `LOCAL_API_BASE_URL` 값을 본인 환경에 맞춰 수정합니다:
     * **안드로이드 에뮬레이터**로 테스트하는 경우: `"http://10.0.2.2:5000/"`
     * **실제 스마트폰**을 USB 연결하여 테스트하는 경우: `"http://본인_PC_로컬_IP:5000/"` *(PC의 로컬 IP는 cmd/터미널에서 `ipconfig`로 확인)*
2. **.env 파일 생성 (선택 사항):**
   * 서버 사이드 Gemini API 직접 호출을 활성화하려는 경우, `voice/.env.example` 파일을 복사하여 `voice/.env` 파일을 만들고 본인의 `GEMINI_API_KEY`를 입력합니다.
