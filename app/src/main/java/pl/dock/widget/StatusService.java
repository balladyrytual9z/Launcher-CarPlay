package pl.dock.widget;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

// Obsługuje widżet Dock: reaguje na zdarzenia systemowe i odświeża widżet tylko przy zmianie.
public class StatusService extends Service {

    private static final String CHANNEL = "dock_status";
    private BroadcastReceiver receiver;
    private BroadcastReceiver bcastReceiver;
    private long lastBattery = 0;
    private Handler handler;
    private Runnable tick;
    private BroadcastReceiver modeReceiver;
    private Runnable nightScheduler;

    public static void start(Context c) {
        try {
            Intent i = new Intent(c, StatusService.class);
            if (Build.VERSION.SDK_INT >= 26) {
                c.startForegroundService(i);
            } else {
                c.startService(i);
            }
        } catch (Exception ignored) {
        }
    }

    public static void stop(Context c) {
        try {
            c.stopService(new Intent(c, StatusService.class));
        } catch (Exception ignored) {
        }
    }

    public static void restart(Context c) {
        stop(c);
        start(c);
    }

    // okresowa kontrola to tylko zabezpieczenie; napięcie z pliku / ustawienia nie ma broadcastu
    private long interval() {
        boolean poll = Prefs.showVoltage(this) && !"battery".equals(Prefs.voltSource(this));
        return poll ? 20000L : 60000L;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        goForeground();
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String a = intent.getAction();
                if (Intent.ACTION_BATTERY_CHANGED.equals(a)) {
                    // napięcie baterii potrzebne tylko, gdy jest pokazywane
                    if (!Prefs.showVoltage(context)) return;
                    long now = System.currentTimeMillis();
                    if (now - lastBattery < 5000) return;
                    lastBattery = now;
                }
                BtDevice.handle(intent);
                PhoneBattery.handle(intent);
                StatusCenter.refresh(context);
                DockWidget.updateAll(context);
            }
        };
        BtDevice.init(this);
        IntentFilter f = new IntentFilter();
        f.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        f.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        f.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        f.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        f.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        f.addAction("android.net.conn.TETHER_STATE_CHANGED");
        f.addAction(Intent.ACTION_BATTERY_CHANGED);
        f.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        f.addAction(Intent.ACTION_SCREEN_ON);
        f.addAction(PhoneBattery.ACTION_DEV);
        f.addAction(PhoneBattery.ACTION_VENDOR);
        f.addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED);
        f.addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED);
        f.addAction(android.bluetooth.BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        f.addAction(android.bluetooth.BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        registerReceiver(receiver, f);

        // własny broadcast z napięciem (jeśli wybrany w ustawieniach)
        String action = Prefs.bcastAction(this);
        if (action.length() > 0) {
            bcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent intent) {
                    try {
                        Bundle ex = intent.getExtras();
                        String name = Prefs.bcastExtra(ctx);
                        if (ex != null && name.length() > 0 && ex.containsKey(name)) {
                            @SuppressWarnings("deprecation")
                            Object o = ex.get(name);
                            Double d = Volt.toDouble(o);
                            if (d != null) Prefs.setBcastVal(ctx, d.floatValue());
                        }
                    } catch (Throwable ignored) {
                    }
                    StatusCenter.refresh(ctx);
                    DockWidget.updateAll(ctx);
                }
            };
            registerReceiver(bcastReceiver, new IntentFilter(action));
        }

        handler = new Handler(Looper.getMainLooper());
        tick = new Runnable() {
            @Override
            public void run() {
                PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                boolean screenOn = pm == null || pm.isInteractive();
                // gdy launcher jest na wierzchu, sam dba o odświeżanie
                if (screenOn && !LauncherActivity.visible) {
                    StatusCenter.refresh(StatusService.this);
                    DockWidget.updateAll(StatusService.this);
                }
                handler.postDelayed(this, interval());
            }
        };
        handler.postDelayed(tick, interval());
        AppListWatcher.register(this);
        AppListWatcher.get().addListener(new AppListWatcher.Listener() {
            @Override
            public void onAppsChanged() {
                DockWidget.updateAll(StatusService.this);
            }
        });

        // Dwukierunkowa pętla z Circadian Display:
        //  • pl.circadiandisplay.STATE_CHANGED  ← nakładka zgłasza zmianę isNight
        //  • pl.dock.widget.NIGHT_MODE_CHANGED  → Dock informuje nakładkę / siebie
        modeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                String a = intent.getAction();
                if (a == null) return;
                if (pl.dock.widget.circadian.CircadianBridge.ACTION_STATE_CHANGED.equals(a)) {
                    // Stan z aplikacji Circadian Display (Night Shift) – natychmiastowa synchronizacja UI
                    boolean isNight = intent.getBooleanExtra("isNight", false);
                    applyCircadianState(ctx, isNight);
                    return;
                }
                if (pl.dock.widget.circadian.CircadianBridge.ACTION_NIGHT.equals(a)
                        || "pl.dock.widget.NIGHT_MODE_CHANGED".equals(a)) {
                    boolean isNight = intent.getBooleanExtra("isNight", false);
                    // Wewnętrzny / wychodzący sygnał: aktywuj profil nocny Circadian gdy trzeba
                    if (isNight && Prefs.circadianAuto(ctx)
                            && pl.dock.widget.circadian.CircadianBridge.isInstalled(ctx)) {
                        pl.dock.widget.circadian.CircadianBridge.activateForNight(ctx);
                    }
                }
            }
        };
        IntentFilter modeFilter = new IntentFilter();
        modeFilter.addAction(pl.dock.widget.circadian.CircadianBridge.ACTION_NIGHT);
        modeFilter.addAction(pl.dock.widget.circadian.CircadianBridge.ACTION_STATE_CHANGED);
        modeFilter.addAction("pl.dock.widget.NIGHT_MODE_CHANGED");
        registerReceiver(modeReceiver, modeFilter);

        nightScheduler = new Runnable() {
            @Override
            public void run() {
                try {
                    Boolean was = Look.wasNight;
                    boolean nowNight = Look.night(StatusService.this);
                    if (was == null || was.booleanValue() != nowNight) {
                        if (Prefs.circadianAuto(StatusService.this)
                                && pl.dock.widget.circadian.CircadianBridge.isInstalled(StatusService.this)) {
                            pl.dock.widget.circadian.CircadianBridge.setNightMode(
                                    StatusService.this, nowNight);
                        }
                        sendBroadcast(new Intent(pl.dock.widget.circadian.CircadianBridge.ACTION_NIGHT)
                                .setPackage(getPackageName())
                                .putExtra("isNight", nowNight));
                    }
                } catch (Throwable ignored) {
                }
                if (handler != null) handler.postDelayed(this, 30000L);
            }
        };
        handler.postDelayed(nightScheduler, 30000L);
    }

    /**
     * Synchronizacja trybu dzienny/nocny z Circadian Display.
     * Aktualizuje Prefs, odświeża widżet i emituje wewnętrzny NIGHT_MODE_CHANGED
     * (LauncherActivity nasłuchuje i od razu przebudowuje tapetę / ikony).
     */
    private void applyCircadianState(Context ctx, boolean isNight) {
        try {
            if (!Prefs.circadianAuto(ctx)) return;
            String want = isNight ? "night" : "day";
            // Nie nadpisuj trybu godzinowego – tylko day/night oraz followSystem
            if (Prefs.followSystem(ctx)) {
                // tryb systemowy: nie ruszamy Prefs.mode, ale i tak odświeżamy UI
            } else if (!"time".equals(Prefs.mode(ctx))) {
                if (!want.equals(Prefs.mode(ctx))) {
                    Prefs.setMode(ctx, want);
                }
            } else {
                // tryb "time": ustaw ręczny day/night, by natychmiast odzwierciedlić wybór użytkownika
                Prefs.setMode(ctx, want);
            }
            Look.wasNight = isNight;
            StatusCenter.refresh(ctx);
            DockWidget.updateAll(ctx);
            // Powiadom launcher (ten sam proces) – bez setPackage do Circadian, żeby nie zapętlić
            sendBroadcast(new Intent(pl.dock.widget.circadian.CircadianBridge.ACTION_NIGHT)
                    .setPackage(getPackageName())
                    .putExtra("isNight", isNight)
                    .putExtra("from_circadian", true));
        } catch (Throwable ignored) {
        }
    }

    private void goForeground() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL, "Dock", NotificationManager.IMPORTANCE_MIN));
        }
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL)
                : new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setContentTitle("Dock");
        startForeground(1, b.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        goForeground();
        StatusCenter.refresh(this);
        DockWidget.updateAll(this);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        try {
            if (handler != null && tick != null) handler.removeCallbacks(tick);
            if (handler != null && nightScheduler != null) handler.removeCallbacks(nightScheduler);
            unregisterReceiver(receiver);
            if (bcastReceiver != null) unregisterReceiver(bcastReceiver);
            if (modeReceiver != null) unregisterReceiver(modeReceiver);
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}





