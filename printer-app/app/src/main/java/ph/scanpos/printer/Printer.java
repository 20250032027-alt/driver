package ph.scanpos.printer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Prints over "classic" Bluetooth (SPP), the same way the printer maker's own SDK does:
 * connect to the PAIRED printer and write the bytes. Classic Bluetooth has flow control built in,
 * so the printer can tell the phone to wait: this is why it doesn't freeze like the browser route.
 */
final class Printer {
    static final UUID SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    interface Progress { void on(int sent, int total); }

    static BluetoothAdapter adapter(Context ctx) {
        BluetoothManager m = (BluetoothManager) ctx.getSystemService(Context.BLUETOOTH_SERVICE);
        return m == null ? null : m.getAdapter();
    }

    /** Connect: the standard way first, then the two fallbacks that cheap printers often need. */
    private static BluetoothSocket connect(BluetoothDevice d) throws IOException {
        IOException last;
        try { BluetoothSocket s = d.createRfcommSocketToServiceRecord(SPP); s.connect(); return s; }
        catch (IOException e) { last = e; }
        try { BluetoothSocket s = d.createInsecureRfcommSocketToServiceRecord(SPP); s.connect(); return s; }
        catch (IOException e) { last = e; }
        try {
            Method m = d.getClass().getMethod("createRfcommSocket", int.class);
            BluetoothSocket s = (BluetoothSocket) m.invoke(d, 1);
            s.connect();
            return s;
        } catch (Exception e) {
            throw new IOException("Couldn't connect to the printer. Check it's switched on and nearby. (" + last.getMessage() + ")");
        }
    }

    /**
     * Send a print job. Normal: 256-byte pieces, relying on Bluetooth's own flow control (like the maker's SDK).
     * Slow: 64-byte pieces at about 900 bytes a second (for printers that still stop part-way).
     */
    static void print(Context ctx, String address, byte[] data, boolean slow, Progress p) throws IOException {
        BluetoothAdapter a = adapter(ctx);
        if (a == null) throw new IOException("This phone has no Bluetooth.");
        if (!a.isEnabled()) throw new IOException("Bluetooth is off. Turn it on and try again.");
        if (address == null || address.isEmpty()) throw new IOException("No printer chosen yet. Open Scan POS Printer and choose your printer.");
        BluetoothDevice d = a.getRemoteDevice(address);
        try { a.cancelDiscovery(); } catch (SecurityException ignored) { /* needs a permission we don't ask for; harmless */ }
        BluetoothSocket s = connect(d);
        try {
            OutputStream out = s.getOutputStream();
            int chunk = slow ? 64 : 256;
            long gap = slow ? 70 : 15;
            for (int i = 0; i < data.length; i += chunk) {
                int n = Math.min(chunk, data.length - i);
                out.write(data, i, n);
                out.flush();
                if (p != null) p.on(i + n, data.length);
                sleep(gap);
            }
            // Let the printer take in the rest before hanging up (its link runs at about 960 bytes a second).
            sleep(Math.max(800, data.length * 1000L / 900));
        } catch (IOException e) {
            throw new IOException("The printer stopped part-way. Check it's on and has paper, then tap Try again.");
        } finally {
            try { s.close(); } catch (IOException ignored) { }
        }
    }

    private static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); } }
}
