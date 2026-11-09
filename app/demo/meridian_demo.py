"""Recordable demo: a robot drives Meridian ERP desktop through the Java Access Bridge.

Starts the app, logs on, posts a vendor invoice (FB60), shows a duplicate-invoice error,
creates sales orders (VA01, incl. a credit block), and displays the results (VA03, FB03).
Typing is slowed down so the run can be screen-recorded.

Usage: python demo/meridian_demo.py [--jar dist/MeridianErp.jar] [--speed 1.0] [--start-delay 5] [--close]
"""
import argparse
import ctypes
import datetime
import logging
import os
import queue
import subprocess
import sys
import tempfile
import threading
import time
from ctypes import byref, wintypes

os.environ.setdefault(
    "RC_JAVA_ACCESS_BRIDGE_DLL",
    os.path.join(os.environ.get("JAVA_HOME", ""), "bin", "WindowsAccessBridge-64.dll"),
)
# The wrapper writes jab_wrapper.log into ROBOT_ARTIFACTS (default: the current folder) on import.
os.environ.setdefault("ROBOT_ARTIFACTS", os.path.join(tempfile.gettempdir(), "meridian-demo"))

from JABWrapper import jab_wrapper as _jab_module  # noqa: E402
from JABWrapper.context_tree import ContextTree, SearchElement  # noqa: E402
from JABWrapper.jab_wrapper import JavaAccessBridgeWrapper  # noqa: E402

# Keep the terminal for the demo captions: only warnings from the wrapper, no per-call INFO lines.
logging.getLogger().setLevel(logging.INFO)
_jab_module.logging_stream_handler.setLevel(logging.WARNING)

TITLE = "Meridian ERP"
TODAY = datetime.date.today().strftime("%m/%d/%Y")


def start_bridge():
    """The bridge needs a Windows message pump in the thread that created it."""
    pipe = queue.Queue()

    def pump():
        try:
            jab = JavaAccessBridgeWrapper(ignore_callbacks=True)
            pipe.put(jab)
            msg = byref(wintypes.MSG())
            while ctypes.windll.user32.GetMessageW(msg, 0, 0, 0) > 0:
                ctypes.windll.user32.TranslateMessage(msg)
                ctypes.windll.user32.DispatchMessageW(msg)
        except Exception as err:  # noqa: BLE001
            print("Java Access Bridge error:", err)
            pipe.put(None)

    threading.Thread(target=pump, daemon=True).start()
    jab = pipe.get()
    if jab is None:
        sys.exit("Could not start the Java Access Bridge. Is it enabled (jabswitch -enable)?")
    return jab


class Robot:
    def __init__(self, jab, speed):
        self.jab = jab
        self.speed = speed
        self.window = None
        self.tree = None
        self.main_tree = None

    # ---- pacing and captions ----

    def pause(self, seconds=1.0):
        time.sleep(seconds / self.speed)

    def caption(self, text):
        print(f"\n>>> {text}", flush=True)
        self.pause(1.2)

    # ---- element access ----

    def switch(self, title=TITLE):
        """Targets a window. The main window's element tree is built once and reused; popups are
        new windows every time, so their (small) trees are always built fresh."""
        if title == self.window and title == TITLE:
            return
        self.window = title
        self.jab.switch_window_by_title(title)
        if title == TITLE and self.main_tree is not None:
            self.tree = self.main_tree
        else:
            self.rebuild()

    def rebuild(self):
        """Reads the whole element tree of the current window (about 2 s for the main window)."""
        self.tree = ContextTree(self.jab)
        if self.window == TITLE:
            self.main_tree = self.tree

    def find(self, role, name):
        """Finds a showing element. States are read live, so a cached tree stays usable across screens;
        the tree is only rebuilt when the element isn't in it yet (new rows, new toolbar buttons)."""
        for _ in range(2):
            found = self.tree.get_by_attrs([SearchElement("role", role, True), SearchElement("name", name, True)])
            showing = [n for n in found if self.is_showing(n)]
            if showing:
                return showing[0]
            self.rebuild()
        raise RuntimeError(f"{role} {name!r} not found in window {self.window!r}")

    def is_showing(self, node):
        try:
            return "showing" in self.jab.get_context_info(node.context).states
        except Exception:  # noqa: BLE001 - element was removed from the screen
            return False

    def type(self, name, value, role="text"):
        """Types like a person: focus the field, then one character at a time."""
        field = self.find(role, name)
        field.request_focus()
        for i in range(1, len(value) + 1):
            field.insert_text(value[:i])
            time.sleep(0.05 / self.speed)
        self.pause(0.3)

    def click(self, name, wait=1.0):
        self.find("push button", name).click()
        self.pause(wait)

    def label(self, name):
        return self.jab.get_context_info(self.find("label", name).context).description

    def status(self):
        self.switch(TITLE)
        return self.label("Message Type"), self.label("Status Bar")

    def report(self):
        msg_type, text = self.status()
        kind = {"S": "success", "W": "warning", "E": "error", "I": "info"}.get(msg_type, "")
        print(f"    status bar ({kind}): {text}", flush=True)
        self.pause(2.0)
        return text

    def tcode(self, code):
        self.switch(TITLE)
        self.type("Command Field", code)
        self.click("Enter", wait=1.2)

    def wait_window(self, title, timeout=20):
        end = time.time() + timeout
        while time.time() < end:
            if any(w.title == title for w in self.jab.get_windows()):
                return
            time.sleep(0.3)
        raise RuntimeError(f"Window {title!r} did not appear")


def demo(r):
    r.caption("Log on as the RPA robot user")
    r.type("RSYST-BNAME", "RPA_BOT")
    r.type("RSYST-BCODE", "Robot#2026", role="password text")
    r.click("Log On", wait=2.0)
    print("    screen:", r.label("Screen Title"))

    r.caption("FB60 - enter a vendor invoice")
    r.tcode("FB60")
    r.wait_window("Enter Company Code")
    r.switch("Enter Company Code")
    r.type("BKPF-BUKRS", "1000")
    r.click("Continue", wait=1.5)
    r.switch()
    r.type("INVFO-ACCNT", "V-1001")
    r.type("INVFO-BLDAT", TODAY)
    r.type("INVFO-XBLNR", "INV-2026-0417")
    r.type("INVFO-WRBTR", "4,850.00")
    r.type("INVFO-SGTXT", "Market data subscription")
    r.type("ACGL_ITEM-HKONT-1", "6300")
    r.type("ACGL_ITEM-WRBTR-1", "4850.00")
    r.type("ACGL_ITEM-KOSTL-1", "CC100")
    r.type("ACGL_ITEM-SGTXT-1", "Terminal licenses Q4")

    r.caption("Press Enter: vendor data, due date and the balance light are filled in")
    r.click("Enter", wait=1.5)
    print(f"    vendor: {r.label('VD-NAME')}, balance {r.label('balance-amt')} ({r.label('balance-light')})")

    r.caption("Simulate the posting")
    r.click("Simulate", wait=2.5)
    r.report()

    r.caption("Post the invoice")
    r.click("Post", wait=1.5)
    r.report()

    r.caption("Try to post the same invoice again - the duplicate check stops it")
    r.type("INVFO-ACCNT", "V-1001")
    r.type("INVFO-XBLNR", "INV-2026-0417")
    r.click("Enter", wait=1.5)
    r.report()

    r.caption("VA01 - create a standard sales order")
    r.tcode("/nVA01")
    r.type("VBAK-AUART", "OR")
    r.type("VBAK-VKORG", "1000")
    r.type("VBAK-VTWEG", "10")
    r.type("VBAK-SPART", "00")
    r.click("Continue", wait=1.5)
    r.type("KUAGV-KUNNR", "100003")
    r.type("VBKD-BSTKD", "PO-CRMC-7781")
    r.type("RV45A-MABNR-1", "HT-1010")
    r.type("RV45A-KWMENG-1", "5")
    r.type("RV45A-MABNR-2", "HT-1035")
    r.type("RV45A-KWMENG-2", "10")
    r.type("RV45A-MABNR-3", "HT-1070")
    r.type("RV45A-KWMENG-3", "5")

    r.caption("Press Enter: prices, net value and confirmed quantities")
    r.click("Enter", wait=1.5)
    print(f"    sold-to: {r.label('DESC-KUAGV-KUNNR')}")
    print(f"    item 10: {r.label('DESC-RV45A-MABNR-1')}  {r.label('VBAP-NETWR-1')} USD")
    r.pause(2.0)

    r.caption("Save the order")
    r.click("Save Order", wait=1.5)
    r.report()

    r.caption("A second order for Yellowstone Outfitters exceeds its 5,000 USD credit limit")
    r.click("Continue", wait=1.5)
    r.type("KUAGV-KUNNR", "100025")
    r.type("RV45A-MABNR-1", "HT-1000")
    r.type("RV45A-KWMENG-1", "8")
    r.click("Enter", wait=1.5)
    r.report()
    r.click("Save Order", wait=1.5)
    r.report()

    r.caption("VA03 - display the first order")
    r.tcode("/nVA03")
    r.type("VBAK-VBELN", "5000001")
    r.click("Enter", wait=1.5)
    print("    screen:", r.label("Screen Title"))
    print("    net value:", r.label("order-net"))
    r.pause(3.0)

    r.caption("FB03 - display the posted invoice")
    r.tcode("/nFB03")
    r.type("RF05L-BELNR", "1900000001")
    r.click("Enter", wait=1.5)
    print("    document:", r.label("doc-number"), "-", r.label("doc-amount"))
    r.pause(3.0)

    r.caption("Back to Easy Access - the key figures show what the robot did")
    r.click("Back", wait=1.5)
    print(f"    invoices posted: {r.label('kpi-docs')}, sales orders: {r.label('kpi-orders')}")
    r.pause(3.0)


def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--jar", default=os.path.join(root, "dist", "MeridianErp.jar"))
    parser.add_argument("--speed", type=float, default=1.0, help="2 = twice as fast, 0.5 = half speed")
    parser.add_argument("--start-delay", type=float, default=5, help="seconds to get the recorder ready")
    parser.add_argument("--close", action="store_true", help="close the app when the demo ends")
    args = parser.parse_args()

    jab = start_bridge()
    if any(w.title == TITLE for w in jab.get_windows()):
        sys.exit(f"A '{TITLE}' window is already open. Close it first, so the demo starts from zero state.")
    app = subprocess.Popen(["java", "-jar", args.jar])
    robot = Robot(jab, args.speed)
    try:
        robot.wait_window(TITLE)
        for remaining in range(int(args.start_delay), 0, -1):
            print(f"Demo starts in {remaining} s ...", flush=True)
            time.sleep(1)
        robot.switch()
        demo(robot)
        print("\nDemo finished." + ("" if args.close else " The app stays open; close it when you're done."))
    finally:
        if args.close:
            app.terminate()
        jab.shutdown()


if __name__ == "__main__":
    main()
