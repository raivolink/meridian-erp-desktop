package meridian.ui;

import meridian.data.ErpSystem;
import meridian.data.FieldError;
import meridian.data.Message;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;

/** Base class of a transaction screen. */
abstract class Screen extends JPanel {
    private static final Border ERROR_BORDER = BorderFactory.createLineBorder(new Color(0xC83737), 2);

    protected final Shell shell;
    protected final ErpSystem sys;
    private final Map<String, JComponent> fields = new HashMap<>();
    private JComponent errorField;
    private Border errorFieldBorder;

    Screen(Shell shell) {
        super(new BorderLayout());
        this.shell = shell;
        this.sys = shell.system();
    }

    /** Transaction code shown in the status bar. */
    abstract String tcode();

    /** Called every time the transaction is started; resets the screen like a fresh page load. */
    abstract void start();

    /** Enter key / Enter button. */
    void enter() {
    }

    /** Ctrl+S / Save button. */
    void save() {
        enter();
    }

    /** Registers an input under its technical name so errors can point at it. */
    protected <T extends JComponent> T reg(T field) {
        fields.put(field.getName(), field);
        return field;
    }

    protected JComponent field(String name) {
        return fields.get(name);
    }

    /** Shows the error in the status bar, marks the field red, selects its tab and focuses it. */
    protected void showError(FieldError error) {
        shell.status().show(Message.error(error.text));
        clearError();
        JComponent field = fields.get(error.field);
        if (field == null) {
            return;
        }
        selectTabs(field);
        errorField = field;
        errorFieldBorder = field.getBorder();
        field.setBorder(ERROR_BORDER);
        field.requestFocusInWindow();
    }

    protected void clearError() {
        if (errorField != null) {
            errorField.setBorder(errorFieldBorder);
            errorField = null;
        }
    }

    private static void selectTabs(Component component) {
        for (Component c = component; c.getParent() != null; c = c.getParent()) {
            if (c.getParent() instanceof JTabbedPane) {
                ((JTabbedPane) c.getParent()).setSelectedComponent(c);
            }
        }
    }

    /** Sections stacked top to bottom at their natural height, full width. */
    protected static JPanel stack(JComponent... parts) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        for (int i = 0; i < parts.length; i++) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = i;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(0, 0, 6, 0);
            panel.add(parts[i], c);
        }
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = parts.length;
        filler.weighty = 1;
        panel.add(Box.createVerticalGlue(), filler);
        return panel;
    }

    /** Content area with padding. */
    protected static JPanel padded(Component content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }
}
