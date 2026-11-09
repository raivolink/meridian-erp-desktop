package meridian.data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Saved sales order (VBAK + VBAP). */
public final class SalesOrder {
    public final String vbeln;
    public final String auart;
    public final String vkorg;
    public final String vtweg;
    public final String spart;
    public final String soldTo;
    public final String shipTo;
    public final String custPo;
    public final LocalDate custPoDate;
    public final LocalDate reqDeliveryDate;
    public final String paymentTerms;
    public final double netValue;
    public final String currency;
    /** A = released, B = blocked (credit limit). */
    public final String creditStatus;
    public final String status;
    public final String createdBy;
    public final LocalDateTime createdAt;
    public final List<Item> items;

    SalesOrder(String vbeln, String auart, String vkorg, String vtweg, String spart, String soldTo, String shipTo,
               String custPo, LocalDate custPoDate, LocalDate reqDeliveryDate, String paymentTerms, double netValue,
               String creditStatus, String createdBy, List<Item> items) {
        this.vbeln = vbeln;
        this.auart = auart;
        this.vkorg = vkorg;
        this.vtweg = vtweg;
        this.spart = spart;
        this.soldTo = soldTo;
        this.shipTo = shipTo;
        this.custPo = custPo;
        this.custPoDate = custPoDate;
        this.reqDeliveryDate = reqDeliveryDate;
        this.paymentTerms = paymentTerms;
        this.netValue = netValue;
        this.currency = "USD";
        this.creditStatus = creditStatus;
        this.status = "Open";
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
    }

    public boolean isCreditBlocked() {
        return "B".equals(creditStatus);
    }

    public static final class Item {
        public final int posnr;
        public final String matnr;
        public final String description;
        public final int qty;
        public final int confirmedQty;
        public final String uom;
        public final double netPrice;
        public final double netValue;
        public final String plant;

        Item(int posnr, String matnr, String description, int qty, int confirmedQty, double netPrice,
             double netValue, String plant) {
            this.posnr = posnr;
            this.matnr = matnr;
            this.description = description;
            this.qty = qty;
            this.confirmedQty = confirmedQty;
            this.uom = "EA";
            this.netPrice = netPrice;
            this.netValue = netValue;
            this.plant = plant;
        }
    }
}
