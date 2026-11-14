package ph.scanpos.printer;

import android.content.Context;
import android.content.SharedPreferences;

/** The few things this app remembers: which printer, paper width, speed. */
final class Prefs {
    private final SharedPreferences p;
    Prefs(Context c) { p = c.getSharedPreferences("printer", Context.MODE_PRIVATE); }
    String address() { return p.getString("address", ""); }
    String name() { return p.getString("name", ""); }
    void setPrinter(String address, String name) { p.edit().putString("address", address).putString("name", name).apply(); }
    int paper() { return p.getInt("paper", 58); }
    void setPaper(int mm) { p.edit().putInt("paper", mm).apply(); }
    boolean slow() { return p.getBoolean("slow", false); }
    void setSlow(boolean v) { p.edit().putBoolean("slow", v).apply(); }
}
