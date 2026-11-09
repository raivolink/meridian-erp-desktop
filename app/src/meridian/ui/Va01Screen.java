package meridian.ui;

import meridian.data.Customer;
import meridian.data.FieldError;
import meridian.data.Fmt;
import meridian.data.Message;
import meridian.data.Va01;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** VA01 Create Sales Order: initial screen (order type, sales area), then the overview with items. */
final class Va01Screen extends Screen {
    private static final String INITIAL = "initial";
    private static final String OVERVIEW = "overview";

    private final CardLayout cards = new CardLayout();
    private final JPanel steps = new JPanel(cards);
    private String step = INITIAL;

    // Initial screen
    private final JTextField auart = reg(Widgets.field("VBAK-AUART", "Order Type", 4));
    private final JLabel auartDesc = Widgets.valueLabel("DESC-VBAK-AUART", "");
    private final JTextField vkorg = reg(Widgets.field("VBAK-VKORG", "Sales Organization", 5));
    private final JTextField vtweg = reg(Widgets.field("VBAK-VTWEG", "Distribution Channel", 3));
    private final JTextField spart = reg(Widgets.field("VBAK-SPART", "Division", 3));

    // Overview header
    private final JLabel orderNameCaption = new JLabel("Standard Order");
    private final JTextField vbeln = reg(Widgets.readOnly("VBAK-VBELN", "Order", 10));
    private final JTextField soldTo = reg(Widgets.field("KUAGV-KUNNR", "Sold-To Party", 10));
    private final JLabel soldToDesc = Widgets.valueLabel("DESC-KUAGV-KUNNR", "");
    private final JTextField shipTo = reg(Widgets.field("KUWEV-KUNNR", "Ship-To Party", 10));
    private final JLabel shipToDesc = Widgets.valueLabel("DESC-KUWEV-KUNNR", "");
    private final JTextField custPo = reg(Widgets.field("VBKD-BSTKD", "Cust. Reference", 20));
    private final JTextField custPoDate = reg(Widgets.field("VBKD-BSTDK", "Cust. Ref. Date", 11));
    private final JLabel creditLimit = Widgets.valueLabel("credit-limit", "");

    // Sales tab
    private final JTextField reqDate = reg(Widgets.field("RV45A-KETDAT", "Req. deliv.date", 11));
    private final JTextField plant = reg(Widgets.field("RV45A-DWERK", "Deliver.Plant", 5));
    private final JLabel plantDesc = Widgets.valueLabel("DESC-RV45A-DWERK", "");
    private final JTextField terms = reg(Widgets.field("VBKD-ZTERM", "Payment terms", 5));
    private final JComboBox<Widgets.Option> incoterms = reg(Widgets.combo("VBKD-INCO1", "Incoterms",
            Widgets.option("FOB", "FOB"), Widgets.option("CIF", "CIF"), Widgets.option("EXW", "EXW"),
            Widgets.option("DAP", "DAP"), Widgets.option("DDP", "DDP")));
    private final JTextField incoterms2 = reg(Widgets.field("VBKD-INCO2", "Incoterms location", 18));
    private final JTextField netValue = reg(Widgets.readOnly("VBAK-NETWR", "Net value", 14));

    // Shipping tab
    private final JComboBox<Widgets.Option> shipCond = reg(Widgets.combo("VBAK-VSBED", "Shipping Conditions",
            Widgets.option("01", "01 Standard"), Widgets.option("02", "02 Express"),
            Widgets.option("03", "03 Pick up")));
    private final JTextField shipToAddress = reg(Widgets.readOnly("SHIPTO-ADDR", "Ship-to address", 40));

    private final JTabbedPane tabs = A11y.name(new JTabbedPane(), "TAB");
    private final JScrollPane itemsScroll = new JScrollPane();
    private final List<ItemRow> items = new ArrayList<>();
    private ItemGrid grid;

    private final JButton continueButton = Widgets.button("Continue", "Continue", Icons.ENTER, e -> proceed());
    private final JButton saveButton = Widgets.button("Save Order", "Save", Icons.SAVE, e -> save());
    private final JButton ordersButton = Widgets.button("Orders", "Orders", Icons.DISPLAY, e -> shell.open("VA03"));

    /** One item row of the "All items" table. */
    private final class ItemRow {
        final JLabel posnr;
        final JTextField matnr;
        final JTextField qty;
        final JLabel desc;
        final JLabel price;
        final JLabel value;
        final JLabel curr;
        final JTextField plant;
        final JLabel confirmed;

        ItemRow(int i) {
            posnr = new JLabel(String.valueOf(i * 10));
            matnr = reg(Widgets.field("RV45A-MABNR-" + i, "Material", 10));
            qty = reg(Widgets.field("RV45A-KWMENG-" + i, "Order Quantity", 6));
            desc = Widgets.valueLabel("DESC-RV45A-MABNR-" + i, "");
            desc.setPreferredSize(new Dimension(200, 18));
            price = Widgets.valueLabel("VBAP-NETPR-" + i, "");
            price.setPreferredSize(new Dimension(80, 18));
            value = Widgets.valueLabel("VBAP-NETWR-" + i, "");
            value.setPreferredSize(new Dimension(90, 18));
            curr = Widgets.valueLabel("VBAP-WAERK-" + i, "");
            plant = reg(Widgets.field("VBAP-WERKS-" + i, "Plnt", 4));
            confirmed = Widgets.valueLabel("VBEP-BMENG-" + i, "");
            Widgets.valueHelp(matnr, () -> ValueHelpDialog.open(shell, "materials", matnr));
            Widgets.onChange(matnr, () -> A11y.setValue(desc, sys.describe("materials", matnr.getText())));
            for (JTextField f : new JTextField[]{matnr, qty, plant}) {
                f.addActionListener(e -> enter());
            }
        }

        JComponent[] cells() {
            return new JComponent[]{posnr, matnr, qty, new JLabel("EA"), desc, price, value, curr, plant, confirmed};
        }

        Va01.Item read() {
            Va01.Item item = new Va01.Item();
            item.matnr = Fmt.up(matnr.getText());
            item.qty = Fmt.trim(qty.getText());
            item.plant = Fmt.up(plant.getText());
            return item;
        }

        void show(Va01.CheckedItem item) {
            A11y.setValue(desc, item == null ? sys.describe("materials", matnr.getText()) : item.description);
            A11y.setValue(price, item == null ? "" : Fmt.amount(item.price));
            A11y.setValue(value, item == null ? "" : Fmt.amount(item.value));
            A11y.setValue(curr, item == null ? "" : "USD");
            A11y.setValue(confirmed, item == null ? "" : String.valueOf(item.confirmed));
        }
    }

    Va01Screen(Shell shell) {
        super(shell);
        Widgets.valueHelp(auart, () -> ValueHelpDialog.open(shell, "ordertypes", auart));
        Widgets.valueHelp(soldTo, () -> ValueHelpDialog.open(shell, "customers", soldTo));
        Widgets.valueHelp(shipTo, () -> ValueHelpDialog.open(shell, "customers", shipTo));
        Widgets.valueHelp(terms, () -> ValueHelpDialog.open(shell, "terms", terms));
        Widgets.onChange(auart, () -> A11y.setValue(auartDesc, sys.describe("ordertypes", auart.getText())));
        Widgets.onChange(soldTo, () -> A11y.setValue(soldToDesc, sys.describe("customers", soldTo.getText())));
        Widgets.onChange(shipTo, () -> A11y.setValue(shipToDesc, sys.describe("customers", shipTo.getText())));
        Widgets.onChange(plant, () -> A11y.setValue(plantDesc,
                "1000".equals(Fmt.up(plant.getText())) ? "Meridian DC Newark" : ""));
        for (JTextField f : new JTextField[]{auart, vkorg, vtweg, spart, soldTo, shipTo, custPo, custPoDate, reqDate,
                plant, terms, incoterms2}) {
            f.addActionListener(e -> enter());
        }

        // Initial screen
        FormPanel initial = new FormPanel();
        initial.addField("Order Type", auart, auartDesc);
        FormPanel org = new FormPanel();
        org.addField("Sales Organization", vkorg);
        org.addField("Distribution Channel", vtweg);
        org.addField("Division", spart);
        org.addLine(new JLabel("Sales area for this system: 1000 / 10 / 00 (Meridian Supply – Domestic)"));
        JPanel initialPage = padded(stack(initial, Widgets.group("Organizational Data", org)));

        // Overview
        FormPanel headLeft = new FormPanel();
        // The caption of VBAK-VBELN shows the order type name, as on the web.
        headLeft.addField(orderNameCaption, vbeln);
        headLeft.addField("Sold-To Party", soldTo, soldToDesc);
        headLeft.addField("Ship-To Party", shipTo, shipToDesc);
        FormPanel headRight = new FormPanel();
        headRight.addField("Cust. Reference", custPo);
        headRight.addField("Cust. Ref. Date", custPoDate);
        headRight.addField("Credit limit", creditLimit);
        JPanel head = new JPanel(new GridLayout(1, 2, 10, 0));
        head.add(headLeft);
        head.add(headRight);

        FormPanel salesLeft = new FormPanel();
        salesLeft.addField("Req. deliv.date", reqDate);
        salesLeft.addField("Deliver.Plant", plant, plantDesc);
        salesLeft.addField("Payment terms", terms);
        FormPanel salesRight = new FormPanel();
        salesRight.addField("Incoterms", incoterms, incoterms2);
        salesRight.addField("Net value", netValue, new JLabel("USD"));
        JPanel sales = new JPanel(new GridLayout(1, 2, 10, 0));
        sales.add(salesLeft);
        sales.add(salesRight);
        FormPanel shipping = new FormPanel();
        shipping.addField("Shipping Conditions", shipCond);
        shipping.addField("Ship-to address", shipToAddress);
        tabs.addTab("Sales", top(sales));
        tabs.addTab("Shipping", top(shipping));

        itemsScroll.setPreferredSize(new Dimension(900, 230));
        JPanel itemsHeader = new JPanel(new BorderLayout());
        itemsHeader.add(new JLabel("All items"), BorderLayout.WEST);
        itemsHeader.add(Widgets.button("Insert rows", "Insert rows", Icons.NEW_ROWS, e -> grid.addRows(5)),
                BorderLayout.EAST);

        JScrollPane overviewScroll = new JScrollPane(padded(stack(head, tabs, itemsHeader, itemsScroll)));
        overviewScroll.setBorder(null);

        steps.add(initialPage, INITIAL);
        steps.add(overviewScroll, OVERVIEW);
        add(steps, BorderLayout.CENTER);
    }

    @Override
    String tcode() {
        return "VA01";
    }

    @Override
    void start() {
        showInitial("", "", "", "");
    }

    @Override
    void enter() {
        if (INITIAL.equals(step)) {
            proceed();
            return;
        }
        shell.run(() -> {
            Va01.Check c = check(false);
            FieldError error = c.firstError();
            if (error != null) {
                showError(error);
            } else if (!c.warnings.isEmpty()) {
                shell.status().show(Message.warning(c.warnings.get(0)));
            }
        });
    }

    @Override
    void save() {
        if (INITIAL.equals(step)) {
            proceed();
            return;
        }
        shell.run(() -> {
            Va01.Form form = readForm();
            Va01.Check c = check(true);
            FieldError error = c.firstError();
            if (error != null) {
                showError(error);
                return;
            }
            Message message = Va01.save(sys, form, c, shell.session().user.username);
            showInitial(form.auart, form.vkorg, form.vtweg, form.spart);
            shell.status().show(message);
        });
    }

    /** Continue from the initial screen to the overview. */
    private void proceed() {
        shell.run(() -> {
            clearError();
            FieldError error = Va01.checkSalesArea(Fmt.up(auart.getText()), Fmt.up(vkorg.getText()),
                    Fmt.up(vtweg.getText()), Fmt.up(spart.getText()));
            if (error != null) {
                showError(error);
                return;
            }
            showOverview();
            shell.maybeInfoPopup("VA01");
        });
    }

    private void showInitial(String type, String org, String channel, String division) {
        clearError();
        step = INITIAL;
        auart.setText(type);
        vkorg.setText(org);
        vtweg.setText(channel);
        spart.setText(division);
        cards.show(steps, INITIAL);
        shell.setScreenTitle("Create Sales Documents");
        shell.setAppButtons(continueButton);
        auart.requestFocusInWindow();
    }

    private void showOverview() {
        step = OVERVIEW;
        String type = Fmt.up(auart.getText());
        auart.setText(type);
        vkorg.setText(Fmt.up(vkorg.getText()));
        vtweg.setText(Fmt.up(vtweg.getText()));
        spart.setText(Fmt.up(spart.getText()));
        String orderName = Va01.ORDER_TYPES.get(type);
        orderNameCaption.setText(orderName);
        vbeln.getAccessibleContext().setAccessibleDescription(orderName);
        for (JTextField f : new JTextField[]{vbeln, soldTo, shipTo, custPo, custPoDate, terms, incoterms2, netValue,
                shipToAddress}) {
            f.setText("");
        }
        A11y.setValue(creditLimit, "");
        reqDate.setText(Fmt.date(LocalDate.now().plusDays("RO".equals(type) ? 1 : 7)));
        plant.setText("1000");
        Widgets.select(incoterms, "FOB");
        Widgets.select(shipCond, "01");
        tabs.setSelectedIndex(0);
        items.clear();
        grid = new ItemGrid(new String[]{"Item", "Material", "Order Quantity", "Un", "Description", "Net price",
                "Net value", "Curr.", "Plnt", "Confirmed"}, row -> {
            ItemRow item = new ItemRow(row);
            items.add(item);
            return item.cells();
        });
        grid.addRows(Va01.ITEMS_DEFAULT);
        itemsScroll.setViewportView(grid);
        cards.show(steps, OVERVIEW);
        shell.setScreenTitle("Create " + orderName + ": Overview");
        shell.setAppButtons(saveButton, ordersButton);
        soldTo.requestFocusInWindow();
    }

    private Va01.Check check(boolean full) {
        clearError();
        Va01.Check c = Va01.check(sys, readForm(), full);
        display(c);
        return c;
    }

    private Va01.Form readForm() {
        Va01.Form f = new Va01.Form();
        f.auart = Fmt.up(auart.getText());
        f.vkorg = Fmt.up(vkorg.getText());
        f.vtweg = Fmt.up(vtweg.getText());
        f.spart = Fmt.up(spart.getText());
        f.soldTo = Fmt.trim(soldTo.getText());
        f.shipTo = Fmt.trim(shipTo.getText());
        f.custPo = Fmt.trim(custPo.getText());
        f.custPoDate = Fmt.trim(custPoDate.getText());
        f.reqDate = Fmt.trim(reqDate.getText());
        String p = Fmt.up(plant.getText());
        f.plant = p.isEmpty() ? "1000" : p;
        f.terms = Fmt.up(terms.getText());
        for (ItemRow row : items) {
            f.items.add(row.read());
        }
        return f;
    }

    /** Shows the results of a check: partner data, credit limit, prices and confirmed quantities. */
    private void display(Va01.Check c) {
        Customer sold = c.soldTo;
        if (sold != null) {
            A11y.setValue(creditLimit, Fmt.amount(sold.creditLimit) + " USD" + (sold.salesBlock ? " Order block" : ""));
            if (terms.getText().trim().isEmpty()) {
                terms.setText(sold.terms);
            }
        } else {
            A11y.setValue(creditLimit, "");
        }
        Customer ship = c.shipTo;
        shipToAddress.setText(ship == null ? "" : ship.street + ", " + ship.city);
        netValue.setText(Fmt.amount(c.net));
        for (int i = 0; i < items.size(); i++) {
            items.get(i).show(c.item(i + 1));
        }
    }

    private static JPanel top(JComponent content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(content, BorderLayout.NORTH);
        return panel;
    }
}
