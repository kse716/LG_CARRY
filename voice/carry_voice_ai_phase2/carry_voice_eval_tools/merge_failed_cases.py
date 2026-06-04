import argparse
import csv
from pathlib import Path


def read_csv(path: Path) -> list[dict]:
    with path.open("r", encoding="utf-8-sig", newline="") as f:
        return list(csv.DictReader(f))


def write_csv(path: Path, rows: list[dict]) -> None:
    with path.open("w", encoding="utf-8-sig", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=["text", "label"])
        writer.writeheader()
        writer.writerows(rows)


def main() -> int:
    parser = argparse.ArgumentParser(description="Merge failed API test cases into training CSV without duplicates.")
    parser.add_argument("--base", required=True, help="base training CSV path, text,label")
    parser.add_argument("--failures", default="voice_api_failed_cases_for_training.csv", help="failed cases CSV path, text,label")
    parser.add_argument("--out", default="carry_voice_commands_seed_next.csv", help="merged output training CSV path")
    args = parser.parse_args()

    base_rows = read_csv(Path(args.base))
    failure_rows = read_csv(Path(args.failures))

    seen = set()
    merged = []
    for row in base_rows + failure_rows:
        text = row["text"].strip()
        label = row["label"].strip()
        key = (text, label)
        if text and label and key not in seen:
            seen.add(key)
            merged.append({"text": text, "label": label})

    write_csv(Path(args.out), merged)
    print(f"base_rows={len(base_rows)}")
    print(f"failure_rows={len(failure_rows)}")
    print(f"merged_rows={len(merged)}")
    print(f"output={args.out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
