# BTCPay stores: POS vs Links

BTCPay's payment methods and checkout appearance are **per store**, so the
terminal and shared invoice links use **two stores**:

| Setting | **POS store** (terminal) | **Links store** (customer links) |
|---|---|---|
| Store name | `SovTech POS` | `SovTech` |
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

## Links store wallet

The **SovTech** (links) store was given the **same wallet as the POS store** by
copying `BTC-CHAIN` (xpub), `BTC-LN` (Lightning node connection) and `BTC-LNURL`
from the POS store's `DerivationStrategies` — **excluding `CASH`**, so the links
checkout stays cash-free. Methods on **SovTech**: `BTC-LN, BTC-CHAIN, BTC-LNURL`.

> Caveat: both stores now derive from the **same xpub** (address reuse across the
> POS and link stores). That is fine for a single merchant, but it is why the two
> stores' on-chain addresses are not disjoint.

## Verification checklist

- **POS store** (terminal): checkout shows **CASH**, **sats**, no **"Pay in wallet"** — verified on the terminal.
- **SovTech** (links): an invoice link shows **"Pay in wallet"**, **sats**, and **no CASH**.

## Plugins and API keys

- **Plugins are installed once, server-wide**, and shared by both stores:
  `MarkPaidCheckout`, `Blink`, `NIP05`, `BTCMap`, `Monero`. A plugin's feature is
  **enabled per store** — MarkPaid CASH is enabled on **SovTech POS** only; the
  **SovTech** (links) store has it disabled, so CASH never appears on link
  checkouts.
- **No API keys exist** (0 in the DB), and the terminal needs none: the PoS uses
  the browser session and the cash "Mark Settled" endpoint is anonymous. API
  keys in BTCPay are scoped per store/user and would only be created
  deliberately (e.g. if the helper app were to create invoices via the API).

## Applied via (record)

The POS store's `StoreBlob` (wallet off, sats on, unify on) and the Links store
row were applied directly in the BTCPay Postgres DB on advin-vps
(`btcpayservermainnet`), then `kata-btcpayserver.service` restarted. Backup of
the pre-change blob: `advin-vps:/root/btcpay-store-backup-20261002/`.
