package meridian.data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** FB60 Enter Vendor Invoice: form data, validation and posting. Messages match the web app exactly. */
public final class Fb60 {
    public static final int LINES_DEFAULT = 5;

    private Fb60() {
    }

    public static final class Line {
        public String gl = "";
        /** S = debit, H = credit. */
        public String dc = "S";
        public String amount = "";
        public String tax = "";
        public String cc = "";
        public String text = "";

        boolean isUsed() {
            return !gl.isEmpty() || !amount.isEmpty() || !cc.isEmpty();
        }
    }

    /** Raw screen values. Callers trim and upper-case them the way the web form reader does. */
    public static final class Form {
        /** R = invoice, G = credit memo. */
        public String trans = "R";
        public String vendor = "";
        public String docDate = "";
        public String postDate = "";
        public String reference = "";
        public String amount = "";
        public String currency = "USD";
        public boolean calcTax;
        public String taxCode = "";
        public String text = "";
        public String baseline = "";
        public String terms = "";
        public String headerText = "";
        public List<Line> lines = new ArrayList<>();

        public boolean isCreditMemo() {
            return "G".equals(trans);
        }
    }

    public static final class Posting {
        public final String pk;
        /** K = vendor, S = G/L account. */
        public final String type;
        public final String account;
        public final String dc;
        public final double amount;
        public final String tax;
        public final String cc;
        public final String text;
        public final String description;

        Posting(String pk, String type, String account, String dc, double amount, String tax, String cc,
                String text, String description) {
            this.pk = pk;
            this.type = type;
            this.account = account;
            this.dc = dc;
            this.amount = amount;
            this.tax = tax;
            this.cc = cc;
            this.text = text;
            this.description = description;
        }
    }

    public static final class Check {
        public final List<FieldError> errors = new ArrayList<>();
        public final List<Posting> postings = new ArrayList<>();
        public Vendor vendor;
        public LocalDate dueDate;
        public double balance;
        public double tax;
        public LocalDate docDate;
        public LocalDate postDate;
        public LocalDate baselineDate;

        public FieldError firstError() {
            return errors.isEmpty() ? null : errors.get(0);
        }
    }

    /** full = false checks only what has been typed so far (Enter); true checks everything (Simulate, Post). */
    public static Check check(ErpSystem sys, String bukrs, Form f, boolean full) {
        Check c = new Check();

        if (!f.vendor.isEmpty()) {
            c.vendor = sys.vendor(f.vendor);
            if (c.vendor == null) {
                err(c, "INVFO-ACCNT", "Vendor " + f.vendor + " is not defined in company code " + bukrs);
            } else if (c.vendor.postingBlock || !c.vendor.active) {
                err(c, "INVFO-ACCNT", "Vendor " + f.vendor + " is blocked for posting in company code " + bukrs);
            }
        } else if (full) {
            err(c, "INVFO-ACCNT", "Make an entry in all required fields: Vendor");
        }

        c.docDate = f.docDate.isEmpty() ? null : Fmt.parseDate(f.docDate);
        if (!f.docDate.isEmpty() && c.docDate == null) {
            err(c, "INVFO-BLDAT", "Invalid date: " + f.docDate + " (use MM/DD/YYYY)");
        } else if (f.docDate.isEmpty() && full) {
            err(c, "INVFO-BLDAT", "Make an entry in all required fields: Invoice date");
        }

        c.postDate = f.postDate.isEmpty() ? null : Fmt.parseDate(f.postDate);
        if (!f.postDate.isEmpty() && c.postDate == null) {
            err(c, "INVFO-BUDAT", "Invalid date: " + f.postDate + " (use MM/DD/YYYY)");
        } else if (f.postDate.isEmpty() && full) {
            err(c, "INVFO-BUDAT", "Make an entry in all required fields: Posting Date");
        } else if (c.postDate != null) {
            // Open periods: current and previous month.
            LocalDate now = LocalDate.now();
            int diff = (now.getYear() - c.postDate.getYear()) * 12 + now.getMonthValue() - c.postDate.getMonthValue();
            if (diff > 1 || diff < 0) {
                err(c, "INVFO-BUDAT", String.format("Posting period %03d %d is not open",
                        c.postDate.getMonthValue(), c.postDate.getYear()));
            }
        }

        if (f.reference.isEmpty() && full) {
            err(c, "INVFO-XBLNR", "Make an entry in all required fields: Reference");
        }
        if (!f.reference.isEmpty() && c.vendor != null) {
            FiDocument dup = sys.findInvoice(bukrs, c.vendor.id, f.reference);
            if (dup != null) {
                err(c, "INVFO-XBLNR", "Check whether document has already been entered under number "
                        + bukrs + " " + dup.docNo + " " + dup.gjahr);
            }
        }

        Double gross = Fmt.parseAmount(f.amount);
        if (gross != null && gross.isNaN()) {
            err(c, "INVFO-WRBTR", "Entry " + f.amount + " is not in the correct format (e.g. 1,234.56)");
        } else if (gross == null && full) {
            err(c, "INVFO-WRBTR", "Make an entry in all required fields: Amount");
        } else if (gross != null && gross <= 0) {
            err(c, "INVFO-WRBTR", "Amount must be greater than zero");
        }

        if (!ErpSystem.CURRENCIES.contains(f.currency)) {
            err(c, "INVFO-WAERS", "Currency " + f.currency + " is not defined");
        }
        if (!f.taxCode.isEmpty() && sys.taxCode(f.taxCode) == null) {
            err(c, "INVFO-MWSKZ", "Tax code " + f.taxCode + " is not defined for country US");
        }

        String termsCode = !f.terms.isEmpty() ? f.terms : c.vendor != null ? c.vendor.terms : "";
        PaymentTerm terms = sys.paymentTerm(termsCode);
        if (!f.terms.isEmpty() && terms == null) {
            err(c, "INVFO-ZTERM", "Terms of payment " + f.terms + " are not defined");
        }
        c.baselineDate = !f.baseline.isEmpty() ? Fmt.parseDate(f.baseline) : c.docDate;
        if (!f.baseline.isEmpty() && Fmt.parseDate(f.baseline) == null) {
            err(c, "INVFO-ZFBDT", "Invalid date: " + f.baseline);
        }
        c.dueDate = c.baselineDate != null && terms != null ? c.baselineDate.plusDays(terms.days) : null;

        // Line items. A credit memo flips debit and credit.
        int sign = f.isCreditMemo() ? -1 : 1;
        double sumLines = 0;
        double taxTotal = 0;
        Map<String, Double> taxByCode = new LinkedHashMap<>();
        boolean anyUsed = false;
        for (int idx = 0; idx < f.lines.size(); idx++) {
            Line l = f.lines.get(idx);
            if (!l.isUsed()) {
                continue;
            }
            anyUsed = true;
            int i = idx + 1;
            GlAccount gl = l.gl.isEmpty() ? null : sys.glAccount(l.gl);
            if (l.gl.isEmpty()) {
                err(c, "ACGL_ITEM-HKONT-" + i, "Line " + i + ": Enter a G/L account");
            } else if (gl == null) {
                err(c, "ACGL_ITEM-HKONT-" + i, "G/L account " + l.gl + " is not defined in chart of accounts MCOA");
            }
            Double amt = Fmt.parseAmount(l.amount);
            if (amt == null) {
                err(c, "ACGL_ITEM-WRBTR-" + i, "Line " + i + ": Enter an amount");
            } else if (amt.isNaN() || amt <= 0) {
                err(c, "ACGL_ITEM-WRBTR-" + i, "Entry " + l.amount + " is not in the correct format");
            }
            if (!l.tax.isEmpty() && sys.taxCode(l.tax) == null) {
                err(c, "ACGL_ITEM-MWSKZ-" + i, "Tax code " + l.tax + " is not defined for country US");
            }
            if (!l.cc.isEmpty() && sys.costCenter(l.cc) == null) {
                err(c, "ACGL_ITEM-KOSTL-" + i, "Cost center " + l.cc + " does not exist on "
                        + Fmt.date(c.postDate != null ? c.postDate : LocalDate.now()));
            } else if (l.cc.isEmpty() && gl != null && gl.requiresCc) {
                err(c, "ACGL_ITEM-KOSTL-" + i, "Account " + l.gl + " requires an assignment to a CO object");
            }
            if (amt != null && !amt.isNaN() && amt != 0) {
                String tc = !l.tax.isEmpty() ? l.tax : f.taxCode;
                TaxCode taxCode = sys.taxCode(tc);
                double tax = f.calcTax ? Fmt.round2(amt * (taxCode == null ? 0 : taxCode.rate)) : 0;
                int s = "H".equals(l.dc) ? -1 : 1;
                sumLines += s * (amt + tax);
                taxTotal += s * tax;
                if (tax != 0) {
                    taxByCode.merge(tc, s * tax, Double::sum);
                }
                String dc = s * sign > 0 ? "S" : "H";
                c.postings.add(new Posting("S".equals(dc) ? "40" : "50", "S", l.gl, dc, amt, tc, l.cc, l.text,
                        gl == null ? "" : gl.description));
            }
        }
        if (full && !anyUsed) {
            err(c, "ACGL_ITEM-HKONT-1", "No line items have been entered");
        }
        for (Map.Entry<String, Double> entry : taxByCode.entrySet()) {
            TaxCode t = sys.taxCode(entry.getKey());
            String dc = entry.getValue() * sign > 0 ? "S" : "H";
            c.postings.add(new Posting("S".equals(dc) ? "40" : "50", "S", t.gl, dc,
                    Math.abs(Fmt.round2(entry.getValue())), entry.getKey(), "", "Tax", "Tax " + entry.getKey()));
        }
        double g = gross != null && !gross.isNaN() ? gross : 0;
        if (c.vendor != null) {
            c.postings.add(0, new Posting(f.isCreditMemo() ? "21" : "31", "K", c.vendor.id,
                    f.isCreditMemo() ? "S" : "H", g, "", "", f.text, c.vendor.name));
        }
        c.balance = Fmt.round2(sumLines - g);
        c.tax = Fmt.round2(taxTotal);
        if (full && c.errors.isEmpty() && c.balance != 0) {
            err(c, "ACGL_ITEM-WRBTR-1", "Balance not zero: " + Fmt.amount(c.balance) + "  debits: "
                    + Fmt.amount(sumLines) + "  credits: " + Fmt.amount(g));
        }
        return c;
    }

    /** Posts a checked, error-free form and returns the document number. */
    public static String post(ErpSystem sys, String bukrs, Form f, Check c, String user) {
        String docNo = sys.nextDocumentNumber();
        List<FiDocument.Line> lines = new ArrayList<>();
        int n = 1;
        for (Posting p : c.postings) {
            lines.add(new FiDocument.Line(n++, p.type, p.account, p.dc, p.amount, emptyToNull(p.tax),
                    emptyToNull(p.cc), emptyToNull(p.text)));
        }
        sys.addDocument(new FiDocument(docNo, bukrs, c.postDate.getYear(), f.isCreditMemo() ? "KG" : "KR",
                c.vendor.id, c.docDate, c.postDate, f.reference, emptyToNull(f.headerText), f.currency,
                Fmt.parseAmount(f.amount), c.tax, !f.terms.isEmpty() ? f.terms : c.vendor.terms, c.baselineDate,
                c.dueDate, user, lines));
        return docNo;
    }

    private static void err(Check c, String field, String text) {
        c.errors.add(new FieldError(field, text));
    }

    private static String emptyToNull(String s) {
        return s == null || s.isEmpty() ? null : s;
    }
}
