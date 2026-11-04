


package pl.dock.widget;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import java.util.Locale;

// Stan połączenia CarPlay (aplikacja DiPlay streamuje CarPlay z iPhone'a).
// DiPlay nie udostępnia oficjalnego API, więc łączymy kilka niezależnych sygnałów
// (każdy można włączyć/wyłączyć w ustawieniach):
//  1. aktywna sesja multimedialna DiPlay (wymaga dostępu do powiadomień),
//  2. powiadomienie DiPlay (wymaga dostępu do powiadomień),
//  3. iPhone podłączony przez USB (producent Apple, VID 0x05AC),
//  4. opcjonalny broadcast ze stanem (jeśli DiPlay lub jego fork taki wysyła).
public class CarplayState {
    public static final int OFF = 0;        // brak telefonu / CarPlay nieaktywny
    public static final int PHONE = 1;      // iPhone wykryty lub aplikacja się łączy
    public static final int CONNECTED = 2;  // sesja CarPlay aktywna

    public static int read(Context c) {
        Context app = c.getApplicationContext();
        if (!Prefs.cpShow(app)) return OFF;
        int st = OFF;
        String pkg = Prefs.diplayPkg(app);

        String b = Prefs.cpbVal(app).toLowerCase(Locale.US);
        if (b.length() > 0) {
            if (b.contains("connected") || b.contains("streaming") || b.contains("active")
                    || b.equals("true") || b.equals("1")) {
                st = Math.max(st, CONNECTED);
            } else if (b.contains("connecting") || b.contains("waiting") || b.contains("phone")) {
                st = Math.max(st, PHONE);
            }
            // słowa o błędzie/braku nie podnoszą stanu
        }
        try {
            if (Prefs.cpUseSession(app) && MediaHub.hasSession(app, pkg)) {
                st = Math.max(st, CONNECTED);
            }
        } catch (Throwable ignored) {
        }
        try {
            if (Prefs.cpUseNotif(app)) {
                // powiadomienie "DiPlay / CarPlay connection running" = połączenie działa
                String[] n = DockNotificationListener.notificationText(pkg);
                if (n != null) {
                    String all = (n[0] + " " + n[1]).toLowerCase(Locale.US);
                    if (all.contains("connecting") || all.contains("waiting")
                            || all.contains("searching") || all.contains("łączenie")) {
                        st = Math.max(st, PHONE);
                    } else {
                        st = Math.max(st, CONNECTED);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            if (Prefs.cpUseUsb(app) && appleUsbPresent(app)) st = Math.max(st, PHONE);
        } catch (Throwable ignored) {
        }
        try {
            if (foregroundCarplay(app)) st = Math.max(st, CONNECTED);
        } catch (Throwable ignored) {
        }
        return st;
    }

    // DiPlay na pierwszym planie w ostatnich 12 s = aktywna sesja CarPlay
    // (UsageStats; wymaga zgody na "Dostęp do danych o użyciu" — bez niej zwraca false)
    public static boolean foregroundCarplay(Context c) {
        try {
            android.app.AppOpsManager aom = (android.app.AppOpsManager)
                    c.getSystemService(Context.APP_OPS_SERVICE);
            if (aom == null) return false;
            int mode = aom.checkOpNoThrow("android:get_usage_stats",
                    android.os.Process.myUid(), c.getPackageName());
            if (mode != android.app.AppOpsManager.MODE_ALLOWED) return false;
            android.app.usage.UsageStatsManager usm = (android.app.usage.UsageStatsManager)
                    c.getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm == null) return false;
            long now = System.currentTimeMillis();
            android.app.usage.UsageEvents ev = usm.queryEvents(now - 12000, now);
            if (ev == null) return false;
            String pkg = Prefs.diplayPkg(c);
            android.app.usage.UsageEvents.Event e = new android.app.usage.UsageEvents.Event();
            while (ev.hasNextEvent()) {
                ev.getNextEvent(e);
                if (e.getEventType() == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED
                        && pkg.equals(e.getPackageName())) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static boolean appleUsbPresent(Context c) {
        UsbManager um = (UsbManager) c.getSystemService(Context.USB_SERVICE);
        if (um == null || um.getDeviceList() == null) return false;
        for (UsbDevice d : um.getDeviceList().values()) {
            if (d.getVendorId() == 0x05AC) return true;
        }
        return false;
    }

    public static int color(int state, int fg) {
        if (state == CONNECTED) return 0xFF30D158;
        if (state == PHONE) return 0xFFFF9F0A;
        return (fg & 0x00FFFFFF) | 0x40000000;
    }

    public static String label(int state) {
        if (state == CONNECTED) return "CarPlay połączony";
        if (state == PHONE) return "iPhone wykryty / łączenie";
        return "CarPlay nieaktywny";
    }
}





