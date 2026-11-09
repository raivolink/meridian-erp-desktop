package meridian.data;

public final class TaxCode {
    public final String id;
    public final String description;
    public final double rate;
    /** Tax G/L account; null for the exempt code. */
    public final String gl;

    public TaxCode(String id, String description, double rate, String gl) {
        this.id = id;
        this.description = description;
        this.rate = rate;
        this.gl = gl;
    }
}
