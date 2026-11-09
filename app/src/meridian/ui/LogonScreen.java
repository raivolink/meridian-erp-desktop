package meridian.ui;

import meridian.data.FieldError;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.function.Consumer;

/** Logon screen: client, user, password, language. */
final class LogonScreen extends Screen {
    private final JTextField client = reg(Widgets.field("RSYST-MANDT", "Client", 4));
    private final JTextField user = reg(Widgets.field("RSYST-BNAME", "User", 14));
    private final JPasswordField password = reg(Widgets.password("RSYST-BCODE", "Password", 14));
    private final JTextField language = reg(Widgets.field("RSYST-LANGU", "Logon Language", 3));
    private final Consumer<String> onLogon;

    /** onLogon receives the user name after a successful check. */
    LogonScreen(Shell shell, Consumer<String> onLogon) {
        super(shell);
        this.onLogon = onLogon;

        FormPanel form = new FormPanel();
        form.addField("Client", client);
        form.addField("User", user);
        form.addField("Password", password);
        form.addField("Logon Language", language);
        JButton logOn = Widgets.button("Log On", "Log On", Icons.ENTER, e -> enter());
        form.addField("", logOn);
        client.addActionListener(e -> enter());
        user.addActionListener(e -> enter());
        password.addActionListener(e -> enter());

        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(BorderFactory.createEmptyBorder(30, 24, 24, 24));
        left.add(Widgets.group("Logon", form), BorderLayout.NORTH);

        add(left, BorderLayout.WEST);
        add(hero(), BorderLayout.CENTER);
    }

    @Override
    String tcode() {
        return "";
    }

    @Override
    void start() {
        clearError();
        client.setText("100");
        user.setText("");
        password.setText("");
        language.setText("EN");
        shell.setScreenTitle("Meridian ERP Logon");
        shell.setAppButtons();
        user.requestFocusInWindow();
    }

    @Override
    void enter() {
        clearError();
        String pw = new String(password.getPassword());
        FieldError error = sys.checkLogon(client.getText(), user.getText(), pw);
        if (error != null) {
            showError(error);
            return;
        }
        onLogon.accept(user.getText());
    }

    private static JPanel hero() {
        JPanel hero = new JPanel();
        hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
        hero.setBackground(new Color(0x2B5170));
        hero.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JLabel brand = new JLabel("<html>MERIDIAN GROUP<br><span style='font-size:11px;font-weight:normal;"
                + "color:#C8DDF2'>ENTERPRISE RESOURCE PLANNING</span></html>", AppIcon.logo(52), JLabel.LEADING);
        brand.setFont(ErpTheme.titleFont().deriveFont(Font.BOLD, 30f));
        brand.setForeground(Color.WHITE);
        brand.setIconTextGap(14);
        hero.add(brand);
        hero.add(Box.createVerticalStrut(40));

        String month = LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.US);
        JLabel info = new JLabel("<html><div style='width:380px'><h2>Information</h2>"
                + "<p><b>Welcome to Meridian ERP &ndash; Production System PRD, client 100.</b></p><br>"
                + "<p>Scheduled maintenance: Saturday 22:00&ndash;02:00 EST. Please log off before the maintenance window.</p><br>"
                + "<p>Period-end close for " + month + " starts on business day +3. Post all vendor invoices by then.</p><br>"
                + "<p>This is a demonstration system with fictitious data. Authorized users only.</p></div></html>");
        info.setForeground(Color.WHITE);
        hero.add(info);
        return hero;
    }
}
