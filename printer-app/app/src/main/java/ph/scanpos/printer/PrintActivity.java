package ph.scanpos.printer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.InputStream;

/**
 * Opens when Scan POS sends a slip (a "scanposprint:" link with the printer commands inside),
 * or when a picture is shared to this app. Shows a small "Printing…" box, prints, and closes by
 * itself so the cashier is back in Scan POS. On a problem it says what to do, with Try again.
 */
public class PrintActivity extends Activity {
    private TextView status;
    private Button retry, setup, close;
    private byte[] job;
    private boolean slowOverride;
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setFinishOnTouchOutside(false);
        LinearLayout box = Ui.column(this, 24);
        box.setMinimumWidth(Ui.dp(this, 300));
        box.addView(Ui.text(this, "Scan POS Printer", 20, true));
        status = Ui.text(this, "Getting the slip ready…", 17, false);
        box.addView(status);
        retry = Ui.button(this, "Try again", true);
        setup = Ui.button(this, "Choose printer", false);
        close = Ui.button(this, "Close", false);
        box.addView(retry); box.addView(setup); box.addView(close);
        retry.setOnClickListener(v -> start());
        setup.setOnClickListener(v -> { startActivity(new Intent(this, MainActivity.class)); finish(); });
        close.setOnClickListener(v -> finish());
        setContentView(box);
        try { job = readJob(getIntent()); }
        catch (Exception e) { fail("Couldn't read the slip: " + e.getMessage(), false); return; }
        if (job == null || job.length == 0) { fail("There was nothing to print.", false); return; }
        if (!Perm.ok(this)) { status.setText("Allow \"Nearby devices\" so this app can reach the printer."); Perm.ask(this); buttons(false, false); return; }
        start();
    }

    /** The slip from Scan POS (base64url in the "job" extra), or a shared picture. */
    private byte[] readJob(Intent in) throws Exception {
        Prefs prefs = new Prefs(this);
        if (Intent.ACTION_SEND.equals(in.getAction())) {
            Uri u = in.getParcelableExtra(Intent.EXTRA_STREAM);
            if (u == null) return null;
            Bitmap bmp;
            try (InputStream s = getContentResolver().openInputStream(u)) { bmp = BitmapFactory.decodeStream(s); }
            if (bmp == null) throw new Exception("that file isn't a picture");
            return Escpos.picture(bmp, prefs.paper());
        }
        String data = in.getStringExtra("job");
        if (data == null && in.getData() != null) data = in.getData().getQueryParameter("job");
        slowOverride = "slow".equals(in.getStringExtra("speed"));
        return data == null ? null : Base64.decode(data, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

    private void start() {
        Prefs prefs = new Prefs(this);
        if (prefs.address().isEmpty()) { fail("Choose your printer first (one time only).", true); return; }
        buttons(false, false);
        status.setText("Printing on " + prefs.name() + "…");
        final boolean slow = prefs.slow() || slowOverride;
        new Thread(() -> {
            try {
                Printer.print(this, prefs.address(), job, slow, (sent, total) -> main.post(() -> status.setText("Printing on " + prefs.name() + "… " + (100 * sent / total) + "%")));
                main.post(() -> { status.setText("✓ Printed"); main.postDelayed(this::finish, 700); });
            } catch (SecurityException e) {
                main.post(() -> { status.setText("Allow \"Nearby devices\" so this app can reach the printer."); Perm.ask(this); });
            } catch (Exception e) {
                main.post(() -> fail(e.getMessage(), true));
            }
        }).start();
    }

    private void fail(String msg, boolean canRetry) {
        status.setText(msg);
        buttons(canRetry, true);
    }

    private void buttons(boolean retryOn, boolean others) {
        retry.setVisibility(retryOn ? View.VISIBLE : View.GONE);
        setup.setVisibility(others ? View.VISIBLE : View.GONE);
        close.setVisibility(others ? View.VISIBLE : View.GONE);
    }

    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        if (code != Perm.CODE) return;
        if (Perm.ok(this)) start();
        else fail("Without \"Nearby devices\" this app can't reach the printer. Allow it in Android Settings > Apps > Scan POS Printer > Permissions.", false);
    }
}
