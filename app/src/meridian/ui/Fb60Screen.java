package meridian.ui;

import meridian.data.Fb60;
import meridian.data.FieldError;
import meridian.data.Fmt;
import meridian.data.GlAccount;
import meridian.data.Message;
import meridian.data.Vendor;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** FB60 Enter Vendor Invoice. */
final class Fb60Screen extends Screen {
    private final JComboBox<Widgets.Option> trans = reg(Widgets.combo("ACGL_HEAD-TRANS", "Transaction",
            Widgets.option("R", "Invoice"), Widgets.option("G", "Credit memo")));
    private final JLabel balance = Widgets.valueLabel("balance-amt", "0.00");
    private final JLabel balanceCurrency = new JLabel("USD");
    private final JLabel light = Widgets.valueLabel("balance-light", "red");

    private final JTextField vendor = reg(Widgets.field("INVFO-ACCNT", "Vendor", 12));
    private final JLabel vendorDesc = Widgets.valueLabel("DESC-INVFO-ACCNT", "");
    private final JTextField docDate = reg(Widgets.field("INVFO-BLDAT", "Invoice date", 11));
    private final JTextField postDate = reg(Widgets.field("INVFO-BUDAT", "Posting Date", 11));
    private final JTextField reference = reg(Widgets.field("INVFO-XBLNR", "Reference", 18));
    private final JTextField amount = reg(Widgets.field("INVFO-WRBTR", "Amount", 14));
    private final JTextField currency = reg(Widgets.field("INVFO-WAERS", "Currency", 4));
    private final JCheckBox calcTax = reg(Widgets.checkBox("INVFO-XMWST", "Calculate tax"));
    private final JTextField taxCode = reg(Widgets.field("INVFO-MWSKZ", "Tax code", 12));
    private final JTextField text = reg(Widgets.field("INVFO-SGTXT", "Text", 40));

    private final JTextField baseline = reg(Widgets.field("INVFO-ZFBDT", "Baseline date", 11));
    private final JTextField terms = reg(Widgets.field("INVFO-ZTERM", "Payment terms", 5));
    private final JTextField dueDate = reg(Widgets.readOnly("INVFO-FAEDT", "Due on", 11));

    private final JTextField headerText = reg(Widgets.field("INVFO-BKTXT", "Header text", 40));
    private final JTextField companyCode = reg(Widgets.readOnly("INVFO-BUKRS", "Company code", 5));
    private final JTextField docType = reg(Widgets.readOnly("INVFO-BLART", "Document type", 3));
    private final JLabel docTypeDesc = Widgets.valueLabel("DESC-INVFO-BLART", "Vendor invoice");

    private final JLabel vdId = Widgets.valueLabel("VD-ACCNT", "");
    private final JLabel vdName = Widgets.valueLabel("VD-NAME", "");
    private final JLabel vdAddress = Widgets.valueLabel("VD-ADDRESS", "");
    private final JLabel vdTerms = Widgets.valueLabel("VD-ZTERM", "");
    private final JLabel vdDefaults = Widgets.valueLabel("VD-DEFAULTS", "");
    private final JLabel vdStatus = Widgets.valueLabel("VD-STATUS", "");
    private final JLabel vendorHint = new JLabel("Enter a vendor and press Enter to display vendor data.");
    private final JPanel vendorData = new JPanel(new BorderLayout());
    private final FormPanel vendorForm = new FormPanel();

    private final JLabel itemsTitle = Widgets.valueLabel("items-title", "");
    private final JScrollPane itemsScroll = new JScrollPane();
    private final List<LineRow> lines = new ArrayList<>();
    private ItemGrid grid;

    private final ListTableModel<Fb60.Posting> simulationModel = new ListTableModel<Fb60.Posting>()
            .column("Itm", p -> this.simulationModel.getRows().indexOf(p) + 1)
            .column("PK", p -> p.pk)
            .column("Account", p -> p.account)
            .column("Account short text", p -> p.description)
            .column("Amount", p -> ("H".equals(p.dc) ? "-" : "") + Fmt.amount(p.amount))
            .column("Curr.", p -> currencyCode())
            .column("Tx", p -> p.tax)
            .column("Cost Ctr", p -> p.cc);
    private final JTable simulation = Widgets.table("simulation", simulationModel);
    private final JPanel simulationGroup;

    private final JButton companyCodeButton = Widgets.button("Company Code", "Company Code", Icons.DISPLAY,
            A11y.deferred(this::changeCompanyCode));
    private final JButton simulateButton = Widgets.button("Simulate", "Simulate", Icons.SIMULATE, e -> simulate());
    private final JButton postButton = Widgets.button("Post", "Post", Icons.POST, e -> save());

    /** One G/L line item row. */
    private final class LineRow {
        final JTextField gl;
        final JLabel glDesc;
        final JComboBox<Widgets.Option> dc;
        final JTextField amount;
        final JTextField tax;
        final JTextField cc;
        final JTextField text;

        LineRow(int i) {
            gl = reg(Widgets.field("ACGL_ITEM-HKONT-" + i, "G/L acct", 7));
            glDesc = Widgets.valueLabel("DESC-ACGL_ITEM-HKONT-" + i, "");
            glDesc.setPreferredSize(new Dimension(190, 18));
            dc = reg(Widgets.combo("ACGL_ITEM-SHKZG-" + i, "D/C",
                    Widgets.option("S", "Debit"), Widgets.option("H", "Credit")));
            amount = reg(Widgets.field("ACGL_ITEM-WRBTR-" + i, "Amount in doc.curr.", 12));
            tax = reg(Widgets.field("ACGL_ITEM-MWSKZ-" + i, "Tax code", 10));
            cc = reg(Widgets.field("ACGL_ITEM-KOSTL-" + i, "Cost Center", 7));
            text = reg(Widgets.field("ACGL_ITEM-SGTXT-" + i, "Text", 24));
            Widgets.valueHelp(gl, () -> ValueHelpDialog.open(shell, "gl", gl));
            Widgets.valueHelp(tax, () -> ValueHelpDialog.open(shell, "tax", tax));
            Widgets.valueHelp(cc, () -> ValueHelpDialog.open(shell, "cc", cc));
            Widgets.onChange(gl, () -> A11y.setValue(glDesc, sys.describe("gl", gl.getText())));
            Widgets.onChange(amount, Fb60Screen.this::updateBalance);
            Widgets.onChange(tax, Fb60Screen.this::updateBalance);
            dc.addActionListener(e -> updateBalance());
            for (JTextField f : new JTextField[]{gl, amount, tax, cc, text}) {
                f.addActionListener(e -> enter());
            }
        }

        JComponent[] cells() {
            return new JComponent[]{gl, glDesc, dc, amount, tax, cc, text};
        }

        Fb60.Line read() {
            Fb60.Line l = new Fb60.Line();
            l.gl = Fmt.trim(gl.getText());
            l.dc = Widgets.value(dc);
            l.amount = Fmt.trim(amount.getText());
            l.tax = Fmt.up(tax.getText());
            l.cc = Fmt.up(cc.getText());
            l.text = Fmt.trim(text.getText());
            return l;
        }
    }

    Fb60Screen(Shell shell) {
        super(shell);
        Widgets.valueHelp(vendor, () -> ValueHelpDialog.open(shell, "vendors", vendor));
        Widgets.valueHelp(taxCode, () -> ValueHelpDialog.open(shell, "tax", taxCode));
        Widgets.valueHelp(terms, () -> ValueHelpDialog.open(shell, "terms", terms));
        Widgets.onChange(vendor, () -> A11y.setValue(vendorDesc, sys.describe("vendors", vendor.getText())));
        Widgets.onChange(amount, this::updateBalance);
        Widgets.onChange(taxCode, this::updateBalance);
        Widgets.onChange(currency, () -> balanceCurrency.setText(currencyCode()));
        calcTax.addActionListener(e -> updateBalance());
        trans.addActionListener(e -> {
            updateTitle();
            updateBalance();
        });
        for (JTextField f : new JTextField[]{vendor, docDate, postDate, reference, amount, currency, taxCode, text,
                baseline, terms, headerText}) {
            f.addActionListener(e -> enter());
        }
        light.setText(null);
        light.setIcon(Icons.LIGHT_RED);

        // Header line: transaction type and balance light.
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        FormPanel transForm = new FormPanel();
        transForm.addField("Transaction", trans);
        head.add(transForm, BorderLayout.WEST);
        JPanel balanceBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        balanceBox.setOpaque(false);
        JLabel balanceCaption = new JLabel("Bal.");
        balanceCaption.setLabelFor(balance);
        balanceBox.add(balanceCaption);
        balanceBox.add(balance);
        balanceBox.add(balanceCurrency);
        balanceBox.add(light);
        head.add(balanceBox, BorderLayout.EAST);

        FormPanel basic = new FormPanel();
        basic.addField("Vendor", vendor, vendorDesc);
        basic.addField("Invoice date", docDate);
        basic.addField("Posting Date", postDate);
        basic.addField("Reference", reference);
        basic.addField("Amount", amount, currency, calcTax);
        basic.addField("Tax code", taxCode);
        basic.addField("Text", text);

        FormPanel payment = new FormPanel();
        payment.addField("Baseline date", baseline);
        payment.addField("Payment terms", terms);
        payment.addField("Due on", dueDate);

        FormPanel details = new FormPanel();
        details.addField("Header text", headerText);
        details.addField("Company code", companyCode, new JLabel("Meridian Group Inc. New York"));
        details.addField("Document type", docType, docTypeDesc);

        JTabbedPane tabs = A11y.name(new JTabbedPane(), "TAB");
        tabs.addTab("Basic data", top(basic));
        tabs.addTab("Payment", top(payment));
        tabs.addTab("Details", top(details));

        vendorForm.addField("Vendor", vdId);
        vendorForm.addField("Name", vdName);
        vendorForm.addField("Address", vdAddress);
        vendorForm.addField("Payment terms", vdTerms);
        vendorForm.addField("Default G/L / CC", vdDefaults);
        vendorForm.addField("Status", vdStatus);
        vendorData.setOpaque(false);
        vendorData.add(vendorHint, BorderLayout.NORTH);
        JPanel vendorGroup = Widgets.group("Vendor data", vendorData);
        vendorGroup.setPreferredSize(new Dimension(380, 230));

        JPanel middle = new JPanel(new BorderLayout(10, 0));
        middle.setOpaque(false);
        middle.add(tabs, BorderLayout.CENTER);
        middle.add(vendorGroup, BorderLayout.EAST);

        itemsScroll.setPreferredSize(new Dimension(900, 170));
        JPanel itemsHeader = new JPanel(new BorderLayout());
        itemsHeader.setOpaque(false);
        itemsHeader.add(itemsTitle, BorderLayout.WEST);
        itemsHeader.add(Widgets.button("Insert rows", "Insert rows", Icons.NEW_ROWS, e -> addLines(5)),
                BorderLayout.EAST);
        JPanel items = new JPanel(new BorderLayout(0, 2));
        items.setOpaque(false);
        items.add(itemsHeader, BorderLayout.NORTH);
        items.add(itemsScroll, BorderLayout.CENTER);

        JScrollPane simulationScroll = new JScrollPane(simulation);
        simulationScroll.setPreferredSize(new Dimension(900, 130));
        simulationGroup = Widgets.group("Document Overview – Simulate", simulationScroll);

        JScrollPane scroll = new JScrollPane(padded(stack(head, middle, items, simulationGroup)));
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);
    }

    @Override
    String tcode() {
        return "FB60";
    }

    @Override
    void start() {
        resetForm();
        shell.setAppButtons(companyCodeButton, simulateButton, postButton);
        if (shell.session().companyCode == null) {
            shell.setScreenTitle("Enter Vendor Invoice");
            SwingUtilities.invokeLater(() -> {
                if (CompanyCodeDialog.ask(shell)) {
                    ready();
                } else {
                    shell.goHome();
                }
            });
        } else {
            ready();
        }
    }

    private void ready() {
        companyCode.setText(shell.session().companyCode);
        updateTitle();
        vendor.requestFocusInWindow();
        shell.maybeInfoPopup("FB60");
    }

    private void changeCompanyCode() {
        if (CompanyCodeDialog.ask(shell)) {
            companyCode.setText(shell.session().companyCode);
            updateTitle();
        }
    }

    @Override
    void enter() {
        shell.run(() -> {
            Fb60.Check c = check(false);
            display(c);
            FieldError error = c.firstError();
            if (error != null) {
                showError(error);
            }
        });
    }

    private void simulate() {
        shell.run(() -> {
            Fb60.Check c = check(true);
            display(c);
            FieldError error = c.firstError();
            if (error != null) {
                showError(error);
                return;
            }
            simulationModel.setRows(c.postings);
            simulationGroup.setVisible(true);
            shell.status().show(Message.info("Document balanced. Choose Post to post the document."));
        });
    }

    /** Post. */
    @Override
    void save() {
        shell.run(() -> {
            Fb60.Form form = readForm();
            Fb60.Check c = Fb60.check(sys, shell.session().companyCode, form, true);
            display(c);
            FieldError error = c.firstError();
            if (error != null) {
                showError(error);
                return;
            }
            String docNo = Fb60.post(sys, shell.session().companyCode, form, c, shell.session().user.username);
            resetForm();
            updateTitle();
            shell.status().show(Message.success("Document " + docNo + " was posted in company code "
                    + shell.session().companyCode));
            shell.maybeInfoPopup("FB60");
        });
    }

    private Fb60.Check check(boolean full) {
        clearError();
        return Fb60.check(sys, shell.session().companyCode, readForm(), full);
    }

    private Fb60.Form readForm() {
        Fb60.Form f = new Fb60.Form();
        f.trans = Widgets.value(trans);
        f.vendor = Fmt.up(vendor.getText());
        f.docDate = Fmt.trim(docDate.getText());
        f.postDate = Fmt.trim(postDate.getText());
        f.reference = Fmt.trim(reference.getText());
        f.amount = Fmt.trim(amount.getText());
        f.currency = currencyCode();
        f.calcTax = calcTax.isSelected();
        f.taxCode = Fmt.up(taxCode.getText());
        f.text = Fmt.trim(text.getText());
        f.baseline = Fmt.trim(baseline.getText());
        f.terms = Fmt.up(terms.getText());
        f.headerText = Fmt.trim(headerText.getText());
        for (LineRow row : lines) {
            f.lines.add(row.read());
        }
        return f;
    }

    /** Shows the results of a check: vendor data, defaults, due date, short texts and balance. */
    private void display(Fb60.Check c) {
        Vendor v = c.vendor;
        vendorData.removeAll();
        if (v != null) {
            A11y.setValue(vdId, v.id);
            A11y.setValue(vdName, v.name);
            A11y.setValue(vdAddress, v.street + ", " + v.city);
            A11y.setValue(vdTerms, v.terms);
            A11y.setValue(vdDefaults, v.defaultGl + " / " + v.defaultCc);
            A11y.setValue(vdStatus, v.postingBlock ? "Posting block" : "Active");
            vendorData.add(vendorForm, BorderLayout.NORTH);
            if (terms.getText().trim().isEmpty()) {
                terms.setText(v.terms);
            }
        } else {
            vendorData.add(vendorHint, BorderLayout.NORTH);
        }
        vendorData.revalidate();
        vendorData.repaint();
        dueDate.setText(Fmt.date(c.dueDate));
        for (LineRow row : lines) {
            GlAccount gl = sys.glAccount(Fmt.trim(row.gl.getText()));
            A11y.setValue(row.glDesc, gl == null ? "" : gl.description);
        }
        showBalance(c.balance);
        updateItemsTitle();
    }

    private void updateBalance() {
        if (shell.session() == null || shell.session().companyCode == null) {
            return;
        }
        showBalance(Fb60.check(sys, shell.session().companyCode, readForm(), false).balance);
    }

    private void showBalance(double value) {
        A11y.setValue(balance, Fmt.amount(value));
        Double gross = Fmt.parseAmount(amount.getText());
        boolean green = value == 0 && gross != null && !gross.isNaN() && gross != 0;
        light.setIcon(green ? Icons.LIGHT_GREEN : Icons.LIGHT_RED);
        light.getAccessibleContext().setAccessibleDescription(green ? "green" : "red");
        updateItemsTitle();
    }

    private void updateTitle() {
        boolean memo = "G".equals(Widgets.value(trans));
        String cc = shell.session() == null ? null : shell.session().companyCode;
        shell.setScreenTitle("Enter Vendor " + (memo ? "Credit Memo" : "Invoice")
                + (cc == null ? "" : ": Company Code " + cc));
        docType.setText(memo ? "KG" : "KR");
        A11y.setValue(docTypeDesc, memo ? "Vendor credit memo" : "Vendor invoice");
    }

    private void updateItemsTitle() {
        int used = 0;
        for (LineRow row : lines) {
            if (!row.gl.getText().trim().isEmpty() || !row.amount.getText().trim().isEmpty()) {
                used++;
            }
        }
        A11y.setValue(itemsTitle, used + " Items ( No entry variant selected )");
    }

    private void resetForm() {
        clearError();
        Widgets.select(trans, "R");
        for (JTextField f : new JTextField[]{vendor, docDate, reference, amount, taxCode, text, baseline, terms,
                headerText}) {
            f.setText("");
        }
        postDate.setText(Fmt.date(LocalDate.now()));
        currency.setText("USD");
        calcTax.setSelected(false);
        dueDate.setText("");
        lines.clear();
        grid = new ItemGrid(new String[]{"G/L acct", "Short Text", "D/C", "Amount in doc.curr.", "Tax code",
                "Cost Center", "Text"}, row -> {
            LineRow line = new LineRow(row);
            lines.add(line);
            return line.cells();
        });
        grid.addRows(Fb60.LINES_DEFAULT);
        itemsScroll.setViewportView(grid);
        simulationModel.setRows(new ArrayList<Fb60.Posting>());
        simulationGroup.setVisible(false);
        display(new Fb60.Check());
    }

    private void addLines(int count) {
        grid.addRows(count);
    }

    private String currencyCode() {
        String c = Fmt.up(currency.getText());
        return c.isEmpty() ? "USD" : c;
    }

    private static JPanel top(JComponent content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(content, BorderLayout.NORTH);
        return panel;
    }
}
