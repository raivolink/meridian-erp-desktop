package meridian.data;

public final class GlAccount {
    public final String id;
    public final String description;
    /** Expense and P&L accounts need a cost center (CO object). */
    public final boolean requiresCc;

    public GlAccount(String id, String description, boolean requiresCc) {
        this.id = id;
        this.description = description;
        this.requiresCc = requiresCc;
    }
}
