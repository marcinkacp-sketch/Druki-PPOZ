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
  self.assertIn('versionCode = 9',gradle)
  self.assertIn('stableRelease',gradle)
  self.assertIn('versionName = "1.3.4"',gradle)
 def test_thunderbird_share_with_confirmation(self):
  index=(ROOT/'app/src/main/assets/index.html').read_text('utf8')
  for token in ('id="sendNomaPdf"','Zatwierdź i otwórz Thunderbird',
                'window.AtomShare.openEmail','window.onNativeShareResult',
                'confirm(confirmText)','sma@noma2.pl','Przekazano do poczty',
                'wysyłka niepotwierdzona','id="mail-not-supported"'):
   self.assertIn(token,index)
  for forbidden in ('AtomMail','smtp-password','window.AtomMail.sendPdf',
                    'window.onNativeMailResult','Serwer SMTP przyjął wiadomość'):
   self.assertNotIn(forbidden,index)
  self.assertIn('pdfFromCanvas(',index)
 def test_datetime_typography_and_alignment(self):
  import re
  html=(ROOT/'app/src/main/assets/index.html').read_text('utf8')
  fields=('startDatePdf','startTimePdf','endDatePdf','endTimePdf')
  self.assertIn('id="noma-datetime-row"',html)
  self.assertEqual(html.count('id="noma-datetime-row"'),1)
  self.assertIn('.noma-datetime-row{position:absolute;left:7%;top:42.60%;width:79%;height:2.50%;',html)
  self.assertIn('ctx.fillRect((rect.left-paperRect.left)*sx',html)
  self.assertIn("const dateRow=paper.querySelector('#noma-datetime-row');",html)
  self.assertIn('startDatePdf:prettyDate(d.startDate)',html)
  self.assertIn('endDatePdf:prettyDate(d.endDate)',html)
  self.assertIn('startTimePdf:prettyTime(d.startTime)',html)
  self.assertIn('endTimePdf:prettyTime(d.endTime)',html)
  self.assertNotIn("startTimePdf:'. '+prettyTime(d.startTime)",html)
  row=re.search(r'<div class="noma-datetime-row" id="noma-datetime-row"[^>]*>(.*?)</div>',html,re.S)
  self.assertIsNotNone(row)
  tags=re.findall(r'<span class="([^"]+)"(?: data-out="([^"]+)")?>(.*?)</span>',row.group(1),re.S)
  self.assertEqual(len(tags),8)
  self.assertEqual([x[1] for x in tags if x[1]],list(fields))
  self.assertEqual([x[2].strip() for x in tags if not x[1]],['od dnia','godz.','do dnia','godz.'])
  self.assertEqual(len(re.findall(r'data-out="(startDatePdf|startTimePdf|endDatePdf|endTimePdf)"',html)),4)
  self.assertNotIn('class="noma-field small datetime-time"',html)
  self.assertIn('Zatwierdź i otwórz Thunderbird',html)
  sw=(ROOT/'service-worker.js').read_text('utf8')
  self.assertIn('druki-ppoz-v12',sw)
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
