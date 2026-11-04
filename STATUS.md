# Dock 2.0 CarPlay Launcher - backup status
Data: 2026-11-03

Wszystkie poprawki ZAKOŃCZONE (109 plikow projektu + .github/workflows/build-release.yml).
Zakres: MediaHub retencje, Net BT fallback, Launch flags, CarplayState foreground, Prefs (cornerRadius/ccSwipe/hideFromGrid), Manifest+ControlCenterGestureService, BootReceiver, NightShiftService clip, EditModeController (nowy), LauncherActivity (overlay/edit/uninstall/dock/gestureStrip), DockView glass, ConnectivityActivity swipe_x, ConnectivityPanel rework (slots/drag/hotspot=openTetherSettings/bMode 3-stanowy/ContentObserver), Popups glassSnap, ConfigActivity (installWithDock/quickRow/ccSwipe/skróty/corner-radius/themeChanged), AppsGridPage edit mode, WidgetPage edit mode (kompletne), BuiltinIcons (phone annular-sector / airdrop arcs / appstore litera A) + Night Shift Kelvin (ColorTemp + FilterView, dwie warstwy dim+warm), usuniecie Circadian (folder, manifest, StatusService, LauncherActivity, ConfigActivity, BackupManager, Prefs), animacja usuwania ikony (postDelayed 220ms przed onRemove), nowa ic_battery.xml (prostokat z wypelnieniem 70%), versionName 1.0.6-dock2-nightshift.
Sanity: nawiasy OK w 107 javach, brak bDay/bNight/bAuto, sygnatury zweryfikowane. Zip central dir zbudowany jednolicie wg standardu (30B local / 46B central).

POZOSTAŁO TYLKO:
1. Push na GitHub balladyrytual9z/Launcher-CarPlay (branch main) przez github_app.push_files, batche; NIE wypychac .github/workflows/build-release.yml.
2. Po pushu: weryfikacja buildu (workflow build-release.yml buduje APK i publikuje Release z tagiem v1.0.x) i przekazanie linku do Release.
