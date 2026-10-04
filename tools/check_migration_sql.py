#!/usr/bin/env python3
"""Host-side check: every CREATE statement in MIGRATION_21_22 equals Room's exported 22.json
(createSql / index sql), and 21.json tables are unchanged in 22.json. Run: python3 tools/check_migration_sql.py"""
import json, re, sys, pathlib
root = pathlib.Path(__file__).resolve().parent.parent
sd = root / "data/schemas/io.github.gonbei774.calisthenicsmemory.data.AppDatabase"
v21 = json.load(open(sd / "21.json"))["database"]; v22 = json.load(open(sd / "22.json"))["database"]
src = (root / "data/src/main/java/io/github/gonbei774/calisthenicsmemory/data/AppDatabase.kt").read_text()
block = src[src.index("MIGRATION_21_22 = object"):]
block = block[:block.index("val MIGRATION_9_10")]
mig = set(re.findall(r'execSQL\("(.*?)"\)', block))
norm = lambda s: s.replace("${TABLE_NAME}", "").strip()
t21 = {e["tableName"]: e for e in v21["entities"]}
new = {}
for e in v22["entities"]:
    if e["tableName"] not in t21:
        new[e["createSql"].replace("${TABLE_NAME}", e["tableName"])] = 1
        for i in e.get("indices", []):
            new[i["createSql"].replace("${TABLE_NAME}", e["tableName"])] = 1
    elif e != t21[e["tableName"]]:
        print("CHANGED existing table:", e["tableName"]); sys.exit(1)
missing = [s for s in new if s not in mig]; extra = [s for s in mig if s not in new]
print(f"new statements from Room: {len(new)}; in migration: {len(mig)}")
for m in missing: print("MISSING in migration:", m)
for x in extra: print("EXTRA in migration:", x)
sys.exit(1 if missing or extra else 0)
