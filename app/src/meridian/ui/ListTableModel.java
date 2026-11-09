package meridian.ui;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/** Read-only table model over a list. All cells are strings so JAB reads them as shown. */
class ListTableModel<T> extends AbstractTableModel {
    private final List<String> columns = new ArrayList<>();
    private final List<Function<T, Object>> getters = new ArrayList<>();
    private List<T> rows = Collections.emptyList();

    ListTableModel<T> column(String name, Function<T, Object> getter) {
        columns.add(name);
        getters.add(getter);
        return this;
    }

    void setRows(List<T> newRows) {
        rows = new ArrayList<>(newRows);
        fireTableDataChanged();
    }

    List<T> getRows() {
        return Collections.unmodifiableList(rows);
    }

    T getRow(int modelIndex) {
        return rows.get(modelIndex);
    }

    int indexOf(Predicate<T> predicate) {
        for (int i = 0; i < rows.size(); i++) {
            if (predicate.test(rows.get(i))) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public String getColumnName(int column) {
        return columns.get(column);
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return String.class;
    }

    @Override
    public Object getValueAt(int row, int column) {
        return String.valueOf(getters.get(column).apply(rows.get(row)));
    }
}
