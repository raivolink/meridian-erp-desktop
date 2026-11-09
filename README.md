# Meridian ERP desktop

A SAP GUI–style ERP demo as a Java Swing app, plus a robot that automates it through the Windows **Java Access Bridge**.

| Folder | What it is |
|---|---|
| [`app/`](app) | **Meridian ERP desktop**: vendor invoices (FB60/FB03) and sales orders (VA01/VA03) with SAP-style rules and status-bar messages. Fields are named by their SAP technical names (`INVFO-ACCNT`, `RV45A-MABNR-1`), so robots can find them. Data is in memory and starts from zero on every launch. Also includes a recordable robot demo (`demo.bat`). |
| [`robot/`](robot) | **Producer–consumer robot** (Robocorp / Sema4.ai, `RPA.JavaAccessBridge`) that enters 10 cases (5 invoices, 5 sales orders), each designed to end in a specific outcome: posted, duplicate, blocked vendor, credit limit warning, and so on. It bundles a build of the app and brings its own OpenJDK, so the machine needs no Java setup. |

## Quick start (Windows)

```
cd app
build.bat          :: needs JDK 9+ on PATH
run.bat            :: log on as DEMO / Demo#2026
demo.bat           :: watch a robot drive the app
```

```
cd robot
rcc task run -t "Produce Cases" -e devdata/env-produce.json
rcc task run -t "Process Cases" -e devdata/env-process.json
```

See [app/README.md](app/README.md) and [robot/README.md](robot/README.md) for details.

## License

[MIT](LICENSE)
