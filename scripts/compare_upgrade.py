from pathlib import Path
import sqlite3, sys, hashlib, xml.etree.ElementTree as ET
before, after = (Path(p) for p in sys.argv[1:])
def snapshot(root):
    def pref_value(element):
        children = tuple(sorted(pref_value(child) for child in element))
        return (element.tag, tuple(sorted(element.attrib.items())), element.text or "", children)
    prefs = {p.name: tuple(sorted(pref_value(e) for e in ET.parse(p).getroot()))
             for p in (root / 'shared_prefs').glob('*.xml')}
    db = sqlite3.connect(f'file:{root / "databases/bodyweight_trainer_database"}?mode=ro', uri=True)
    tables = [row[0] for row in db.execute("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name != 'android_metadata' ORDER BY name")]
    rows = {}
    for table in tables:
        safe = table.replace('"','""')
        records = sorted(repr(row) for row in db.execute(f'SELECT * FROM "{safe}"'))
        rows[table] = (len(records), hashlib.sha256('\n'.join(records).encode()).hexdigest())
    db.close()
    return prefs, rows
oldprefs, oldrows = snapshot(before); newprefs, newrows = snapshot(after)
print('Preferences:', len(oldprefs), 'files identical:', oldprefs == newprefs)
print('Database:', len(oldrows), 'tables identical:', oldrows == newrows)
print('Seeded workout sessions:', oldrows.get('workout_sessions', (0,))[0])
if oldprefs != newprefs or oldrows != newrows or oldrows.get('workout_sessions', (0,))[0] < 9:
    print('Changed preference files:', [k for k in sorted(set(oldprefs) | set(newprefs)) if oldprefs.get(k) != newprefs.get(k)])
    print('Changed tables:', [k for k in sorted(set(oldrows) | set(newrows)) if oldrows.get(k) != newrows.get(k)])
    sys.exit(1)
