"""The 10 demo cases the producer creates: vendor invoices (FB60) and sales orders (VA01).

Each case is one work item. The payload uses the same document format as the web app's robot,
plus `case`, `title`, `kind` and `expected` (the outcome the case is designed to produce).
Dates are relative to today, so the posting period is always open. The outcomes assume a fresh
Meridian ERP (zero state), which the consumer guarantees by starting its own instance.
"""

import datetime


def build() -> list[dict]:
    today = datetime.date.today()
    day = today.strftime("%m/%d/%Y")
    delivery = (today + datetime.timedelta(days=7)).strftime("%m/%d/%Y")
    stamp = today.strftime("%Y%m%d")

    def invoice(case, title, expected, **document):
        document.setdefault("invoice_date", day)
        document.setdefault("currency", "USD")
        return {"case": case, "kind": "invoice", "title": title, "expected": expected, **document}

    def order(case, title, expected, **document):
        document.setdefault("customer_po_date", day)
        document.setdefault("requested_delivery_date", delivery)
        return {"case": case, "kind": "sales_order", "title": title, "expected": expected, **document}

    return [
        invoice(
            1, "Market data invoice from Bloomberg", "POSTED",
            vendor_id="V-1001", reference=f"BLP-{stamp}-01", amount=4850.00,
            text="Terminal licenses",
            lines=[{"gl_account": "6300", "amount": 4850.00, "cost_center": "CC100", "text": "Terminal licenses"}],
        ),
        invoice(
            2, "Refinitiv invoice with NYC sales tax (8.875 %)", "POSTED",
            vendor_id="V-1002", reference=f"RFT-{stamp}-02", amount=2177.50,
            calculate_tax=True, tax_code="TX-NYC-SALES", text="Real-time data feed",
            lines=[{"gl_account": "6310", "amount": 2000.00, "cost_center": "CC100", "text": "Real-time data feed"}],
        ),
        invoice(
            3, "Same Bloomberg invoice sent twice", "DUPLICATE_INVOICE",
            vendor_id="V-1001", reference=f"BLP-{stamp}-01", amount=4850.00,
            text="Terminal licenses (re-sent)",
            lines=[{"gl_account": "6300", "amount": 4850.00, "cost_center": "CC100", "text": "Terminal licenses"}],
        ),
        invoice(
            4, "Office rent from WeWork, a vendor with a posting block", "VENDOR_BLOCKED",
            vendor_id="V-2015", reference=f"WW-{stamp}-04", amount=12500.00,
            text="Office rent",
            lines=[{"gl_account": "6500", "amount": 12500.00, "cost_center": "CC530", "text": "Office rent"}],
        ),
        invoice(
            5, "FactSet invoice whose lines don't add up", "BALANCE_NOT_ZERO",
            vendor_id="V-1003", reference=f"FDS-{stamp}-05", amount=1000.00,
            text="Research workstation",
            lines=[{"gl_account": "6330", "amount": 1100.00, "cost_center": "CC200", "text": "Research workstation"}],
        ),
        order(
            6, "Standard order from Cedar Ridge Medical Center", "SAVED",
            sold_to="100003", customer_po=f"CRMC-{stamp}-06",
            items=[{"material": "HT-1010", "quantity": 5}, {"material": "HT-1035", "quantity": 10}],
        ),
        order(
            7, "Yellowstone Outfitters order above its 5,000 USD credit limit", "SAVED_WITH_WARNING",
            sold_to="100025", customer_po=f"YO-{stamp}-07",
            items=[{"material": "HT-1000", "quantity": 8}],
        ),
        order(
            8, "Bluewater order for 5 label printers, only 3 in stock", "SAVED_WITH_WARNING",
            sold_to="100002", customer_po=f"BHG-{stamp}-08",
            items=[{"material": "HT-1270", "quantity": 5}],
        ),
        order(
            9, "Order from Summit Health Clinics, a customer with an order block", "CUSTOMER_BLOCKED",
            sold_to="100019", customer_po=f"SHC-{stamp}-09",
            items=[{"material": "HT-1060", "quantity": 20}],
        ),
        order(
            10, "Juniper Software order for a discontinued paper shredder", "MATERIAL_DELETED",
            sold_to="100010", customer_po=f"JSC-{stamp}-10",
            items=[{"material": "HT-1250", "quantity": 2}],
        ),
    ]
