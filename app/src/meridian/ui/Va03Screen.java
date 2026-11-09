package meridian.ui;

import meridian.data.Customer;
import meridian.data.FieldError;
import meridian.data.Fmt;
import meridian.data.SalesOrder;
import meridian.data.Va01;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/** VA03 Display Sales Order: order list with the order number field, and the order detail view. */
final class Va03Screen extends Screen {
    private static final String LIST = "list";
    private static final String DETAIL = "detail";

    private final CardLayout cards = new CardLayout();
    private final JPanel pages = new JPanel(cards);
    private String page = LIST;

    private final JTextField vbeln = reg(Widgets.field("VBAK-VBELN", "Order", 10));
    private final JLabel listTitle = Widgets.valueLabel("order-list-title", "");
    private final ListTableModel<SalesOrder> listModel = new ListTableModel<SalesOrder>()
            .column("Order", o -> o.vbeln)
            .column("Type", o -> o.auart)
            .column("Sold-To", o -> o.soldTo)
            .column("Name", o -> customerName(o.soldTo))
            .column("Cust. Reference", o -> o.custPo == null ? "" : o.custPo)
            .column("Req. Deliv.", o -> Fmt.date(o.reqDeliveryDate))
            .column("Net value", o -> Fmt.amount(o.netValue))
            .column("Credit", o -> o.isCreditBlocked() ? "Blocked" : "OK")
            .column("User", o -> o.createdBy);
    private final JTable list = Widgets.table("order-list", listModel);

    private final JLabel orderTypeCaption = new JLabel("Standard Order");
    private final JLabel orderNumber = Widgets.valueLabel("order-number", "");
    private final JLabel soldTo = Widgets.valueLabel("order-soldto", "");
    private final JLabel shipTo = Widgets.valueLabel("order-shipto", "");
    private final JLabel custPo = Widgets.valueLabel("order-custpo", "");
    private final JLabel custPoDate = Widgets.valueLabel("order-custpo-date", "");
    private final JLabel salesArea = Widgets.valueLabel("order-sales-area", "");
    private final JLabel reqDate = Widgets.valueLabel("order-reqdate", "");
    private final JLabel terms = Widgets.valueLabel("order-terms", "");
    private final JLabel netValue = Widgets.valueLabel("order-net", "");
    private final JLabel creditStatus = Widgets.valueLabel("order-credit", "");
    private final JLabel createdBy = Widgets.valueLabel("order-created-by", "");
    private final ListTableModel<SalesOrder.Item> itemsModel = new ListTableModel<SalesOrder.Item>()
            .column("Item", i -> i.posnr)
            .column("Material", i -> i.matnr)
            .column("Order Qty", i -> i.qty)
            .column("Un", i -> i.uom)
            .column("Description", i -> i.description)
            .column("Net price", i -> Fmt.amount(i.netPrice))
            .column("Net value", i -> Fmt.amount(i.netValue))
            .column("Plnt", i -> i.plant)
            .column("Confirmed", i -> i.confirmedQty);
    private final JTable itemsTable = Widgets.table("order-items", itemsModel);

    private final JButton listButton = Widgets.button("Order list", "Order list", Icons.DISPLAY,
            e -> shell.run(() -> showList(null)));

    Va03Screen(Shell shell) {
        super(shell);
        vbeln.addActionListener(e -> enter());
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = list.getSelectedRow();
                if (e.getClickCount() == 2 && row >= 0) {
                    String number = listModel.getRow(row).vbeln;
                    shell.run(() -> display(number));
                }
            }
        });

        FormPanel keys = new FormPanel();
        keys.addField("Order", vbeln);
        JPanel listTable = new JPanel(new BorderLayout(0, 2));
        listTable.add(listTitle, BorderLayout.NORTH);
        listTable.add(new JScrollPane(list), BorderLayout.CENTER);
        JPanel listTop = new JPanel(new BorderLayout());
        listTop.add(keys, BorderLayout.WEST);
        JPanel listPage = new JPanel(new BorderLayout(0, 8));
        listPage.add(listTop, BorderLayout.NORTH);
        listPage.add(listTable, BorderLayout.CENTER);

        FormPanel headLeft = new FormPanel();
        headLeft.addField(orderTypeCaption, orderNumber);
        headLeft.addField("Sold-To Party", soldTo);
        headLeft.addField("Ship-To Party", shipTo);
        headLeft.addField("Cust. Reference", custPo);
        headLeft.addField("Cust. Ref. Date", custPoDate);
        FormPanel headRight = new FormPanel();
        headRight.addField("Sales area", salesArea);
        headRight.addField("Req. deliv.date", reqDate);
        headRight.addField("Payment terms", terms);
        headRight.addField("Net value", netValue);
        headRight.addField("Credit status", creditStatus);
        headRight.addField("Created by", createdBy);
        JPanel head = new JPanel(new GridLayout(1, 2, 10, 0));
        head.add(headLeft);
        head.add(headRight);
        JPanel itemsBox = new JPanel(new BorderLayout(0, 2));
        itemsBox.add(new JLabel("All items"), BorderLayout.NORTH);
        itemsBox.add(new JScrollPane(itemsTable), BorderLayout.CENTER);
        JPanel detailPage = new JPanel(new BorderLayout(0, 8));
        detailPage.add(Widgets.group("Header", head), BorderLayout.NORTH);
        detailPage.add(itemsBox, BorderLayout.CENTER);

        pages.add(padded(listPage), LIST);
        pages.add(padded(detailPage), DETAIL);
        add(pages, BorderLayout.CENTER);
    }

    @Override
    String tcode() {
        return "VA03";
    }

    @Override
    void start() {
        vbeln.setText("");
        showList(null);
    }

    @Override
    void enter() {
        if (LIST.equals(page)) {
            String number = Fmt.trim(vbeln.getText());
            shell.run(() -> display(number));
        }
    }

    private void display(String number) {
        clearError();
        if (number.isEmpty()) {
            showList(null);
            return;
        }
        SalesOrder o = sys.order(number);
        if (o == null) {
            showList(new FieldError("VBAK-VBELN", "SD document " + number + " is not in the database or has been archived"));
            return;
        }
        String typeName = Va01.ORDER_TYPES.containsKey(o.auart) ? Va01.ORDER_TYPES.get(o.auart) : o.auart;
        Customer sold = sys.customer(o.soldTo);
        orderTypeCaption.setText(typeName);
        A11y.setValue(orderNumber, o.vbeln);
        A11y.setValue(soldTo, o.soldTo + " " + (sold == null ? "" : sold.name + " / " + sold.city));
        A11y.setValue(shipTo, o.shipTo + " " + customerName(o.shipTo));
        A11y.setValue(custPo, o.custPo == null ? "" : o.custPo);
        A11y.setValue(custPoDate, Fmt.date(o.custPoDate));
        A11y.setValue(salesArea, o.vkorg + " / " + o.vtweg + " / " + o.spart);
        A11y.setValue(reqDate, Fmt.date(o.reqDeliveryDate));
        A11y.setValue(terms, o.paymentTerms);
        A11y.setValue(netValue, Fmt.amount(o.netValue) + " " + o.currency);
        A11y.setValue(creditStatus, o.isCreditBlocked() ? "Blocked – credit limit" : "Released");
        A11y.setValue(createdBy, o.createdBy + ", " + o.createdAt.format(Fb03Screen.TIMESTAMP));
        itemsModel.setRows(o.items);
        page = DETAIL;
        cards.show(pages, DETAIL);
        shell.setScreenTitle("Display " + typeName + " " + o.vbeln + ": Overview");
        shell.setAppButtons(listButton);
    }

    private void showList(FieldError error) {
        page = LIST;
        List<SalesOrder> orders = sys.getOrders();
        listModel.setRows(orders);
        A11y.setValue(listTitle, orders.isEmpty() ? "Sales documents (0) – No sales documents yet"
                : "Sales documents (" + orders.size() + ")");
        cards.show(pages, LIST);
        shell.setScreenTitle("Display Sales Documents: Initial Screen");
        shell.setAppButtons();
        if (error != null) {
            showError(error);
        } else {
            vbeln.requestFocusInWindow();
        }
    }

    private String customerName(String id) {
        Customer c = sys.customer(id);
        return c == null ? "" : c.name;
    }
}
