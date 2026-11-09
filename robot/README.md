# Meridian ERP desktop – producer–consumer robot example

A Robocorp robot that automates [Meridian ERP desktop](../app) (a Java Swing app) through the Windows **Java Access Bridge** with [`RPA.JavaAccessBridge`](https://rpaframework.org/libraries/javaaccessbridge/). It follows the producer–consumer model with work items, in the spirit of [robocorp/example-java-automation](https://github.com/robocorp/example-java-automation).

| Step | Task | What it does |
|---|---|---|
| 1 | `Produce Cases` | Creates 10 work items, one per case: 5 vendor invoices and 5 sales orders. |
| 2 | `Process Cases` | Starts Meridian ERP, logs on as `RPA_BOT`, and enters each case (**FB60** for invoices, **VA01** for sales orders). Each case gets a result work item. |

## The 10 cases

| # | Case | Transaction | Designed outcome | Work item |
|---|---|---|---|---|
| 1 | Market data invoice from Bloomberg | FB60 | `POSTED` (document 1900000001) | done |
| 2 | Refinitiv invoice with NYC sales tax 8.875 % | FB60 | `POSTED` (tax line added by the ERP) | done |
| 3 | The same Bloomberg invoice again | FB60 | `DUPLICATE_INVOICE` | failed, BUSINESS |
| 4 | WeWork rent, vendor `V-2015` has a posting block | FB60 | `VENDOR_BLOCKED` | failed, BUSINESS |
| 5 | FactSet invoice whose lines don't add up | FB60 | `BALANCE_NOT_ZERO` | failed, BUSINESS |
| 6 | Standard order from Cedar Ridge Medical Center | VA01 | `SAVED` | done |
| 7 | Yellowstone Outfitters order above its 5,000 USD credit limit | VA01 | `SAVED_WITH_WARNING` (credit block) | done |
| 8 | Bluewater order for 5 label printers, 3 in stock | VA01 | `SAVED_WITH_WARNING` (partial confirmation) | done |
| 9 | Order from Summit Health Clinics, which has an order block | VA01 | `CUSTOMER_BLOCKED` | failed, BUSINESS |
| 10 | Juniper Software order for a discontinued shredder | VA01 | `MATERIAL_DELETED` | failed, BUSINESS |

The cases are defined in `cases.py`. Dates are relative to today, so the posting period is always open.

A rejection fails its work item as a **BUSINESS** exception. The exception has a code and a message saying what to do, followed by the ERP's own status-bar text. For example:

> `VENDOR_BLOCKED` – Invoice WW-20261029-04 from V-2015 was not entered: The vendor has a posting block. Check with vendor master data before paying. (ERP: Vendor V-2015 is blocked for posting in company code 1000)

Technical problems (an element not found, the app not starting) fail the item as **APPLICATION** / `ERP_UI_ERROR`. The robot then continues with the next case.

Orders saved with a warning are reported as `SAVED_WITH_WARNING`, not failed. The order exists and needs follow-up, and a retry would only hit `DUPLICATE_PO`.

At the end, the consumer prints a table comparing each case's expected and actual outcome.

## Result work items

`Process Cases` creates one output work item per case:

```json
{
  "case": 7,
  "title": "Yellowstone Outfitters order above its 5,000 USD credit limit",
  "reference": "YO-20261029-07",
  "expected": "SAVED_WITH_WARNING",
  "outcome": "SAVED_WITH_WARNING",
  "as_expected": true,
  "status": "SAVED_WITH_WARNING",
  "order_number": "5000002",
  "message": "Standard Order 5000002 has been saved (blocked: credit limit exceeded)"
}
```

`status` is `POSTED`, `SAVED`, `SAVED_WITH_WARNING`, `REJECTED` or `ERROR`. On success the item also has `document_number` (invoices) or `order_number` (orders); on a rejection it has the error `code`.

## How it works

- **Nothing to install on the worker.** `conda.yaml` brings OpenJDK, which includes `windowsaccessbridge-64.dll`. The robot starts Meridian ERP with `-Djavax.accessibility.assistive_technologies=com.sun.java.accessibility.AccessBridge`, which switches the Access Bridge on for that JVM only. There's no `jabswitch -enable` and no Java installation needed on the machine.
- **A fresh ERP every run.** The consumer starts its own instance of the bundled `meridian/MeridianErp.jar`, titled "Meridian ERP - Robot". The ERP keeps its data in memory, so every run starts from the zero state, which the designed outcomes assume. It selects the window by process ID, so a Meridian window you have open is never touched, and it closes the app at the end.
- **Elements are found by SAP technical name.** Input fields are named `INVFO-ACCNT`, `ACGL_ITEM-HKONT-1`, `KUAGV-KUNNR`, `RV45A-MABNR-1` and so on, the same names as in the Meridian web app. The robot uses strict locators (`role:text and name:INVFO-ACCNT`).
- **Results come from the status bar.** The `Status Bar` label holds the exact message, and `Message Type` holds S, W, E or I.
- **No keyboard or mouse.** Text is set with the Access Bridge's `insert_text` and buttons are pressed with their click action. The window doesn't need to be in front, and the run can't be disturbed by someone typing.
- **The element tree is reused.** Building it takes about 2 seconds, and all screens live in one window, so `meridian.py` reads each element's `showing` state live and refreshes the tree only when an element is missing (new item rows, a new toolbar button).

The document payload format matches the Playwright robot for the Meridian web app. The same invoices and orders work for both, except that this example enters only debit lines and doesn't look up partners by name.

## Run locally

You need [rcc](https://github.com/robocorp/rcc) or the Sema4.ai VS Code extension, on Windows (the Java Access Bridge is Windows-only).

```
rcc task run -t "Produce Cases" -e devdata/env-produce.json
rcc task run -t "Process Cases" -e devdata/env-process.json
```

- **Producer:** writes the cases to `devdata/work-items-out/produce/work-items.json`.
- **Consumer:** reads that file and writes the results to `devdata/work-items-out/process/work-items.json`. Meridian ERP opens on screen while it works (about 90 seconds). The log is in `output/log.html`.
- **Demo pace:** set `MERIDIAN_STEP_DELAY` in `devdata/env-process.json`, e.g. `"0.3"` (seconds after each field), for a recording.

The first run builds the environment, which takes a few minutes.

To look at the app without the robot, run `rcc task run -t "Start Meridian ERP"`.

## Control Room

1. Create a **Vault** secret `meridian_erp_desktop` with the keys `username` and `password` (demo user `RPA_BOT` / `Robot#2026`). Locally, the robot reads the same secret from `devdata/vault.json`, which holds only the demo credentials.
2. Create a process with two steps, `Produce Cases` then `Process Cases`. The second step needs a **Windows** worker with a desktop session, because the app has a window even though the robot never uses the keyboard or mouse.

| Variable | Default | |
|---|---|---|
| `MERIDIAN_COMPANY_CODE` | `1000` | Company code for FB60 |
| `MERIDIAN_STEP_DELAY` | `0` | Seconds to wait after each field, for demos |

## Updating the bundled app

`meridian/MeridianErp.jar` is a build of [`../app`](../app). After changing the app, rebuild the jar with a JDK 9+ on PATH:

```
tools\update-jar.bat
```

## Files

```
robot.yaml, conda.yaml     tasks and environment (Python 3.12, OpenJDK 25, robocorp, rpaframework)
tasks.py                   Produce Cases / Process Cases, FB60 and VA01 entry, result work items
cases.py                   the 10 cases
meridian.py                MeridianDesktop: start/stop, find elements, status bar; ERP error -> business exception
meridian/MeridianErp.jar   the app under automation
devdata/                   local work items, env files and demo Vault secret
tools/update-jar.bat       rebuilds the jar from ../app
```
