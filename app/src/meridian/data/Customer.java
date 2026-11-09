package meridian.data;

public final class Customer {
    public final String id;
    public final String name;
    public final String street;
    public final String city;
    public final String terms;
    public final double creditLimit;
    public final boolean salesBlock;

    public Customer(String id, String name, String street, String city, String terms,
                    double creditLimit, boolean salesBlock) {
        this.id = id;
        this.name = name;
        this.street = street;
        this.city = city;
        this.terms = terms;
        this.creditLimit = creditLimit;
        this.salesBlock = salesBlock;
    }
}
