package meridian.ui;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

/**
 * Factories for named components. Input fields get the SAP technical field name as their
 * accessible name (e.g. INVFO-ACCNT) and the visible label as their accessible description.
 */
final class Widgets {
    static final Color READ_ONLY = new Color(0xE8EEF3);

    private Widgets() {
    }

    /** A combo box entry: value is what the screen reads, text is what the user sees. */
    static final class Option {
        final String value;
        final String text;

        Option(String value, String text) {
            this.value = value;
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    static JTextField field(String name, String label, int columns) {
        JTextField field = A11y.name(new JTextField(columns), name);
        field.getAccessibleContext().setAccessibleDescription(label);
        return field;
    }

    static JTextField readOnly(String name, String label, int columns) {
        JTextField field = field(name, label, columns);
        field.setEditable(false);
        field.setBackground(READ_ONLY);
        return field;
    }

    static JPasswordField password(String name, String label, int columns) {
        JPasswordField field = A11y.name(new JPasswordField(columns), name);
        field.getAccessibleContext().setAccessibleDescription(label);
        return field;
    }

    static JComboBox<Option> combo(String name, String label, Option... options) {
        JComboBox<Option> combo = A11y.name(new JComboBox<>(options), name);
        combo.getAccessibleContext().setAccessibleDescription(label);
        return combo;
    }

    static Option option(String value, String text) {
        return new Option(value, text);
    }

    static String value(JComboBox<Option> combo) {
        Option selected = (Option) combo.getSelectedItem();
        return selected == null ? "" : selected.value;
    }

    static void select(JComboBox<Option> combo, String value) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).value.equals(value)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    static JCheckBox checkBox(String name, String text) {
        JCheckBox box = A11y.name(new JCheckBox(text), name);
        box.setOpaque(false);
        return box;
    }

    /** Application toolbar / dialog button with an icon. */
    static JButton button(String name, String text, Icon icon, ActionListener listener) {
        JButton button = A11y.name(new JButton(text, icon), name);
        button.addActionListener(listener);
        return button;
    }

    static JLabel valueLabel(String name, String value) {
        JLabel label = A11y.name(new JLabel(), name);
        A11y.setValue(label, value);
        return label;
    }

    static JTable table(String name, TableModel model) {
        JTable table = A11y.name(new JTable(model), name);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowHeight(20);
        return table;
    }

    /** Titled group box, like SAP GUI frames. */
    static JPanel group(String title, Component content) {
        JPanel panel = new JPanel(new java.awt.BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(ErpTheme.BORDER), title),
                BorderFactory.createEmptyBorder(2, 4, 4, 4)));
        panel.add(content, java.awt.BorderLayout.CENTER);
        return panel;
    }

    /** F4 opens the value help for the field. */
    static void valueHelp(JComponent field, Runnable open) {
        field.setToolTipText("F4 = possible entries");
        field.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), "valueHelp");
        field.getActionMap().put("valueHelp", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                open.run();
            }
        });
    }

    static void onChange(JTextComponent field, Runnable listener) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                listener.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                listener.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                listener.run();
            }
        });
    }
}
