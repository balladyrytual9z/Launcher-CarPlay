package pl.dock.widget;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;

public class NightShiftService extends Service {

    private static final long FADE_MS = 800L;

    private WindowManager wm;
    private FilterView overlay;
    private android.animation.ValueAnimator anim;
    private float currentAlpha = 0f;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        goForeground();
        showOverlay();
        animateTo(targetStrengthFraction(), FADE_MS);
        return START_NOT_STICKY;
    }

    private float targetStrengthFraction() {
        int strength = Math.max(5, Math.min(80, Prefs.nightShiftStrength(this)));
        return strength / 100f;
    }

    private void applyFraction(float f) {
        currentAlpha = f;
        if (overlay != null) overlay.setWarmthFraction(f);
    }

    private void animateTo(float target, long duration) {
        if (anim != null) anim.cancel();
        if (overlay == null) return;
        if (duration <= 0 || Prefs.reduceMotion(this)) {
            applyFraction(target);
            return;
        }
        anim = android.animation.ValueAnimator.ofFloat(currentAlpha, target);
        anim.setDuration(duration);
        anim.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(android.animation.ValueAnimator a) {
                applyFraction((Float) a.getAnimatedValue());
            }
        });
        anim.start();
    }

    private void goForeground() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel("dock_ns", "Night Shift",
                    NotificationManager.IMPORTANCE_MIN));
        }
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, "dock_ns") : new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_menu_view).setContentTitle("Night Shift");
        startForeground(2, b.build());
    }

    private void showOverlay() {
        try {
            if (wm == null) wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            if (overlay == null) {
                overlay = new FilterView(this);
                overlay.setClipToOutline(true);
                overlay.setOutlineProvider(new android.view.ViewOutlineProvider() {
                    @Override
                    public void getOutline(View v, android.graphics.Outline o) {
                        float r = Prefs.cornerRadius(NightShiftService.this)
                                * getResources().getDisplayMetrics().density;
                        o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), r);
                    }
                });
                int type = Build.VERSION.SDK_INT >= 26
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
                WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT, type,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        PixelFormat.TRANSLUCENT);
                wm.addView(overlay, lp);
                applyFraction(currentAlpha);
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onDestroy() {
        if (anim != null) anim.cancel();
        try {
            if (wm != null && overlay != null) wm.removeView(overlay);
        } catch (Throwable ignored) {}
        overlay = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    /**
     * Two layers: black (dimming) + color computed from kelvin (warmth).
     * Color is computed dynamically: 6500 K (neutral) -> 2200 K (candle).
     * Zero allocations in onDraw - two plain rectangles are painted.
     */
    private static final class FilterView extends View {

        private static final float DIM_MAX  = 0.30f;
        private static final float WARM_MAX = 0.50f;

        private float warmth = 0f;
        private final Paint dimPaint  = new Paint();
        private final Paint warmPaint = new Paint();

        FilterView(Context c) {
            super(c);
            dimPaint.setColor(0xFF000000);
            warmPaint.setColor(0xFFFFB300);
            setWillNotDraw(false);
        }

        void setWarmthFraction(float f) {
            f = Math.max(0f, Math.min(1f, f));
            if (Math.abs(f - warmth) < 0.0005f) return;
            warmth = f;

            double kelvin = ColorTemp.fractionToKelvin(f);
            int[] rgb = ColorTemp.kelvinToRgb(kelvin);
            warmPaint.setColor(Color.argb(255, rgb[0], rgb[1], rgb[2]));

            dimPaint.setAlpha(Math.round(DIM_MAX  * f * 255f));
            warmPaint.setAlpha(Math.round(WARM_MAX * f * 255f));
            invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            if (warmth <= 0f) return;
            c.drawRect(0f, 0f, getWidth(), getHeight(), dimPaint);
            c.drawRect(0f, 0f, getWidth(), getHeight(), warmPaint);
        }
    }
}
