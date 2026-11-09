package meridian.ui;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;

/** Small icons painted in code, so the app needs no image resources. */
final class Icons {
    private static final Color GREEN = new Color(0x3C9A3C);
    private static final Color RED = new Color(0xC83737);
    private static final Color ORANGE = new Color(0xE3A21A);
    private static final Color BLUE = new Color(0x3D6EA5);
    private static final Color FOLDER_YELLOW = new Color(0xE8B84A);

    static final Icon ENTER = icon(g -> {
        circle(g, GREEN);
        check(g);
    });
    static final Icon SAVE = icon(g -> {
        g.setColor(BLUE);
        g.fillRoundRect(1, 1, 14, 14, 3, 3);
        g.setColor(Color.WHITE);
        g.fillRect(4, 2, 8, 5);
        g.setColor(new Color(0xC9D8EA));
        g.fillRect(5, 10, 6, 4);
    });
    static final Icon BACK = icon(g -> {
        circle(g, GREEN);
        g.setColor(Color.WHITE);
        g.fill(new Polygon(new int[]{3, 8, 8, 12, 12, 8, 8}, new int[]{8, 3, 6, 6, 10, 10, 13}, 7));
    });
    static final Icon EXIT = icon(g -> {
        circle(g, ORANGE);
        g.setColor(Color.WHITE);
        g.fill(new Polygon(new int[]{8, 13, 10, 10, 6, 6, 3}, new int[]{3, 8, 8, 12, 12, 8, 8}, 7));
    });
    static final Icon CANCEL = icon(g -> {
        circle(g, RED);
        cross(g);
    });
    static final Icon MESSAGE_SUCCESS = icon(g -> {
        g.setColor(GREEN);
        g.fillRoundRect(1, 1, 14, 14, 3, 3);
        check(g);
    });
    static final Icon MESSAGE_ERROR = icon(g -> {
        circle(g, RED);
        cross(g);
    });
    static final Icon MESSAGE_WARNING = icon(g -> {
        g.setColor(ORANGE);
        g.fill(new Polygon(new int[]{8, 15, 1}, new int[]{1, 14, 14}, 3));
        g.setColor(Color.BLACK);
        g.fillRect(7, 5, 2, 5);
        g.fillRect(7, 11, 2, 2);
    });
    static final Icon FOLDER = icon(g -> {
        g.setColor(FOLDER_YELLOW);
        g.fillRoundRect(1, 3, 6, 4, 2, 2);
        g.fillRoundRect(1, 5, 14, 9, 2, 2);
        g.setColor(FOLDER_YELLOW.darker());
        g.drawRoundRect(1, 5, 14, 9, 2, 2);
    });
    static final Icon TRANSACTION = icon(g -> {
        g.setColor(new Color(0x5B8BC9));
        g.fillRoundRect(2, 1, 12, 14, 2, 2);
        g.setColor(Color.WHITE);
        g.fillRect(4, 4, 8, 1);
        g.fillRect(4, 7, 8, 1);
        g.fillRect(4, 10, 5, 1);
    });

    static final Icon MESSAGE_INFO = icon(g -> {
        circle(g, BLUE);
        g.setColor(Color.WHITE);
        g.fillRect(7, 6, 2, 6);
        g.fillRect(7, 3, 2, 2);
    });
    static final Icon QUESTION = icon(g -> {
        circle(g, BLUE);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(5, 3, 6, 5, 0, 220);
        g.drawLine(8, 8, 8, 9);
        g.fillRect(7, 11, 2, 2);
    });
    static final Icon LOGOFF = icon(g -> {
        circle(g, new Color(0x6B7C8D));
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(4, 4, 8, 8, 120, 300);
        g.drawLine(8, 3, 8, 8);
    });
    static final Icon SIMULATE = icon(g -> {
        g.setColor(BLUE);
        g.fillRoundRect(1, 2, 14, 12, 2, 2);
        g.setColor(Color.WHITE);
        g.fillRect(3, 5, 4, 1);
        g.fillRect(3, 8, 10, 1);
        g.fillRect(3, 11, 7, 1);
    });
    static final Icon POST = icon(g -> {
        g.setColor(new Color(0xE3A21A));
        g.fillRoundRect(1, 1, 14, 14, 3, 3);
        check(g);
    });
    static final Icon DISPLAY = icon(g -> {
        g.setColor(new Color(0x5B8BC9));
        g.fillRoundRect(2, 1, 12, 14, 2, 2);
        g.setColor(Color.WHITE);
        g.drawOval(5, 5, 5, 5);
        g.drawLine(9, 9, 12, 12);
    });
    static final Icon NEW_ROWS = icon(g -> {
        g.setColor(GREEN);
        g.fillRect(7, 3, 2, 10);
        g.fillRect(3, 7, 10, 2);
    });
    static final Icon LIGHT_RED = icon(g -> {
        g.setColor(RED);
        g.fillOval(2, 2, 12, 12);
    });
    static final Icon LIGHT_GREEN = icon(g -> {
        g.setColor(GREEN);
        g.fillOval(2, 2, 12, 12);
    });

    private Icons() {
    }

    private interface Painter {
        void paint(Graphics2D g);
    }

    private static Icon icon(Painter painter) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.translate(x, y);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                painter.paint(g2);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 16;
            }

            @Override
            public int getIconHeight() {
                return 16;
            }
        };
    }

    private static void circle(Graphics2D g, Color color) {
        g.setColor(color);
        g.fillOval(0, 0, 15, 15);
    }

    private static void check(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawPolyline(new int[]{4, 7, 12}, new int[]{8, 11, 5}, 3);
    }

    private static void cross(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(5, 5, 10, 10);
        g.drawLine(10, 5, 5, 10);
    }
}
