import pandas as pd

INPUT = "carry_voice_commands_seed.csv"
OUTPUT = "carry_voice_commands_seed_weighted.csv"

hard_cases = {
    "케리야 물 티슈 갖다줘",
    "비타민 챙겨줘"
}

df = pd.read_csv(INPUT)

df["text"] = df["text"].astype(str).str.strip()
df["label"] = df["label"].astype(str).str.strip()

if "weight" not in df.columns:
    df["weight"] = 1

df.loc[df["text"].isin(hard_cases), "weight"] = 5

df.to_csv(OUTPUT, index=False, encoding="utf-8-sig")

print(f"saved: {OUTPUT}")
print(df["weight"].value_counts().sort_index())