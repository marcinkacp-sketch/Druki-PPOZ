package pl.atomservice.drukippoz;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Keeps the SMTP password solely on this Android installation, encrypted with
 * an AES-256 key from Android Keystore. The secret is never stored in HTML,
 * JavaScript localStorage, public GitHub sources or any build artifact.
 */
final class MailCredentialStore {
    private static final String KEY_ALIAS = "atom_ppoz_smtp_aes_v1";
    private static final String PREF_NAME = "atom_ppoz_native_mail_v1";
    private static final String PREF_CREDENTIAL = "encrypted_password";
    private MailCredentialStore() {}

    static boolean isConfigured(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .contains(PREF_CREDENTIAL);
    }

    private static SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(KEY_ALIAS))
            return ((KeyStore.SecretKeyEntry) store.getEntry(KEY_ALIAS, null)).getSecretKey();
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .build());
        return generator.generateKey();
    }

    static void save(Context context, String password) throws Exception {
        if (password == null || password.isEmpty() || password.length() > 500)
            throw new IllegalArgumentException("Hasło musi mieć od 1 do 500 znaków.");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        byte[] iv = cipher.getIV();
        byte[] encrypted = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
        byte[] packed = Arrays.copyOf(iv, iv.length + encrypted.length);
        System.arraycopy(encrypted, 0, packed, iv.length, encrypted.length);
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString(PREF_CREDENTIAL, Base64.encodeToString(packed, Base64.NO_WRAP)).commit();
        Arrays.fill(packed, (byte) 0);
        Arrays.fill(encrypted, (byte) 0);
    }

    static String load(Context context) throws Exception {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String stored = prefs.getString(PREF_CREDENTIAL, null);
        if (stored == null || stored.isEmpty())
            throw new IllegalStateException("Najpierw zapisz hasło SMTP w Ustawieniach.");
        byte[] packed = Base64.decode(stored, Base64.DEFAULT);
        if (packed.length < 29) throw new IllegalStateException("Zapisane dane SMTP są uszkodzone.");
        byte[] iv = Arrays.copyOfRange(packed, 0, 12);
        byte[] ciphertext = Arrays.copyOfRange(packed, 12, packed.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
        byte[] clear = cipher.doFinal(ciphertext);
        String password = new String(clear, StandardCharsets.UTF_8);
        Arrays.fill(clear, (byte) 0);
        Arrays.fill(ciphertext, (byte) 0);
        Arrays.fill(packed, (byte) 0);
        return password;
    }

    static void clear(Context context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().remove(PREF_CREDENTIAL).commit();
    }
}
