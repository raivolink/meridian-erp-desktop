package meridian.ui;

import meridian.data.Customer;
import meridian.data.Fmt;
import meridian.data.Material;
import meridian.data.Message;
import meridian.data.User;
import meridian.data.Vendor;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;

/** ZDEMO_ADMIN Demo Maintenance: reset to zero state, hard mode, master data lists. ADMIN role only. */
final class AdminScreen extends Screen {
    private static final DateTimeFormatter MINUTES = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JLabel docs = Widgets.valueLabel("adm-docs", "0");
    private final JLabel orders = Widgets.valueLabel("adm-orders", "0");
    private final JLabel resetAt = Widgets.valueLabel("adm-reset-at", "");
    private final JLabel hard = Widgets.valueLabel("adm-hard", "OFF");
    private final JButton hardButton = Widgets.button("Switch hard mode", "Switch hard mode ON", null,
            e -> toggleHardMode());

    private final ListTableModel<User> usersModel = new ListTableModel<User>()
            .column("User", u -> u.username)
            .column("Name", u -> u.fullName)
            .column("Role", u -> u.role)
            .column("Last logon", u -> u.getLastLogin() == null ? "-" : u.getLastLogin().format(MINUTES));
    private final ListTableModel<Vendor> vendorsModel = new ListTableModel<Vendor>()
            .column("Vendor", v -> v.id)
            .column("Name", v -> v.name)
            .column("City", v -> v.city)
            .column("Terms", v -> v.terms)
            .column("G/L", v -> v.defaultGl)
            .column("Cost Ctr", v -> v.defaultCc)
            .column("Block", v -> v.postingBlock ? "Posting block" : "");
    private final ListTableModel<Customer> customersModel = new ListTableModel<Customer>()
            .column("Customer", c -> c.id)
            .column("Name", c -> c.name)
            .column("City", c -> c.city)
            .column("Terms", c -> c.terms)
            .column("Credit limit", c -> Fmt.amount(c.creditLimit))
            .column("Block", c -> c.salesBlock ? "Order block" : "");
    private final ListTableModel<Material> materialsModel = new ListTableModel<Material>()
            .column("Material", m -> m.id)
            .column("Description", m -> m.name)
            .column("Price", m -> Fmt.amount(m.price))
            .column("Stock", Material::getStock)
            .column("Status", m -> m.deletionFlag ? "Deletion flag" : m.getStock() < 10 ? "Low stock" : "");
    private final JTabbedPane masterData = A11y.name(new JTabbedPane(), "Master Data");

    AdminScreen(Shell shell) {
        super(shell);
        FormPanel status = new FormPanel();
        status.addField("Vendor invoices", docs);
        status.addField("Sales orders", orders);
        status.addField("Last reset", resetAt);
        status.addLine(new JLabel("<html><div style='width:380px'>Reset drops all posted documents and sales orders,"
                + " restores master data and stock, and restarts number ranges (invoices 1900000001,"
                + " orders 5000001).</div></html>"));
        status.addLine(Widgets.button("Reset to zero state", "Reset to zero state", Icons.CANCEL,
                A11y.deferred(this::confirmReset)));

        FormPanel difficulty = new FormPanel();
        difficulty.addField("Hard mode", hard);
        difficulty.addLine(new JLabel("<html><div style='width:380px'>Hard mode adds typical RPA obstacles:"
                + " slow responses (0.5&ndash;3 s), occasional information popups on FB60/VA01, and the"
                + " multiple-logon dialog after logon. Names and labels stay stable.</div></html>"));
        difficulty.addLine(hardButton);

        JPanel top = new JPanel(new GridLayout(1, 2, 10, 0));
        top.add(Widgets.group("Demo data status", status));
        top.add(Widgets.group("Robot difficulty", difficulty));

        masterData.addTab("Users", scroll(Widgets.table("adm-users", usersModel)));
        masterData.addTab("Vendors", scroll(Widgets.table("adm-vendors", vendorsModel)));
        masterData.addTab("Customers", scroll(Widgets.table("adm-customers", customersModel)));
        masterData.addTab("Materials", scroll(Widgets.table("adm-materials", materialsModel)));

        JPanel page = new JPanel(new BorderLayout(0, 8));
        page.add(top, BorderLayout.NORTH);
        page.add(masterData, BorderLayout.CENTER);
        add(padded(page), BorderLayout.CENTER);
        sys.addChangeListener(this::refresh);
    }

    @Override
    String tcode() {
        return "ZDEMO_ADMIN";
    }

    @Override
    void start() {
        shell.setScreenTitle("Demo Maintenance (ZDEMO_ADMIN)");
        shell.setAppButtons();
        refresh();
    }

    private void refresh() {
        A11y.setValue(docs, String.valueOf(sys.documentCount()));
        A11y.setValue(orders, String.valueOf(sys.orderCount()));
        A11y.setValue(resetAt, sys.getResetAt().format(Fb03Screen.TIMESTAMP));
        A11y.setValue(hard, sys.isHardMode() ? "ON" : "OFF");
        hardButton.setText(sys.isHardMode() ? "Switch hard mode OFF" : "Switch hard mode ON");
        usersModel.setRows(sys.getUsers());
        vendorsModel.setRows(sys.getVendors());
        customersModel.setRows(sys.getCustomers());
        materialsModel.setRows(sys.getMaterials());
        masterData.setTitleAt(1, "Vendors (" + sys.getVendors().size() + ")");
        masterData.setTitleAt(2, "Customers (" + sys.getCustomers().size() + ")");
        masterData.setTitleAt(3, "Materials (" + sys.getMaterials().size() + ")");
    }

    private void confirmReset() {
        int answer = MessageDialog.show(shell.window(), "Reset Demo Data", Icons.MESSAGE_WARNING,
                "Reset all demo data to zero state?\n\n" + sys.documentCount() + " vendor invoices and "
                        + sys.orderCount() + " sales orders will be deleted. This cannot be undone.", "Yes", "No");
        if (answer == 0) {
            shell.run(() -> {
                sys.resetToZeroState();
                shell.status().show(Message.success("Demo data has been reset to zero state"));
            });
        }
    }

    private void toggleHardMode() {
        shell.run(() -> {
            boolean on = !sys.isHardMode();
            sys.setHardMode(on);
            shell.status().show(Message.success("Hard mode switched " + (on ? "ON" : "OFF")));
        });
    }

    private static JScrollPane scroll(JTable table) {
        return new JScrollPane(table);
    }
}
