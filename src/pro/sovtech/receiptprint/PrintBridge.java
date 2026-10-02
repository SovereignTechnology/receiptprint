package pro.sovtech.receiptprint;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import net.nyx.printerservice.print.IPrinterService;
import net.nyx.printerservice.print.PrintTextFormat;

/**
 * Binds to the vendor thermal printer service (net.nyx.printerservice) and
 * exposes simple print helpers. The service is exported with no permission,
 * so any app may bind to it.
 */
public class PrintBridge {

    private static final String TAG = "ReceiptPrint";
    public static final String PRINTER_PKG = "net.nyx.printerservice";
    public static final String PRINTER_ACTION = "net.nyx.printerservice.IPrinterService";

    private final Context ctx;
    private volatile IPrinterService svc;
    private volatile boolean bound = false;
    private final Object lock = new Object();

    private final ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            svc = IPrinterService.Stub.asInterface(service);
            bound = true;
            synchronized (lock) { lock.notifyAll(); }
            Log.i(TAG, "printer service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            svc = null;
            bound = false;
            Log.w(TAG, "printer service disconnected");
        }
    };

    public PrintBridge(Context context) {
        this.ctx = context.getApplicationContext();
    }

    public void bind() {
        if (bound) return;
        Intent intent = new Intent();
        intent.setPackage(PRINTER_PKG);
        intent.setAction(PRINTER_ACTION);
        try {
            ctx.bindService(intent, conn, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            Log.e(TAG, "bindService failed", e);
        }
    }

    public void unbind() {
        try {
            if (bound) ctx.unbindService(conn);
        } catch (Exception ignored) {
        }
        bound = false;
        svc = null;
    }

    /** Wait up to timeoutMs for the service to connect. */
    public boolean awaitConnection(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        synchronized (lock) {
            while (svc == null && System.currentTimeMillis() < deadline) {
                try {
                    lock.wait(Math.max(1, deadline - System.currentTimeMillis()));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return svc != null;
    }

    public boolean isConnected() {
        return svc != null;
    }

    public String serviceVersion() {
        try {
            return svc != null ? svc.getServiceVersion() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public int printerStatus() {
        try {
            return svc != null ? svc.getPrinterStatus() : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    public static PrintTextFormat receiptFormat() {
        PrintTextFormat f = new PrintTextFormat();
        f.setFont(4);            // MONOSPACE so columns line up
        f.setTextSize(22);
        f.setAli(0);             // left
        f.setLineSpacing(2);
        return f;
    }

    /** Print text and feed to the tear-off position. Returns true on success. */
    public boolean printText(String text) {
        if (svc == null) return false;
        try {
            int ret = svc.printText(text, receiptFormat());
            if (ret != 0) {
                Log.w(TAG, "printText ret=" + ret);
            }
            svc.printEndAutoOut();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "printText failed", e);
            return false;
        }
    }

    public boolean openCashBox() {
        if (svc == null) return false;
        try {
            return svc.openCashBox() == 0;
        } catch (Exception e) {
            Log.e(TAG, "openCashBox failed", e);
            return false;
        }
    }
}
