"""Producer-consumer example against Meridian ERP desktop (Java Swing) through the Java Access Bridge.

Produce Cases: creates 10 work items, one per case (vendor invoices and sales orders).
Process Cases: starts Meridian ERP, logs on and enters each case: FB60 for invoices, VA01 for
sales orders. Cases the ERP rejects fail as BUSINESS exceptions with a code; technical problems
fail as APPLICATION exceptions. Every case gets a result work item that compares the actual
outcome with the outcome the case was designed for.
"""

from robocorp import vault, workitems
from robocorp.tasks import task
from robocorp.workitems import ApplicationException, BusinessException

import cases
from meridian import MeridianDesktop, money, rejection, require

SECRET_NAME = "meridian_erp_desktop"  # Vault secret with `username` and `password`


@task
def produce_cases():
    """Create one output work item per demo case."""
    for case in cases.build():
        workitems.outputs.create(case)
        print(f"Case {case['case']:>2}: {case['title']} (expected {case['expected']})")


@task
def process_cases():
    """Enter every input case in Meridian ERP and report the outcome."""
    secret = vault.get_secret(SECRET_NAME)
    erp = MeridianDesktop()
    results = []
    try:
        erp.start()
        erp.log_on(secret["username"], secret["password"])
        for item in workitems.inputs:
            payload = item.payload or {}
            with item:
                try:
                    if payload.get("kind") == "invoice":
                        result = post_invoice(erp, payload)
                    elif payload.get("kind") == "sales_order":
                        result = create_sales_order(erp, payload)
                    else:
                        raise BusinessException(f"Unknown case kind {payload.get('kind')!r}", code="INVALID_INPUT")
                except BusinessException as err:
                    results.append(report(payload, outcome=err.code, status="REJECTED", message=err.message))
                    raise
                except Exception as err:
                    results.append(report(payload, outcome="ERP_UI_ERROR", status="ERROR", message=str(err)))
                    raise ApplicationException(f"Case {payload.get('case')}: {err}", code="ERP_UI_ERROR") from err
                results.append(report(payload, outcome=result["status"], **result))
    finally:
        erp.stop()
        summary(results)


def report(payload: dict, outcome: str, **result) -> dict:
    """Result work item for the next step: what happened, and whether that is what the case expected."""
    row = {
        "case": payload.get("case"),
        "title": payload.get("title"),
        "reference": payload.get("reference") or payload.get("customer_po"),
        "expected": payload.get("expected"),
        "outcome": outcome,
        "as_expected": outcome == payload.get("expected"),
        **result,
    }
    workitems.outputs.create(row)
    return row


def summary(results: list[dict]):
    print("\nCase  Expected             Outcome              OK")
    for r in results:
        print(f"{r['case']!s:>4}  {r['expected']!s:<20} {r['outcome']!s:<20} {'yes' if r['as_expected'] else 'NO'}")
    matched = sum(r["as_expected"] for r in results)
    print(f"{matched} of {len(results)} cases ended as designed.")


# ---------------------------------------------------------------- FB60 vendor invoice


def post_invoice(erp: MeridianDesktop, p: dict) -> dict:
    require(p, "vendor_id", "invoice_date", "reference", "amount", "lines")
    subject = f"Invoice {p['reference']} from {p['vendor_id']}"
    lines = p["lines"]
    for n, line in enumerate(lines, start=1):
        require(line, "gl_account", "amount")
        if str(line.get("dc", "debit")).lower() != "debit":
            raise BusinessException(f"Line {n}: this example only enters debit lines", code="INVALID_INPUT")

    erp.transaction("FB60")
    erp.enter_company_code_if_asked()
    erp.set_text("INVFO-ACCNT", p["vendor_id"])
    erp.set_text("INVFO-BLDAT", p["invoice_date"])
    erp.set_text("INVFO-BUDAT", p.get("posting_date") or p["invoice_date"])
    erp.set_text("INVFO-XBLNR", p["reference"])
    erp.set_text("INVFO-WRBTR", money(p["amount"]))
    erp.set_text("INVFO-WAERS", p.get("currency") or "USD")
    erp.set_checked("INVFO-XMWST", bool(p.get("calculate_tax")))
    erp.set_text("INVFO-MWSKZ", p.get("tax_code") or "")
    erp.set_text("INVFO-SGTXT", p.get("text") or "")

    erp.ensure_rows("ACGL_ITEM-HKONT", len(lines))
    for i, line in enumerate(lines, start=1):
        erp.set_text(f"ACGL_ITEM-HKONT-{i}", line["gl_account"])
        erp.set_text(f"ACGL_ITEM-WRBTR-{i}", money(line["amount"]))
        erp.set_text(f"ACGL_ITEM-MWSKZ-{i}", line.get("tax_code") or "")
        erp.set_text(f"ACGL_ITEM-KOSTL-{i}", line.get("cost_center") or "")
        erp.set_text(f"ACGL_ITEM-SGTXT-{i}", line.get("text") or "")

    erp.click("Post")
    msg_type, msg_text = erp.wait_status()
    if msg_type != "S":
        raise rejection(msg_text, subject)
    # "Document 1900000001 was posted in company code 1000"
    return {"status": "POSTED", "document_number": msg_text.split()[1], "message": msg_text}


# ---------------------------------------------------------------- VA01 sales order


def create_sales_order(erp: MeridianDesktop, p: dict) -> dict:
    require(p, "sold_to", "customer_po", "requested_delivery_date", "items")
    subject = f"Sales order for PO {p['customer_po']} of customer {p['sold_to']}"
    items = p["items"]
    for item in items:
        require(item, "material", "quantity")

    # Initial screen: order type and sales area
    erp.transaction("VA01")
    erp.set_text("VBAK-AUART", "RO" if p.get("rush") else p.get("order_type") or "OR")
    erp.set_text("VBAK-VKORG", p.get("sales_org") or "1000")
    erp.set_text("VBAK-VTWEG", p.get("distribution_channel") or "10")
    erp.set_text("VBAK-SPART", p.get("division") or "00")
    erp.click("Continue")
    msg_type, msg_text = erp.status()
    if msg_type == "E":
        raise rejection(msg_text, subject)

    # Overview: header and items
    erp.set_text("KUAGV-KUNNR", p["sold_to"])
    erp.set_text("KUWEV-KUNNR", p.get("ship_to") or "")
    erp.set_text("VBKD-BSTKD", p["customer_po"])
    erp.set_text("VBKD-BSTDK", p.get("customer_po_date") or "")
    erp.set_text("RV45A-KETDAT", p["requested_delivery_date"])
    erp.ensure_rows("RV45A-MABNR", len(items))
    for i, item in enumerate(items, start=1):
        erp.set_text(f"RV45A-MABNR-{i}", item["material"])
        erp.set_text(f"RV45A-KWMENG-{i}", item["quantity"])

    erp.click("Save Order")
    msg_type, msg_text = erp.wait_status()
    if msg_type == "E":
        raise rejection(msg_text, subject)
    order = next(word for word in msg_text.split() if word.isdigit())
    if msg_type == "W":
        # Saved, but someone has to follow up (credit block or partial stock confirmation).
        return {"status": "SAVED_WITH_WARNING", "order_number": order, "message": msg_text}
    return {"status": "SAVED", "order_number": order, "message": msg_text}

