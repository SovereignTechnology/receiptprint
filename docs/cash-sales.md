# Cash sales (Option A — BTCPay checkout CASH button)

Cash sales are recorded in **BTCPay** using the **MarkPaid Checkout** plugin, so
they appear in the BTCPay database and reports like any other settled invoice.
The terminal prints the receipt via the `pro.sovtech.receiptprint` bridge.

## Prerequisite (one-time, on the BTCPay server)

The plugin ships with a `CASH` method but it must be **enabled per store**:

1. Log in to `https://pay.sovtech.pro`.
2. Open the store → **Payment methods** (the MarkPaid plugin adds a section here,
   or use the store's **MarkPaid** nav entry).
3. Set **CASH** to **Enabled** and save.

> Without this, the checkout shows only Bitcoin/Lightning and there is no CASH
> button. You can confirm it took effect by creating an invoice and checking that
> the checkout offers a **CASH** method.

Optional: additional methods (e.g. `CREDIT`) can be added in the MarkPaid
**Server settings** textbox (`CASH,CREDIT`), which requires a server restart.

## Terminal flow (operator)

1. On the PoS keypad, enter the **amount**.
2. Tap **Charge** — the BTCPay checkout opens.
3. Select the **CASH** payment method.
4. Tap **Mark Settled**.
5. The settled receipt page appears. Tap **🖨 Print receipt** (bottom-right) to
   print it on the built-in thermal printer.

## What gets recorded

`Mark Settled` calls the plugin's endpoint, which:

- records a **settled payment** for the full invoice amount, and
- moves the invoice `New → Settled`.

The invoice is then visible in BTCPay (Invoices, Reports) with the CASH payment
method, and the receipt carries the order id, amount and timestamp.

## Receipt printing

The kiosk injects a **Print receipt** button on receipt pages (see
`kiosk/print_button.js`). It posts the page text to the helper app's loopback
endpoint (`http://127.0.0.1:8088/print`), which prints via the vendor
`IPrinterService` and feeds the paper to the tear-off.

## Notes

- The CASH payment is recorded at the invoice's **full amount**; partial cash
  payments are not supported by this flow.
- The plugin endpoint is anonymous but only settles invoices that already have a
  CASH prompt and belong to the store — it cannot settle arbitrary invoices.
