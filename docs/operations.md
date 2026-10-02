# Terminal operations

## Power off

The Android power menu is **disabled by the kiosk lock task** by default
(`device.owner.lock_task_feature.global_actions = false`), so a long power-button
press only reboots. It is now **enabled**, so:

- **Long-press the power button** → **Power off** (menu: Emergency / Power off /
  Restart / Screenshot).

Via ADB:

```sh
adb shell svc power shutdown      # power off
adb shell svc power reboot        # restart
```

To hide the power menu again (e.g. stricter kiosk), set
`device.owner.lock_task_feature.global_actions = false` in the kiosk settings.

## Hide the "Open in wallet" button (POS, not a personal device)

The BTCPay checkout shows a **"Pay in wallet"** button that tries to open a
wallet app on the device — undesirable on a POS. Disable it store-wide:

1. BTCPay → your store → **Settings → Checkout Appearance**.
2. Uncheck **Show "Pay in wallet" button**.
3. Save.

(The MarkPaid **CASH** method renders its own **Mark Settled** button, so cash
sales are unaffected.)

## Demo from a phone hotspot

The terminal reaches BTCPay over whatever Wi-Fi it is joined to. The **kiosk
URL whitelist (`pay.sovtech.pro`) is enforced on the device**, so it holds on any
network — the GL.iNet router egress rule is production-only defence in depth and
does not travel with the device.

For a clean demo:

- **Pre-add the hotspot SSID** to the terminal so it auto-connects (unlock the
  kiosk → Settings → More → Wi-Fi, or add it ahead of time).
- Or carry a small **travel router** programmed with the production SSID, so the
  terminal behaves identically and you never touch its Wi-Fi.
- **Avoid captive portals** — the kiosk browser cannot complete a portal login.
  Use a plain phone hotspot or a router that handles the portal itself.
