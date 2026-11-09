package meridian.data;

public final class PaymentTerm {
    public final String code;
    public final String description;
    public final int days;

    public PaymentTerm(String code, String description, int days) {
        this.code = code;
        this.description = description;
        this.days = days;
    }
}
