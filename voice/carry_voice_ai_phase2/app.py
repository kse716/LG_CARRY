from pathlib import Path
import json
import joblib
import re
from flask import Flask, request, jsonify


MODEL_PATH = Path("model_output/carry_voice_command_model.pkl")
MAPPING_PATH = Path("command_mapping.json")

app = Flask(__name__)

model = joblib.load(MODEL_PATH)
mapping = json.loads(MAPPING_PATH.read_text(encoding="utf-8"))
threshold = float(mapping.get("confidence_threshold", 0.70))

PARKING_KEYWORDS = (
    "주차",
    "충전소",
    "충전 위치",
    "충전위치",
    "대기 위치",
    "대기위치",
    "스테이션",
    "station",
    "자리로",
    "제자리",
    "복귀",
    "돌아가",
)


def is_parking_command(text):
    normalized = re.sub(r"\s+", "", str(text)).lower()
    return any(re.sub(r"\s+", "", keyword).lower() in normalized for keyword in PARKING_KEYWORDS)

LOCATION_PHRASES = (
    "\uc548\ubc29 \ucabd\uc73c\ub85c",
    "\uc548\ubc29\ucabd\uc73c\ub85c",
    "\uc548\ubc29\uc73c\ub85c",
    "\uc548\ubc29\uae4c\uc9c0",
    "\uc548\ubc29\uc5d0",
    "\uce68\uc2e4 \ucabd\uc73c\ub85c",
    "\uce68\uc2e4\uc73c\ub85c",
    "\uce68\uc2e4\uae4c\uc9c0",
    "\uce68\uc2e4\uc5d0",
    "\uc544\uc774\ubc29 \ucabd\uc73c\ub85c",
    "\uc544\uc774\ubc29\ucabd\uc73c\ub85c",
    "\uc544\uc774\ubc29\uc73c\ub85c",
    "\uc544\uc774\ubc29\uae4c\uc9c0",
    "\uc544\uc774\ubc29\uc5d0",
    "\uc544\uae30\ubc29 \ucabd\uc73c\ub85c",
    "\uc544\uae30\ubc29\uc73c\ub85c",
    "\uc544\uae30\ubc29\uae4c\uc9c0",
    "\uc544\uae30\ubc29\uc5d0",
    "\ud604\uad00 \ucabd\uc73c\ub85c",
    "\ud604\uad00\ucabd\uc73c\ub85c",
    "\ud604\uad00\uc73c\ub85c",
    "\ud604\uad00\uae4c\uc9c0",
    "\ud604\uad00 \uc55e\uc5d0",
    "\ud604\uad00\uc55e\uc5d0",
    "\ud604\uad00\uc5d0",
    "\uac70\uc2e4 \ucabd\uc73c\ub85c",
    "\uac70\uc2e4\ucabd\uc73c\ub85c",
    "\uac70\uc2e4\ub85c",
    "\uac70\uc2e4\uae4c\uc9c0",
    "\uac70\uc2e4\uc5d0",
)


def strip_location_for_command(text):
    cleaned = str(text)
    for phrase in LOCATION_PHRASES:
        cleaned = cleaned.replace(phrase, " ")
    return re.sub(r"\s+", " ", cleaned).strip()


def infer_label_location(text):
    normalized = text.replace(" ", "").lower()
    master_bedroom_keywords = (
        "\uc548\ubc29",
        "\uce68\uc2e4",
        "\ubd80\ubaa8\ubc29",
        "\uba54\uc778\ub8f8",
        "masterbedroom",
        "masterroom",
    )
    child_room_keywords = (
        "\uc544\uc774\ubc29",
        "\uc544\uae30\ubc29",
        "\uc790\ub140\ubc29",
        "\uc720\uc544\ubc29",
        "\ud0a4\uc988\ub8f8",
        "childroom",
        "kidsroom",
        "babyroom",
    )
    porch_keywords = (
        "\ud604\uad00",
        "\uc785\uad6c",
        "\ubb38\uc55e",
        "\ubb38\uac00",
    )
    living_room_keywords = (
        "\uac70\uc2e4",
        "\uc18c\ud30c",
        "\uc1fc\ud30c",
        "tv",
        "\ud2f0\ube44",
    )

    if any(keyword in normalized for keyword in master_bedroom_keywords):
        return "master_bedroom"
    if any(keyword in normalized for keyword in child_room_keywords):
        return "child_room"
    if any(keyword in normalized for keyword in porch_keywords):
        return "porch"
    if any(keyword in normalized for keyword in living_room_keywords):
        return "living_room"
    return None


@app.post("/api/ai/voice-intent")
def voice_intent():
    body = request.get_json(silent=True) or {}
    text = str(body.get("text", "")).strip()

    if not text:
        return jsonify({
            "text": text,
            "label": "UNKNOWN",
            "label_location": None,
            "confidence": 0.0,
            "intent": "UNKNOWN",
            "targetTrayId": None,
            "requiresConfirm": False,
            "confirmText": None,
            "accepted": False,
            "message": "\uc74c\uc131 \uc778\uc2dd \uacb0\uacfc\uac00 \ube44\uc5b4 \uc788\uc2b5\ub2c8\ub2e4."
        }), 400

    if is_parking_command(text):
        return jsonify({
            "ok": True,
            "text": text,
            "mission": 7,
            "label": "PARKING",
            "label_location": "station",
            "confidence": 1.0,
            "intent": "RETURN_HOME",
            "targetTrayId": None,
            "requiresConfirm": True,
            "confirmText": "주차 위치로 복귀할까요?",
            "accepted": True,
            "message": "주차 미션 후보가 생성되었습니다."
        })

    command_text = strip_location_for_command(text)
    probs = model.predict_proba([command_text])[0]
    idx = probs.argmax()
    label = str(model.classes_[idx])
    confidence = float(probs[idx])
    label_location = infer_label_location(text)

    if confidence < threshold:
        return jsonify({
            "text": text,
            "label": label,
            "label_location": label_location,
            "confidence": round(confidence, 4),
            "intent": "UNKNOWN",
            "targetTrayId": None,
            "requiresConfirm": False,
            "confirmText": None,
            "accepted": False,
            "message": "\uba85\ub839\uc774 \ubd88\ud655\uc2e4\ud569\ub2c8\ub2e4. \ub2e4\uc2dc \ub9d0\uc500\ud574\uc8fc\uc138\uc694."
        })

    command = mapping["labels"].get(label, mapping["labels"]["UNKNOWN"])
    accepted = command["intent"] != "UNKNOWN"
    message = "\uba85\ub839 \ud6c4\ubcf4\uac00 \uc0dd\uc131\ub418\uc5c8\uc2b5\ub2c8\ub2e4." if accepted else "\uc9c0\uc6d0\ud558\uc9c0 \uc54a\ub294 \uba85\ub839\uc785\ub2c8\ub2e4."
    if accepted and not label_location:
        message = "\uc5b4\ub514\ub85c \uac00\uc838\uac08\uae4c\uc694"

    return jsonify({
        "text": text,
        "label": label,
        "label_location": label_location,
        "confidence": round(confidence, 4),
        "intent": command["intent"],
        "targetTrayId": command["targetTrayId"],
        "requiresConfirm": command["requiresConfirm"],
        "confirmText": command["confirmText"],
        "accepted": accepted,
        "message": message
    })


@app.get("/health")
def health():
    return jsonify({"status": "ok"})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
