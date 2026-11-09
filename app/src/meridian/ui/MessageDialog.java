package meridian.ui;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;

/**
 * Modal popup with an icon, a message ("Dialog Message") and named buttons, like SAP GUI popups.
 * Used instead of JOptionPane so titles and button names are under our control.
 */
final class MessageDialog extends JDialog {
    private int choice = -1;

    private MessageDialog(Window owner, String title, Icon icon, String message, String[] options) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        JLabel label = Widgets.valueLabel("Dialog Message", message);
        // HTML keeps long texts wrapped; the accessible description keeps the plain text.
        label.setText("<html><div style='width:360px'>" + message.replace("\n", "<br>") + "</div></html>");
        label.setIcon(icon);
        label.setIconTextGap(12);
        label.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        for (int i = 0; i < options.length; i++) {
            final int index = i;
            JButton button = Widgets.button(options[i], options[i], null, e -> {
                choice = index;
                dispose();
            });
            buttons.add(button);
            if (i == 0) {
                getRootPane().setDefaultButton(button);
            }
        }

        add(label, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(owner);
    }

    /** Shows the popup and returns the index of the clicked button, or -1 when closed. */
    static int show(Window owner, String title, Icon icon, String message, String... options) {
        MessageDialog dialog = new MessageDialog(owner, title, icon, message, options);
        dialog.setVisible(true);
        return dialog.choice;
    }
}
