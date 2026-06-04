# train_carry_voice_classifier.py
# Carry Voice Command Classifier
# 사용법:
#   pip install pandas scikit-learn matplotlib joblib
#   python train_carry_voice_classifier.py --data carry_voice_commands_seed_weighted.csv

import argparse
from pathlib import Path

import joblib
import pandas as pd
import matplotlib.pyplot as plt

from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (
    classification_report,
    confusion_matrix,
    ConfusionMatrixDisplay,
    accuracy_score,
)


def build_model():
    return Pipeline([
        ("tfidf", TfidfVectorizer(
            analyzer="char_wb",
            ngram_range=(2, 5),
            min_df=1,
            sublinear_tf=True
        )),
        ("clf", LogisticRegression(
            max_iter=3000,
            C=5.0,
            class_weight=None,
            solver="lbfgs"
        ))
    ])


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", default="carry_voice_commands_seed.csv")
    parser.add_argument("--outdir", default="model_output")
    args = parser.parse_args()

    outdir = Path(args.outdir)
    outdir.mkdir(parents=True, exist_ok=True)

    df = pd.read_csv(args.data)

    if "text" not in df.columns or "label" not in df.columns:
        raise ValueError("CSV must contain 'text' and 'label' columns.")

    # 기본 정리
    df = df.dropna(subset=["text", "label"]).copy()
    df["text"] = df["text"].astype(str).str.strip()
    df["label"] = df["label"].astype(str).str.strip()
    df = df[(df["text"] != "") & (df["label"] != "")]

    # weight 컬럼이 없으면 전부 1로 처리
    if "weight" not in df.columns:
        df["weight"] = 1.0

    df["weight"] = pd.to_numeric(df["weight"], errors="coerce").fillna(1.0).astype(float)

    # 주의:
    # sample_weight를 쓰려면 중복 제거를 무조건 하면 안 됨.
    # 단, 같은 text,label이 여러 번 있고 weight도 같으면 데이터가 불필요하게 커질 수 있음.
    # 여기서는 중복 제거하지 않고, weight를 그대로 반영함.
    # df = df.drop_duplicates(subset=["text", "label"])

    X = df["text"]
    y = df["label"]
    weights = df["weight"]

    print("=== DATA SUMMARY ===")
    print(f"rows={len(df)}")
    print("\nlabel counts:")
    print(y.value_counts())
    print("\nweight counts:")
    print(weights.value_counts().sort_index())

    X_train, X_test, y_train, y_test, w_train, w_test = train_test_split(
        X,
        y,
        weights,
        test_size=0.2,
        random_state=42,
        stratify=y
    )

    # 1) 평가용 모델: train set으로 학습 후 test set 평가
    eval_model = build_model()
    eval_model.fit(X_train, y_train, clf__sample_weight=w_train)

    y_pred = eval_model.predict(X_test)
    labels = sorted(y.unique())

    report = classification_report(y_test, y_pred, labels=labels, digits=4)
    acc = accuracy_score(y_test, y_pred)

    (outdir / "classification_report.txt").write_text(
        f"Accuracy: {acc:.4f}\n\n{report}",
        encoding="utf-8"
    )

    cm = confusion_matrix(y_test, y_pred, labels=labels)
    fig, ax = plt.subplots(figsize=(11, 9))
    disp = ConfusionMatrixDisplay(confusion_matrix=cm, display_labels=labels)
    disp.plot(ax=ax, xticks_rotation=45, values_format="d", colorbar=False)
    plt.tight_layout()
    plt.savefig(outdir / "confusion_matrix.png", dpi=180)
    plt.close(fig)

    # 2) API 배포용 모델: 전체 데이터로 다시 학습 후 저장
    # 중요: hard case가 test set으로 빠져도 최종 모델에는 반드시 반영되게 함
    final_model = build_model()
    final_model.fit(X, y, clf__sample_weight=weights)

    joblib.dump(final_model, outdir / "carry_voice_command_model.pkl")

    # 간단 추론 테스트
    samples = [
        "캐리야 기저귀 가져와",
        "케리야 물 티슈 갖다줘",
        "비타민 챙겨줘",
        "약 트레이 불러줘",
        "게임 패드 불러줘",
        "멈춰",
        "제자리로 돌아가",
        "배터리 알려줘",
        "날씨 알려줘"
    ]

    proba = final_model.predict_proba(samples)
    classes = final_model.classes_

    lines = ["text\tpredicted_label\tconfidence"]
    for text, probs in zip(samples, proba):
        idx = probs.argmax()
        lines.append(f"{text}\t{classes[idx]}\t{probs[idx]:.4f}")

    (outdir / "sample_predictions.tsv").write_text(
        "\n".join(lines),
        encoding="utf-8"
    )

    print("\n=== TRAINING RESULT ===")
    print(f"Saved model and reports to: {outdir.resolve()}")
    print(f"Accuracy: {acc:.4f}")
    print("\nSample predictions:")
    for line in lines[1:]:
        print(line)


if __name__ == "__main__":
    main()