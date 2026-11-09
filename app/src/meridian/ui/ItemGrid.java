package meridian.ui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Table control made of real input fields, one row per line item (like FB60 / VA01 items).
 * Rows are created by a factory so every cell gets a numbered technical name, e.g. RV45A-MABNR-3.
 */
final class ItemGrid extends JPanel {
    interface RowFactory {
        /** Components for row number {@code row} (1-based), one per header column. */
        JComponent[] create(int row);
    }

    private final RowFactory factory;
    private int rowCount;

    ItemGrid(String[] headers, RowFactory factory) {
        super(new GridBagLayout());
        this.factory = factory;
        setBackground(java.awt.Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        for (int col = 0; col < headers.length; col++) {
            JLabel header = new JLabel(headers[col]);
            header.setFont(header.getFont().deriveFont(Font.BOLD));
            header.setOpaque(true);
            header.setBackground(ErpTheme.TOOLBAR);
            header.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
            add(header, cell(col, 0, true));
        }
        // Fillers keep the cells packed at the top left.
        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 100;
        right.gridy = 0;
        right.weightx = 1;
        add(new JLabel(), right);
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridx = 0;
        filler.gridy = 1000;
        filler.weighty = 1;
        add(new JLabel(), filler);
    }

    void addRows(int count) {
        for (int i = 0; i < count; i++) {
            rowCount++;
            JComponent[] cells = factory.create(rowCount);
            for (int col = 0; col < cells.length; col++) {
                add(cells[col], cell(col, rowCount, false));
            }
        }
        revalidate();
        repaint();
    }

    int getRowCount() {
        return rowCount;
    }

    private static GridBagConstraints cell(int col, int row, boolean header) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = col;
        c.gridy = row;
        c.fill = header ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(1, 1, 1, 1);
        return c;
    }
}
