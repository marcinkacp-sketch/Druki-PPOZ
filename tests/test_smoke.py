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
  for token in ['app-bottom-nav','setupAppTabs','data-screen="home"','data-screen="request"','data-go="objects"','data-part="signatures"','home-recent-list','hero-reference-2.webp','document-history','addDocumentHistory']:
   self.assertIn(token,index)
  gradle=(ROOT/'app/build.gradle.kts').read_text('utf8')
  self.assertIn('versionCode = 6',gradle)
  self.assertIn('stableRelease',gradle)
  self.assertIn('versionName = "1.3.1"',gradle)
 def test_native_smtp_ui_is_explicit(self):
  index=(ROOT/'app/src/main/assets/index.html').read_text('utf8')
  for token in ('id="sendNomaPdf"','id="smtp-password"','id="smtp-test"','id="smtp-inspect"',
                'sma@noma2.pl','mail.atomservice.eu','window.AtomMail.sendPdf',
                'confirm(confirmText)','window.onNativeMailResult',
                'Nie zamykaj aplikacji','Status wysyłki'):
   self.assertIn(token,index)
  self.assertNotIn('smtp-password-storage',index)
 def test_datetime_typography_and_alignment(self):
  import re
  index=(ROOT/'app/src/main/assets/index.html').read_text('utf8')
  fields=('startDatePdf','startTimePdf','endDatePdf','endTimePdf')
  positions=[]
  for field in fields:
   m=re.search(r'data-out="'+field+r'" style="[^"]*top:([0-9.]+)%;',index)
   self.assertIsNotNone(m,field)
   positions.append(m.group(1))
  self.assertEqual(len(set(positions)),1,positions)
  self.assertIn('sans-serif-condensed',index)
  self.assertIn('.noma-field.start-time{font-size:2.03cqw}',index)
  self.assertIn("startTimePdf:'.'+prettyTime(d.startTime)",index)
  self.assertNotIn("startTimePdf:'.\\u00a0'+prettyTime(d.startTime)",index)
  sw=(ROOT/'service-worker.js').read_text('utf8')
  self.assertIn('druki-ppoz-v9',sw)
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
