package ph.scanpos.printer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/** Small ESC/POS helpers: the test page, and turning a shared picture (a receipt image) into printer commands. */
final class Escpos {
    static final byte ESC = 0x1b, GS = 0x1d;

    static byte[] testPage(int paperMm, boolean slow) {
        int cols = paperMm == 80 ? 48 : 32;
        StringBuilder sb = new StringBuilder();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(ESC); out.write('@');
        out.write(ESC); out.write('a'); out.write(1);                 // centre
        out.write(GS); out.write('!'); out.write(1);                  // double height
        write(out, "SCAN POS PRINTER\n");
        out.write(GS); out.write('!'); out.write(0);
        write(out, "TEST PRINT\n");
        out.write(ESC); out.write('a'); out.write(0);
        sb.setLength(0);
        for (int i = 0; i < cols; i++) sb.append('-');
        write(out, sb + "\n");
        write(out, pair("Paper", paperMm + " mm", cols));
        write(out, pair("Speed", slow ? "Slow" : "Normal", cols));
        write(out, pair("Connection", "Classic Bluetooth", cols));
        for (int i = 1; i <= 6; i++) write(out, pair("Test line " + i + " of 6", String.format("PHP %d.00", 95 * i), cols));
        write(out, sb + "\n");
        out.write(ESC); out.write('a'); out.write(1);
        write(out, "If you can read the last line,\nprinting works!\n");
        out.write(ESC); out.write('E'); out.write(1);
        write(out, "*** END OF TEST ***\n");
        out.write(ESC); out.write('E'); out.write(0);
        write(out, "\n\n\n");
        return out.toByteArray();
    }

    static String pair(String left, String right, int cols) {
        int room = cols - right.length() - 1;
        if (left.length() > room) left = left.substring(0, Math.max(1, room - 1)) + ".";
        StringBuilder sb = new StringBuilder(left);
        while (sb.length() + right.length() < cols) sb.append(' ');
        return sb.append(right).append('\n').toString();
    }

    private static void write(ByteArrayOutputStream out, String s) {
        byte[] b = s.getBytes(StandardCharsets.US_ASCII);
        out.write(b, 0, b.length);
    }

    /** A picture as printer commands: scaled to the paper width, dithered to black and white, in strips of 24 dot rows. */
    static byte[] picture(Bitmap src, int paperMm) {
        int w = paperMm == 80 ? 576 : 384;
        int h = Math.max(1, Math.round(src.getHeight() * (w / (float) src.getWidth())));
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.drawColor(Color.WHITE);
        c.drawBitmap(Bitmap.createScaledBitmap(src, w, h, true), 0, 0, null);
        float[] grey = new float[w * h];
        int[] px = new int[w * h];
        bmp.getPixels(px, 0, w, 0, 0, w, h);
        for (int i = 0; i < px.length; i++) {
            int p = px[i];
            grey[i] = 0.299f * Color.red(p) + 0.587f * Color.green(p) + 0.114f * Color.blue(p);
        }
        boolean[] black = new boolean[w * h];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {          // Floyd–Steinberg dithering
            int i = y * w + x; float old = grey[i]; float v = old < 128 ? 0 : 255; float err = old - v;
            black[i] = v == 0;
            if (x + 1 < w) grey[i + 1] += err * 7 / 16;
            if (y + 1 < h) { if (x > 0) grey[i + w - 1] += err * 3 / 16; grey[i + w] += err * 5 / 16; if (x + 1 < w) grey[i + w + 1] += err / 16; }
        }
        int bpr = (w + 7) / 8;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(ESC); out.write('@');
        out.write(ESC); out.write('a'); out.write(1);
        for (int y0 = 0; y0 < h; y0 += 24) {
            int sh = Math.min(24, h - y0);
            out.write(GS); out.write('v'); out.write('0'); out.write(0);
            out.write(bpr & 0xff); out.write(bpr >> 8); out.write(sh & 0xff); out.write(sh >> 8);
            for (int y = y0; y < y0 + sh; y++) {
                byte[] row = new byte[bpr];
                for (int x = 0; x < w; x++) if (black[y * w + x]) row[x >> 3] |= (byte) (0x80 >> (x & 7));
                out.write(row, 0, bpr);
            }
        }
        write(out, "\n\n\n");
        return out.toByteArray();
    }
}
