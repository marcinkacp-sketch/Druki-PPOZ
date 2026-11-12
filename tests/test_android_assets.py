import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]

class TestAndroidPackage(unittest.TestCase):
    def test_packaged_assets_match_web(self):
        for path in ('index.html','templates/noma2_blank.png','graphics/firetruck.webp','graphics/hero-reference-2.webp','icons/atom-app-icon.png'):
            self.assertEqual((ROOT/path).read_bytes(),(ROOT/'app/src/main/assets'/path).read_bytes(),path)
    def test_offline_asset_app_with_smtp_permission(self):
        manifest=(ROOT/'app/src/main/AndroidManifest.xml').read_text(encoding='utf-8')
        self.assertIn('android.permission.INTERNET',manifest)
        self.assertIn('android:usesCleartextTraffic="false"',manifest)
        self.assertIn('MainActivity',manifest)
        java=(ROOT/'app/src/main/java/pl/atomservice/drukippoz/MainActivity.java').read_text(encoding='utf-8')
        self.assertIn('@JavascriptInterface public void saveFileBase64',java)
        self.assertIn('Intent.ACTION_CREATE_DOCUMENT',java)
        self.assertIn('Intent.ACTION_OPEN_DOCUMENT',java)
        self.assertIn('new NativeMailBridge()',java)
        self.assertIn('mailExecutor.execute(',java)
        self.assertIn('MailCredentialStore.save',java)
        self.assertIn('SmtpSender.send(',java)
        smtp=(ROOT/'app/src/main/java/pl/atomservice/drukippoz/SmtpSender.java').read_text('utf-8')
        self.assertIn('setEndpointIdentificationAlgorithm("HTTPS")',smtp)
        self.assertIn('mail.atomservice.eu',smtp)
        self.assertIn('sma@noma2.pl',smtp)
        self.assertIn('new UnknownDelivery()',smtp)
        vault=(ROOT/'app/src/main/java/pl/atomservice/drukippoz/MailCredentialStore.java').read_text('utf-8')
        self.assertIn('AndroidKeyStore',vault)
        self.assertIn('AES/GCM/NoPadding',vault)
        self.assertNotIn('localStorage.setItem(',java)
    def test_page_and_native_bridge(self):
        html=(ROOT/'index.html').read_text(encoding='utf-8')
        self.assertIn('window.AtomNative.saveFileBase64(',html)
        self.assertIn('hero-reference-2.webp',html)
        self.assertIn('ATOM',html)
        self.assertIn('makePdfCanvas()',html)
        self.assertIn('pdfFromCanvas(',html)

if __name__=='__main__': unittest.main()
