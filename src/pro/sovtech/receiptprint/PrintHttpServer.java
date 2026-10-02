package pro.sovtech.receiptprint;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Tiny loopback HTTP server used by the kiosk browser to request a print.
 * Binds to 127.0.0.1 only (never exposed on the network).
 *
 * Endpoints:
 *   GET  /status         -> {"connected":bool,"version":str,"status":int}
 *   POST /print          -> body: plain text  OR  {"text":"..."}; prints it
 *   POST /cashbox        -> opens the cash drawer
 */
public class PrintHttpServer {

    private static final String TAG = "ReceiptPrint";
    public static final int PORT = 8088;
    private static final int MAX_BODY_BYTES = 256 * 1024;

    private final PrintBridge bridge;
    private final Context ctx;
    private ServerSocket server;
    private Thread acceptThread;
    private volatile boolean running = false;

    public PrintHttpServer(Context ctx, PrintBridge bridge) {
        this.ctx = ctx.getApplicationContext();
        this.bridge = bridge;
    }

    public synchronized void start() {
        if (running) return;
        try {
            server = new ServerSocket(PORT, 8, InetAddress.getByName("127.0.0.1"));
            running = true;
            acceptThread = new Thread(this::acceptLoop, "print-http");
            acceptThread.setDaemon(true);
            acceptThread.start();
            Log.i(TAG, "print http server on 127.0.0.1:" + PORT);
        } catch (IOException e) {
            Log.e(TAG, "failed to start http server", e);
        }
    }

    public synchronized void stop() {
        running = false;
        try {
            if (server != null) server.close();
        } catch (IOException ignored) {
        }
        server = null;
    }

    public boolean isRunning() {
        return running;
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket s = server.accept();
                handle(s);
            } catch (IOException e) {
                if (running) Log.w(TAG, "accept error: " + e.getMessage());
            }
        }
    }

    private void handle(Socket s) {
        try {
            s.setSoTimeout(10000);
            InputStream in = new BufferedInputStream(s.getInputStream());
            OutputStream out = s.getOutputStream();

            String requestLine = readLine(in);
            if (requestLine == null || requestLine.isEmpty()) {
                s.close();
                return;
            }
            String[] parts = requestLine.split(" ");
            String method = parts.length > 0 ? parts[0] : "GET";
            String path = parts.length > 1 ? parts[1] : "/";

            int contentLength = 0;
            String line;
            while ((line = readLine(in)) != null && !line.isEmpty()) {
                int idx = line.indexOf(':');
                if (idx > 0) {
                    String h = line.substring(0, idx).trim().toLowerCase();
                    String v = line.substring(idx + 1).trim();
                    if (h.equals("content-length")) {
                        try { contentLength = Integer.parseInt(v); } catch (NumberFormatException ignored) {}
                    }
                }
            }

            byte[] body = new byte[0];
            if (contentLength > 0) {
                if (contentLength > MAX_BODY_BYTES) {
                    contentLength = MAX_BODY_BYTES;
                }
                body = readN(in, contentLength);
            }

            String responseBody;
            int code = 200;
            String bodyStr = new String(body, StandardCharsets.UTF_8);

            if (method.equals("OPTIONS")) {
                byte[] resp = new byte[0];
                String headers = "HTTP/1.1 204 No Content\r\n"
                        + corsHeaders()
                        + "Content-Length: 0\r\n"
                        + "Connection: close\r\n\r\n";
                out.write(headers.getBytes(StandardCharsets.UTF_8));
                out.flush();
                s.close();
                return;
            }

            if (path.startsWith("/status")) {
                JSONObject o = new JSONObject();
                o.put("connected", bridge.isConnected());
                o.put("version", bridge.serviceVersion());
                o.put("status", bridge.printerStatus());
                responseBody = o.toString();
            } else if (path.startsWith("/print") && method.equals("POST")) {
                String text = bodyStr;
                try {
                    if (bodyStr.trim().startsWith("{")) {
                        text = new JSONObject(bodyStr).optString("text", "");
                    }
                } catch (Exception ignored) {
                }
                boolean ok = bridge.printText(text);
                responseBody = "{\"ok\":" + ok + "}";
                if (!ok) code = 503;
            } else if (path.startsWith("/cashbox") && method.equals("POST")) {
                boolean ok = bridge.openCashBox();
                responseBody = "{\"ok\":" + ok + "}";
                if (!ok) code = 503;
            } else if (path.startsWith("/clipboard") && method.equals("POST")) {
                boolean ok = false;
                try {
                    ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(ClipData.newPlainText("cfg", bodyStr));
                        ok = true;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "clipboard set failed: " + e.getMessage());
                }
                responseBody = "{\"ok\":" + ok + "}";
            } else {
                code = 404;
                responseBody = "{\"error\":\"not found\"}";
            }

            byte[] resp = responseBody.getBytes(StandardCharsets.UTF_8);
            String statusText = code == 200 ? "OK" : (code == 404 ? "Not Found" : "Service Unavailable");
            String headers = "HTTP/1.1 " + code + " " + statusText + "\r\n"
                    + "Content-Type: application/json; charset=utf-8\r\n"
                    + corsHeaders()
                    + "Content-Length: " + resp.length + "\r\n"
                    + "Connection: close\r\n\r\n";
            out.write(headers.getBytes(StandardCharsets.UTF_8));
            out.write(resp);
            out.flush();
            s.close();
        } catch (Exception e) {
            Log.w(TAG, "request error: " + e.getMessage());
            try { s.close(); } catch (IOException ignored) {}
        }
    }

    private static String corsHeaders() {
        return "Access-Control-Allow-Origin: *\r\n"
                + "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n"
                + "Access-Control-Allow-Headers: Content-Type\r\n";
    }

    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') break;
            if (b != '\r') buf.write(b);
        }
        if (b == -1 && buf.size() == 0) return null;
        return buf.toString("UTF-8");
    }

    private static byte[] readN(InputStream in, int n) throws IOException {
        byte[] data = new byte[n];
        int off = 0;
        while (off < n) {
            int r = in.read(data, off, n - off);
            if (r == -1) break;
            off += r;
        }
        if (off == n) return data;
        byte[] trimmed = new byte[off];
        System.arraycopy(data, 0, trimmed, 0, off);
        return trimmed;
    }
}
