import argparse
import re
from pathlib import Path

import joblib
import pandas as pd

from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import classification_report


DATA_PATH = "carry_voice_commands_seed_weighted.csv"
MODEL_DIR = "model_output"
MODEL_FILENAME = "carry_voice_command_model.pkl"

LOCATION_PHRASES = (
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


def build_model():
    return Pipeline([
        ("tfidf", TfidfVectorizer(
            analyzer="char_wb",
            ngram_range=(2, 5)
        )),
        ("clf", LogisticRegression(
            max_iter=1500,
            class_weight="balanced"
        ))
    ])


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", default=DATA_PATH)
    parser.add_argument("--model-dir", default=MODEL_DIR)
    args = parser.parse_args()

    model_dir = Path(args.model_dir)
    model_dir.mkdir(parents=True, exist_ok=True)

    df = pd.read_csv(args.data, encoding="utf-8-sig")

    required_columns = ["text", "label", "label_location", "weight"]
    for col in required_columns:
        if col not in df.columns:
            raise ValueError(f"Missing required column: {col}")

    df = df.dropna(subset=["text", "label"]).copy()
    df["text"] = df["text"].astype(str).str.strip()
    df["label"] = df["label"].astype(str).str.strip()
    df["label_location"] = df["label_location"].fillna("").astype(str).str.strip()
    df["weight"] = pd.to_numeric(df["weight"], errors="coerce").fillna(1.0).astype(float)
    df["text_for_command"] = df["text"].map(strip_location_for_command)
    df = df[(df["text_for_command"] != "") & (df["label"] != "")]

    X = df["text_for_command"].astype(str)
    y = df["label"].astype(str)
    sample_weight = df["weight"]

    print("rows:", len(df))
    print("\nlabel counts")
    print(y.value_counts())
    print("\nlabel_location counts")
    print(df["label_location"].value_counts(dropna=False))
    print("\nweight counts")
    print(sample_weight.value_counts().sort_index())

    X_train, X_test, y_train, y_test, w_train, _ = train_test_split(
        X,
        y,
        sample_weight,
        test_size=0.2,
        random_state=42,
        stratify=y
    )

    model = build_model()
    model.fit(X_train, y_train, clf__sample_weight=w_train)

    y_pred = model.predict(X_test)
    report = classification_report(y_test, y_pred, digits=4)

    print("\n===== command classification report =====")
    print(report)

    (model_dir / "classification_report.txt").write_text(report, encoding="utf-8")

    save_path = model_dir / MODEL_FILENAME
    joblib.dump(model, save_path)
    print(f"\nsaved: {save_path}")


if __name__ == "__main__":
    main()
