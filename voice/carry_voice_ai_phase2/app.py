# app.py
# Carry Voice Intent API
# 사용법:
#   pip install flask joblib scikit-learn pandas
#   python app.py
# 요청:
#   curl -X POST http://127.0.0.1:5000/api/ai/voice-intent ^
#     -H "Content-Type: application/json" ^
#     -d "{\"text\":\"캐리야 기저귀 가져와\"}"

from pathlib import Path
import json
import joblib
from flask import Flask, request, jsonify

MODEL_PATH = Path("model_output/carry_voice_command_model.pkl")
MAPPING_PATH = Path("command_mapping.json")

app = Flask(__name__)

model = joblib.load(MODEL_PATH)
mapping = json.loads(MAPPING_PATH.read_text(encoding="utf-8"))
threshold = float(mapping.get("confidence_threshold", 0.70))


@app.post("/api/ai/voice-intent")
def voice_intent():
    body = request.get_json(silent=True) or {}
    text = str(body.get("text", "")).strip()

    if not text:
        return jsonify({
            "text": text,
            "label": "UNKNOWN",
            "confidence": 0.0,
            "intent": "UNKNOWN",
            "targetTrayId": None,
            "accepted": False,
            "message": "음성 인식 결과가 비어 있습니다."
        }), 400

    probs = model.predict_proba([text])[0]
    idx = probs.argmax()
    label = str(model.classes_[idx])
    confidence = float(probs[idx])

    if confidence < threshold:
        command = mapping["labels"]["UNKNOWN"]
        return jsonify({
            "text": text,
            "label": label,
            "confidence": round(confidence, 4),
            "intent": "UNKNOWN",
            "targetTrayId": None,
            "accepted": False,
            "message": "명령이 불확실합니다. 다시 말씀해주세요."
        })

    command = mapping["labels"].get(label, mapping["labels"]["UNKNOWN"])
    accepted = label != "UNKNOWN"

    return jsonify({
        "text": text,
        "label": label,
        "confidence": round(confidence, 4),
        "intent": command["intent"],
        "targetTrayId": command["targetTrayId"],
        "requiresConfirm": command["requiresConfirm"],
        "confirmText": command["confirmText"],
        "accepted": accepted,
        "message": "명령 후보가 생성되었습니다." if accepted else "지원하지 않는 명령입니다."
    })


@app.get("/health")
def health():
    return jsonify({"status": "ok"})


@app.get("/ping")
def ping():
    return "CARRY voice server OK", 200, {"Content-Type": "text/plain; charset=utf-8"}


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
