package meridian.ui;

import meridian.data.ErpSystem;
import meridian.data.Fmt;
import meridian.data.Message;
import meridian.data.User;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Main window "Meridian ERP": menu bar, system toolbar with command field, screen title,
 * application toolbar, the current screen, and the status bar.
 */
public final class MainFrame extends JFrame implements Shell {
    public static final String TITLE = "Meridian ERP";
    private static final String LOGON = "LOGON";
    private static final String HOME = "SESSION_MANAGER";
    private static final List<String> LOG_OFF_COMMANDS = Arrays.asList("EX", "END", "NEND", "NEX");
    private static final List<String> HOME_COMMANDS = Arrays.asList("SESSION_MANAGER", "SMEN", "S000");
    private static final double POPUP_CHANCE = 0.4;

    private final ErpSystem sys;
    private final StatusBar status = new StatusBar();
    private final JTextField commandField = Widgets.field("Command Field", "Command field", 16);
    private final JLabel screenTitle = Widgets.valueLabel("Screen Title", "");
    private final JToolBar appToolbar = A11y.name(new JToolBar(), "Application Toolbar");
    private final CardLayout cards = new CardLayout();
    private final JPanel screens = new JPanel(cards);
    private final Map<String, Screen> transactions = new LinkedHashMap<>();
    private final LogonScreen logon;
    private final Screen home;
    private final List<JComponent> sessionControls;
    private final Random random = new Random();
    private Session session;
    private Screen current;
    private boolean busy;

    public MainFrame(ErpSystem sys, String titleSuffix) {
        super(titleSuffix == null || titleSuffix.isEmpty() ? TITLE : TITLE + " - " + titleSuffix);
        this.sys = sys;

        logon = new LogonScreen(this, this::logOn);
        home = new HomeScreen(this);
        for (Screen s : new Screen[]{home, new Fb60Screen(this), new Fb03Screen(this), new Va01Screen(this),
                new Va03Screen(this), new AdminScreen(this)}) {
            transactions.put(s.tcode(), s);
            screens.add(s, s.tcode());
        }
        screens.add(logon, LOGON);

        JButton save = toolButton("Save", Icons.SAVE, "Save (Ctrl+S)", e -> save());
        JButton back = toolButton("Back", Icons.BACK, "Back (F3)", e -> back());
        JButton exit = toolButton("Exit", Icons.EXIT, "Exit (Shift+F3)", e -> back());
        JButton cancel = toolButton("Cancel", Icons.CANCEL, "Cancel (F12)", e -> back());
        JButton logOff = toolButton("Log Off", Icons.LOGOFF, "Log off", e -> logOff());
        JMenu favorites = buildFavoritesMenu();
        JMenu system = buildSystemMenu();
        sessionControls = Arrays.asList(commandField, save, back, exit, cancel, logOff, favorites, system);

        JToolBar toolbar = A11y.name(new JToolBar(), "System Toolbar");
        toolbar.setFloatable(false);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ErpTheme.BORDER),
                BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        commandField.setMaximumSize(commandField.getPreferredSize());
        commandField.setToolTipText("Command field: transaction code, e.g. FB60 or /nVA01");
        commandField.addActionListener(e -> enter());
        toolbar.add(toolButton("Enter", Icons.ENTER, "Enter", e -> enter()));
        toolbar.add(commandField);
        toolbar.addSeparator();
        toolbar.add(save);
        toolbar.add(back);
        toolbar.add(exit);
        toolbar.add(cancel);
        toolbar.addSeparator();
        toolbar.add(logOff);
        toolbar.add(Box.createHorizontalGlue());

        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(ErpTheme.TITLE_BAR);
        titleBar.setBorder(BorderFactory.createEmptyBorder(6, 12, 4, 12));
        screenTitle.setFont(ErpTheme.titleFont());
        screenTitle.setForeground(ErpTheme.TITLE_TEXT);
        titleBar.add(screenTitle, BorderLayout.WEST);

        appToolbar.setFloatable(false);
        appToolbar.setBackground(ErpTheme.TITLE_BAR);
        appToolbar.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));
        appToolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ErpTheme.BORDER),
                BorderFactory.createEmptyBorder(0, 8, 2, 8)));
        appToolbar.setPreferredSize(new Dimension(100, 34));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(toolbar);
        header.add(titleBar);
        header.add(appToolbar);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(favorites);
        menuBar.add(system);
        menuBar.add(buildHelpMenu());
        setJMenuBar(menuBar);

        add(header, BorderLayout.NORTH);
        add(screens, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        JPanel glass = new JPanel();
        glass.setOpaque(false);
        glass.addMouseListener(new MouseAdapter() {
        });
        glass.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        setGlassPane(glass);

        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "back", this::back);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK), "exit", this::back);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), "cancel", this::back);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "save", this::save);

        setIconImages(AppIcon.windowImages());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1240, 860);
        setLocationRelativeTo(null);
        showLogon();
    }

    // ---- Shell ----

    @Override
    public ErpSystem system() {
        return sys;
    }

    @Override
    public Session session() {
        return session;
    }

    @Override
    public StatusBar status() {
        return status;
    }

    @Override
    public Window window() {
        return this;
    }

    @Override
    public void setScreenTitle(String title) {
        A11y.setValue(screenTitle, title);
    }

    @Override
    public void setAppButtons(JButton... buttons) {
        appToolbar.removeAll();
        for (JButton button : buttons) {
            appToolbar.add(button);
        }
        appToolbar.revalidate();
        appToolbar.repaint();
    }

    @Override
    public void open(String command) {
        if (session == null) {
            return;
        }
        String code = Fmt.up(command).replaceFirst("^/[NO*]?", "");
        if (LOG_OFF_COMMANDS.contains(code)) {
            logOff();
            return;
        }
        if (code.isEmpty() || HOME_COMMANDS.contains(code)) {
            goHome();
            return;
        }
        Screen screen = transactions.get(code);
        if (screen == null) {
            goHome();
            status.show(Message.error("Transaction " + code + " does not exist"));
            return;
        }
        if ("ZDEMO_ADMIN".equals(code) && !session.user.isAdmin()) {
            goHome();
            status.show(Message.error("You are not authorized to use transaction ZDEMO_ADMIN"));
            return;
        }
        run(() -> show(screen));
    }

    @Override
    public void goHome() {
        if (session != null) {
            status.clear();
            show(home);
        }
    }

    @Override
    public void run(Runnable action) {
        if (busy) {
            return;
        }
        status.clear();
        if (!sys.isHardMode()) {
            action.run();
            return;
        }
        busy = true;
        getGlassPane().setVisible(true);
        Timer timer = new Timer(500 + random.nextInt(2500), e -> {
            busy = false;
            getGlassPane().setVisible(false);
            action.run();
        });
        timer.setRepeats(false);
        timer.start();
    }

    @Override
    public void maybeInfoPopup(String tcode) {
        if (!sys.isHardMode() || random.nextDouble() >= POPUP_CHANCE) {
            return;
        }
        List<String> messages = "VA01".equals(tcode)
                ? Arrays.asList(
                "Pricing conditions were updated on 10/01. Check net prices before saving.",
                "Batch jobs are running in the background. Expect slower response times.",
                "Credit management: credit limit checks are active for all customers.")
                : Arrays.asList(
                "Posting period 0" + String.format("%02d", LocalDate.now().getMonthValue())
                        + " will be closed at month end. Post outstanding invoices in time.",
                "Vendor master data was changed recently. Please verify bank details before posting.",
                "Batch jobs are running in the background. Expect slower response times.");
        String text = messages.get(random.nextInt(messages.size()));
        SwingUtilities.invokeLater(() -> MessageDialog.show(this, "Information", Icons.MESSAGE_INFO, text, "Continue"));
    }

    // ---- Session ----

    private void showLogon() {
        session = null;
        setSessionControlsEnabled(false);
        status.setUser("");
        current = logon;
        cards.show(screens, LOGON);
        status.setTransaction("");
        logon.start();
    }

    private void logOn(String username) {
        User user = sys.logOn(username);
        session = new Session(user);
        status.setUser(user.username);
        setSessionControlsEnabled(true);
        goHome();
        if (sys.isHardMode()) {
            SwingUtilities.invokeLater(() -> {
                if (MultiLogonDialog.ask(this, user.username)) {
                    logOff();
                }
            });
        }
    }

    private void logOff() {
        status.clear();
        showLogon();
    }

    private void show(Screen screen) {
        current = screen;
        cards.show(screens, screen.tcode());
        status.setTransaction(screen.tcode());
        screen.start();
    }

    // ---- Toolbar actions ----

    /** Enter: runs the command field if it has a command, otherwise the screen's Enter (check) action. */
    private void enter() {
        if (session == null) {
            logon.enter();
            return;
        }
        String command = commandField.getText().trim();
        if (!command.isEmpty()) {
            commandField.setText("");
            open(command);
            return;
        }
        current.enter();
    }

    private void save() {
        if (session != null) {
            current.save();
        }
    }

    /** Back, Exit and Cancel all return to Easy Access, as in the web app. */
    private void back() {
        if (session != null) {
            goHome();
        }
    }

    private void setSessionControlsEnabled(boolean enabled) {
        for (JComponent c : sessionControls) {
            c.setEnabled(enabled);
        }
    }

    // ---- Menus ----

    private JMenu buildFavoritesMenu() {
        JMenu menu = menu("Favorites", KeyEvent.VK_A);
        menu.add(menuItem("FB60 - Enter Vendor Invoice", e -> open("FB60")));
        menu.add(menuItem("VA01 - Create Sales Order", e -> open("VA01")));
        menu.add(menuItem("FB03 - Display Document", e -> open("FB03")));
        menu.add(menuItem("VA03 - Display Sales Order", e -> open("VA03")));
        return menu;
    }

    private JMenu buildSystemMenu() {
        JMenu menu = menu("System", KeyEvent.VK_Y);
        menu.add(menuItem("User Menu", e -> goHome()));
        menu.add(menuItem("Demo Maintenance", e -> open("ZDEMO_ADMIN")));
        menu.addSeparator();
        menu.add(menuItem("Log Off", e -> logOff()));
        return menu;
    }

    private JMenu buildHelpMenu() {
        JMenu menu = menu("Help", KeyEvent.VK_H);
        menu.add(menuItem("About", A11y.deferred(() -> MessageDialog.show(this, "About Meridian ERP",
                Icons.MESSAGE_INFO, "Meridian ERP desktop – Java Swing version of the Meridian ERP demo, "
                        + "a target for Java Access Bridge automation.", "OK"))));
        return menu;
    }

    private static JMenu menu(String name, int mnemonic) {
        JMenu menu = A11y.name(new JMenu(name), name);
        menu.setMnemonic(mnemonic);
        return menu;
    }

    private static JMenuItem menuItem(String name, ActionListener listener) {
        JMenuItem item = A11y.name(new JMenuItem(name), name);
        // A JAB click runs the action without closing the popup, so close it explicitly.
        item.addActionListener(e -> MenuSelectionManager.defaultManager().clearSelectedPath());
        item.addActionListener(listener);
        return item;
    }

    private static JButton toolButton(String name, Icon icon, String tooltip, ActionListener listener) {
        JButton button = A11y.name(new JButton(icon), name);
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.addActionListener(listener);
        return button;
    }

    private void bindKey(KeyStroke key, String name, Runnable handler) {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
        getRootPane().getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handler.run();
            }
        });
    }
}
