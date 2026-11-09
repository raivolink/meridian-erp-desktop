"""Meridian ERP desktop (Java Swing) driven through the Java Access Bridge with RPA.JavaAccessBridge.

Elements are found by role and accessible name. Input fields are named by their SAP technical
name (INVFO-ACCNT, RV45A-MABNR-1, ...), see ../app/README.md for the full contract.
"""

import logging
import os
import re
import subprocess
import time
from pathlib import Path

from robocorp.workitems import BusinessException

ROBOT_DIR = Path(__file__).parent
JAR = ROBOT_DIR / "meridian" / "MeridianErp.jar"
TITLE = "Meridian ERP - Robot"  # the --suffix keeps us away from any Meridian window a person has open
COMPANY_CODE = os.getenv("MERIDIAN_COMPANY_CODE", "1000")
STEP_DELAY = float(os.getenv("MERIDIAN_STEP_DELAY", "0"))  # seconds after each field, for demos


class MeridianDesktop:
    """Starts its own Meridian ERP instance (fresh zero state) and operates it."""

    def __init__(self):
        self.process = None
        self.jab = None

    # ---------------------------------------------------------------- start / stop

    def start(self):
        java_home = Path(os.environ["JAVA_HOME"])  # set by the conda openjdk package
        os.environ.setdefault("RC_JAVA_ACCESS_BRIDGE_DLL", str(java_home / "bin" / "windowsaccessbridge-64.dll"))
        # Import after the DLL path is known; the library reads it on initialization.
        from JABWrapper import jab_wrapper
        from RPA.JavaAccessBridge import JavaAccessBridge

        # The wrapper echoes every element search to the console; keep only warnings there.
        # Everything still goes to output/jab_wrapper.log, and robocorp's output/log.html.
        jab_wrapper.logging_stream_handler.setLevel(logging.WARNING)

        # The system property switches the Access Bridge on for this JVM only,
        # so the machine needs no `jabswitch -enable`.
        self.process = subprocess.Popen([
            str(java_home / "bin" / "java.exe"),
            "-Djavax.accessibility.assistive_technologies=com.sun.java.accessibility.AccessBridge",
            "-jar", str(JAR), f"--suffix={TITLE.split(' - ', 1)[1]}",
        ])
        self.jab = JavaAccessBridge(ignore_callbacks=True)
        self._select_main_window(timeout=60)

    def _select_main_window(self, timeout: int = 10):
        # By process id, so a leftover window with the same title can't be picked. The robot only
        # uses Access Bridge actions (no keyboard or mouse), so the window needn't be in front.
        self.jab.select_window_by_pid(self.process.pid, bring_foreground=False, timeout=timeout)

    def stop(self):
        if self.jab is not None and self.jab.jab_wrapper is not None:
            self.jab.shutdown_jab()
        if self.process is not None and self.process.poll() is None:
            self.process.terminate()
            self.process.wait(timeout=10)

    # ---------------------------------------------------------------- elements

    def find(self, role: str, name: str):
        """A showing element with exactly this role and accessible name.

        The element tree is a snapshot, and every screen lives in the same window, so states are
        read live and the tree is only refreshed when the element isn't in it yet.
        """
        for _ in range(2):
            nodes = self.jab.get_elements(f"role:{role} and name:{name}", strict=True)
            showing = [n for n in nodes if "showing" in self._info(n).states]
            if showing:
                return showing[0]
            self.jab.application_refresh()
        raise RuntimeError(f"Element {role} '{name}' not found on the current screen of {TITLE}")

    def set_text(self, name: str, value, role: str = "text"):
        self.find(role, name).insert_text("" if value is None else str(value))
        if STEP_DELAY:
            time.sleep(STEP_DELAY)

    def click(self, name: str):
        self.find("push button", name).click()

    def set_checked(self, name: str, checked: bool):
        box = self.find("check box", name)
        if ("checked" in self._info(box).states.split(",")) != checked:
            box.click()

    def label(self, name: str) -> str:
        """Value labels keep a fixed name and expose their value as the accessible description."""
        return self._info(self.find("label", name)).description

    def _info(self, node):
        return self.jab.jab_wrapper.get_context_info(node.context)

    # ---------------------------------------------------------------- screens

    def status(self) -> tuple[str, str]:
        """Message type (S/W/E/I or empty) and the exact status bar text."""
        return self.label("Message Type"), self.label("Status Bar")

    def wait_status(self, timeout: float = 10) -> tuple[str, str]:
        """The app clears the status bar when an action starts; wait for the result message."""
        end = time.time() + timeout
        while True:
            msg_type, text = self.status()
            if text or time.time() > end:
                return msg_type, text
            time.sleep(0.2)

    def log_on(self, username: str, password: str):
        self.set_text("RSYST-BNAME", username)
        self.set_text("RSYST-BCODE", password, role="password text")
        self.click("Log On")
        if not self.label("Screen Title").startswith("Meridian Easy Access"):
            raise RuntimeError(f"Logon to Meridian ERP as {username} failed: {self.status()[1]}")

    def transaction(self, tcode: str):
        """Starts a transaction from the command field (/n leaves the current one first)."""
        self.set_text("Command Field", f"/n{tcode}")
        self.click("Enter")
        if self.label("Current Transaction") != tcode:
            raise RuntimeError(f"Could not start {tcode}: {self.status()[1]}")

    def enter_company_code_if_asked(self):
        """FB60 asks for the company code once per session, in a popup window."""
        popup = "Enter Company Code"
        for _ in range(10):
            if any(w.title == popup and w.pid == self.process.pid for w in self.jab.list_java_windows()):
                self.jab.select_window_by_title(popup, bring_foreground=False, timeout=5)
                self.set_text("BKPF-BUKRS", COMPANY_CODE)
                self.click("Continue")
                break
            time.sleep(0.3)
        self._select_main_window()

    def ensure_rows(self, field_prefix: str, count: int):
        """Item tables start with a few rows; "Insert rows" adds five more."""
        while not self._exists("text", f"{field_prefix}-{count}"):
            self.click("Insert rows")

    def _exists(self, role: str, name: str) -> bool:
        try:
            self.find(role, name)
            return True
        except RuntimeError:
            return False


# ---------------------------------------------------------------- business exceptions
# ERP status bar error -> (code, what a person should do). Adapted from the web app's robot.

_RULES = [
    (r"^Balance not zero: (?P<diff>\S+)\s+debits: (?P<debits>\S+)\s+credits: (?P<credits>\S+)",
     "BALANCE_NOT_ZERO",
     "The line items total {debits} but the invoice amount is {credits} (difference {diff}). "
     "Get the difference approved and add it as a separate line, or return the invoice to the vendor."),
    (r"Posting period .* is not open", "PERIOD_CLOSED",
     "The invoice date is in a closed posting period. Ask accounting where to post it."),
    (r"is blocked for posting", "VENDOR_BLOCKED",
     "The vendor has a posting block. Check with vendor master data before paying."),
    (r"already been entered under number (?P<doc>.+)$", "DUPLICATE_INVOICE",
     "This vendor and reference are already posted as {doc}. Do not pay twice."),
    (r"requires an assignment to a CO object", "MISSING_COST_CENTER",
     "The expense account needs a cost center but the invoice has none. Ask the requester for one."),
    (r"is blocked for sales", "CUSTOMER_BLOCKED",
     "The customer has an order block. Ask credit management before accepting the order."),
    (r"already exists for sold-to party .* \(order (?P<order>\d+)\)", "DUPLICATE_PO",
     "The customer's PO is already entered as order {order}. Check whether the PO was re-sent."),
    (r"is flagged for deletion", "MATERIAL_DELETED",
     "A material on the PO is discontinued. Contact the customer for a replacement item."),
    (r"does not exist|is not defined", "MASTER_DATA_NOT_FOUND",
     "The document refers to master data that the ERP does not know. Check the IDs on the document."),
]


def rejection(erp_message: str, subject: str) -> BusinessException:
    """Turns an ERP error message into a business exception with a clear code and next step."""
    for pattern, code, advice in _RULES:
        m = re.search(pattern, erp_message)
        if m:
            return BusinessException(f"{subject} was not entered: {advice.format(**m.groupdict())} "
                                     f"(ERP: {erp_message})", code=code)
    return BusinessException(f"{subject} was not entered: the ERP rejected it (ERP: {erp_message})",
                             code="ERP_REJECTED")


def require(payload: dict, *fields: str):
    missing = [f for f in fields if payload.get(f) in (None, "", [])]
    if missing:
        raise BusinessException(f"The work item is missing required fields: {', '.join(missing)}",
                                code="INVALID_INPUT")


def money(value) -> str:
    return f"{float(value):,.2f}"
