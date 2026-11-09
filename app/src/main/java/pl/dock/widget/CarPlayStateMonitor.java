package pl.dock.widget;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.Locale;

/**
 * Sensor Fusion CarPlay state machine.
 * Stany: DISCONNECTED, CONNECTING, CONNECTED_BG, CONNECTED_FG, ERROR.
 */
public final class CarPlayStateMonitor {

    public static final int DISCONNECTED = 0;
    public static final int CONNECTING = 1;
    public static final int CONNECTED_BG = 2;
    public static final int CONNECTED_FG = 3;
    public static final int ERROR = 4;

    private static volatile int currentState = DISCONNECTED;
    private static volatile String lastMessage = "";
    private static Handler main = new Handler(Looper.getMainLooper());
    private static boolean polling = false;
    private static Runnable pollRunnable;
    private static Context appCtx;

    public interface Listener { void onStateChanged(int newState, String message); }
    private static Listener listener;

    public static void setListener(Listener l) { listener = l; }

    public static int get() { return currentState; }

    public static String message() { return lastMessage; }

    public static void start(Context c) {
        appCtx = c.getApplicationContext();
        if (polling) return;
        polling = true;
        pollRunnable = new Runnable() {
            @Override public void run() {
                if (!polling) return;
                evaluate();
                main.postDelayed(this, 1000);
            }
        };
        main.post(pollRunnable);
    }

    public static void stop() {
        polling = false;
        if (pollRunnable != null) main.removeCallbacks(pollRunnable);
    }

    /** Wywołaj ręcznie po każdym zdarzeniu systemowym (powiadomienie, USB, media). */
    public static void evaluate() {
        if (appCtx == null) return;
        String pkg = Prefs.diplayPkg(appCtx);

        // 1. NotificationListener
        String[] notif = DockNotificationListener.notificationText(pkg);
        boolean notifActive = notif != null;
        String notifText = notif == null ? "" : (notif[0] + " " + notif[1]).toLowerCase(Locale.US);
        boolean notifError = notifText.contains("error") || notifText.contains("błąd")
                || notifText.contains("odmowa") || notifText.contains("failures");
        boolean notifConnecting = notifText.contains("connecting") || notifText.contains("waiting")
                || notifText.contains("searching") || notifText.contains("łączenie")
                || notifText.contains("pairing");

        // 2. MediaSession
        boolean mediaActive = false;
        try {
            MediaSessionManager msm = (MediaSessionManager)
                    appCtx.getSystemService(Context.MEDIA_SESSION_SERVICE);
            List<MediaController> ctrls = msm.getActiveSessions(
                    new ComponentName(appCtx, DockNotificationListener.class));
            if (ctrls != null) {
                for (MediaController mc : ctrls) {
                    if (!pkg.equals(mc.getPackageName())) continue;
                    PlaybackState ps = mc.getPlaybackState();
                    if (ps != null && ps.getState() != PlaybackState.STATE_STOPPED
                            && ps.getState() != PlaybackState.STATE_NONE) {
                        mediaActive = true;
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 3. Foreground
        boolean fgActive = recentActivityForeground(appCtx, pkg);

        // 4. USB
        boolean usbPresent = CarplayState.appleUsbPresent(appCtx);

        // Maszyna stanów
        int newState;
        String msg;
        if (notifError) {
            newState = ERROR;
            msg = "Błąd CarPlay (sprawdź powiadomienie DiPlay)";
        } else if (mediaActive) {
            newState = fgActive ? CONNECTED_FG : CONNECTED_BG;
            msg = fgActive ? "CarPlay połączony (na wierzchu)" : "CarPlay połączony (w tle)";
        } else if (notifActive || notifConnecting || usbPresent) {
            newState = CONNECTING;
            msg = "Łączenie z iPhone...";
        } else {
            newState = DISCONNECTED;
            msg = "CarPlay nieaktywny";
        }

        if (newState != currentState) {
            currentState = newState;
            lastMessage = msg;
            if (listener != null) {
                final int st = newState;
                final String m = msg;
                main.post(new Runnable() {
                    @Override public void run() {
                        if (listener != null) listener.onStateChanged(st, m);
                    }
                });
            }
        }
    }

    private static boolean recentActivityForeground(Context c, String pkg) {
        if (Build.VERSION.SDK_INT < 22) return false;
        UsageStatsManager usm = (UsageStatsManager) c.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) return false;
        long now = System.currentTimeMillis();
        UsageEvents ev = usm.queryEvents(now - 8000, now);
        if (ev == null) return false;
        UsageEvents.Event e = new UsageEvents.Event();
        String[] hosts = { pkg, "com.shilapi.xcertplay", "com.carplay.dipower", "com.shihab.diplay" };
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e);
            if (e.getEventType() != UsageEvents.Event.MOVE_TO_FOREGROUND
                    && e.getEventType() != 1) continue;
            String pn = e.getPackageName();
            String cn = e.getClassName();
            if (pn == null) continue;
            if (cn == null) cn = "";
            boolean pkgMatch = false;
            for (String h : hosts) if (pn.equals(h) || pn.startsWith(h)) { pkgMatch = true; break; }
            if (!pkgMatch) continue;
            if (cn.contains("CarPlayHost") || cn.contains("CarPlay")
                    || cn.contains("Projection") || cn.contains("HostActivity")
                    || cn.contains("SameTaskCarPlay")) return true;
        }
        return false;
    }

    /** Kolor ikony docka dla danego stanu. */
    public static int color(int state, int fg) {
        switch (state) {
            case CONNECTED_FG: return 0xFF30D158;   // zielony
            case CONNECTED_BG: return 0xFF30D158;
            case CONNECTING:   return 0xFFFF9F0A;   // pomarańczowy
            case ERROR:        return 0xFFFF453A;   // czerwony
            default:           return (fg & 0x00FFFFFF) | 0x40000000;
        }
    }

    public static String label(int state) {
        switch (state) {
            case CONNECTED_FG: return "CarPlay połączony (aktywny)";
            case CONNECTED_BG: return "CarPlay połączony (w tle)";
            case CONNECTING:   return "Łączenie z iPhone...";
            case ERROR:        return "Błąd CarPlay";
            default:           return "CarPlay nieaktywny";
        }
    }
}
