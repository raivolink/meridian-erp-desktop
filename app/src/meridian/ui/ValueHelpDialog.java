package meridian.ui;

import meridian.data.ErpSystem;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** F4 value help: search master data and copy the chosen key into the field. */
final class ValueHelpDialog extends JDialog {
    private final ErpSystem sys;
    private final String kind;
    private final JTextField target;
    private final JTextField search = Widgets.field("F4-SEARCH", "Search", 20);
    private final ListTableModel<String[]> model = new ListTableModel<>();
    private final JTable table;

    private ValueHelpDialog(Shell shell, String kind, JTextField target) {
        super(shell.window(), "", ModalityType.APPLICATION_MODAL);
        this.sys = shell.system();
        this.kind = kind;
        this.target = target;
        ErpSystem.ValueHelp help = sys.valueHelp(kind, "");
        setTitle(help.title);
        for (int i = 0; i < help.columns.length; i++) {
            final int col = i;
            model.column(help.columns[i], row -> row[col]);
        }
        table = Widgets.table("f4-results", model);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel label = new JLabel("Search");
        label.setLabelFor(search);
        top.add(label);
        top.add(search);
        Widgets.onChange(search, this::refresh);
        search.addActionListener(e -> choose());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(560, 320));
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    choose();
                }
            }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(Widgets.button("Choose", "Choose", Icons.ENTER, e -> choose()));
        buttons.add(Widgets.button("Cancel", "Cancel", Icons.CANCEL, e -> dispose()));

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        content.add(top, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        add(content);
        refresh();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(getOwner());
    }

    static void open(Shell shell, String kind, JTextField target) {
        new ValueHelpDialog(shell, kind, target).setVisible(true);
    }

    private void refresh() {
        model.setRows(sys.valueHelp(kind, search.getText()).rows);
        if (model.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
        }
    }

    private void choose() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        target.setText(model.getRow(table.convertRowIndexToModel(row))[0]);
        target.requestFocusInWindow();
        dispose();
    }
}
