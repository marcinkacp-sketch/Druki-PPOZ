#!/usr/bin/env python3
"""Offline build: python scripts/build.py (from repo root)."""
from pathlib import Path
import json,base64
ROOT=Path(__file__).resolve().parents[1]
s=(ROOT/'src/app.template.html').read_text(encoding='utf-8')
s=s.replace('/*ZIP_JS*/',(ROOT/'vendor/jszip.min.js').read_text(encoding='utf-8'))
s=s.replace('/*OBJECTS_JSON*/',json.dumps(json.loads((ROOT/'data/objects.json').read_text(encoding='utf-8')),ensure_ascii=False,separators=(',',':')))
s=s.replace('/*TEMPLATE_B64*/',base64.b64encode((ROOT/'templates/szablon_odlaczenia.docx').read_bytes()).decode('ascii'))
(ROOT/'index.html').write_text(s,encoding='utf-8')
print('Created index.html:',len(s),'chars')
