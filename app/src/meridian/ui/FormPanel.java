package meridian.ui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Rows of "Label  field [extra components]", like an SAP GUI screen. Fields keep their natural width. */
class FormPanel extends JPanel {
    private int rows;

    FormPanel() {
        super(new GridBagLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    /** Adds a row; the label is linked to the field with labelFor. Extra components follow the field. */
    void addField(String label, JComponent field, JComponent... extras) {
        addField(new JLabel(label), field, extras);
    }

    /** Adds a row with a caption label the caller keeps (e.g. to change its text later). */
    void addField(JLabel caption, JComponent field, JComponent... extras) {
        caption.setLabelFor(field);
        addRow(caption, field, extras);
    }

    /** Adds a row whose left column is free text (no labelFor), e.g. a hint. */
    void addLine(JComponent content) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = rows++;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(4, 4, 2, 4);
        add(content, c);
    }

    private void addRow(JLabel caption, JComponent field, JComponent... extras) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = rows;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(2, 4, 2, 12);
        add(caption, c);

        JPanel cell = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        cell.setOpaque(false);
        cell.add(field);
        for (JComponent extra : extras) {
            cell.add(extra);
        }
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = rows++;
        c.weightx = 1;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(2, 0, 2, 4);
        add(cell, c);
    }
}
