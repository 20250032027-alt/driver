package ph.scanpos.printer;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Tiny helpers to build simple screens in code (no layout files needed). */
final class Ui {
    static final int GREEN = Color.rgb(0x1f, 0x7a, 0x4d);

    static int dp(Context c, int v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }

    static LinearLayout column(Context c, int padDp) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = dp(c, padDp);
        l.setPadding(p, p, p, p);
        return l;
    }

    static TextView text(Context c, String s, float sp, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(c, 6), 0, dp(c, 6));
        return t;
    }

    static Button button(Context c, String s, boolean primary) {
        Button b = new Button(c);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(17);
        b.setMinHeight(dp(c, 52));
        if (primary) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(GREEN);
            bg.setCornerRadius(dp(c, 12));
            b.setBackground(bg);
            b.setTextColor(Color.WHITE);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, 8);
        b.setLayoutParams(lp);
        b.setGravity(Gravity.CENTER);
        return b;
    }
}
