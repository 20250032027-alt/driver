package ph.scanpos.printer;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

/** Android 12 and newer ask once for "Nearby devices" (Bluetooth connect). Older Android: nothing to ask. */
final class Perm {
    static final int CODE = 7;
    static boolean ok(Activity a) {
        return Build.VERSION.SDK_INT < 31 || a.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }
    static void ask(Activity a) {
        if (Build.VERSION.SDK_INT >= 31) a.requestPermissions(new String[] { Manifest.permission.BLUETOOTH_CONNECT }, CODE);
    }
}
