# BTCPay stores: POS vs Links

BTCPay's payment methods and checkout appearance are **per store**, so the
terminal and shared invoice links use **two stores**:

| Setting | **POS store** (terminal) | **Links store** (customer links) |
|---|---|---|
| Store name | `Sovereign Technology S.A.S. de C.V.` | `SovTech Links` |
| Store id | `2sg7cNkRW52iT624jHeVdN2V73sEfMnqvt15vu3yzbik` | `LmEWuY3RUaLB9F2BfdDqsNGTFipisPU3BhULEcF8ehmHgt5R1ZuS` |
| CASH (MarkPaid) | **enabled** | disabled |
| "Show 'Pay in wallet' button" | **off** | **on** |
| Display Lightning amounts in Satoshis | **on** | **on** |
| Unify on-chain + lightning QR | **on** (sats everywhere) | **on** |

The terminal's PoS app points at the **POS store**; customer invoice links are
created from the **Links store**.

## Where each setting lives (BTCPay UI)

- **Payment methods (CASH)** — store → **Payment methods** / the **MarkPaid**
  store nav entry.
- **"Pay in wallet" + sats + unify** — store → **Settings → Checkout Appearance**:
  - "Show 'Pay in wallet' button"
  - "Display Lightning payment amounts in Satoshis"
  - "Unify on-chain and lightning payment URL/QR code"

## Sats display

`LightningAmountInSatoshi` shows Lightning amounts in sats; with
`OnChainWithLnInvoiceFallback` (unify) enabled, on-chain amounts show sats too.
Both stores have both enabled, so amounts read e.g. `12 sats` instead of
`0.00000012 BTC`.

## Remaining setup for the Links store

The Links store was created as a shell with the toggles above. It still needs a
**wallet / Lightning node connected** (a UI action with your keys):

1. BTCPay → **SovTech Links** → connect the Lightning node (and/or an on-chain
   wallet) the same way the POS store is connected.
2. Create a test invoice from the Links store and confirm the checkout shows the
   **"Pay in wallet"** button, **sats**, and **no CASH**.
3. From the terminal (POS store) confirm the checkout shows **CASH**, **sats**,
   and **no "Pay in wallet"** button.

## Applied via (record)

The POS store's `StoreBlob` (wallet off, sats on, unify on) and the Links store
row were applied directly in the BTCPay Postgres DB on advin-vps
(`btcpayservermainnet`), then `kata-btcpayserver.service` restarted. Backup of
the pre-change blob: `advin-vps:/root/btcpay-store-backup-20261002/`.
