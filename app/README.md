# Meridian ERP desktop

A Java Swing desktop version of the Meridian ERP web demo, the SAP GUI–style demo target for RPA. It's built to showcase and test [java-access-bridge-wrapper](https://github.com/robocorp/java-access-bridge-wrapper) (`JABWrapper`) through the Windows Java Access Bridge.

The desktop app uses the **same transaction codes, master data, business rules, status-bar messages and number ranges** as the web app. Robot input documents and answer keys written for the web app therefore work against both versions.

## Requirements

- **Windows, 64-bit.**
- **JDK 9 or newer to build.** The jar targets Java 8, so it runs on any Java 8+ runtime.
- **Java Access Bridge enabled.** Run this once per user:
  ```
  "%JAVA_HOME%\bin\jabswitch" -enable
  ```

## Build and run

```
build.bat                       # -> dist\MeridianErp.jar
run.bat                         # builds if needed, then starts the app
run.bat --hard                  # start with hard mode on
run.bat --suffix=Bot1           # window title "Meridian ERP - Bot1"
```

There are no third-party dependencies.

```
git clone https://github.com/raivolink/meridian-erp-desktop.git
cd meridian-erp-desktop\app
build.bat
```

A robot that uses the app is in [`../robot`](../robot).

## Robot demo (for recordings)

```
demo.bat                        # starts the app and drives it through the Java Access Bridge
demo.bat --speed 0.7            # slower (2 = twice as fast)
demo.bat --start-delay 10       # more time to start the screen recorder
demo.bat --close                # close the app at the end (default: leave it open)
```

The first run creates `.venv` and installs `java-access-bridge-wrapper`. The robot types at a visible pace and prints a caption for each step in the terminal. The steps:
- Log on as `RPA_BOT`.
- FB60: post a vendor invoice (Enter, Simulate, Post), then show the duplicate-invoice error.
- VA01: create a three-item sales order, then a second order that is blocked by the credit limit.
- VA03 and FB03: display the results.
- Return to Easy Access, where the key figures have changed.

Close any open Meridian ERP window first, so the demo starts from the zero state. The script is `demo/meridian_demo.py`.

## Users

| User | Password | Role |
|---|---|---|
| `DEMO` | `Demo#2026` | ADMIN (can use ZDEMO_ADMIN) |
| `RPA_BOT` | `Robot#2026` | USER |
| `APCLERK` | `Clerk#2026` | USER |

Client `100`, logon language `EN`.

## Transactions

| T-code | Screen | Notes |
|---|---|---|
| `FB60` | Enter Vendor Invoice | Asks for the company code (`1000`) once per session. Tabs: Basic data / Payment / Details. G/L line items, a live balance light, Simulate, Post. |
| `FB03` | Display Document | Document list and key fields, then the document detail. |
| `VA01` | Create Sales Order | Initial screen (order type `OR`/`RO`, sales area `1000/10/00`), then the overview with items, pricing, stock confirmation and credit check. |
| `VA03` | Display Sales Order | Order list, then the order detail. |
| `ZDEMO_ADMIN` | Demo Maintenance | Reset to zero state, hard mode toggle, master data lists. ADMIN only. |

- **Starting a transaction:** type the t-code in the command field (`/nFB60` works too), or double-click it in the Easy Access tree.
- **Navigation commands:** `/n` returns to Easy Access, and `/nex` logs off.
- **Keys:** F3, Shift+F3 and F12 go back to Easy Access. Ctrl+S saves or posts, Enter checks, and F4 opens value help on fields that have it.

## Robot-facing contract

- **Input fields are named by their SAP technical name.** The accessible name is, for example, `INVFO-ACCNT` (vendor), `INVFO-BLDAT`, `INVFO-XBLNR`, `INVFO-WRBTR`, `ACGL_ITEM-HKONT-1` / `-WRBTR-1` / `-KOSTL-1`, `VBAK-AUART`, `KUAGV-KUNNR`, `VBKD-BSTKD`, `RV45A-KETDAT`, or `RV45A-MABNR-1` / `RV45A-KWMENG-1`.
  - These are the same names as the web form fields and the answer-key column headers.
  - Each field's visible label is its accessible description, and the caption label is linked with `labelFor`.
  - Item rows are numbered from 1. **Insert rows** adds 5 more.
- **The status bar holds the result.**
  - `Status Bar` (a label) holds the exact message text, e.g. `Document 1900000001 was posted in company code 1000` or `Standard Order 5000001 has been saved`.
  - `Message Type` (a label) has `S`, `W`, `E` or `I` as its accessible description.
  - The message is cleared when an action starts, so a robot can wait for it to become non-empty.
- **Readable values are labels with a fixed name.** The value is in the label's text and accessible description:
  - Shell: `Screen Title` (the page title, e.g. `Enter Vendor Invoice: Company Code 1000`), `Current Transaction` and `User`.
  - FB60: `balance-amt` and `balance-light` (`red`/`green`).
  - Descriptions next to fields: `DESC-<field>`, e.g. `DESC-INVFO-ACCNT`.
  - VA01 items: `VBAP-NETPR-n`, `VBAP-NETWR-n`, `VBEP-BMENG-n` (confirmed quantity).
  - FB03: `doc-number`, `doc-reference`. VA03: `order-number`, `order-custpo`.
  - Easy Access: `kpi-docs`, `kpi-orders`. ZDEMO_ADMIN: `adm-docs`, `adm-orders`, `adm-hard`.
- **Tables use the web app's element ids as names:** `doc-list`, `doc-lines`, `order-list`, `order-items`, `simulation`, `adm-users`, `adm-vendors`, `adm-customers`, `adm-materials`, `f4-results`.
- **Window titles:**
  - Main window: `Meridian ERP`.
  - Popups: `Enter Company Code`, `Information`, `Reset Demo Data`, `License Information for Multiple Logons`.
  - F4 value help: the help's own title, e.g. `Vendors`.
- **Dates and amounts:** dates accept `MM/DD/YYYY`, `YYYY-MM-DD` and `DD.MM.YYYY`. Amounts accept `1,234.56`.

### Built-in business exceptions (always on)

| Case | Trigger |
|---|---|
| Duplicate invoice | Same vendor + reference already posted → `E` |
| Vendor posting block | `V-2015` (WeWork) |
| Missing CO object | G/L ≥ 5000 without cost center |
| Closed posting period | Posting date older than previous month |
| Balance not zero | Header amount ≠ lines (+ tax when *Calculate tax* is on) |
| Customer order block | `100019` Summit Health Clinics |
| Credit limit | `100025` Yellowstone Outfitters (limit 5,000) → saved with credit block (`W`) |
| Duplicate customer PO | Same sold-to + PO number → `E` |
| Material flagged for deletion | `HT-1250` |
| Low stock / partial confirmation | `HT-1270` (3 EA), `HT-1190` (8 EA) → `W` |

### Hard mode (toggle in ZDEMO_ADMIN, or start with `--hard`)

Hard mode adds three obstacles:
- **Latency:** every transaction call and screen action takes 0.5–3 s. The window shows a wait cursor, and clicks made while it's busy are dropped.
- **Random popups:** an `Information` popup sometimes appears on FB60 and VA01 (40% chance).
- **Multiple-logon dialog:** the `License Information for Multiple Logons` dialog appears after logon.

Names and labels stay stable. The web app's randomized element ids have no desktop equivalent.

## Differences from the web app

- **Data is in memory.** Every launch starts in the zero state: master data only, invoices numbered from `1900000001`, orders from `5000001`, hard mode off. ZDEMO_ADMIN can reset it at any time. There's no Postgres and no JSON API. To verify results, read FB03/VA03 or the status bar.
- **Field values are copied from the web app, not shared with it.** Master data comes from `src/data/master.ts` and `ap-master.json`, and the rules and messages from `src/screens/fi.ts` and `sd.ts`.
- **Command field:** pressing Enter with an empty command field runs the screen's Enter (check), as in SAP GUI. The web app goes to Easy Access instead.

## Automation notes

- **Use strict matching:** `SearchElement("name", "INVFO-ACCNT", True)`. Non-strict matching is a regex prefix match, so `INVFO-WRBTR` would also match `INVFO-WRBTR-…`.
- **Prefer elements whose states include `showing`.** All screens live in one window, and some names exist on two screens; `VBAK-VBELN` is on both VA01 and VA03.
- **Role names:**
  - The password field's role is `password text`.
  - Combo boxes (`ACGL_HEAD-TRANS`, `ACGL_ITEM-SHKZG-n`, `VBKD-INCO1`, `VBAK-VSBED`) are set through the JAB selection API.
- **Popups:** after an action that opens one, call `switch_window_by_title(...)` and build a new `ContextTree`. Popups open on the next event, so a JAB `click()` never blocks.
- **Hard mode synchronization:** wait for `Current Transaction`, `Screen Title` or the status bar to change before the next step.

## Project structure

```
build.bat, run.bat               build / start scripts (plain javac + jar)
tools/gen_ap_master.py           regenerates ApMaster.java from the web app's ap-master.json
src/meridian/
  MeridianApp.java               entry point (--hard, --suffix), fixed Locale.US
  data/                          the "server": no Swing here
    ErpSystem.java               in-memory database: zero state, logon, lookups, F4 value help, number ranges
    MasterData.java, ApMaster.java   zero-state master data (ApMaster is generated)
    Fb60.java, Va01.java         validation, posting and saving, ported 1:1 from the web app
    Fmt.java                     lenient date/amount parsing and US formatting
    Vendor, Customer, Material, ... , FiDocument, SalesOrder   records
  ui/
    MainFrame.java               shell: menus, system toolbar + command field, title, app toolbar,
                                 screen switching, hard mode latency and popups
    LogonScreen, HomeScreen, Fb60Screen, Fb03Screen, Va01Screen, Va03Screen, AdminScreen
    CompanyCodeDialog, MultiLogonDialog, ValueHelpDialog, MessageDialog
    Screen.java                  base class: field registry, error marking (red border, tab switch, focus)
    Widgets, FormPanel, ItemGrid, ListTableModel, StatusBar, A11y, ErpTheme, Icons
```

The data layer works like the web app's request handlers. A screen reads its fields into a `Form`, calls `Fb60.check` or `Va01.check`, shows the first error (or warning), and on save calls `post` or `save`. The status bar shows the resulting message.

If you are changing the code, read [AGENTS.md](AGENTS.md) first.
