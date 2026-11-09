package meridian.ui;

import meridian.data.ErpSystem;
import meridian.data.Fmt;
import meridian.data.Message;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;

/** "Enter Company Code" popup shown on the first FB60 call of a session. */
final class CompanyCodeDialog extends JDialog {
    private final Shell shell;
    private final JTextField companyCode;
    private boolean confirmed;

    private CompanyCodeDialog(Shell shell) {
        super(shell.window(), "Enter Company Code", ModalityType.APPLICATION_MODAL);
        this.shell = shell;
        String current = shell.session().companyCode;
        companyCode = Widgets.field("BKPF-BUKRS", "Company Code", 5);
        companyCode.setText(current == null ? "" : current);
        companyCode.addActionListener(e -> confirm());

        JLabel text = new JLabel("Specify the company code for document entry.", Icons.QUESTION, JLabel.LEADING);
        text.setIconTextGap(10);
        FormPanel form = new FormPanel();
        form.addLine(text);
        form.addField("Company Code", companyCode);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = Widgets.button("Continue", "Continue", Icons.ENTER, e -> confirm());
        buttons.add(ok);
        buttons.add(Widgets.button("Cancel", "Cancel", Icons.CANCEL, e -> dispose()));
        getRootPane().setDefaultButton(ok);

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 6, 10));
        content.add(form, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        add(content);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(getOwner());
    }

    /** Returns true when a valid company code was entered (stored in the session). */
    static boolean ask(Shell shell) {
        CompanyCodeDialog dialog = new CompanyCodeDialog(shell);
        dialog.setVisible(true);
        return dialog.confirmed;
    }

    private void confirm() {
        String cc = Fmt.up(companyCode.getText());
        if (!ErpSystem.COMPANY_CODE.equals(cc)) {
            shell.status().show(Message.error(cc.isEmpty() ? "Enter a company code" : "Company code " + cc + " does not exist"));
            companyCode.setBorder(BorderFactory.createLineBorder(new Color(0xC83737), 2));
            companyCode.requestFocusInWindow();
            return;
        }
        shell.status().clear();
        shell.session().companyCode = cc;
        confirmed = true;
        dispose();
    }
}
