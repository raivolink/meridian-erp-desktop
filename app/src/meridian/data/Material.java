package meridian.data;

public final class Material {
    public final String id;
    public final String name;
    public final String group;
    public final double price;
    public final boolean deletionFlag;
    /** Unrestricted stock in plant 1000; reduced by confirmed order quantities. */
    int stock;

    public Material(String id, String name, String group, double price, int stock, boolean deletionFlag) {
        this.id = id;
        this.name = name;
        this.group = group;
        this.price = price;
        this.stock = stock;
        this.deletionFlag = deletionFlag;
    }

    public int getStock() {
        return stock;
    }
}
