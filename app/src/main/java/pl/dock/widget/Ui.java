


package pl.dock.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

// Wspólne elementy interfejsu w stylu CarPlay / Apple
public class Ui {

    // Okna ustawień i podręczne zaczynają się obok docka, więc dock zostaje widoczny i klikalny
    public static final int DOCK_OFFSET_DP = 77;

    public static int dp(Context c, float v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static boolean dockLeft(Context c) {
        return !"right".equals(Prefs.dockSide(c));
    }

    public static int windowWidth(Context c) {
        DisplayMetrics dm = c.getResources().getDisplayMetrics();
        return dm.widthPixels - dp(c, DOCK_OFFSET_DP);
    }

    public static void dockWindow(Window w, Context c) {
        try {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout(windowWidth(c), ViewGroup.LayoutParams.MATCH_PARENT);
            w.setGravity((dockLeft(c) ? Gravity.END : Gravity.START) | Gravity.TOP);
            w.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        } catch (Throwable ignored) {
        }
    }

    public static GradientDrawable round(int color, float radiusDp, Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    // tło ekranu ustawień (zaokrąglone od strony docka)
    public static GradientDrawable pageBg(Context c, boolean night) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                night ? new int[]{0xFF161617, 0xFF000000} : new int[]{0xFFF2F2F7, 0xFFE2E2E8});
        float r = dp(c, 26);
        g.setCornerRadii(dockLeft(c) ? new float[]{r, r, 0, 0, 0, 0, r, r}
                : new float[]{0, 0, r, r, r, r, 0, 0});
        return g;
    }

    // karta z listą (jak w ustawieniach CarPlay)
    public static GradientDrawable cardBg(Context c, boolean night) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                night ? new int[]{0xFF232325, 0xFF2F2F32, 0xFF232325}
                        : new int[]{0xFFFFFFFF, 0xFFFFFFFF});
        g.setCornerRadius(dp(c, 24));
        return g;
    }

    // okno podręczne (jak wyskakujące menu Apple)
    public static GradientDrawable popupBg(Context c, boolean night) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(night ? 0xF20E0E0F : 0xF2F5F5F7);
        g.setCornerRadius(dp(c, 34));
        g.setStroke(Math.max(1, dp(c, 1)), night ? 0x33FFFFFF : 0x22000000);
        return g;
    }

    // karta: polprzezroczyste tlo (jak iOS 18 widgets) + delikatny gradient u gory
    public static void ringCard(Canvas cv, RectF r, float radius, float stroke, boolean night,
                                boolean ring) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        // noc: ~70% czerni, dzien: ~75% bieli
        p.setColor(night ? 0xB3101012 : 0xBFFFFFFF);
        cv.drawRoundRect(r, radius, radius, p);

        // delikatny gradient u gory (efekt "szkla")
        p.setShader(new LinearGradient(r.left, r.top, r.left, r.top + r.height() * 0.45f,
                new int[]{0x40FFFFFF, 0x00FFFFFF},
                new float[]{0f, 1f}, Shader.TileMode.CLAMP));
        cv.drawRoundRect(r, radius, radius, p);
        p.setShader(null);

        if (!ring) return;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(stroke);
        p.setShader(new LinearGradient(r.left, r.top, r.right, r.bottom,
                night ? new int[]{0xCCFFFFFF, 0x4DFFFFFF, 0x14FFFFFF, 0x33FFFFFF}
                        : new int[]{0x99FFFFFF, 0x55808090, 0x22808090, 0x40808090},
                new float[]{0f, 0.3f, 0.75f, 1f}, Shader.TileMode.CLAMP));
        RectF in = new RectF(r.left + stroke / 2, r.top + stroke / 2, r.right - stroke / 2,
                r.bottom - stroke / 2);
        cv.drawRoundRect(in, radius - stroke / 2, radius - stroke / 2, p);
        p.setShader(null);
    }

    // ------------------------------------------------------------ strzałka / znacznik
    public static class Chev extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int dir;   // 0 >, 1 ^, 2 v, 3 <, 4 ✓

        public Chev(Context c, int dir, int color) {
            super(c);
            this.dir = dir;
            p.setColor(color);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            p.setStrokeWidth(dp(c, 2.4f));
        }

        @Override
        protected void onDraw(Canvas cv) {
            float w = getWidth();
            float h = getHeight();
            float s = p.getStrokeWidth() / 2f + 1f;
            Path path = new Path();
            if (dir == 0) {
                path.moveTo(s, s);
                path.lineTo(w - s, h / 2f);
                path.lineTo(s, h - s);
            } else if (dir == 1) {
                path.moveTo(s, h - s);
                path.lineTo(w / 2f, s);
                path.lineTo(w - s, h - s);
            } else if (dir == 2) {
                path.moveTo(s, s);
                path.lineTo(w / 2f, h - s);
                path.lineTo(w - s, s);
            } else if (dir == 3) {
                path.moveTo(w - s, s);
                path.lineTo(s, h / 2f);
                path.lineTo(w - s, h - s);
            } else {
                path.moveTo(s, h * 0.55f);
                path.lineTo(w * 0.38f, h - s);
                path.lineTo(w - s, s);
            }
            cv.drawPath(path, p);
        }
    }

    // ------------------------------------------------------------ przełącznik jak w iOS
    public static class IosSwitch extends View {
        public interface Listener {
            void onChanged(boolean on);
        }

        private boolean on;
        private final int offColor;
        private final Listener listener;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        public IosSwitch(Context c, boolean on, int offColor, Listener l) {
            super(c);
            this.on = on;
            this.offColor = offColor;
            this.listener = l;
            setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    IosSwitch.this.on = !IosSwitch.this.on;
                    invalidate();
                    if (listener != null) listener.onChanged(IosSwitch.this.on);
                }
            });
        }

        @Override
        protected void onDraw(Canvas cv) {
            float w = getWidth();
            float h = getHeight();
            p.setStyle(Paint.Style.FILL);
            p.setColor(on ? 0xFF30D158 : offColor);
            cv.drawRoundRect(new RectF(0, 0, w, h), h / 2f, h / 2f, p);
            float m = h * 0.08f;
            float r = h / 2f - m;
            float cx = on ? w - h / 2f : h / 2f;
            p.setColor(0xFFFFFFFF);
            cv.drawCircle(cx, h / 2f, r, p);
        }
    }
}





