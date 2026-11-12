"""Sanity checks of files used to build the offline app, stdlib only."""
import json, unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
class TestDrukiPpoz(unittest.TestCase):
 def test_catalog(self):
  objects=json.loads((ROOT/'data/objects.json').read_text(encoding='utf-8'))
  self.assertEqual(73,len(objects))
  self.assertEqual(len(objects),len({x['id'] for x in objects}))
  self.assertTrue(all(x.get('name') for x in objects))
 def test_pwa(self):
  m=json.loads((ROOT/'manifest.webmanifest').read_text(encoding='utf-8'))
  self.assertEqual('standalone',m['display'])
  for icon in m['icons']:
   self.assertTrue((ROOT/icon['src']).is_file())
 def test_index(self):
  t=(ROOT/'index.html').read_text(encoding='utf-8')
  for token in ['INCLUDED_OBJECTS','TEMPLATE_BASE64','startDate','startTime','makeWord']:
   self.assertIn(token,t)
if __name__=='__main__':
 unittest.main()
