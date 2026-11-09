package meridian;

import meridian.data.ErpSystem;
import meridian.ui.ErpTheme;
import meridian.ui.MainFrame;

import javax.swing.SwingUtilities;
import java.util.Locale;

/**
 * Entry point of Meridian ERP desktop.
 *
 * <p>Arguments:
 * <ul>
 *   <li>{@code --hard} start with hard mode on</li>
 *   <li>{@code --suffix=NAME} window title becomes "Meridian ERP - NAME"</li>
 * </ul>
 */
public final class MeridianApp {

    private MeridianApp() {
    }

    public static void main(String[] args) {
        // Fixed locale so numbers and dates always render the same way for automation.
        Locale.setDefault(Locale.US);

        boolean hard = false;
        String suffix = null;
        for (String arg : args) {
            if ("--hard".equals(arg)) {
                hard = true;
            } else if (arg.startsWith("--suffix=")) {
                suffix = arg.substring("--suffix=".length());
            } else {
                System.err.println("Unknown argument: " + arg);
                System.err.println("Usage: java -jar MeridianErp.jar [--hard] [--suffix=NAME]");
                System.exit(2);
            }
        }

        final boolean hardMode = hard;
        final String titleSuffix = suffix;
        SwingUtilities.invokeLater(() -> {
            ErpTheme.install();
            ErpSystem system = new ErpSystem();
            system.setHardMode(hardMode);
            new MainFrame(system, titleSuffix).setVisible(true);
        });
    }
}
