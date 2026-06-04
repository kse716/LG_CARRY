import argparse
import csv
import json
import sys
import time
from pathlib import Path
from urllib import request, error


def post_json(url: str, payload: dict, timeout: float = 5.0) -> dict:
    data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    req = request.Request(
        url,
        data=data,
        headers={"Content-Type": "application/json; charset=utf-8"},
        method="POST",
    )
    with request.urlopen(req, timeout=timeout) as resp:
        body = resp.read().decode("utf-8")
        return json.loads(body)


def read_cases(path: Path) -> list[dict]:
    with path.open("r", encoding="utf-8-sig", newline="") as f:
        return list(csv.DictReader(f))


def write_rows(path: Path, rows: list[dict], fieldnames: list[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8-sig", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)


def main() -> int:
    parser = argparse.ArgumentParser(description="Evaluate Carry Voice AI API with fixed test cases.")
    parser.add_argument("--cases", default="carry_voice_test_cases.csv", help="test case CSV path")
    parser.add_argument("--url", default="http://127.0.0.1:5000/api/ai/voice-intent", help="AI inference API URL")
    parser.add_argument("--threshold", type=float, default=0.70, help="accepted threshold to verify")
    parser.add_argument("--out", default="voice_api_eval_results.csv", help="result CSV path")
    parser.add_argument("--fail-out", default="voice_api_failed_cases_for_training.csv", help="failed cases CSV path")
    parser.add_argument("--sleep", type=float, default=0.05, help="sleep seconds between requests")
    args = parser.parse_args()

    cases = read_cases(Path(args.cases))
    results = []
    failures = []

    for i, case in enumerate(cases, start=1):
        text = case["text"].strip()
        expected = case["expected_label"].strip()
        category = case.get("category", "").strip()

        try:
            res = post_json(args.url, {"text": text})
            actual = str(res.get("label", ""))
            confidence = float(res.get("confidence", 0.0) or 0.0)
            accepted = bool(res.get("accepted", False))
            intent = str(res.get("intent", ""))
            target = str(res.get("targetTrayId", "") or "")
            message = str(res.get("message", "") or "")
            error_msg = ""
        except (error.URLError, TimeoutError, json.JSONDecodeError, ConnectionError) as e:
            actual = "API_ERROR"
            confidence = 0.0
            accepted = False
            intent = ""
            target = ""
            message = ""
            error_msg = repr(e)

        label_ok = actual == expected
        threshold_ok = confidence >= args.threshold

        # UNKNOWN은 실행 차단이 정상, 나머지는 threshold 이상이면 accepted가 True여야 정상으로 봄
        if expected == "UNKNOWN":
            accepted_ok = accepted is False
        else:
            accepted_ok = accepted is True if threshold_ok and label_ok else False

        # 최종 PASS 기준: 라벨 정답 + threshold/accepted 정책 정상
        if expected == "UNKNOWN":
            final_pass = label_ok and accepted_ok
        else:
            final_pass = label_ok and threshold_ok and accepted_ok

        row = {
            "no": i,
            "text": text,
            "category": category,
            "expected_label": expected,
            "actual_label": actual,
            "confidence": f"{confidence:.4f}",
            "threshold": f"{args.threshold:.2f}",
            "accepted": str(accepted),
            "intent": intent,
            "targetTrayId": target,
            "label_ok": str(label_ok),
            "threshold_ok": str(threshold_ok),
            "accepted_ok": str(accepted_ok),
            "final_pass": str(final_pass),
            "message": message,
            "error": error_msg,
        }
        results.append(row)

        if not final_pass:
            failures.append({"text": text, "label": expected})

        print(f"[{i:02d}/{len(cases)}] {text} -> {actual} ({confidence:.4f}) pass={final_pass}")
        if args.sleep > 0:
            time.sleep(args.sleep)

    fieldnames = list(results[0].keys()) if results else []
    write_rows(Path(args.out), results, fieldnames)
    write_rows(Path(args.fail_out), failures, ["text", "label"])

    total = len(results)
    passed = sum(r["final_pass"] == "True" for r in results)
    label_ok_count = sum(r["label_ok"] == "True" for r in results)
    print("\n=== SUMMARY ===")
    print(f"total={total}")
    print(f"label_accuracy={label_ok_count}/{total} = {label_ok_count / total:.2%}" if total else "label_accuracy=N/A")
    print(f"final_pass={passed}/{total} = {passed / total:.2%}" if total else "final_pass=N/A")
    print(f"result_file={args.out}")
    print(f"failed_training_file={args.fail_out}")

    return 0 if passed == total else 1


if __name__ == "__main__":
    sys.exit(main())
