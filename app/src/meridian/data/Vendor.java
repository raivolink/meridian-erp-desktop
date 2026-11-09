package meridian.data;

public final class Vendor {
    public final String id;
    public final String name;
    public final String street;
    public final String city;
    public final String country;
    public final String terms;
    public final String defaultGl;
    public final String defaultCc;
    public final boolean active;
    public final boolean postingBlock;

    public Vendor(String id, String name, String street, String city, String country, String terms,
                  String defaultGl, String defaultCc, boolean active, boolean postingBlock) {
        this.id = id;
        this.name = name;
        this.street = street;
        this.city = city;
        this.country = country;
        this.terms = terms;
        this.defaultGl = defaultGl;
        this.defaultCc = defaultCc;
        this.active = active;
        this.postingBlock = postingBlock;
    }
}
