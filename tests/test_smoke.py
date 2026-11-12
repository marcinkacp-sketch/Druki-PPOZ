"""Offline, stdlib-only smoke checks for the NOMA 2 form."""
from pathlib import Path
import json,unittest
ROOT=Path(__file__).resolve().parents[1]
class TestDrukiPPOZ(unittest.TestCase):
 def test_object_catalog(self):
  objects=json.loads((ROOT/'data/objects.json').read_text('utf8'))
  self.assertEqual(len(objects),73)
  self.assertEqual(len({o['id'] for o in objects}),73)
  self.assertTrue(all(o.get('name') for o in objects))
 def test_tabs_and_android_version(self):
  index=(ROOT/'app/src/main/assets/index.html').read_text('utf8')
  for token in ['app-bottom-nav','setupAppTabs','data-screen="home"','data-screen="request"','data-screen="objects"','data-part="signatures"','document-history','addDocumentHistory']:
   self.assertIn(token,index)
  gradle=(ROOT/'app/build.gradle.kts').read_text('utf8')
  self.assertIn('versionCode = 2',gradle)
  self.assertIn('stableRelease',gradle)
 def test_official_template(self):
  f=ROOT/'templates/noma2_blank.png'
  self.assertTrue(f.exists())
  self.assertGreater(f.stat().st_size,50000)
  self.assertEqual(f.read_bytes()[:8],b'\x89PNG\r\n\x1a\n')
 def test_offline_dependencies(self):
  manifest=json.loads((ROOT/'manifest.webmanifest').read_text('utf8'))
  self.assertEqual(manifest['display'],'standalone')
  sw=(ROOT/'service-worker.js').read_text('utf8')
  self.assertIn('noma2_blank.png',sw)
  for icon in manifest['icons']:
   self.assertTrue((ROOT/icon['src']).exists())
 def test_generated_page(self):
  index=(ROOT/'index.html').read_text('utf8')
  for needle in ['INCLUDED_OBJECTS','noma2_blank.png','signature-file','signatureKey','currentSignerKey','startDate','startTime','downloadPdf','pdfFromCanvas','Pobierz PDF','locativeCity','cityLocative','pspCityLocative',"'Gliwice':'Gliwicach'","'Katowice':'Katowicach'"]:
   self.assertIn(needle,index)
  self.assertNotIn('Pobierz Word (.docx)',index)
if __name__=='__main__':unittest.main()
