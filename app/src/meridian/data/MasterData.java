package meridian.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Master data of the zero state, identical to the Meridian web app (src/data/master.ts).
 * Every method returns fresh objects, so a reset restores stock.
 */
final class MasterData {
    static final String COMPANY_CODE = "1000";
    static final String COMPANY_NAME = "Meridian Group Inc.";
    static final String CLIENT = "100";

    private MasterData() {
    }

    static List<PaymentTerm> paymentTerms() {
        return Arrays.asList(
                new PaymentTerm("0001", "Payable immediately, due net", 0),
                new PaymentTerm("N015", "Within 15 days due net", 15),
                new PaymentTerm("N030", "Within 30 days due net", 30),
                new PaymentTerm("N045", "Within 45 days due net", 45),
                new PaymentTerm("N060", "Within 60 days due net", 60));
    }

    static List<Customer> customers() {
        return Arrays.asList(
                customer("100001", "Atlas Logistics Inc.", "400 Harbor Blvd", "Newark, NJ 07114", "N030", 500000),
                customer("100002", "Bluewater Hospitality Group", "1200 Ocean Dr", "Miami, FL 33139", "N030", 250000),
                customer("100003", "Cedar Ridge Medical Center", "88 Cedar Ridge Rd", "Albany, NY 12203", "N045", 750000),
                customer("100004", "Driftwood Media LLC", "55 Water St", "Brooklyn, NY 11201", "N015", 60000),
                customer("100005", "Evergreen School District", "17 Schoolhouse Ln", "Hartford, CT 06103", "N060", 400000),
                customer("100006", "Falcon Aerospace Components", "9 Hangar Way", "Wichita, KS 67209", "N030", 900000),
                customer("100007", "Granite State Insurance", "1 Granite Plaza", "Manchester, NH 03101", "N030", 300000),
                customer("100008", "Harbor Point Bank", "200 Federal St", "Boston, MA 02110", "N030", 1000000),
                customer("100009", "Ironclad Manufacturing Co.", "3100 Foundry Ave", "Pittsburgh, PA 15222", "N045", 650000),
                customer("100010", "Juniper Software Corp.", "600 Congress Ave", "Austin, TX 78701", "N030", 350000),
                customer("100011", "Keystone Engineering Partners", "42 Market St", "Philadelphia, PA 19106", "N030", 200000),
                customer("100012", "Lakeside Dental Partners", "15 Lakeshore Dr", "Chicago, IL 60611", "N015", 40000),
                customer("100013", "Maple Leaf Consulting", "77 King St W", "Buffalo, NY 14202", "N030", 120000),
                customer("100014", "Northstar Energy Corp.", "1000 Main St", "Houston, TX 77002", "N045", 1000000),
                customer("100015", "Orchard Retail Co.", "310 Orchard Rd", "Columbus, OH 43215", "N030", 275000),
                customer("100016", "Pinnacle Law Group", "1 Pinnacle Tower", "Washington, DC 20005", "N030", 150000),
                customer("100017", "Quarry Construction LLC", "5 Bedrock Pkwy", "Denver, CO 80202", "N060", 450000),
                customer("100018", "Redwood Analytics", "901 Mission St", "San Francisco, CA 94103", "N030", 300000),
                new Customer("100019", "Summit Health Clinics", "22 Summit Ave", "Portland, OR 97205", "N030", 500000, true),
                customer("100020", "Tidewater Shipping Lines", "1 Pier Rd", "Norfolk, VA 23510", "N030", 600000),
                customer("100021", "Union Square Hotels", "33 Union Sq", "New York, NY 10003", "N030", 350000),
                customer("100022", "Vantage Point Realty", "450 Peachtree St", "Atlanta, GA 30308", "N030", 100000),
                customer("100023", "Willow Creek Foods", "12 Mill Rd", "Madison, WI 53703", "N015", 180000),
                customer("100024", "Xenon Labs Inc.", "700 Research Dr", "Raleigh, NC 27606", "N030", 250000),
                customer("100025", "Yellowstone Outfitters", "18 Canyon St", "Bozeman, MT 59715", "N015", 5000),
                customer("100026", "Zenith Education Group", "250 Campus Dr", "Ann Arbor, MI 48104", "N045", 300000));
    }

    static List<Material> materials() {
        return new ArrayList<>(Arrays.asList(
                material("HT-1000", "Notebook Basic 15", "Notebooks", 956.0, 120),
                material("HT-1001", "Notebook Basic 17", "Notebooks", 1249.0, 80),
                material("HT-1010", "Notebook Professional 15", "Notebooks", 1999.0, 60),
                material("HT-1011", "Notebook Professional 17", "Notebooks", 2299.0, 25),
                material("HT-1030", "Flat Screen 21\"", "Monitors", 269.0, 200),
                material("HT-1035", "Flat Screen 24\"", "Monitors", 339.0, 150),
                material("HT-1037", "Ultrawide Screen 34\"", "Monitors", 749.0, 40),
                material("HT-1040", "Laser Printer Office", "Printers", 489.0, 35),
                material("HT-1050", "Ink Jet Printer Color", "Printers", 179.0, 70),
                material("HT-1060", "Wireless Mouse", "Accessories", 29.9, 1000),
                material("HT-1063", "Ergonomic Keyboard", "Accessories", 79.0, 500),
                material("HT-1070", "Docking Station USB-C", "Accessories", 219.0, 180),
                material("HT-1080", "Webcam HD 1080p", "Accessories", 89.0, 300),
                material("HT-1090", "Headset Pro Noise Cancelling", "Accessories", 149.0, 250),
                material("HT-1100", "USB-C Hub 7-in-1", "Accessories", 49.0, 600),
                material("HT-1110", "Portable SSD 1TB", "Storage", 119.0, 220),
                material("HT-1120", "Tablet 10\" 128GB", "Mobile", 449.0, 90),
                material("HT-1130", "Smartphone X 256GB", "Mobile", 899.0, 75),
                material("HT-1140", "Conference Speakerphone", "Collaboration", 299.0, 45),
                material("HT-1150", "Projector Full HD", "Collaboration", 649.0, 20),
                material("HT-1160", "Network Switch 24-Port", "Networking", 389.0, 30),
                material("HT-1170", "WiFi 6 Access Point", "Networking", 229.0, 85),
                material("HT-1180", "UPS 1500VA", "Networking", 329.0, 40),
                material("HT-1190", "Server Rack 42U", "Networking", 1189.0, 8),
                material("HT-1200", "Office Chair Ergo", "Furniture", 399.0, 60),
                material("HT-1210", "Standing Desk Electric", "Furniture", 649.0, 30),
                material("HT-1220", "Monitor Arm Dual", "Furniture", 99.0, 140),
                material("HT-1230", "Laptop Bag 15\"", "Accessories", 59.0, 400),
                material("HT-1240", "Cable Kit Office", "Accessories", 24.5, 800),
                new Material("HT-1250", "Paper Shredder Cross-Cut", "Office", 159.0, 0, true),
                material("HT-1260", "Whiteboard Mobile", "Office", 289.0, 15),
                material("HT-1270", "Label Printer", "Printers", 139.0, 3)));
    }

    static List<User> users() {
        return Arrays.asList(
                new User("DEMO", "Demo Presenter", "ADMIN", "Demo#2026"),
                new User("RPA_BOT", "RPA Robot", "USER", "Robot#2026"),
                new User("APCLERK", "Alex Parker (AP Clerk)", "USER", "Clerk#2026"));
    }

    private static Customer customer(String id, String name, String street, String city, String terms, double limit) {
        return new Customer(id, name, street, city, terms, limit, false);
    }

    private static Material material(String id, String name, String group, double price, int stock) {
        return new Material(id, name, group, price, stock, false);
    }
}
