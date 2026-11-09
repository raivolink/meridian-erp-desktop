package meridian.data;

/** Status bar message. Types as in SAP: S success, W warning, E error, I information. */
public final class Message {
    public final char type;
    public final String text;

    private Message(char type, String text) {
        this.type = type;
        this.text = text;
    }

    public static Message success(String text) {
        return new Message('S', text);
    }

    public static Message warning(String text) {
        return new Message('W', text);
    }

    public static Message error(String text) {
        return new Message('E', text);
    }

    public static Message info(String text) {
        return new Message('I', text);
    }
}
