package ph.scanpos.printer;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** One-time setup: pair the printer in Android, choose it here, Test print. */
public class MainActivity extends Activity {
    private LinearLayout list;
    private TextView result;
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView scroll = new ScrollView(this);
        LinearLayout col = Ui.column(this, 20);
        scroll.addView(col);
        col.addView(Ui.text(this, "Scan POS Printer", 26, true));
        col.addView(Ui.text(this, "Prints Scan POS slips on a Bluetooth receipt printer. Set it up once:", 16, false));
        col.addView(Ui.text(this, "1. Turn the printer on.\n2. Pair it in Android's Bluetooth settings (the PIN is usually 1234).\n3. Choose it below and tap Test print.", 16, false));
        Button bt = Ui.button(this, "Open Bluetooth settings", false);
        bt.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        col.addView(bt);

        col.addView(Ui.text(this, "Your printer", 19, true));
        list = Ui.column(this, 0);
        col.addView(list);

        Prefs prefs = new Prefs(this);
        col.addView(Ui.text(this, "Paper width", 19, true));
        RadioGroup paper = new RadioGroup(this);
        RadioButton p58 = radio("58 mm (small)"), p80 = radio("80 mm (wide)");
        paper.addView(p58); paper.addView(p80);
        (prefs.paper() == 80 ? p80 : p58).setChecked(true);
        paper.setOnCheckedChangeListener((g, id) -> prefs.setPaper(id == p80.getId() ? 80 : 58));
        col.addView(paper);

        col.addView(Ui.text(this, "Speed", 19, true));
        RadioGroup speed = new RadioGroup(this);
        RadioButton normal = radio("Normal (recommended)"), slow = radio("Slow (if slips stop part-way)");
        speed.addView(normal); speed.addView(slow);
        (prefs.slow() ? slow : normal).setChecked(true);
        speed.setOnCheckedChangeListener((g, id) -> prefs.setSlow(id == slow.getId()));
        col.addView(speed);

        Button test = Ui.button(this, "Test print", true);
        result = Ui.text(this, "", 16, false);
        test.setOnClickListener(v -> testPrint());
        col.addView(test);
        col.addView(result);
        col.addView(Ui.text(this, "After this, tap Print in Scan POS: slips come straight here. You can also share a receipt picture to this app.", 15, false));
        setContentView(scroll);
    }

    @Override protected void onResume() {
        super.onResume();
        if (!Perm.ok(this)) { showMessage("Allow \"Nearby devices\" so this app can see your paired printer."); Perm.ask(this); return; }
        fillList();
    }

    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        if (code == Perm.CODE && Perm.ok(this)) fillList();
    }

    private RadioButton radio(String s) {
        RadioButton r = new RadioButton(this);
        r.setText(s);
        r.setTextSize(17);
        r.setMinHeight(Ui.dp(this, 48));
        r.setId(android.view.View.generateViewId());
        return r;
    }

    private void showMessage(String s) {
        list.removeAllViews();
        list.addView(Ui.text(this, s, 16, false));
    }

    /** Paired devices, printers first. */
    private void fillList() {
        BluetoothAdapter a = Printer.adapter(this);
        if (a == null) { showMessage("This phone has no Bluetooth."); return; }
        if (!a.isEnabled()) { showMessage("Bluetooth is off. Turn it on, then come back here."); return; }
        Set<BluetoothDevice> bonded;
        try { bonded = a.getBondedDevices(); } catch (SecurityException e) { showMessage("Allow \"Nearby devices\" first."); Perm.ask(this); return; }
        List<BluetoothDevice> devices = new ArrayList<>(bonded);
        java.util.Collections.sort(devices, (x, y) -> Boolean.compare(!looksLikePrinter(x), !looksLikePrinter(y)));   // works on Android 5+
        list.removeAllViews();
        if (devices.isEmpty()) { showMessage("No paired devices yet. Tap \"Open Bluetooth settings\", pair the printer (PIN 1234), then come back."); return; }
        Prefs prefs = new Prefs(this);
        RadioGroup g = new RadioGroup(this);
        for (BluetoothDevice d : devices) {
            RadioButton r = radio(name(d) + (looksLikePrinter(d) ? "" : "  (not a printer?)"));
            g.addView(r);
            if (d.getAddress().equals(prefs.address())) r.setChecked(true);
            r.setOnClickListener(v -> { prefs.setPrinter(d.getAddress(), name(d)); result.setText("Chosen: " + name(d) + ". Now tap Test print."); });
        }
        list.addView(g);
    }

    private static String name(BluetoothDevice d) {
        try { String n = d.getName(); return n == null || n.isEmpty() ? d.getAddress() : n; } catch (SecurityException e) { return d.getAddress(); }
    }

    private static boolean looksLikePrinter(BluetoothDevice d) {
        String n = name(d).toLowerCase();
        return n.contains("printer") || n.startsWith("pos") || n.startsWith("mpt") || n.startsWith("mtp") || n.startsWith("pt-") || n.startsWith("xp") || n.contains("thermal") || n.startsWith("p58") || n.startsWith("rpp");
    }

    private void testPrint() {
        Prefs prefs = new Prefs(this);
        if (prefs.address().isEmpty()) { result.setText("Choose your printer above first."); return; }
        result.setText("Printing the test…");
        byte[] page = Escpos.testPage(prefs.paper(), prefs.slow());
        new Thread(() -> {
            try {
                Printer.print(this, prefs.address(), page, prefs.slow(), null);
                main.post(() -> result.setText("✓ Test sent. The paper should end with \"*** END OF TEST ***\"."));
            } catch (SecurityException e) {
                main.post(() -> { result.setText("Allow \"Nearby devices\" first."); Perm.ask(this); });
            } catch (Exception e) {
                main.post(() -> result.setText(e.getMessage()));
            }
        }).start();
    }
}
