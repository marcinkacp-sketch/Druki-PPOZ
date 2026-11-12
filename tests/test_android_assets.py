import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]

class TestAndroidPackage(unittest.TestCase):
    def test_packaged_assets_match_web(self):
        for path in ('index.html','templates/noma2_blank.png','graphics/firetruck.webp','graphics/hero-reference-2.webp','icons/atom-app-icon.png'):
            self.assertEqual((ROOT/path).read_bytes(),(ROOT/'app/src/main/assets'/path).read_bytes(),path)
    def test_thunderbird_secure_pdf_sharing(self):
        manifest=(ROOT/'app/src/main/AndroidManifest.xml').read_text('utf-8')
        self.assertNotIn('android.permission.INTERNET',manifest)
        self.assertIn('android:exported="false"',manifest)
        self.assertIn('android:grantUriPermissions="true"',manifest)
        self.assertIn('androidx.core.content.FileProvider',manifest)
        self.assertIn('android:resource="@xml/noma_mail_paths"',manifest)
        self.assertIn('net.thunderbird.android',manifest)
        xml=(ROOT/'app/src/main/res/xml/noma_mail_paths.xml').read_text('utf-8')
        self.assertIn('<cache-path name="mail_pdf" path="noma-mail/"',xml)
        self.assertNotIn('<root-path',xml)
        self.assertNotIn('path="."',xml)
        java=(ROOT/'app/src/main/java/pl/atomservice/drukippoz/MainActivity.java').read_text('utf-8')
        for needle in ('@JavascriptInterface public void saveFileBase64',
                       'Intent.ACTION_CREATE_DOCUMENT','Intent.ACTION_OPEN_DOCUMENT',
                       'new NativeShareBridge()','Intent.ACTION_SEND',
                       'Intent.EXTRA_STREAM','Intent.EXTRA_EMAIL',
                       'FileProvider.getUriForFile','Intent.FLAG_GRANT_READ_URI_PERMISSION',
                       'ClipData.newUri','setPackage("net.thunderbird.android")',
                       'window.onNativeShareResult','getSharedPreferences("atom_ppoz_native_mail_v1"'):
            self.assertIn(needle,java)
        for forbidden in ('new NativeMailBridge()','SmtpSender.send(', 'MailCredentialStore.save('):
            self.assertNotIn(forbidden,java)
        gradle=(ROOT/'app/build.gradle.kts').read_text('utf-8')
        self.assertIn('androidx.core:core:1.15.0',gradle)
        self.assertIn('versionCode = 8',gradle)
    def test_page_and_native_bridge(self):
        html=(ROOT/'index.html').read_text(encoding='utf-8')
        self.assertIn('window.AtomNative.saveFileBase64(',html)
        self.assertIn('hero-reference-2.webp',html)
        self.assertIn('ATOM',html)
        self.assertIn('makePdfCanvas()',html)
        self.assertIn('pdfFromCanvas(',html)
        self.assertIn('window.AtomShare.openEmail(',html)

if __name__=='__main__': unittest.main()
