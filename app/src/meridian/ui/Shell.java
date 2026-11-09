package meridian.ui;

import meridian.data.ErpSystem;

import javax.swing.JButton;
import java.awt.Window;

/** What a screen can ask of the main window. */
interface Shell {
    ErpSystem system();

    Session session();

    StatusBar status();

    Window window();

    void setScreenTitle(String title);

    /** Replaces the application toolbar (the row of screen-specific buttons under the title). */
    void setAppButtons(JButton... buttons);

    /** Starts a transaction, like typing it in the command field. */
    void open(String tcode);

    void goHome();

    /**
     * Runs a screen action like a server round trip: clears the status bar, and in hard mode
     * blocks the window for 0.5-3 s first. Actions arriving while blocked are dropped.
     */
    void run(Runnable action);

    /** Hard mode: sometimes shows a blocking information popup (FB60, VA01). */
    void maybeInfoPopup(String tcode);
}
