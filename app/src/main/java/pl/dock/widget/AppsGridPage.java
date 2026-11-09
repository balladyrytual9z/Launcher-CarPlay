package pl.dock.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.ArrayList;
import java.util.List;

// Jedna strona siatki aplikacji (5 x 3) w układzie jak w CarPlay
public class AppsGridPage extends View {

    public static final int COLS = 5;
    public static final int ROWS = 3;
    public static final int PER_PAGE = COLS * ROWS;

    public static class Item {
        public String label = "";
        public String pkg = "";
        public AppEntry entry;       // aplikacja z listy Docka (ikona wg ustawień)
        public boolean settings;     // skrót do ustawień Docka
        public String builtinId = "";   // wbudowana ikona (CarPlay, Telefon, Ustawienia)
        public String action = "";      // carplay | phone
        String shown = "";
        float textW;
    }

    public interface Listener {
        void onTap(Item it);

        void onLong(Item it);
    }

    private final float d;
    private final List<Item> items = new ArrayList<Item>();
    private Listener listener;
    private boolean night = true;
    private final Paint pill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint text = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bmp = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF r = new RectF();
    private float sc = 1f, iconSz, colPitch, rowPitch, left, top, pillH, labelMax;
    private int pressed = -1;
    private float downX, downY;
    private boolean longFired;
    private final Handler h = new Handler();
    private final int slop;
    private final Runnable longPress = new Runnable() {
        @Override
        public void run() {
            if (pressed >= 0 && pressed < items.size() && listener != null) {
                longFired = true;
                listener.onLong(items.get(pressed));
                pressed = -1;
                invalidate();
            }
        }
    };

    public AppsGridPage(Context c) {
        super(c);
        d = c.getResources().getDisplayMetrics().density;
        slop = ViewConfiguration.get(c).getScaledTouchSlop();
        text.setTextAlign(Paint.Align.CENTER);
    }

    public void setListener(Listener l) {
        listener = l;
    }

    public void setItems(List<Item> list, boolean night) {
        items.clear();
        items.addAll(list);
        this.night = night;
        measureLabels();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int hh, int ow, int oh) {
        super.onSizeChanged(w, hh, ow, oh);
        // Keep grid clear of the dock overlay while still allowing liquid-glass underlap
        float dockInset = Ui.dp(getContext(), Ui.DOCK_OFFSET_DP) * 0.55f;
        boolean dockLeft = Ui.dockLeft(getContext());
        float usableW = w - dockInset;
        sc = Math.min(1f, Math.min(usableW / (773f * d), hh / (520f * d)));
        iconSz = 85f * d * sc;
        colPitch = 162f * d * sc;
        rowPitch = 160f * d * sc;
        float gridW = 4 * colPitch + iconSz;
        left = (dockLeft ? dockInset : 0) + (usableW - gridW) / 2f;
        top = 34f * d * sc;
        pillH = 29f * d * sc;
        labelMax = colPitch - 14f * d * sc;
        text.setTextSize(18f * getResources().getDisplayMetrics().scaledDensity * sc);
        measureLabels();
    }

    private void measureLabels() {
        if (labelMax <= 0) return;
        for (Item it : items) {
            it.shown = TextUtils.ellipsize(it.label, text, labelMax - 20f * d * sc,
                    TextUtils.TruncateAt.END).toString();
            it.textW = text.measureText(it.shown);
        }
    }

    private Bitmap bitmapFor(Item it) {
        Context c = getContext();
        if (it.builtinId.length() > 0) return Icons.builtin(c, it.builtinId);
        if (it.settings) return Icons.builtin(c, "settings");
        if (it.entry != null) return Icons.forEntry(c, it.entry);
        return Icons.forPackage(c, it.pkg);
    }

    @Override
    protected void onDraw(Canvas cv) {
        pill.setColor(night ? 0xB31E1E20 : 0xCCFFFFFF);
        text.setColor(night ? 0xFFFFFFFF : 0xFF111114);
        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            int row = i / COLS;
            int col = i % COLS;
            float x = left + col * colPitch;
            float y = top + row * rowPitch;
            Bitmap b = bitmapFor(it);
            float s = (pressed == i) ? 0.93f : 1f;
            float inset = iconSz * (1f - s) / 2f;
            bmp.setAlpha(pressed == i ? 190 : 255);
            r.set(x + inset, y + inset, x + iconSz - inset, y + iconSz - inset);
            cv.drawBitmap(b, null, r, bmp);

            float pw = it.textW + 24f * d * sc;
            float px = x + iconSz / 2f - pw / 2f;
            float py = y + iconSz + 7f * d * sc;
            r.set(px, py, px + pw, py + pillH);
            cv.drawRoundRect(r, pillH / 2f, pillH / 2f, pill);
            cv.drawText(it.shown, x + iconSz / 2f,
                    py + pillH / 2f - (text.descent() + text.ascent()) / 2f, text);
        }
    }

    private int hit(float x, float y) {
        for (int i = 0; i < items.size(); i++) {
            int row = i / COLS;
            int col = i % COLS;
            float cx = left + col * colPitch - 10f * d * sc;
            float cy = top + row * rowPitch - 6f * d * sc;
            float cw = iconSz + 20f * d * sc;
            float ch = iconSz + 7f * d * sc + pillH + 12f * d * sc;
            if (x >= cx && x <= cx + cw && y >= cy && y <= cy + ch) return i;
        }
        return -1;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                longFired = false;
                pressed = hit(downX, downY);
                if (pressed < 0) return false;
                invalidate();
                h.postDelayed(longPress, ViewConfiguration.getLongPressTimeout());
                return true;
            case MotionEvent.ACTION_MOVE:
                if (pressed >= 0 && (Math.abs(e.getX() - downX) > slop
                        || Math.abs(e.getY() - downY) > slop)) {
                    h.removeCallbacks(longPress);
                    pressed = -1;
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                h.removeCallbacks(longPress);
                int p = pressed;
                pressed = -1;
                invalidate();
                if (p >= 0 && !longFired && p < items.size() && listener != null) {
                    listener.onTap(items.get(p));
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                h.removeCallbacks(longPress);
                pressed = -1;
                invalidate();
                return true;
            default:
                return true;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        h.removeCallbacks(longPress);
    }
}





