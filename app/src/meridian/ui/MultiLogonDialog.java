package meridian.ui;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalTime;
import java.util.Random;

/** Hard mode: "License Information for Multiple Logons" after logon. */
final class MultiLogonDialog extends JDialog {
    static final String CONTINUE_END = "Continue with this logon and end any other logons in system";
    static final String CONTINUE = "Continue with this logon, without ending any other logons in system";
    static final String TERMINATE = "Terminate this logon";

    private final JRadioButton terminate;
    private boolean terminateChosen;

    private MultiLogonDialog(Shell shell, String username) {
        super(shell.window(), "License Information for Multiple Logons", ModalityType.APPLICATION_MODAL);
        int terminal = new Random().nextInt(90) + 10;
        String since = LocalTime.now().minusHours(2).withSecond(0).withNano(0).toString();
        String message = "User " + username + " is already logged on in client 100 (terminal meridian-ws" + terminal
                + ", since " + since + ").\n\nNote that multiple logons to the production system using the same"
                + " user ID are not part of the license agreement.";
        JLabel text = Widgets.valueLabel("Dialog Message", message);
        text.setText("<html><div style='width:420px'>" + message.replace("\n", "<br>") + "</div></html>");
        text.setIcon(Icons.MESSAGE_WARNING);
        text.setIconTextGap(12);

        JRadioButton continueEnd = A11y.name(new JRadioButton(CONTINUE_END, true), CONTINUE_END);
        JRadioButton continueOnly = A11y.name(new JRadioButton(CONTINUE), CONTINUE);
        terminate = A11y.name(new JRadioButton(TERMINATE), TERMINATE);
        ButtonGroup group = new ButtonGroup();
        JPanel options = new JPanel();
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
        options.setBorder(BorderFactory.createEmptyBorder(10, 28, 0, 0));
        for (JRadioButton radio : new JRadioButton[]{continueEnd, continueOnly, terminate}) {
            group.add(radio);
            options.add(radio);
        }

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = Widgets.button("Continue", "Continue", Icons.ENTER, e -> {
            terminateChosen = terminate.isSelected();
            dispose();
        });
        buttons.add(ok);
        getRootPane().setDefaultButton(ok);

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(14, 14, 6, 14));
        content.add(text, BorderLayout.NORTH);
        content.add(options, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        add(content);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(getOwner());
    }

    /** Returns true when the user chose to terminate the logon. */
    static boolean ask(Shell shell, String username) {
        MultiLogonDialog dialog = new MultiLogonDialog(shell, username);
        dialog.setVisible(true);
        return dialog.terminateChosen;
    }
}
