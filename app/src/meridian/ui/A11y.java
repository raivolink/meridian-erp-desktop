package meridian.ui;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.event.ActionListener;

/** Helpers that keep the app easy to drive through the Java Access Bridge. */
public final class A11y {

    private A11y() {
    }

    /** Sets a stable component name and accessible name, used as the JAB locator "name". */
    public static <T extends JComponent> T name(T component, String name) {
        component.setName(name);
        component.getAccessibleContext().setAccessibleName(name);
        return component;
    }

    /**
     * Updates a value label. The accessible name stays fixed (the locator), while the
     * value is exposed both as text and as the accessible description.
     */
    public static void setValue(JLabel label, String value) {
        label.setText(value);
        label.getAccessibleContext().setAccessibleDescription(value);
    }

    /**
     * Runs the action after the current event. Use for buttons that open modal dialogs:
     * a JAB "click" waits for the action to return, so opening the dialog inline would
     * block the automation client until the dialog is closed.
     */
    public static ActionListener deferred(Runnable action) {
        return e -> SwingUtilities.invokeLater(action);
    }
}
