package pl.dock.widget;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * Cienki, przezroczysty pasek nad dolną krawędzią ekranu (TYPE_APPLICATION_OVERLAY).
 * Gest w górę (> 48 dp) otwiera Centrum Łączności także nad innymi aplikacjami.
 * Włączany/wyłączany w ustawieniach (Prefs.ccSwipe) i po boocie.
 */
public class ControlCenterGestureService extends Service {

    private WindowManager wm;
    private View strip;

    public static void start(Context c) {
        if (!Prefs.ccSwipe(c)) return;
        try {
            c.startService(new Intent(c, ControlCenterGestureService.class));
        } catch (Throwable ignored) {
        }
    }

    public static void stop(Context c) {
        try {
            c.stopService(new Intent(c, ControlCenterGestureService.class));
        } catch (Throwable ignored) {
        }
    }

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        strip = new View(this) {
            private float downY;

            @Override
            public boolean onTouchEvent(MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downY = e.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        float dy = downY - e.getRawY();
                        if (dy > 48f * getResources().getDisplayMetrics().density) {
                            Intent i = new Intent(getContext(), ConnectivityActivity.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            i.putExtra("swipe_x", e.getRawX());
                            try {
                                getContext().startActivity(i);
                            } catch (Throwable ignored) {
                            }
                        }
                        return true;
                    default:
                        return true;
                }
            }
        };
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                (int) (20f * getResources().getDisplayMetrics().density),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        try {
            wm.addView(strip, lp);
        } catch (Throwable ignored) {
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent i, int f, int id) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (strip != null && wm != null) wm.removeView(strip);
        } catch (Throwable ignored) {
        }
    }
}
