# AGENTS.md

Guidance for AI coding agents and contributors working on this repository.

## What this project is

Meridian ERP desktop is a Java Swing copy of the Meridian ERP web demo. It is a **UI automation target** for the Windows Java Access Bridge (JAB) and [java-access-bridge-wrapper](https://github.com/robocorp/java-access-bridge-wrapper).

Two things are effectively public API:
- **Parity with the web app.** The same t-codes, master data, rules, status messages and number ranges let the web app's test data and answer keys work here too.
- **The accessible names robots use.**

See `README.md` for the full contract.

## Build and run

- `build.bat` compiles with `javac --release 8` into `out\` and packages `dist\MeridianErp.jar`. It needs JDK 9 or newer on the PATH.
- `run.bat [--hard] [--suffix=NAME]` builds if the jar is missing, then starts the app.
- There is no Maven/Gradle and there are no dependencies. Don't add either without being asked.
- There is no automated test suite in the repository yet (see "Verifying" below).

## The web app is the source of truth

- **Rules and messages:** `src/meridian/data/Fb60.java` and `Va01.java` are 1:1 ports of `src/screens/fi.ts` (`checkFb60`/`postFb60`) and `src/screens/sd.ts` (`checkSalesArea`/`checkVa01`/`saveVa01`).
  - Keep the same check order, field names and message texts, character for character (including double spaces).
  - When the web app changes, port the change. Don't "improve" a message on one side only.
  - The save/post messages and the routing (`/go`, not-authorized, unknown t-code) come from `src/index.ts`.
- **Master data:** `MasterData.java` mirrors `src/data/master.ts`. `ApMaster.java` is **generated**. Regenerate it from this folder with `python tools/gen_ap_master.py <web-app>/src/data/ap-master.json src/meridian/data/ApMaster.java` and never edit it by hand.
- **Screen titles, labels and popup texts** follow `src/screens/*.ts` and `src/ui.ts`.

## Hard rules

1. **Java 8 source level.** No `var`, records, text blocks, switch expressions, `List.of`, or other APIs newer than Java 8.
2. **Input fields are named by their SAP technical name** (`INVFO-ACCNT`, `RV45A-MABNR-3`), which is the web form's `name`.
   - Create fields with `Widgets.field(name, label, columns)`, which sets the accessible name and puts the label in the description.
   - Register fields with `reg(...)` in the screen, so `showError` can mark them.
   - Lay out with `FormPanel.addField(label, field, extras...)`, which sets `labelFor`.
3. **Readable values are labels with a fixed name.** Update them with `A11y.setValue`, which sets the text and the accessible description.
   - Where the web app has an element id (`doc-number`, `balance-amt`, `kpi-docs`, ...), use it as the name.
   - Descriptions next to fields are named `DESC-<field>`.
4. **The status bar text must be the exact message, with no prefix.**
   - Report results with `shell.status().show(Message.success/warning/error/info(...))`.
   - Show validation errors with `showError(FieldError)`. It shows only the **first** error, as the web does.
5. **Screen actions go through `shell.run(...)`.** It clears the status bar and adds the hard-mode latency. Wrap every action that corresponds to a web request to `/tx/...` (Enter, Save/Post, Simulate, Continue, display queries, admin actions).
6. **Modal popups must open on the next event:** `SwingUtilities.invokeLater` or `A11y.deferred(...)`. A JAB click waits for the action to return, so an inline modal dialog would block the robot.
7. **Use `MessageDialog`, not `JOptionPane`**, so dialog titles and button names are controlled.
8. **Each screen's `start()` resets the screen** like a fresh page load. Screens are created once and reused.
9. **No Swing in `meridian.data`.** It is the "server" side and must stay UI-free.
10. **Determinism.**
    - Zero state on every launch.
    - `Locale.US`.
    - Amounts through `Fmt.amount` (`1,234.56`), dates through `Fmt.date` (`MM/DD/YYYY`).
11. **Swing threading.** All UI and `ErpSystem` access happens on the Event Dispatch Thread.

## Adding to a transaction (typical pattern)

1. Port the rule or field from the web app into `Fb60`/`Va01` (the `Form` field, the check, the save).
2. In the screen: create and register the field, add it to the right `FormPanel`/tab, read it in `readForm()`, and show the derived values in `display(...)`.
3. Update `README.md` (contract, exceptions).
4. Run `build.bat` and verify through JAB.

A new transaction gets a `Screen` subclass, an entry in `MainFrame` (the `transactions` map, built in the constructor), a leaf in `HomeScreen.buildMenu()`, and a menu item if the web app has one.

## Verifying through the Java Access Bridge

- **Setup:**
  - JAB must be enabled: `"%JAVA_HOME%\bin\jabswitch" -enable`.
  - Install the wrapper with `pip install java-access-bridge-wrapper` and set `RC_JAVA_ACCESS_BRIDGE_DLL=%JAVA_HOME%\bin\WindowsAccessBridge-64.dll`.
  - Create `JavaAccessBridgeWrapper` in a background thread that runs a Windows message pump. Then call `switch_window_by_title("Meridian ERP")` and build a `ContextTree`.
- **Locating elements:**
  - Use strict locators (`SearchElement("name", "INVFO-ACCNT", True)`), and prefer nodes whose states include `showing`.
  - Without callbacks the tree is a snapshot. Rebuild it after every navigation or action.
- **Typical flow:** log on as `RPA_BOT`, then:
  - FB60: post an invoice and check `Document 1900000001 was posted in company code 1000`.
  - VA01: save an order and check `Standard Order 5000001 has been saved`.
  - Try the exception cases from the README table.
- **Cleanup:** close any app instances you start, and don't leave `jab_wrapper.log` or scratch scripts in the repository.
