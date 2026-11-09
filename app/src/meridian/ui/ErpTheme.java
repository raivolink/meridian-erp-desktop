package meridian.ui;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.Color;
import java.awt.Font;

/** Flat blue-grey look in the style of classic ERP desktop clients. Install before creating components. */
public final class ErpTheme {
    static final Color BACKGROUND = new Color(0xEFF4F9);
    static final Color TOOLBAR = new Color(0xDCE4EC);
    static final Color TITLE_BAR = new Color(0xF7F9FB);
    static final Color TITLE_TEXT = new Color(0x1F3A57);
    static final Color BORDER = new Color(0xB4C2CF);
    static final Color FIELD_BORDER = new Color(0x8A9BAB);
    static final Color SELECTION = new Color(0xBFD9F2);

    private static final String FONT = "Segoe UI";

    private ErpTheme() {
    }

    public static void install() {
        MetalLookAndFeel.setCurrentTheme(new Theme());
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (UnsupportedLookAndFeelException e) {
            return;
        }
        UIManager.put("Table.gridColor", new ColorUIResource(0xD5DDE5));
        UIManager.put("Table.selectionBackground", new ColorUIResource(SELECTION));
        UIManager.put("Table.selectionForeground", new ColorUIResource(Color.BLACK));
        UIManager.put("TableHeader.background", new ColorUIResource(TOOLBAR));
        UIManager.put("Tree.selectionBackground", new ColorUIResource(SELECTION));
        UIManager.put("Tree.selectionForeground", new ColorUIResource(Color.BLACK));
        UIManager.put("TitledBorder.titleColor", new ColorUIResource(TITLE_TEXT));
        UIManager.put("Button.background", new ColorUIResource(0xF7F9FB));
        UIManager.put("ToolBar.background", new ColorUIResource(TOOLBAR));
        UIManager.put("MenuBar.background", new ColorUIResource(0xF2F5F8));
    }

    static Font titleFont() {
        return new Font(FONT, Font.PLAIN, 17);
    }

    private static final class Theme extends DefaultMetalTheme {
        private final FontUIResource regular = new FontUIResource(FONT, Font.PLAIN, 12);
        private final FontUIResource small = new FontUIResource(FONT, Font.PLAIN, 11);

        @Override
        public String getName() {
            return "Demo ERP";
        }

        @Override
        protected ColorUIResource getPrimary1() {
            return new ColorUIResource(0x2E5C8A);
        }

        @Override
        protected ColorUIResource getPrimary2() {
            return new ColorUIResource(0x7AA7D6);
        }

        @Override
        protected ColorUIResource getPrimary3() {
            return new ColorUIResource(SELECTION);
        }

        @Override
        protected ColorUIResource getSecondary1() {
            return new ColorUIResource(FIELD_BORDER);
        }

        @Override
        protected ColorUIResource getSecondary2() {
            return new ColorUIResource(0xC3CFDA);
        }

        @Override
        protected ColorUIResource getSecondary3() {
            return new ColorUIResource(BACKGROUND);
        }

        @Override
        public FontUIResource getControlTextFont() {
            return regular;
        }

        @Override
        public FontUIResource getSystemTextFont() {
            return regular;
        }

        @Override
        public FontUIResource getUserTextFont() {
            return regular;
        }

        @Override
        public FontUIResource getMenuTextFont() {
            return regular;
        }

        @Override
        public FontUIResource getWindowTitleFont() {
            return regular;
        }

        @Override
        public FontUIResource getSubTextFont() {
            return small;
        }
    }
}
