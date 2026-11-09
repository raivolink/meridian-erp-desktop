package meridian.ui;

import meridian.data.FieldError;
import meridian.data.FiDocument;
import meridian.data.Fmt;
import meridian.data.GlAccount;
import meridian.data.Vendor;

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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** FB03 Display Document: document list with key fields, and the document detail view. */
final class Fb03Screen extends Screen {
    private static final String LIST = "list";
    private static final String DETAIL = "detail";
    static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CardLayout cards = new CardLayout();
    private final JPanel pages = new JPanel(cards);
    private String page = LIST;

    private final JTextField belnr = reg(Widgets.field("RF05L-BELNR", "Document Number", 12));
    private final JTextField bukrs = reg(Widgets.field("RF05L-BUKRS", "Company Code", 5));
    private final JTextField gjahr = reg(Widgets.field("RF05L-GJAHR", "Fiscal Year", 5));
    private final JLabel listTitle = Widgets.valueLabel("doc-list-title", "");
    private final ListTableModel<FiDocument> listModel = new ListTableModel<FiDocument>()
            .column("Document", d -> d.docNo)
            .column("Type", d -> d.docType)
            .column("Vendor", d -> d.vendorId)
            .column("Name", d -> vendorName(d.vendorId))
            .column("Reference", d -> d.reference)
            .column("Doc. Date", d -> Fmt.date(d.docDate))
            .column("Pstng Date", d -> Fmt.date(d.postingDate))
            .column("Amount", d -> Fmt.amount(d.grossAmount))
            .column("Curr.", d -> d.currency)
            .column("User", d -> d.enteredBy);
    private final JTable list = Widgets.table("doc-list", listModel);

    private final JLabel docNumber = Widgets.valueLabel("doc-number", "");
    private final JLabel docBukrs = Widgets.valueLabel("doc-bukrs", "");
    private final JLabel docYear = Widgets.valueLabel("doc-gjahr", "");
    private final JLabel docType = Widgets.valueLabel("doc-type", "");
    private final JLabel docStatus = Widgets.valueLabel("doc-status", "");
    private final JLabel docVendor = Widgets.valueLabel("doc-vendor", "");
    private final JLabel docReference = Widgets.valueLabel("doc-reference", "");
    private final JLabel docDate = Widgets.valueLabel("doc-date", "");
    private final JLabel docPostingDate = Widgets.valueLabel("doc-posting-date", "");
    private final JLabel docAmount = Widgets.valueLabel("doc-amount", "");
    private final JLabel docTerms = Widgets.valueLabel("doc-terms", "");
    private final JLabel docEnteredBy = Widgets.valueLabel("doc-entered-by", "");
    private FiDocument shown;
    private final ListTableModel<FiDocument.Line> linesModel = new ListTableModel<FiDocument.Line>()
            .column("Itm", l -> l.lineNo)
            .column("PK", FiDocument.Line::postingKey)
            .column("Account", l -> l.account)
            .column("Description", this::lineDescription)
            .column("Amount", l -> ("H".equals(l.dc) ? "-" : "") + Fmt.amount(l.amount))
            .column("Curr.", l -> shown == null ? "" : shown.currency)
            .column("Tx", l -> nz(l.taxCode))
            .column("Cost Ctr", l -> nz(l.costCenter))
            .column("Text", l -> nz(l.text));
    private final JTable lines = Widgets.table("doc-lines", linesModel);

    private final JButton listButton = Widgets.button("Document list", "Document list", Icons.DISPLAY,
            e -> shell.run(() -> showList(null)));

    Fb03Screen(Shell shell) {
        super(shell);
        belnr.addActionListener(e -> enter());
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = list.getSelectedRow();
                if (e.getClickCount() == 2 && row >= 0) {
                    String docNo = listModel.getRow(row).docNo;
                    shell.run(() -> display(docNo));
                }
            }
        });

        FormPanel keys = new FormPanel();
        keys.addField("Document Number", belnr);
        keys.addField("Company Code", bukrs);
        keys.addField("Fiscal Year", gjahr);
        JPanel listTop = new JPanel(new BorderLayout());
        listTop.add(Widgets.group("Keys for Entry View", keys), BorderLayout.WEST);
        JPanel listTable = new JPanel(new BorderLayout(0, 2));
        listTable.add(listTitle, BorderLayout.NORTH);
        listTable.add(new JScrollPane(list), BorderLayout.CENTER);
        JPanel listPage = new JPanel(new BorderLayout(0, 8));
        listPage.add(listTop, BorderLayout.NORTH);
        listPage.add(listTable, BorderLayout.CENTER);

        FormPanel headLeft = new FormPanel();
        headLeft.addField("Document Number", docNumber);
        headLeft.addField("Company Code", docBukrs);
        headLeft.addField("Fiscal Year", docYear);
        headLeft.addField("Document Type", docType);
        headLeft.addField("Status", docStatus);
        FormPanel headRight = new FormPanel();
        headRight.addField("Vendor", docVendor);
        headRight.addField("Reference", docReference);
        headRight.addField("Document Date", docDate);
        headRight.addField("Posting Date", docPostingDate);
        headRight.addField("Amount", docAmount);
        headRight.addField("Terms / Due", docTerms);
        headRight.addField("Entered by", docEnteredBy);
        JPanel head = new JPanel(new GridLayout(1, 2, 10, 0));
        head.add(headLeft);
        head.add(headRight);
        JPanel linesBox = new JPanel(new BorderLayout(0, 2));
        linesBox.add(new JLabel("Line items"), BorderLayout.NORTH);
        linesBox.add(new JScrollPane(lines), BorderLayout.CENTER);
        JPanel detailPage = new JPanel(new BorderLayout(0, 8));
        detailPage.add(Widgets.group("Document Header", head), BorderLayout.NORTH);
        detailPage.add(linesBox, BorderLayout.CENTER);

        pages.add(padded(listPage), LIST);
        pages.add(padded(detailPage), DETAIL);
        add(pages, BorderLayout.CENTER);
    }

    @Override
    String tcode() {
        return "FB03";
    }

    @Override
    void start() {
        belnr.setText("");
        showList(null);
    }

    @Override
    void enter() {
        if (LIST.equals(page)) {
            String docNo = Fmt.trim(belnr.getText());
            shell.run(() -> display(docNo));
        }
    }

    private void display(String docNo) {
        clearError();
        if (docNo.isEmpty()) {
            showList(null);
            return;
        }
        FiDocument d = sys.document(docNo);
        if (d == null) {
            showList(new FieldError("RF05L-BELNR", "Document " + docNo + " does not exist in company code 1000"));
            return;
        }
        shown = d;
        A11y.setValue(docNumber, d.docNo);
        A11y.setValue(docBukrs, d.bukrs);
        A11y.setValue(docYear, String.valueOf(d.gjahr));
        A11y.setValue(docType, d.docType + " " + (d.isCreditMemo() ? "Vendor credit memo" : "Vendor invoice"));
        A11y.setValue(docStatus, d.status);
        A11y.setValue(docVendor, d.vendorId + " " + vendorName(d.vendorId));
        A11y.setValue(docReference, d.reference);
        A11y.setValue(docDate, Fmt.date(d.docDate));
        A11y.setValue(docPostingDate, Fmt.date(d.postingDate));
        A11y.setValue(docAmount, Fmt.amount(d.grossAmount) + " " + d.currency + " (tax " + Fmt.amount(d.taxAmount) + ")");
        A11y.setValue(docTerms, d.paymentTerms + " / " + Fmt.date(d.dueDate));
        A11y.setValue(docEnteredBy, d.enteredBy + ", " + d.enteredAt.format(TIMESTAMP));
        linesModel.setRows(d.lines);
        page = DETAIL;
        cards.show(pages, DETAIL);
        shell.setScreenTitle("Display Document: Data Entry View");
        shell.setAppButtons(listButton);
    }

    private void showList(FieldError error) {
        page = LIST;
        bukrs.setText("1000");
        gjahr.setText(String.valueOf(LocalDate.now().getYear()));
        List<FiDocument> docs = sys.getDocuments();
        listModel.setRows(docs);
        A11y.setValue(listTitle, docs.isEmpty() ? "Document list (0) – No documents posted yet"
                : "Document list (" + docs.size() + ")");
        cards.show(pages, LIST);
        shell.setScreenTitle("Display Document: Initial Screen");
        shell.setAppButtons();
        if (error != null) {
            showError(error);
        } else {
            belnr.requestFocusInWindow();
        }
    }

    private String lineDescription(FiDocument.Line l) {
        if ("K".equals(l.accountType)) {
            return vendorName(l.account);
        }
        GlAccount gl = sys.glAccount(l.account);
        return gl == null ? "" : gl.description;
    }

    private String vendorName(String id) {
        Vendor v = sys.vendor(id);
        return v == null ? "" : v.name;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
