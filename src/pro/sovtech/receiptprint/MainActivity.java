package pro.sovtech.receiptprint;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView status;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private PrintBridge probe;

    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            String v = probe.serviceVersion();
            status.setText(
                    "Printer service: " + (probe.isConnected() ? "connected" : "not connected") + "\n" +
                    "Service version: " + (v == null ? "-" : v) + "\n" +
                    "HTTP endpoint: http://127.0.0.1:" + PrintHttpServer.PORT + "/print");
            handler.postDelayed(this, 2000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        probe = new PrintBridge(this);
        startService();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 60, 40, 40);

        TextView title = new TextView(this);
        title.setText("Receipt Printer Bridge");
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        status = new TextView(this);
        status.setTextSize(16);
        status.setPadding(0, 30, 0, 30);
        root.addView(status);

        Button test = new Button(this);
        test.setText("Print test receipt");
        test.setOnClickListener(v -> printTest());
        root.addView(test, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button cash = new Button(this);
        cash.setText("Open cash drawer");
        cash.setOnClickListener(v -> new Thread(() -> probe.openCashBox()).start());
        root.addView(cash, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView hint = new TextView(this);
        hint.setText("\nKiosk: use the injected Print button on the receipt page, or POST to the HTTP endpoint.");
        hint.setTextSize(14);
        hint.setTextColor(Color.GRAY);
        root.addView(hint);

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        probe.bind();
        handler.post(refresh);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refresh);
    }

    private void startService() {
        Intent svc = new Intent(this, PrintForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc);
        } else {
            startService(svc);
        }
    }

    private void printTest() {
        String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
        String receipt =
                "      SOVTECH TEST PRINT\n" +
                "--------------------------------\n" +
                "Receipt printer bridge: OK\n" +
                "Service: " + (probe.serviceVersion() == null ? "-" : probe.serviceVersion()) + "\n" +
                "Time: " + ts + "\n" +
                "--------------------------------\n" +
                "\n\n\n";
        new Thread(() -> probe.printText(receipt)).start();
    }
}
