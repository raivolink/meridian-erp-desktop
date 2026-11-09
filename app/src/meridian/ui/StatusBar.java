package meridian.ui;

import meridian.data.Message;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * Status bar like the web app's: message text ("Status Bar", the exact message), message type
 * ("Message Type": icon, accessible description S/W/E/I) and system information on the right.
 */
public class StatusBar extends JPanel {
    private final JLabel type = Widgets.valueLabel("Message Type", "");
    private final JLabel message = Widgets.valueLabel("Status Bar", "");
    private final JLabel user = Widgets.valueLabel("User", "");
    private final JLabel transaction = Widgets.valueLabel("Current Transaction", "");

    public StatusBar() {
        super(new BorderLayout());
        setBackground(ErpTheme.TOOLBAR);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ErpTheme.BORDER));
        type.setText(null);
        type.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 4));
        message.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 8));

        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.add(type, BorderLayout.WEST);
        left.add(message, BorderLayout.CENTER);

        JPanel info = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
        info.setOpaque(false);
        info.add(segment(Widgets.valueLabel("System Info", "PRD (1) 100"), 90));
        info.add(segment(user, 80));
        info.add(segment(transaction, 130));
        info.add(segment(Widgets.valueLabel("Server", "meridian-app01"), 110));
        info.add(segment(Widgets.valueLabel("Input Mode", "INS"), 40));

        add(left, BorderLayout.CENTER);
        add(info, BorderLayout.EAST);
    }

    public void show(Message m) {
        Icon icon;
        switch (m.type) {
            case 'S':
                icon = Icons.MESSAGE_SUCCESS;
                break;
            case 'W':
                icon = Icons.MESSAGE_WARNING;
                break;
            case 'E':
                icon = Icons.MESSAGE_ERROR;
                break;
            default:
                icon = Icons.MESSAGE_INFO;
        }
        type.setIcon(icon);
        type.getAccessibleContext().setAccessibleDescription(String.valueOf(m.type));
        A11y.setValue(message, m.text);
    }

    public void clear() {
        type.setIcon(null);
        type.getAccessibleContext().setAccessibleDescription("");
        A11y.setValue(message, "");
    }

    void setUser(String username) {
        A11y.setValue(user, username);
    }

    void setTransaction(String tcode) {
        A11y.setValue(transaction, tcode);
    }

    private static JLabel segment(JLabel label, int width) {
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLoweredBevelBorder(),
                BorderFactory.createEmptyBorder(1, 6, 1, 6)));
        label.setPreferredSize(new Dimension(width, 20));
        return label;
    }
}
