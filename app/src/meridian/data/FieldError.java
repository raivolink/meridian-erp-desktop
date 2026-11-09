package meridian.data;

/** Validation error on a field, identified by its SAP technical name (e.g. INVFO-ACCNT). */
public final class FieldError {
    public final String field;
    public final String text;

    public FieldError(String field, String text) {
        this.field = field;
        this.text = text;
    }
}
