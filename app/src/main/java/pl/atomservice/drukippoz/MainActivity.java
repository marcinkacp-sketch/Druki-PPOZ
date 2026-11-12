package pl.atomservice.drukippoz;

import android.app.Activity;
import android.content.Intent;
import android.content.ClipData;
import android.content.ActivityNotFoundException;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.Arrays;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

/** Native offline host for the original NOMA 2 form and local-signature editor. */
public final class MainActivity extends Activity {
    private static final int REQUEST_PICK = 1001;
    private static final int REQUEST_SAVE = 1002;
    private static final String LOCAL_ORIGIN = "https://appassets.androidplatform.net";
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private byte[] pendingFile;
    private String pendingMime;
    private final ExecutorService shareExecutor = Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(7,25,53));
        getWindow().setNavigationBarColor(Color.rgb(7,25,53));
        getWindow().getDecorView().setSystemUiVisibility(0);
        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(7,25,53));
        webView.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSupportZoom(false);
        webView.addJavascriptInterface(new NativeFileBridge(),"AtomNative");
        // Previous SMTP credentials are not needed for a Thunderbird handoff.
        // This wipes only the obsolete application's encrypted password record.
        getSharedPreferences("atom_ppoz_native_mail_v1", MODE_PRIVATE).edit().clear().apply();
        webView.addJavascriptInterface(new NativeShareBridge(),"AtomShare");
        webView.setWebViewClient(new LocalAssetClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    picker.addCategory(Intent.CATEGORY_OPENABLE);
                    picker.setType("*/*");
                    picker.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                        "image/png", "image/jpeg", "image/webp",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/json"
                    });
                    startActivityForResult(picker, REQUEST_PICK);
                    return true;
                } catch (Exception ex) {
                    fileCallback = null;
                    callback.onReceiveValue(null);
                    Toast.makeText(MainActivity.this, "Nie można otworzyć wyboru plików",Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        });
        setContentView(webView);
        webView.loadUrl(LOCAL_ORIGIN + "/assets/index.html");
    }

    private final class LocalAssetClient extends WebViewClient {
        @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            if (!"https".equals(uri.getScheme()) || !"appassets.androidplatform.net".equals(uri.getHost()))
                return new WebResourceResponse("text/plain", "UTF-8", 403, "Forbidden", null,
                        new java.io.ByteArrayInputStream(new byte[0]));
            String path = uri.getPath();
            if (path == null || !path.startsWith("/assets/")) return null;
            String name = path.substring(8);
            if (name.isEmpty() || name.contains("..") || name.startsWith("/")) return null;
            try {
                InputStream data = getAssets().open(name);
                String lower = name.toLowerCase(Locale.ROOT);
                String mime = lower.endsWith(".png")?"image/png":
                        lower.endsWith(".webp")?"image/webp":
                        lower.endsWith(".js")?"text/javascript":
                        lower.endsWith(".webmanifest")?"application/manifest+json":
                        lower.endsWith(".html")?"text/html":
                        lower.endsWith(".json")?"application/json":"application/octet-stream";
                return new WebResourceResponse(mime, "UTF-8", data);
            } catch(Exception ex) {
                return null;
            }
        }
        @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            return !("https".equals(uri.getScheme()) && "appassets.androidplatform.net".equals(uri.getHost()));
        }
    }

    /** The only exposed native action: save a locally generated file through Android Storage Access Framework. */
    private final class NativeFileBridge {
        @JavascriptInterface public void saveFileBase64(String suggestedName, String mime, String base64) {
            if (base64 == null || base64.length() > 28_000_000) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Plik jest zbyt duży", Toast.LENGTH_LONG).show());
                return;
            }
            final byte[] decoded;
            try { decoded = Base64.decode(base64, Base64.DEFAULT); }
            catch (IllegalArgumentException ex) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Błąd kodowania pliku", Toast.LENGTH_LONG).show());
                return;
            }
            runOnUiThread(() -> {
                pendingFile = decoded;
                pendingMime = ("application/json".equals(mime) || "application/pdf".equals(mime))
                        ? mime : "application/octet-stream";
                Intent save = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                save.addCategory(Intent.CATEGORY_OPENABLE);
                save.setType(pendingMime);
                String name = suggestedName == null ? "NOMA2.pdf" : suggestedName.replaceAll("[^\\p{L}\\p{N}_.-]","_");
                save.putExtra(Intent.EXTRA_TITLE, name.isEmpty() ? "NOMA2.pdf" : name);
                try { startActivityForResult(save, REQUEST_SAVE); }
                catch(Exception ex) {
                    pendingFile = null;
                    Toast.makeText(MainActivity.this,"Nie można otworzyć zapisu pliku",Toast.LENGTH_LONG).show();
                }
            });
        }
    }


    /** Prepare a read-only, short-lived private PDF attachment for Thunderbird.
     * Android launches an email composer; the user decides whether to send.
     * We do not and cannot claim delivery by opening the compose screen.
     */
    private final class NativeShareBridge {
        @JavascriptInterface public void openEmail(String requestId, String filename,
                                                   String subject, String body, String pdfBase64) {
            if (pdfBase64 == null || pdfBase64.length() > 17_000_000) {
                notifyShare(requestId, "FAILED", "Załącznik PDF jest zbyt duży.");
                return;
            }
            shareExecutor.execute(() -> {
                try {
                    byte[] data = Base64.decode(pdfBase64, Base64.DEFAULT);
                    if (data.length < 16 || data.length > 12_000_000 ||
                            data[0]!='%' || data[1]!='P' || data[2]!='D' || data[3]!='F') {
                        notifyShare(requestId, "FAILED", "Nieprawidłowy plik PDF.");
                        return;
                    }
                    String safeName = (filename != null && filename.matches("[A-Za-z0-9_.-]{1,100}") &&
                            filename.endsWith(".pdf")) ? filename : "NOMA2.pdf";
                    File base = new File(getCacheDir(), "noma-mail");
                    purgeOldShares(base);
                    File directory = new File(base, UUID.randomUUID().toString());
                    if (!directory.mkdirs() && !directory.isDirectory()) {
                        throw new IOException("Nie można utworzyć prywatnego folderu PDF.");
                    }
                    File attachment = new File(directory, safeName);
                    try (FileOutputStream output = new FileOutputStream(attachment)) {
                        output.write(data);
                    }
                    Arrays.fill(data, (byte)0);
                    Uri uri = FileProvider.getUriForFile(
                            MainActivity.this, getPackageName()+".mailshare",attachment);
                    String safeSubject = (subject == null ? "NOMA 2 - zgłoszenie" : subject)
                            .replace('\r',' ').replace('\n',' ').trim();
                    String safeBody = body == null ? "" : body.replace("\r", "");
                    runOnUiThread(() -> openComposer(requestId,uri,safeName,safeSubject,safeBody));
                } catch (Exception ex) {
                    notifyShare(requestId,"FAILED",
                            "Nie udało się przygotować załącznika lub uruchomić poczty.");
                }
            });
        }
    }

    private void openComposer(String requestId, Uri pdfUri, String fileName,
                              String subject, String body) {
        Intent email = new Intent(Intent.ACTION_SEND);
        email.setType("application/pdf");
        email.putExtra(Intent.EXTRA_EMAIL,new String[]{"sma@noma2.pl"});
        email.putExtra(Intent.EXTRA_SUBJECT,subject);
        email.putExtra(Intent.EXTRA_TEXT,body);
        email.putExtra(Intent.EXTRA_STREAM,pdfUri);
        email.setClipData(ClipData.newUri(getContentResolver(),fileName,pdfUri));
        email.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        email.setPackage("net.thunderbird.android");
        try {
            startActivity(email);
            notifyShare(requestId,"OPENED",
                    "Otworzono Thunderbird. Sprawdź adres nadawcy, odbiorcę i PDF, a potem naciśnij Wyślij.");
        } catch (ActivityNotFoundException missingThunderbird) {
            // Do not pretend the message was sent; allow another compatible mail app.
            try {
                email.setPackage(null);
                Intent chooser = Intent.createChooser(email,"Wybierz aplikację pocztową");
                chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(chooser);
                notifyShare(requestId,"OPENED",
                        "Otworzono wybór aplikacji. Wybierz Thunderbird, sprawdź treść i naciśnij Wyślij.");
            } catch (Exception ex) {
                notifyShare(requestId,"FAILED",
                        "Nie znaleziono aplikacji pocztowej zdolnej odebrać załącznik PDF.");
            }
        } catch (Exception ex) {
            notifyShare(requestId,"FAILED","Nie udało się otworzyć okna nowej wiadomości.");
        }
    }

    /** Clear only old cached PDFs. Keep newer files long enough for a draft
     * composer to read its temporary content URI.
     */
    private void purgeOldShares(File root) {
        File[] dirs = root.listFiles();
        if (dirs == null) return;
        long deadline = System.currentTimeMillis() - 48L*60*60*1000;
        for (File folder : dirs) {
            if (!folder.isDirectory() || folder.lastModified() >= deadline) continue;
            File[] files = folder.listFiles();
            if (files != null) for (File file:files) {
                if (file.isFile()) file.delete();
            }
            folder.delete();
        }
    }

    private void notifyShare(String requestId, String status, String message) {
        runOnUiThread(() -> {
            if (webView == null || isDestroyed()) return;
            String js = "window.onNativeShareResult && window.onNativeShareResult("
                    + JSONObject.quote(requestId == null ? "" : requestId) + ","
                    + JSONObject.quote(status) + ","
                    + JSONObject.quote(message) + ");";
            webView.evaluateJavascript(js,null);
        });
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        super.onActivityResult(requestCode,resultCode,result);
        if (requestCode == REQUEST_PICK) {
            ValueCallback<Uri[]> callback = fileCallback;
            fileCallback = null;
            if (callback != null) {
                Uri picked = resultCode == RESULT_OK && result != null ? result.getData() : null;
                callback.onReceiveValue(picked == null ? null : new Uri[] {picked});
            }
            return;
        }
        if (requestCode == REQUEST_SAVE) {
            try {
                if (resultCode == RESULT_OK && result != null && result.getData() != null && pendingFile != null) {
                    try (OutputStream out = getContentResolver().openOutputStream(result.getData(),"w")) {
                        if (out == null) throw new java.io.IOException("Nie można otworzyć dokumentu");
                        out.write(pendingFile);
                        out.flush();
                    }
                    Toast.makeText(this, "Zapisano dokument na telefonie", Toast.LENGTH_LONG).show();
                }
            } catch(Exception ex) {
                Toast.makeText(this, "Nie udało się zapisać dokumentu",Toast.LENGTH_LONG).show();
            } finally {
                pendingFile = null;
                pendingMime = null;
            }
        }
    }
    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
    @Override protected void onDestroy() {
        if (fileCallback != null) fileCallback.onReceiveValue(null);
        shareExecutor.shutdown();
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
