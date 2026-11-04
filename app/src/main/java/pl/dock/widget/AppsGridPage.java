


package pl.dock.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.SystemClock;
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

        void onRemove(Item it);
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
    private int removingIdx = -1;
    private float removingScale = 1f;
    private long removingStart = 0L;
    private float downX, downY;
    private boolean longFired;
    private final Handler h = new Handler();
    private final int slop;
    // ---------- tryb edycji (iOS-style jiggle) ----------
    private boolean editMode;
    private final float[] wobblePhase = new float[PER_PAGE];
    private long lastWobble;
    private final Paint badge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Runnable wobbleTick = new Runnable() {
        @Override
        public void run() {
            if (!editMode) return;
            long now = SystemClock.uptimeMillis();
            if (lastWobble != 0) {
                for (int i = 0; i < PER_PAGE; i++) {
                    wobblePhase[i] = (wobblePhase[i] + (now - lastWobble) / 400f) % 1f;
                }
            }
            lastWobble = now;
            invalidate();
            h.postDelayed(this, 16);
        }
    };

    public void enterEditMode() {
        if (editMode) return;
        editMode = true;
        lastWobble = 0;
        EditModeController.get().setEditMode(true);
        h.postDelayed(wobbleTick, 16);
        invalidate();
    }

    public void exitEditMode() {
        if (!editMode) return;
        editMode = false;
        h.removeCallbacks(wobbleTick);
        lastWobble = 0;
        EditModeController.get().setEditMode(false);
        invalidate();
    }

    public boolean isEditing() {
        return editMode;
    }

    private float itemX(int i) {
        return left + (i % COLS) * colPitch;
    }

    private float itemY(int i) {
        return top + (i / COLS) * rowPitch;
    }

    private void drawRemoveBadge(Canvas cv, float cx, float cy) {
        float rad = 14f * d * sc;
        badge.setStyle(Paint.Style.FILL);
        badge.setColor(0xE61C1C1E);
        cv.drawCircle(cx, cy, rad, badge);
        badge.setStyle(Paint.Style.STROKE);
        badge.setStrokeWidth(3f * d * sc);
        badge.setStrokeCap(Paint.Cap.ROUND);
        badge.setColor(0xFFFFFFFF);
        float k = rad * 0.42f;
        cv.drawLine(cx - k, cy - k, cx + k, cy + k, badge);
        cv.drawLine(cx - k, cy + k, cx + k, cy - k, badge);
    }

    private int hitRemoveBadge(float x, float y) {
        float rad = 16f * d * sc;
        for (int i = items.size() - 1; i >= 0; i--) {
            float cx = itemX(i) + iconSz;
            float cy = itemY(i);
            if ((x - cx) * (x - cx) + (y - cy) * (y - cy) <= rad * rad) return i;
        }
        return -1;
    }
    private final Runnable longPress = new Runnable() {
        @Override
        public void run() {
            if (pressed >= 0 && pressed < items.size() && listener != null) {
                longFired = true;
                if (!editMode) enterEditMode();
                listener.onLong(items.get(pressed));
                pressed = -1;
                invalidate();
            }
        }
    };

    // long-press on empty background enters edit mode (iOS 26 style)
    private final Runnable bgLongPress = new Runnable() {
        @Override
        public void run() {
            if (pressed < 0 && !editMode) {
                enterEditMode();
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
        sc = Math.min(1f, Math.min(w / (773f * d), hh / (520f * d)));
        iconSz = 85f * d * sc;
        colPitch = 162f * d * sc;
        rowPitch = 160f * d * sc;
        left = (w - (4 * colPitch + iconSz)) / 2f;
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
            if (i == removingIdx) {
                // animated icon: scale + alpha shrink linearly over 220 ms
                float t = (android.os.SystemClock.uptimeMillis() - removingStart) / 220f;
                if (t > 1f) t = 1f;
                removingScale = 1f - t;
                if (removingScale <= 0.01f) continue;
            }
            Item it = items.get(i);
            int row = i / COLS;
            int col = i % COLS;
            float x = left + col * colPitch;
            float y = top + row * rowPitch;
            Bitmap b = bitmapFor(it);
            float s = (pressed == i) ? 0.93f : 1f;
            if (i == removingIdx) s *= removingScale;
            float inset = iconSz * (1f - s) / 2f;
            int alpha = pressed == i ? 190 : 255;
            if (i == removingIdx) alpha = (int) (255 * removingScale);
            bmp.setAlpha(alpha);
            r.set(x + inset, y + inset, x + iconSz - inset, y + iconSz - inset);
            if (editMode) {
                cv.save();
                float ang = (float) Math.sin(wobblePhase[i % PER_PAGE] * Math.PI * 2) * 1.8f;
                cv.rotate(ang, x + iconSz / 2f, y + iconSz / 2f);
                cv.drawBitmap(b, null, r, bmp);
                cv.restore();
            } else {
                cv.drawBitmap(b, null, r, bmp);
            }

            float pw = it.textW + 24f * d * sc;
            float px = x + iconSz / 2f - pw / 2f;
            float py = y + iconSz + 7f * d * sc;
            r.set(px, py, px + pw, py + pillH);
            cv.drawRoundRect(r, pillH / 2f, pillH / 2f, pill);
            cv.drawText(it.shown, x + iconSz / 2f,
                    py + pillH / 2f - (text.descent() + text.ascent()) / 2f, text);
            if (editMode && i != removingIdx) drawRemoveBadge(cv, x + iconSz, y);
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
                if (pressed < 0) {
                    // long-press on the empty background = edit mode
                    if (!editMode) {
                        h.postDelayed(bgLongPress, ViewConfiguration.getLongPressTimeout());
                    }
                    return true;
                }
                invalidate();
                if (!editMode) {
                    h.postDelayed(longPress, ViewConfiguration.getLongPressTimeout());
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (pressed >= 0 && (Math.abs(e.getX() - downX) > slop
                        || Math.abs(e.getY() - downY) > slop)) {
                    h.removeCallbacks(longPress);
                    pressed = -1;
                    invalidate();
                }
                if (Math.abs(e.getX() - downX) > slop
                        || Math.abs(e.getY() - downY) > slop) {
                    h.removeCallbacks(bgLongPress);
                }
                return true;
            case MotionEvent.ACTION_UP:
                h.removeCallbacks(longPress);
                h.removeCallbacks(bgLongPress);
                int p = pressed;
                pressed = -1;
                invalidate();
                if (editMode) {
                    int bIdx = hitRemoveBadge(e.getX(), e.getY());
                    if (bIdx >= 0 && bIdx < items.size() && listener != null) {
                        final Item removed = items.get(bIdx);
                        animateRemoveBadge(removed);
                        h.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (listener != null) listener.onRemove(removed);
                            }
                        }, 220);
                        return true;
                    }
                    if (p < 0) exitEditMode();
                    return true;
                }
                if (p >= 0 && !longFired && p < items.size() && listener != null) {
                    listener.onTap(items.get(p));
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                h.removeCallbacks(longPress);
                h.removeCallbacks(bgLongPress);
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
        h.removeCallbacks(bgLongPress);
        h.removeCallbacks(wobbleTick);
    }

    // iOS 26: icon shrinks and fades out (220 ms), then the callback removes it
    private void animateRemoveBadge(Item it) {
        final int idx = items.indexOf(it);
        if (idx < 0) return;
        removingIdx = idx;
        removingStart = android.os.SystemClock.uptimeMillis();
        h.postDelayed(new Runnable() {
            @Override public void run() {
                removingIdx = -1;
                invalidate();
            }
        }, 220);
    }

    @Override
    public void setVisibility(int v) {
        super.setVisibility(v);
        if (v != VISIBLE && editMode) exitEditMode();
    }
}





