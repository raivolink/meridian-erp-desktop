package meridian.data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Posted FI document (vendor invoice KR or credit memo KG). */
public final class FiDocument {
    public final String docNo;
    public final String bukrs;
    public final int gjahr;
    public final String docType;
    public final String vendorId;
    public final LocalDate docDate;
    public final LocalDate postingDate;
    public final String reference;
    public final String headerText;
    public final String currency;
    public final double grossAmount;
    public final double taxAmount;
    public final String paymentTerms;
    public final LocalDate baselineDate;
    public final LocalDate dueDate;
    public final String status;
    public final String enteredBy;
    public final LocalDateTime enteredAt;
    public final List<Line> lines;

    FiDocument(String docNo, String bukrs, int gjahr, String docType, String vendorId, LocalDate docDate,
               LocalDate postingDate, String reference, String headerText, String currency, double grossAmount,
               double taxAmount, String paymentTerms, LocalDate baselineDate, LocalDate dueDate, String enteredBy,
               List<Line> lines) {
        this.docNo = docNo;
        this.bukrs = bukrs;
        this.gjahr = gjahr;
        this.docType = docType;
        this.vendorId = vendorId;
        this.docDate = docDate;
        this.postingDate = postingDate;
        this.reference = reference;
        this.headerText = headerText;
        this.currency = currency;
        this.grossAmount = grossAmount;
        this.taxAmount = taxAmount;
        this.paymentTerms = paymentTerms;
        this.baselineDate = baselineDate;
        this.dueDate = dueDate;
        this.status = "Posted";
        this.enteredBy = enteredBy;
        this.enteredAt = LocalDateTime.now();
        this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
    }

    public boolean isCreditMemo() {
        return "KG".equals(docType);
    }

    /** Document line. Account type K = vendor, S = G/L account. Debit/credit S = debit, H = credit. */
    public static final class Line {
        public final int lineNo;
        public final String accountType;
        public final String account;
        public final String dc;
        public final double amount;
        public final String taxCode;
        public final String costCenter;
        public final String text;

        Line(int lineNo, String accountType, String account, String dc, double amount, String taxCode,
             String costCenter, String text) {
            this.lineNo = lineNo;
            this.accountType = accountType;
            this.account = account;
            this.dc = dc;
            this.amount = amount;
            this.taxCode = taxCode;
            this.costCenter = costCenter;
            this.text = text;
        }

        /** Posting key: 31/21 vendor credit/debit, 40/50 G/L debit/credit. */
        public String postingKey() {
            if ("K".equals(accountType)) {
                return "H".equals(dc) ? "31" : "21";
            }
            return "S".equals(dc) ? "40" : "50";
        }
    }
}
