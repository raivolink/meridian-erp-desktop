package meridian.ui;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * The Meridian logo from the web app (public/static/favicon.svg): a rounded square with an "M",
 * painted in code at any size. Used for the window / taskbar icon and on the logon screen.
 */
final class AppIcon {
    private static final Color BLUE = new Color(0x2B5170);
    private static final int[] WINDOW_SIZES = {16, 20, 24, 32, 40, 48, 64, 128, 256};

    private AppIcon() {
    }

    /** Window icons in all common sizes; Windows picks the best one for title bar and taskbar. */
    static List<Image> windowImages() {
        List<Image> images = new ArrayList<>();
        for (int size : WINDOW_SIZES) {
            images.add(paint(size, BLUE, Color.WHITE));
        }
        return images;
    }

    /** White square with a blue "M", as next to the brand name on the web logon page. */
    static Icon logo(int size) {
        return new ImageIcon(paint(size, Color.WHITE, BLUE));
    }

    private static BufferedImage paint(int size, Color background, Color letter) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        // The SVG is drawn on a 32 x 32 grid.
        g.transform(AffineTransform.getScaleInstance(size / 32.0, size / 32.0));
        g.setColor(background);
        g.fill(new RoundRectangle2D.Double(0, 0, 32, 32, 8, 8));
        g.setColor(letter);
        g.fill(letterM());
        g.dispose();
        return image;
    }

    /** SVG path "M6 24V8h4l6 9 6-9h4v16h-4V15l-6 8-6-8v9z". */
    private static Path2D letterM() {
        Path2D m = new Path2D.Double();
        m.moveTo(6, 24);
        m.lineTo(6, 8);
        m.lineTo(10, 8);
        m.lineTo(16, 17);
        m.lineTo(22, 8);
        m.lineTo(26, 8);
        m.lineTo(26, 24);
        m.lineTo(22, 24);
        m.lineTo(22, 15);
        m.lineTo(16, 23);
        m.lineTo(10, 15);
        m.lineTo(10, 24);
        m.closePath();
        return m;
    }
}
