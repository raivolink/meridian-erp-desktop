package meridian.ui;

import meridian.data.Fmt;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;

/** Meridian Easy Access: the user menu tree plus key figures. */
final class HomeScreen extends Screen {
    /** A menu entry that starts a transaction. */
    private static final class Entry {
        final String tcode;
        final String text;

        Entry(String tcode, String text) {
            this.tcode = tcode;
            this.text = text;
        }

        @Override
        public String toString() {
            return tcode + " - " + text;
        }
    }

    private final JTree tree = A11y.name(new JTree(buildMenu()), "Easy Access Menu");
    private final JLabel docs = Widgets.valueLabel("kpi-docs", "0");
    private final JLabel docsAmount = Widgets.valueLabel("kpi-docs-amount", "");
    private final JLabel orders = Widgets.valueLabel("kpi-orders", "0");
    private final JLabel ordersAmount = Widgets.valueLabel("kpi-orders-amount", "");
    private final JLabel systemMessage = Widgets.valueLabel("System Messages", "");

    HomeScreen(Shell shell) {
        super(shell);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setRowHeight(20);
        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree t, Object value, boolean selected, boolean expanded,
                                                          boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(t, value, selected, expanded, leaf, row, hasFocus);
                Object entry = ((DefaultMutableTreeNode) value).getUserObject();
                setIcon(entry instanceof Entry ? Icons.TRANSACTION : Icons.FOLDER);
                return this;
            }
        });
        expandDefaults();
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    enter();
                }
            }
        });
        tree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "openEntry");
        tree.getActionMap().put("openEntry", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enter();
            }
        });

        JScrollPane treeScroll = new JScrollPane(tree);
        treeScroll.setPreferredSize(new Dimension(380, 0));
        treeScroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, ErpTheme.BORDER));

        add(treeScroll, BorderLayout.WEST);
        add(splash(), BorderLayout.CENTER);
        sys.addChangeListener(this::refresh);
    }

    @Override
    String tcode() {
        return "SESSION_MANAGER";
    }

    @Override
    void start() {
        shell.setScreenTitle("Meridian Easy Access – User Menu for " + shell.session().user.fullName);
        shell.setAppButtons();
        refresh();
    }

    /** Opens the selected menu entry. */
    @Override
    void enter() {
        TreePath path = tree.getSelectionPath();
        if (path == null) {
            return;
        }
        Object entry = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
        if (entry instanceof Entry) {
            shell.open(((Entry) entry).tcode);
        }
    }

    private void refresh() {
        A11y.setValue(docs, String.valueOf(sys.documentCount()));
        A11y.setValue(docsAmount, Fmt.amount(sys.documentAmount()) + " USD");
        A11y.setValue(orders, String.valueOf(sys.orderCount()));
        A11y.setValue(ordersAmount, Fmt.amount(sys.orderAmount()) + " USD · " + sys.creditBlockedOrderCount()
                + " credit blocked");
        String name = shell.session() == null ? "" : shell.session().user.fullName;
        A11y.setValue(systemMessage, "Welcome, " + name + ". Today is " + Fmt.date(LocalDate.now())
                + ". Enter a transaction code in the command field (e.g. FB60, VA01) or use the menu.");
    }

    private JPanel splash() {

        JLabel company = new JLabel("Meridian Group Inc.");
        company.setFont(ErpTheme.titleFont().deriveFont(Font.BOLD, 28f));
        company.setForeground(ErpTheme.TITLE_TEXT);
        JLabel sub = new JLabel("Production System PRD · Client 100 · Company Code 1000 New York");
        sub.setForeground(new Color(0x4A5F75));

        JPanel tiles = new JPanel(new GridLayout(1, 4, 12, 0));
        tiles.setOpaque(false);
        tiles.add(transactionTile("FB60", "Enter Vendor Invoice"));
        tiles.add(transactionTile("VA01", "Create Sales Order"));
        tiles.add(figureTile("Vendor invoices posted", docs, docsAmount));
        tiles.add(figureTile("Sales orders", orders, ordersAmount));
        JPanel messages = Widgets.group("System messages", systemMessage);
        JPanel splash = stack(company, sub, (JComponent) Box.createVerticalStrut(14), left(tiles), messages);
        splash.setBorder(BorderFactory.createEmptyBorder(30, 36, 30, 36));
        return splash;
    }

    private JComponent transactionTile(String tcode, String text) {
        JButton tile = A11y.name(new JButton("<html><b style='font-size:14px'>" + tcode + "</b><br>" + text + "</html>"),
                tcode + " - " + text);
        tile.setHorizontalAlignment(JButton.LEFT);
        tile.setBackground(Color.WHITE);
        tile.addActionListener(e -> shell.open(tcode));
        return tile;
    }

    private static JComponent figureTile(String caption, JLabel value, JLabel amount) {
        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBackground(Color.WHITE);
        tile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ErpTheme.BORDER), BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        JLabel title = new JLabel(caption);
        value.setFont(value.getFont().deriveFont(Font.BOLD, 24f));
        value.setForeground(ErpTheme.TITLE_TEXT);
        tile.add(title);
        tile.add(value);
        tile.add(amount);
        return tile;
    }

    private static JComponent left(JComponent c) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.add(c);
        return row;
    }

    private void expandDefaults() {
        // Everything expanded, so every transaction is one double-click away.
        for (int row = 0; row < tree.getRowCount(); row++) {
            tree.expandRow(row);
        }
    }

    private static DefaultMutableTreeNode buildMenu() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("User Menu");
        DefaultMutableTreeNode favorites = folder(root, "Favorites");
        entry(favorites, "FB60", "Enter Vendor Invoice");
        entry(favorites, "VA01", "Create Sales Order");
        entry(favorites, "FB03", "Display Document");
        entry(favorites, "VA03", "Display Sales Order");
        DefaultMutableTreeNode menu = folder(root, "Meridian Menu");
        DefaultMutableTreeNode payables = folder(folder(folder(menu, "Accounting"), "Financial Accounting"),
                "Accounts Payable");
        entry(folder(payables, "Document Entry"), "FB60", "Invoice");
        entry(folder(payables, "Document"), "FB03", "Display");
        DefaultMutableTreeNode order = folder(folder(folder(folder(menu, "Logistics"), "Sales and Distribution"),
                "Sales"), "Order");
        entry(order, "VA01", "Create");
        entry(order, "VA03", "Display");
        entry(folder(menu, "Tools"), "ZDEMO_ADMIN", "Demo Maintenance");
        return root;
    }

    private static DefaultMutableTreeNode folder(DefaultMutableTreeNode parent, String name) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(name);
        parent.add(node);
        return node;
    }

    private static void entry(DefaultMutableTreeNode parent, String tcode, String text) {
        parent.add(new DefaultMutableTreeNode(new Entry(tcode, text), false));
    }
}
