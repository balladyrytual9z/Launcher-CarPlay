


package pl.dock.widget;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

// Okna podręczne w stylu Apple (nie na cały ekran, obok docka)
public class Popups {

    public interface OnSel {
        void on(int index);
    }

    public static int txt(boolean night) {
        return night ? 0xFFFFFFFF : 0xFF111114;
    }

    public static int sub(boolean night) {
        return night ? 0xFF9A9AA0 : 0xFF6C6C70;
    }

    public static TextView text(Activity a, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(a);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    // zrzut ekranu aktywności rozmyty – tło panelu "ze szkła"
    public static android.graphics.Bitmap glassSnap(Activity a) {
        try {
            android.view.View dec = a.getWindow().getDecorView();
            int k = 6;
            android.graphics.Bitmap b = android.graphics.Bitmap.createBitmap(
                    Math.max(1, dec.getWidth() / k), Math.max(1, dec.getHeight() / k),
                    android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas cv = new android.graphics.Canvas(b);
            cv.scale(1f / k, 1f / k);
            dec.draw(cv);
            Blur.blur(b, 3, 3);
            return b;
        } catch (Throwable t) {
            return null;
        }
    }

    public static Dialog panel(Activity a, View content, int widthDp, int gravity) {
        boolean night = Look.night(a);
        final Dialog d = new Dialog(a, android.R.style.Theme_Translucent_NoTitleBar);
        Ui.dockWindow(d.getWindow(), a);
        FrameLayout root = new FrameLayout(a);
        root.setBackgroundColor(0x55000000);
        root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });
        LinearLayout panel = new LinearLayout(a);
        panel.setOrientation(LinearLayout.VERTICAL);
        android.graphics.Bitmap glass = glassSnap(a);
        if (glass != null) {
            android.widget.ImageView iv = new android.widget.ImageView(a);
            iv.setScaleType(android.widget.ImageView.ScaleType.FIT_XY);
            iv.setImageBitmap(glass);
            panel.addView(iv, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            View tint = new View(a);
            tint.setBackgroundColor(night ? 0x40000000 : 0x30FFFFFF);
            panel.addView(tint, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        panel.setBackground(Ui.popupBg(a, night));
        panel.setClickable(true);
        panel.setElevation(Ui.dp(a, 12));
        panel.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        int w = Math.min(Ui.dp(a, widthDp), Ui.windowWidth(a) - Ui.dp(a, 24));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                w, ViewGroup.LayoutParams.WRAP_CONTENT, gravity);
        int m = Ui.dp(a, 12);
        lp.setMargins(m, m, m, m);
        root.addView(panel, lp);
        d.setContentView(root);
        return d;
    }

    private static View hairline(Activity a, boolean night) {
        View v = new View(a);
        v.setBackgroundColor(night ? 0x33FFFFFF : 0x22000000);
        return v;
    }

    // lista pozycji do wyboru
    public static void menu(final Activity a, String title, String[] items, final OnSel cb) {
        boolean night = Look.night(a);
        final Dialog[] ref = new Dialog[1];
        LinearLayout c = new LinearLayout(a);
        c.setOrientation(LinearLayout.VERTICAL);
        TextView t = text(a, title, 15, sub(night), true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(Ui.dp(a, 20), Ui.dp(a, 16), Ui.dp(a, 20), Ui.dp(a, 10));
        c.addView(t);
        LinearLayout list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            if (i > 0) {
                list.addView(hairline(a, night), new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 1));
            }
            TextView r = text(a, items[i], 20, txt(night), false);
            r.setPadding(Ui.dp(a, 24), Ui.dp(a, 16), Ui.dp(a, 24), Ui.dp(a, 16));
            r.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ref[0].dismiss();
                    cb.on(idx);
                }
            });
            list.addView(r);
        }
        ScrollView s = new ScrollView(a);
        s.addView(list);
        int maxH = a.getResources().getDisplayMetrics().heightPixels - Ui.dp(a, 170);
        c.addView(s, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                items.length > 6 ? maxH : ViewGroup.LayoutParams.WRAP_CONTENT));
        ref[0] = panel(a, c, 480, Gravity.CENTER);
        ref[0].show();
    }

    // potwierdzenie czynności nieodwracalnej
    public static void confirm(final Activity a, String title, String msg, String okLabel,
                               final Runnable ok) {
        boolean night = Look.night(a);
        final Dialog[] ref = new Dialog[1];
        LinearLayout c = new LinearLayout(a);
        c.setOrientation(LinearLayout.VERTICAL);
        TextView t = text(a, title, 20, txt(night), true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(Ui.dp(a, 22), Ui.dp(a, 22), Ui.dp(a, 22), Ui.dp(a, 6));
        c.addView(t);
        if (msg != null && msg.length() > 0) {
            TextView m = text(a, msg, 16, sub(night), false);
            m.setGravity(Gravity.CENTER);
            m.setPadding(Ui.dp(a, 22), 0, Ui.dp(a, 22), Ui.dp(a, 16));
            c.addView(m);
        }
        c.addView(hairline(a, night), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1));
        TextView yes = text(a, okLabel, 20, 0xFFFF453A, true);
        yes.setGravity(Gravity.CENTER);
        yes.setPadding(0, Ui.dp(a, 16), 0, Ui.dp(a, 16));
        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ref[0].dismiss();
                ok.run();
            }
        });
        c.addView(yes);
        c.addView(hairline(a, night), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1));
        TextView no = text(a, "Anuluj", 20, txt(night), false);
        no.setGravity(Gravity.CENTER);
        no.setPadding(0, Ui.dp(a, 16), 0, Ui.dp(a, 16));
        no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ref[0].dismiss();
            }
        });
        c.addView(no);
        ref[0] = panel(a, c, 440, Gravity.CENTER);
        ref[0].show();
    }
}





