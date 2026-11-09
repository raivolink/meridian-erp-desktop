"""Generates src/meridian/data/ApMaster.java from the Meridian web app's src/data/ap-master.json.

Usage: python tools/gen_ap_master.py <path-to>/ap-master.json src/meridian/data/ApMaster.java
"""
import json, sys
d = json.load(open(sys.argv[1], encoding="utf-8"))
out = sys.argv[2]
def s(v):
    if v is None:
        return "null"
    return '"' + str(v).replace("\\", "\\\\").replace('"', '\\"') + '"'
L = []
L.append("package meridian.data;")
L.append("")
L.append("import java.util.Arrays;")
L.append("import java.util.List;")
L.append("")
L.append("/**")
L.append(" * AP master data of the zero state. GENERATED from the Meridian ERP web app")
L.append(" * (the web app's src/data/ap-master.json); regenerate instead of editing by hand.")
L.append(" */")
L.append("final class ApMaster {")
L.append("")
L.append("    private ApMaster() {")
L.append("    }")
L.append("")
L.append("    /** Vendors flagged with a posting block (WeWork). */")
L.append('    static final List<String> BLOCKED_VENDORS = Arrays.asList("V-2015");')
L.append("")
L.append("    static List<Vendor> vendors() {")
L.append("        return Arrays.asList(")
rows = []
for v in d["vendors"]:
    rows.append("                new Vendor(%s, %s, %s, %s, %s, %s, %s, %s, %s, BLOCKED_VENDORS.contains(%s))" % (
        s(v["id"]), s(v["name"]), s(v["street"]), s(v["city"]), s(v["country"]), s(v["terms"]), s(v["gl"]), s(v["cc"]),
        "true" if v["active"] else "false", s(v["id"])))
L.append(",\n".join(rows) + ");")
L.append("    }")
L.append("")
L.append("    /** Accounts from 5000 up (expense, P&L) need a cost center. */")
L.append("    static List<GlAccount> glAccounts() {")
L.append("        return Arrays.asList(")
L.append(",\n".join("                new GlAccount(%s, %s, %s)" % (s(g["id"]), s(g["d"]), "true" if int(g["id"]) >= 5000 else "false") for g in d["gl"]) + ");")
L.append("    }")
L.append("")
L.append("    static List<CostCenter> costCenters() {")
L.append("        return Arrays.asList(")
L.append(",\n".join("                new CostCenter(%s, %s)" % (s(c["id"]), s(c["d"])) for c in d["cc"]) + ");")
L.append("    }")
L.append("")
L.append("    static List<TaxCode> taxCodes() {")
L.append("        return Arrays.asList(")
L.append(",\n".join("                new TaxCode(%s, %s, %s, %s)" % (s(t["id"]), s(t["d"]), repr(float(t["rate"])), s(t["gl"])) for t in d["tax"]) + ");")
L.append("    }")
L.append("}")
open(out, "w", encoding="utf-8", newline="\n").write("\n".join(L) + "\n")
print(len(d["vendors"]), len(d["gl"]), len(d["cc"]), len(d["tax"]))
