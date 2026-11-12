package pl.atomservice.drukippoz;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/** SMTP over implicit TLS (465). Fixed company server and NOMA 2 recipient.
 * No credential or message content is written to logs or GitHub.
 */
final class SmtpSender {
    static final String HOST = "mail.atomservice.eu";
    static final int PORT = 465;
    static final String FROM = "m.kacperski@atomservice.eu";
    static final String TO = "sma@noma2.pl";
    private static final int TIMEOUT_MS = 18000;
    private SmtpSender() {}

    /** The server may have received DATA even if the final response was lost.
     * Caller MUST NOT offer an automatic retry.
     */
    static final class UnknownDelivery extends IOException {
        UnknownDelivery() {super("Nie można potwierdzić przyjęcia wiadomości przez SMTP. Nie ponawiaj automatycznie wysyłki.");}
    }

    static String testLogin(String password) throws IOException {
        try (Session s = connect(password)) {
            try { s.command("QUIT", 221); } catch (IOException ignored) {}
            return s.authMethod;
        }
    }

    /** Safe diagnostic: handshake/EHLO only, without credentials or messages. */
    static String inspectServer() throws IOException {
        try (Session s = openTls()) {
            String ehlo = s.ehlo();
            String methods = supportedMethods(ehlo);
            try { s.command("QUIT", 221); } catch (IOException ignored) {}
            return methods.isEmpty() ? "Serwer nie ogłasza AUTH w EHLO." :
                "Serwer obsługuje: " + methods + ". Wykrywanie nie wymaga hasła.";
        }
    }

    static void send(String password, String title, String message, String filename, byte[] pdf) throws IOException {
        if (pdf == null || pdf.length < 16 || pdf.length > 12_000_000
                || pdf[0] != '%' || pdf[1] != 'P' || pdf[2] != 'D' || pdf[3] != 'F') {
            throw new IOException("Nieprawidłowy lub zbyt duży PDF.");
        }
        try (Session s = connect(password)) {
            s.command("MAIL FROM:<" + FROM + ">", 250);
            s.command("RCPT TO:<" + TO + ">", 250, 251);
            s.command("DATA", 354);
            String safeName = (filename != null && filename.matches("[A-Za-z0-9_.-]{1,100}") && filename.endsWith(".pdf"))
                    ? filename : "NOMA2.pdf";
            String safeTitle = title == null ? "NOMA 2 - zgloszenie" : title.replaceAll("[\r\n]", " ").trim();
            if (safeTitle.length() > 160) safeTitle = safeTitle.substring(0, 160);
            String safeMessage = message == null ? "" : message.replace("\r", "");
            String boundary = "atom-ppoz-" + UUID.randomUUID().toString().replace("-", "");
            String date = rfcDate();
            String encodedTitle = "=?UTF-8?B?" + Base64.getEncoder()
                    .encodeToString(safeTitle.getBytes(StandardCharsets.UTF_8)) + "?=";
            String encodedBody = mime64(safeMessage.getBytes(StandardCharsets.UTF_8));
            String encodedPdf = mime64(pdf);
            StringBuilder data = new StringBuilder(encodedPdf.length() + encodedBody.length() + 1400);
            data.append("From: Atom Serwis <").append(FROM).append(">\r\n")
                .append("To: <").append(TO).append(">\r\n")
                .append("Subject: ").append(encodedTitle).append("\r\n")
                .append("Date: ").append(date).append("\r\n")
                .append("Message-ID: <").append(UUID.randomUUID()).append("@atomservice.eu>\r\n")
                .append("MIME-Version: 1.0\r\n")
                .append("Content-Type: multipart/mixed; boundary=\"").append(boundary).append("\"\r\n")
                .append("\r\n")
                .append("--").append(boundary).append("\r\n")
                .append("Content-Type: text/plain; charset=UTF-8\r\n")
                .append("Content-Transfer-Encoding: base64\r\n\r\n")
                .append(encodedBody).append("\r\n")
                .append("--").append(boundary).append("\r\n")
                .append("Content-Type: application/pdf; name=\"").append(safeName).append("\"\r\n")
                .append("Content-Transfer-Encoding: base64\r\n")
                .append("Content-Disposition: attachment; filename=\"").append(safeName).append("\"\r\n\r\n")
                .append(encodedPdf).append("\r\n")
                .append("--").append(boundary).append("--\r\n");
            // Only MIME base64 is used for the body/attachment, so no DATA lines start with a dot.
            s.write(data.toString());
            try {
                s.write(".\r\n");
                s.expect(250);
            } catch (IOException unknown) {
                // A lost SMTP acknowledgement is NOT a definite failure.
                if (unknown instanceof RejectedData) throw unknown;
                throw new UnknownDelivery();
            }
            try { s.command("QUIT", 221); } catch (IOException ignored) {
                // The message has already been acknowledged with 250.
            }
        }
    }

    private static String mime64(byte[] data) {
        return Base64.getMimeEncoder(76, new byte[]{'\r','\n'}).encodeToString(data);
    }

    private static String rfcDate() {
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
        format.setTimeZone(TimeZone.getDefault());
        return format.format(new Date());
    }

    private static boolean advertises(String ehlo, String mechanism) {
        for (String line : ehlo.split("\n")) {
            String capability = line.trim().toUpperCase(Locale.ROOT);
            if (capability.startsWith("AUTH ") || capability.startsWith("AUTH=")) {
                String list = capability.substring(5).replace('=', ' ').trim();
                for (String entry : list.split("\\s+")) {
                    if (mechanism.equals(entry)) return true;
                }
            }
        }
        return false;
    }

    private static String supportedMethods(String ehlo) {
        boolean login=advertises(ehlo, "LOGIN");
        boolean plain=advertises(ehlo, "PLAIN");
        return login && plain ? "AUTH LOGIN, AUTH PLAIN" :
               login ? "AUTH LOGIN" : plain ? "AUTH PLAIN" : "";
    }

    private static String encode(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    /** Use exactly one advertised mechanism per connection; do not retry
     * another mechanism after a 535 response (account lockout risk).
     */
    private static Session connect(String password) throws IOException {
        if (password == null || password.isEmpty())
            throw new IOException("Brak zapisanego hasła SMTP.");
        Session s = openTls();
        try {
            String ehlo = s.ehlo();
            if (advertises(ehlo, "LOGIN")) {
                // RFC 4954 AUTH LOGIN: initial challenge, username, password.
                s.authMethod = "AUTH LOGIN";
                int code = s.commandAllow("AUTH LOGIN");
                if (code != 334) throw authFailure(code, s.authMethod);
                code = s.commandAllow(encode(FROM));
                if (code != 334) throw authFailure(code, s.authMethod);
                code = s.commandAllow(encode(password));
                if (code != 235) throw authFailure(code, s.authMethod);
            } else if (advertises(ehlo, "PLAIN")) {
                s.authMethod = "AUTH PLAIN";
                String payload = "\0" + FROM + "\0" + password;
                int code = s.commandAllow("AUTH PLAIN " + encode(payload));
                if (code == 334) code = s.commandAllow(encode(payload));
                if (code != 235) throw authFailure(code, s.authMethod);
            } else {
                throw new IOException("Serwer SMTP nie reklamuje obsługi AUTH LOGIN/PLAIN.");
            }
            return s;
        } catch (IOException ex) {
            try { s.close(); } catch (IOException ignored) {}
            throw ex;
        }
    }

    private static IOException authFailure(int code, String method) {
        if (code == 535 || code == 534) {
            return new IOException("Serwer SMTP odrzucił " + method + " (" + code
                    + "). Nie ponawiaj prób wielokrotnie. Sprawdź uprawnienia SMTP konta.");
        }
        return new IOException("Uwierzytelnianie " + method + " nie powiodło się ("
                + code + ").");
    }

    private static Session openTls() throws IOException {
        Socket tcp = new Socket();
        try {
            tcp.connect(new InetSocketAddress(HOST, PORT), TIMEOUT_MS);
            SSLSocket socket = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory.getDefault())
                    .createSocket(tcp, HOST, PORT, true);
            socket.setSoTimeout(TIMEOUT_MS);
            SSLParameters params = socket.getSSLParameters();
            params.setEndpointIdentificationAlgorithm("HTTPS");
            socket.setSSLParameters(params);
            socket.startHandshake();
            Session s = new Session(socket);
            try {
                s.expect(220);
                return s;
            } catch (IOException ex) {
                s.close();
                throw ex;
            }
        } catch (IOException | RuntimeException ex) {
            try { tcp.close(); } catch (IOException ignored) {}
            throw ex;
        }
    }

    private static final class RejectedData extends IOException {
        RejectedData(int code) { super("Serwer SMTP odrzucił wiadomość (" + code + ")."); }
    }

    private static final class Session implements AutoCloseable {
        private final SSLSocket socket;
        private final BufferedReader in;
        private final BufferedWriter out;
        String authMethod = "";
        private String lastReply = "";
        Session(SSLSocket socket) throws IOException {
            this.socket = socket;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII));
        }
        String ehlo() throws IOException {
            command("EHLO atomservice.eu", 250);
            return lastReply;
        }
        int commandAllow(String command) throws IOException {
            write(command + "\r\n");
            return responseCode();
        }
        void command(String command, int... expected) throws IOException {
            int code = commandAllow(command);
            match(code, expected);
        }
        void expect(int... expected) throws IOException {
            int code = responseCode();
            if (expected.length == 1 && expected[0] == 250 && code >= 500) throw new RejectedData(code);
            match(code, expected);
        }
        private void match(int code, int... expected) throws IOException {
            for (int candidate : expected) if (candidate == code) return;
            throw new IOException("Serwer SMTP odmówił wykonania operacji (" + code + ").");
        }
        private int responseCode() throws IOException {
            String line = in.readLine();
            if (line == null || line.length() < 3) throw new IOException("Brak odpowiedzi SMTP.");
            int code;
            try { code = Integer.parseInt(line.substring(0,3)); }
            catch (NumberFormatException ex) { throw new IOException("Nieprawidłowa odpowiedź SMTP."); }
            StringBuilder capabilities = new StringBuilder();
            int lines = 0;
            while (true) {
                if (line.length() < 3 || !line.startsWith(Integer.toString(code))
                        || line.length() > 800) throw new IOException("Niespójna odpowiedź SMTP.");
                if (line.length() > 4) capabilities.append(line.substring(4)).append('\n');
                if (line.length() == 3 || line.charAt(3) != '-') break;
                if (++lines > 60) throw new IOException("Zbyt długa odpowiedź SMTP.");
                line = in.readLine();
                if (line == null) throw new IOException("Przerwana odpowiedź SMTP.");
            }
            lastReply = capabilities.toString();
            return code;
        }
        void write(String value) throws IOException { out.write(value); out.flush(); }
        @Override public void close() throws IOException { socket.close(); }
    }
}
