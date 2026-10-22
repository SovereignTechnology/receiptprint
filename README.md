# sovtech-receiptprint

> **Source of truth:** [`nostr://npub1s0vtechh66tx7vrwdud8zfyheu9zca7swwfrzd4qu2a4f93mxs6qvn9adx/relay.ngit.dev/receiptprint`](https://gitworkshop.dev/npub1s0vtechh66tx7vrwdud8zfyheu9zca7swwfrzd4qu2a4f93mxs6qvn9adx/relay.ngit.dev/receiptprint) — issues and pull requests live there. This GitHub repository is a mirror; push to the Nostr remote, not here.

A tiny Android helper that prints BTCPay Server receipts to the **built-in
thermal printer** of a NYX / Miray POS terminal (Unisoc `TPM4G_E9863`, Android 12).

Built for the SovTech BTC21 Pro payment terminal, where the kiosk browser
([Webview Kiosk](https://github.com/nktnet1/webview-kiosk)) cannot print and the
vendor printer is not exposed to Android's print framework. This app bridges the
gap.

## Why this exists

The terminal's built-in printer is only reachable through the vendor's private
AIDL service (`net.nyx.printerservice.print.IPrinterService`). Neither Android's
print framework nor any browser can reach it:

- the vendor `PrintService` plugin only advertises *Bluetooth* printers;
- Android `WebView` has no `window.print()` support at all.

So a small helper app binds the vendor service and exposes a loopback HTTP
endpoint that the kiosk browser can call.

## How it works

```
BTCPay receipt page (kiosk browser)
        │  injected JS: "Print receipt" button
        ▼  POST http://127.0.0.1:8088/print
pro.sovtech.receiptprint  (foreground service)
        │  bindService(net.nyx.printerservice)
        ▼  IPrinterService.printText() + printEndAutoOut()
built-in thermal printer (/dev/ttySPRD2)
```

## Endpoints (bound to 127.0.0.1 only)

| Method | Path         | Body                        | Action                          |
|--------|--------------|-----------------------------|---------------------------------|
| GET    | `/status`    | –                           | `{"connected":bool,"version":str,"status":int}` |
| POST   | `/print`     | plain text or `{"text":"…"}`| print the text, feed to tear-off |
| POST   | `/cashbox`   | –                           | open the cash drawer            |
| POST   | `/clipboard` | text                        | set the system clipboard (setup helper) |

## Build

`build.sh` performs a manual Android build (no Gradle): `aidl` → `javac` → `d8`
→ `aapt2` → `zipalign` → `apksigner`.

It expects a JDK and Android SDK under `toolchain/` by default (override with
`RECEIPT_TOOLCHAIN`):

```
toolchain/
├── jdk/                              # JDK 17 (javac)
└── android-sdk/
    ├── build-tools/34.0.0/           # aidl, d8, aapt2, zipalign, apksigner
    └── platforms/android-34/android.jar
```

Install them with the Android command-line tools:

```sh
sdkmanager "platforms;android-34" "build-tools;34.0.0"
```

Then:

```sh
./build.sh          # produces receiptprint.apk
adb install -r receiptprint.apk
```

## Deploy

```sh
adb install -r receiptprint.apk
adb shell am start -n pro.sovtech.receiptprint/.MainActivity   # starts the service
# optional: keep it alive against Doze
adb shell cmd deviceidle whitelist +pro.sovtech.receiptprint
```

The app auto-starts on boot (`BootReceiver`) and runs as a foreground service.

## Kiosk integration (manual trigger)

`kiosk/print_button.js` is injected into Webview Kiosk via the setting
`js_scripts.custom_script_on_page_finish` (see `kiosk/print_button.js`). It adds
a green **"Print receipt"** button on receipt pages (detected by the presence of
`Order ID` / `Total` + `Paid`) which POSTs the page text to the helper.

To install it, import this JSON through Webview Kiosk → Settings → Import:

```json
{ "js_scripts.custom_script_on_page_finish": "<contents of kiosk/print_button.js>" }
```

Chromium treats `http://127.0.0.1` as a trustworthy origin, so the `fetch()` from
the HTTPS receipt page is not blocked as mixed content.

## Notes / limitations

- The helper must be running for the button to work; the foreground service and
  the boot receiver keep it up.
- The `aidl/` interface and `src/net/nyx/printerservice/print/PrintTextFormat.java`
  come from the vendor SDK, republished in the MIT-licensed demo
  [SZNoryox/PrinterClient](https://github.com/SZNoryox/PrinterClient). See `LICENSE`.
- Printing is **manual** (button tap). Auto-print on payment is possible by
  calling the same endpoint from the injected script without a button.
