package meridian.data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The in-memory "database" of Meridian ERP. Starts in the zero state: master data only,
 * no documents, number ranges restarted (invoices 1900000001, orders 5000001), hard mode off.
 * All access happens on the Swing event thread.
 */
public final class ErpSystem {
    public static final String COMPANY_CODE = MasterData.COMPANY_CODE;
    public static final String COMPANY_NAME = MasterData.COMPANY_NAME;
    public static final String CLIENT = MasterData.CLIENT;
    public static final List<String> CURRENCIES = Collections.unmodifiableList(Arrays.asList("USD", "EUR", "GBP"));

    private static final long FIRST_DOCUMENT = 1900000001L;
    private static final long FIRST_ORDER = 5000001L;

    private List<Vendor> vendors;
    private List<GlAccount> glAccounts;
    private List<CostCenter> costCenters;
    private List<TaxCode> taxCodes;
    private List<PaymentTerm> paymentTerms;
    private List<Customer> customers;
    private List<Material> materials;
    private List<User> users;
    private final List<FiDocument> documents = new ArrayList<>();
    private final List<SalesOrder> orders = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();
    private long nextDocumentNo;
    private long nextOrderNo;
    private boolean hardMode;
    private LocalDateTime resetAt;

    public ErpSystem() {
        loadZeroState();
    }

    public void addChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    /** ZDEMO_ADMIN reset: drops documents and orders, restores master data and stock, restarts number ranges. */
    public void resetToZeroState() {
        loadZeroState();
        fireChanged();
    }

    private void loadZeroState() {
        vendors = ApMaster.vendors();
        glAccounts = ApMaster.glAccounts();
        costCenters = ApMaster.costCenters();
        taxCodes = ApMaster.taxCodes();
        paymentTerms = MasterData.paymentTerms();
        customers = MasterData.customers();
        materials = MasterData.materials();
        users = MasterData.users();
        documents.clear();
        orders.clear();
        nextDocumentNo = FIRST_DOCUMENT;
        nextOrderNo = FIRST_ORDER;
        hardMode = false;
        resetAt = LocalDateTime.now();
    }

    // ---- Logon ----

    /** Returns the logon error, or null when client, user and password are valid. */
    public FieldError checkLogon(String client, String username, String password) {
        String c = Fmt.trim(client);
        if (!CLIENT.equals(c)) {
            return new FieldError("RSYST-MANDT", "Client " + (c.isEmpty() ? "(blank)" : c) + " is not available in this system");
        }
        String u = Fmt.up(username);
        if (u.isEmpty()) {
            return new FieldError("RSYST-BNAME", "Enter a user name");
        }
        User user = user(u);
        if (user == null || !user.password.equals(password == null ? "" : password)) {
            return new FieldError("RSYST-BCODE", "Name or password is incorrect (repeat logon)");
        }
        return null;
    }

    public User logOn(String username) {
        User user = user(Fmt.up(username));
        user.lastLogin = LocalDateTime.now();
        fireChanged();
        return user;
    }

    // ---- Master data lookups (null when not found) ----

    public Vendor vendor(String id) {
        for (Vendor v : vendors) {
            if (v.id.equals(id)) {
                return v;
            }
        }
        return null;
    }

    public GlAccount glAccount(String id) {
        for (GlAccount g : glAccounts) {
            if (g.id.equals(id)) {
                return g;
            }
        }
        return null;
    }

    public CostCenter costCenter(String id) {
        for (CostCenter c : costCenters) {
            if (c.id.equals(id)) {
                return c;
            }
        }
        return null;
    }

    public TaxCode taxCode(String id) {
        for (TaxCode t : taxCodes) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        return null;
    }

    public PaymentTerm paymentTerm(String code) {
        for (PaymentTerm p : paymentTerms) {
            if (p.code.equals(code)) {
                return p;
            }
        }
        return null;
    }

    public Customer customer(String id) {
        for (Customer c : customers) {
            if (c.id.equals(id)) {
                return c;
            }
        }
        return null;
    }

    public Material material(String id) {
        for (Material m : materials) {
            if (m.id.equals(id)) {
                return m;
            }
        }
        return null;
    }

    public User user(String username) {
        for (User u : users) {
            if (u.username.equals(username)) {
                return u;
            }
        }
        return null;
    }

    public List<Vendor> getVendors() {
        return Collections.unmodifiableList(vendors);
    }

    public List<Customer> getCustomers() {
        return Collections.unmodifiableList(customers);
    }

    public List<Material> getMaterials() {
        return Collections.unmodifiableList(materials);
    }

    public List<User> getUsers() {
        return Collections.unmodifiableList(users);
    }

    // ---- Documents and orders ----

    /** Newest first, as in the FB03 document list. */
    public List<FiDocument> getDocuments() {
        List<FiDocument> list = new ArrayList<>(documents);
        Collections.reverse(list);
        return list;
    }

    /** Newest first, as in the VA03 order list. */
    public List<SalesOrder> getOrders() {
        List<SalesOrder> list = new ArrayList<>(orders);
        Collections.reverse(list);
        return list;
    }

    public FiDocument document(String docNo) {
        for (FiDocument d : documents) {
            if (d.docNo.equals(docNo)) {
                return d;
            }
        }
        return null;
    }

    public SalesOrder order(String vbeln) {
        for (SalesOrder o : orders) {
            if (o.vbeln.equals(vbeln)) {
                return o;
            }
        }
        return null;
    }

    FiDocument findInvoice(String bukrs, String vendorId, String reference) {
        for (FiDocument d : documents) {
            if (d.bukrs.equals(bukrs) && d.vendorId.equals(vendorId) && d.reference.equalsIgnoreCase(reference)) {
                return d;
            }
        }
        return null;
    }

    SalesOrder findOrderByCustomerPo(String soldTo, String custPo) {
        for (SalesOrder o : orders) {
            if (o.soldTo.equals(soldTo) && o.custPo != null && o.custPo.equalsIgnoreCase(custPo)) {
                return o;
            }
        }
        return null;
    }

    double orderValue(String soldTo) {
        double sum = 0;
        for (SalesOrder o : orders) {
            if (o.soldTo.equals(soldTo)) {
                sum += o.netValue;
            }
        }
        return sum;
    }

    String nextDocumentNumber() {
        return String.valueOf(nextDocumentNo++);
    }

    String nextOrderNumber() {
        return String.valueOf(nextOrderNo++);
    }

    void addDocument(FiDocument document) {
        documents.add(document);
        fireChanged();
    }

    void addOrder(SalesOrder order) {
        orders.add(order);
        fireChanged();
    }

    // ---- Key figures (Easy Access tiles, ZDEMO_ADMIN) ----

    public int documentCount() {
        return documents.size();
    }

    public double documentAmount() {
        double sum = 0;
        for (FiDocument d : documents) {
            sum += d.grossAmount;
        }
        return sum;
    }

    public int orderCount() {
        return orders.size();
    }

    public double orderAmount() {
        double sum = 0;
        for (SalesOrder o : orders) {
            sum += o.netValue;
        }
        return sum;
    }

    public int creditBlockedOrderCount() {
        int count = 0;
        for (SalesOrder o : orders) {
            if (o.isCreditBlocked()) {
                count++;
            }
        }
        return count;
    }

    // ---- Settings ----

    public boolean isHardMode() {
        return hardMode;
    }

    public void setHardMode(boolean on) {
        hardMode = on;
        fireChanged();
    }

    public LocalDateTime getResetAt() {
        return resetAt;
    }

    // ---- F4 value help and field descriptions ----

    /** Result of an F4 value help search. Each row starts with the key to copy into the field. */
    public static final class ValueHelp {
        public final String title;
        public final String[] columns;
        public final List<String[]> rows;

        ValueHelp(String title, String[] columns, List<String[]> rows) {
            this.title = title;
            this.columns = columns;
            this.rows = rows;
        }
    }

    /** Kinds: vendors, customers, materials, gl, cc, tax, terms, ordertypes. Matches key or text, max 200 rows. */
    public ValueHelp valueHelp(String kind, String term) {
        String t = Fmt.trim(term).toLowerCase(Locale.ROOT);
        List<String[]> rows = new ArrayList<>();
        switch (kind) {
            case "vendors":
                for (Vendor v : vendors) {
                    addIfMatch(rows, t, v.id, v.name, v.city, v.terms);
                }
                return new ValueHelp("Vendors", new String[]{"Vendor", "Name", "City", "Terms"}, rows);
            case "customers":
                for (Customer c : customers) {
                    addIfMatch(rows, t, c.id, c.name, c.city);
                }
                return new ValueHelp("Customers", new String[]{"Customer", "Name", "City"}, rows);
            case "materials":
                for (Material m : materials) {
                    if (!m.deletionFlag) {
                        addIfMatch(rows, t, m.id, m.name, m.group, Fmt.amount(m.price));
                    }
                }
                return new ValueHelp("Materials", new String[]{"Material", "Description", "Group", "Price"}, rows);
            case "gl":
                for (GlAccount g : glAccounts) {
                    addIfMatch(rows, t, g.id, g.description);
                }
                return new ValueHelp("G/L Accounts (Chart of Accounts MCOA)", new String[]{"G/L Acct", "Description"}, rows);
            case "cc":
                for (CostCenter c : costCenters) {
                    addIfMatch(rows, t, c.id, c.description);
                }
                return new ValueHelp("Cost Centers", new String[]{"Cost Center", "Description"}, rows);
            case "tax":
                for (TaxCode x : taxCodes) {
                    addIfMatch(rows, t, x.id, x.description, String.format(Locale.US, "%.3f", x.rate * 100));
                }
                return new ValueHelp("Tax Codes", new String[]{"Tax Code", "Description", "Rate %"}, rows);
            case "terms":
                for (PaymentTerm p : paymentTerms) {
                    addIfMatch(rows, t, p.code, p.description);
                }
                return new ValueHelp("Terms of Payment", new String[]{"Terms", "Explanation"}, rows);
            case "ordertypes":
                for (String type : Va01.ORDER_TYPES.keySet()) {
                    addIfMatch(rows, t, type, Va01.ORDER_TYPES.get(type));
                }
                return new ValueHelp("Sales Document Types", new String[]{"Type", "Description"}, rows);
            default:
                throw new IllegalArgumentException("Unknown value help: " + kind);
        }
    }

    private static void addIfMatch(List<String[]> rows, String term, String key, String text, String... more) {
        if (rows.size() >= 200) {
            return;
        }
        if (term.isEmpty() || key.toLowerCase(Locale.ROOT).contains(term) || text.toLowerCase(Locale.ROOT).contains(term)) {
            String[] row = new String[2 + more.length];
            row[0] = key;
            row[1] = text;
            System.arraycopy(more, 0, row, 2, more.length);
            rows.add(row);
        }
    }

    /** Short text shown next to a field, or "" when the key is unknown. */
    public String describe(String kind, String key) {
        String k = Fmt.trim(key);
        String upper = Fmt.up(key);
        switch (kind) {
            case "vendors": {
                Vendor v = vendor(upper);
                return v == null ? "" : v.name;
            }
            case "customers": {
                Customer c = customer(k);
                return c == null ? "" : c.name + " / " + c.city;
            }
            case "materials": {
                Material m = material(upper);
                return m == null ? "" : m.name;
            }
            case "gl": {
                GlAccount g = glAccount(k);
                return g == null ? "" : g.description;
            }
            case "cc": {
                CostCenter c = costCenter(upper);
                return c == null ? "" : c.description;
            }
            case "tax": {
                TaxCode t = taxCode(upper);
                return t == null ? "" : t.description;
            }
            case "terms": {
                PaymentTerm p = paymentTerm(upper);
                return p == null ? "" : p.description;
            }
            case "ordertypes": {
                String name = Va01.ORDER_TYPES.get(upper);
                return name == null ? "" : name;
            }
            default:
                return "";
        }
    }

    private void fireChanged() {
        for (Runnable listener : new ArrayList<>(listeners)) {
            listener.run();
        }
    }
}
