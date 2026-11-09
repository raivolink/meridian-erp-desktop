package meridian.data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** VA01 Create Sales Order: form data, validation and saving. Messages match the web app exactly. */
public final class Va01 {
    public static final int ITEMS_DEFAULT = 8;
    public static final Map<String, String> ORDER_TYPES;
    private static final Pattern NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");

    static {
        Map<String, String> types = new LinkedHashMap<>();
        types.put("OR", "Standard Order");
        types.put("RO", "Rush Order");
        ORDER_TYPES = Collections.unmodifiableMap(types);
    }

    private Va01() {
    }

    public static final class Item {
        public String matnr = "";
        public String qty = "";
        public String plant = "";

        boolean isUsed() {
            return !matnr.isEmpty() || !qty.isEmpty();
        }
    }

    /** Raw screen values. Callers trim and upper-case them the way the web form reader does. */
    public static final class Form {
        public String auart = "";
        public String vkorg = "";
        public String vtweg = "";
        public String spart = "";
        public String soldTo = "";
        public String shipTo = "";
        public String custPo = "";
        public String custPoDate = "";
        public String reqDate = "";
        public String plant = "1000";
        public String terms = "";
        public List<Item> items = new ArrayList<>();

        public String orderName() {
            String name = ORDER_TYPES.get(auart);
            return name == null ? "Sales Order" : name;
        }
    }

    public static final class CheckedItem {
        public final int posnr;
        public final int idx;
        public final String matnr;
        public final String description;
        public final int qty;
        public final int confirmed;
        public final double price;
        public final double value;
        public final String plant;

        CheckedItem(int idx, Material m, int qty, int confirmed, String plant) {
            this.posnr = idx * 10;
            this.idx = idx;
            this.matnr = m.id;
            this.description = m.name;
            this.qty = qty;
            this.confirmed = confirmed;
            this.price = m.price;
            this.value = Fmt.round2(m.price * qty);
            this.plant = plant;
        }
    }

    public static final class Check {
        public final List<FieldError> errors = new ArrayList<>();
        public final List<String> warnings = new ArrayList<>();
        public final List<CheckedItem> items = new ArrayList<>();
        public Customer soldTo;
        public Customer shipTo;
        public double net;
        public LocalDate reqDate;
        public LocalDate poDate;
        public String terms;
        public boolean creditBlock;

        public FieldError firstError() {
            return errors.isEmpty() ? null : errors.get(0);
        }

        public CheckedItem item(int idx) {
            for (CheckedItem item : items) {
                if (item.idx == idx) {
                    return item;
                }
            }
            return null;
        }
    }

    /** Initial screen check of order type and sales area; null when valid. */
    public static FieldError checkSalesArea(String auart, String vkorg, String vtweg, String spart) {
        if (auart.isEmpty()) {
            return new FieldError("VBAK-AUART", "Make an entry in all required fields: Order Type");
        }
        if (!ORDER_TYPES.containsKey(auart)) {
            return new FieldError("VBAK-AUART", "Sales document type " + auart + " is not defined");
        }
        if (vkorg.isEmpty() || vtweg.isEmpty() || spart.isEmpty()) {
            String field = vkorg.isEmpty() ? "VBAK-VKORG" : vtweg.isEmpty() ? "VBAK-VTWEG" : "VBAK-SPART";
            return new FieldError(field, "Make an entry in all required fields: Organizational Data");
        }
        if (!"1000".equals(vkorg) || !"10".equals(vtweg) || !"00".equals(spart)) {
            return new FieldError("VBAK-VKORG", "Sales area " + vkorg + " " + vtweg + " " + spart + " is not defined");
        }
        return null;
    }

    /** full = false checks what has been typed so far (Enter); true checks everything (Save). */
    public static Check check(ErpSystem sys, Form f, boolean full) {
        Check c = new Check();

        if (!f.soldTo.isEmpty()) {
            c.soldTo = sys.customer(f.soldTo);
            if (c.soldTo == null) {
                err(c, "KUAGV-KUNNR", "Sold-to party " + f.soldTo + " does not exist in sales area 1000 10 00");
            } else if (c.soldTo.salesBlock) {
                err(c, "KUAGV-KUNNR", "Sold-to party " + f.soldTo + " is blocked for sales (order block 01)");
            }
        } else if (full) {
            err(c, "KUAGV-KUNNR", "Make an entry in all required fields: Sold-To Party");
        }
        if (!f.shipTo.isEmpty() && !f.shipTo.equals(f.soldTo)) {
            c.shipTo = sys.customer(f.shipTo);
            if (c.shipTo == null) {
                err(c, "KUWEV-KUNNR", "Ship-to party " + f.shipTo + " does not exist in sales area 1000 10 00");
            }
        } else {
            c.shipTo = c.soldTo;
        }

        c.poDate = f.custPoDate.isEmpty() ? null : Fmt.parseDate(f.custPoDate);
        if (!f.custPoDate.isEmpty() && c.poDate == null) {
            err(c, "VBKD-BSTDK", "Invalid date: " + f.custPoDate + " (use MM/DD/YYYY)");
        }
        if (!f.custPo.isEmpty() && c.soldTo != null) {
            SalesOrder dup = sys.findOrderByCustomerPo(c.soldTo.id, f.custPo);
            if (dup != null) {
                err(c, "VBKD-BSTKD", "Purchase order number " + f.custPo + " already exists for sold-to party "
                        + c.soldTo.id + " (order " + dup.vbeln + ")");
            }
        }

        c.reqDate = f.reqDate.isEmpty() ? null : Fmt.parseDate(f.reqDate);
        if (!f.reqDate.isEmpty() && c.reqDate == null) {
            err(c, "RV45A-KETDAT", "Invalid date: " + f.reqDate + " (use MM/DD/YYYY)");
        } else if (f.reqDate.isEmpty() && full) {
            err(c, "RV45A-KETDAT", "Make an entry in all required fields: Req. deliv.date");
        } else if (c.reqDate != null && c.reqDate.isBefore(LocalDate.now())) {
            c.warnings.add("Requested delivery date " + Fmt.date(c.reqDate) + " lies in the past");
        }

        if (!"1000".equals(f.plant)) {
            err(c, "RV45A-DWERK", "Plant " + f.plant + " is not defined for sales org. 1000 distr.chan. 10");
        }
        c.terms = !f.terms.isEmpty() ? f.terms : c.soldTo != null ? c.soldTo.terms : "";
        if (!f.terms.isEmpty() && sys.paymentTerm(f.terms) == null) {
            err(c, "VBKD-ZTERM", "Terms of payment " + f.terms + " are not defined");
        }

        boolean anyUsed = false;
        Map<String, Integer> requested = new HashMap<>(); // same material on several lines shares stock
        for (int i = 0; i < f.items.size(); i++) {
            Item it = f.items.get(i);
            if (!it.isUsed()) {
                continue;
            }
            anyUsed = true;
            int idx = i + 1;
            if (it.matnr.isEmpty()) {
                err(c, "RV45A-MABNR-" + idx, "Item " + idx * 10 + ": Enter a material");
                continue;
            }
            Material m = sys.material(it.matnr);
            if (m == null) {
                err(c, "RV45A-MABNR-" + idx, "Material " + it.matnr + " does not exist");
                continue;
            }
            if (m.deletionFlag) {
                err(c, "RV45A-MABNR-" + idx, "Material " + it.matnr + " is flagged for deletion");
                continue;
            }
            int qty = 0;
            if (it.qty.isEmpty()) {
                if (full) {
                    err(c, "RV45A-KWMENG-" + idx, "Item " + idx * 10 + ": Enter an order quantity");
                }
            } else {
                String plain = it.qty.replace(",", "");
                double number = NUMBER.matcher(plain).matches() ? Double.parseDouble(plain) : Double.NaN;
                if (Double.isNaN(number) || number != Math.floor(number) || number <= 0) {
                    err(c, "RV45A-KWMENG-" + idx, "Entry " + it.qty + " is not a valid quantity");
                    continue;
                }
                qty = (int) number;
            }
            String plant = !it.plant.isEmpty() ? it.plant : f.plant;
            if (!"1000".equals(plant)) {
                err(c, "VBAP-WERKS-" + idx, "Plant " + plant + " is not defined");
            }
            int already = requested.getOrDefault(m.id, 0);
            int available = Math.max(0, m.stock - already);
            int confirmed = Math.min(qty, available);
            requested.put(m.id, already + qty);
            if (qty > 0 && confirmed < qty) {
                c.warnings.add("Only " + confirmed + " EA of " + m.id + " can be confirmed for "
                        + Fmt.date(c.reqDate != null ? c.reqDate : LocalDate.now()));
            }
            c.items.add(new CheckedItem(idx, m, qty, confirmed, plant));
        }
        if (full && !anyUsed) {
            err(c, "RV45A-MABNR-1", "No items have been entered");
        }
        double net = 0;
        for (CheckedItem item : c.items) {
            net += item.value;
        }
        c.net = Fmt.round2(net);

        if (c.soldTo != null && c.net > 0) {
            double exposure = sys.orderValue(c.soldTo.id) + c.net;
            if (exposure > c.soldTo.creditLimit) {
                c.creditBlock = true;
                c.warnings.add("Credit limit exceeded for customer " + c.soldTo.id + ": limit "
                        + Fmt.amount(c.soldTo.creditLimit) + " USD, exposure " + Fmt.amount(exposure) + " USD");
            }
        }
        return c;
    }

    /** Saves a checked, error-free form, reduces stock by the confirmed quantities and returns the status message. */
    public static Message save(ErpSystem sys, Form f, Check c, String user) {
        String vbeln = sys.nextOrderNumber();
        List<SalesOrder.Item> items = new ArrayList<>();
        boolean partial = false;
        for (CheckedItem it : c.items) {
            items.add(new SalesOrder.Item(it.posnr, it.matnr, it.description, it.qty, it.confirmed, it.price,
                    it.value, it.plant));
            Material m = sys.material(it.matnr);
            m.stock -= it.confirmed;
            partial |= it.confirmed < it.qty;
        }
        sys.addOrder(new SalesOrder(vbeln, f.auart, f.vkorg, f.vtweg, f.spart, c.soldTo.id,
                c.shipTo != null ? c.shipTo.id : c.soldTo.id, f.custPo.isEmpty() ? null : f.custPo, c.poDate,
                c.reqDate, c.terms, c.net, c.creditBlock ? "B" : "A", user, items));
        String name = "RO".equals(f.auart) ? "Rush Order" : "Standard Order";
        if (c.creditBlock) {
            return Message.warning(name + " " + vbeln + " has been saved (blocked: credit limit exceeded)");
        }
        if (partial) {
            return Message.warning(name + " " + vbeln + " has been saved (not all items could be confirmed)");
        }
        return Message.success(name + " " + vbeln + " has been saved");
    }

    private static void err(Check c, String field, String text) {
        c.errors.add(new FieldError(field, text));
    }
}
