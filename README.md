

# Dock 2.0 (CarPlay Launcher)

Android launcher for car tablets (API 26+, target 28 / Android 9).  
Application ID: `pl.dock.widget`

## What’s new in this package (Night Shift rework, no Circadian)

- **Night Shift Kelvin** – colour computed from blackbody curve (6500 K → 2200 K)
  instead of fixed amber; two layers (black dim + warmth) with independent alpha.
- **Removed Circadian integration** – no `pl.dock.widget.circadian.*`, no external
  broadcasts. Single source of truth for screen colour is `NightShiftService`.
- **Smooth fade in/out** – `ValueAnimator` from/to zero alpha on the overlay.
- Fixed `SYSTEM_UI_FLAG_VISIBLE` in `ConfigActivity`, `ConnectivityActivity`,
  `NowPlayingActivity` → status bar hidden but pullable (IMMERSIVE_STICKY).
- Icon refresh: CarPlay mark (thicker arc + centred triangle), tether chain
  (heavier links), battery rectangle with proportional fill.
- Widgets: `Ui.ringCard` now translucent (`0xB3101012` night / `0xBFFFFFFF` day) with
  top glass gradient; Weather card joined the same style.
- App grid: long-press background enters edit mode; minus badge triggers 220 ms
  shrink-and-fade animation before removal; context menu has „Informacje /
  Usuń z siatki / Odinstaluj”.
- Page dots: count excludes the App Library screen; auto-hide on last screen.
- **Backup / restore** – export/import all Dock prefs as JSON.
- **Apple-like push/pop** animations in settings, recent-apps long-press on dock bottom.
- **CarPlay reconnecting spinner** (amber ring) when phone is detected but not connected.
- **Extended app Overrides** (LocalSend→AirDrop, F-Droid→App Store, Material Files→Pliki, …).
- **Unified settings hub** already present; backup section added.

## What’s new in this package (Dock 2.0 foundation)

- **Media Core contract** – `MediaSnapshot`, `MediaCommand`, `MediaCapabilities` + extended `DockStateProvider.call()` methods (`get_media_snapshot`, `media_play/pause/toggle/next/previous/seek`, …).
- **Media Center** – full-screen Now Playing (`MediaCenterActivity`) driven by the same `MediaHub` as the home widget (single source of truth).
- **Design system** – `DockTheme`, `DockColors`, `DockMotion` (Graphite night / Day palette, shared radii & motion).
- **DockTaskManager** – central network / image / main-thread executors.
- **Diagnostics** – `DockDiagnosticsActivity` (heap, PSS, media status, device info).
- Existing DiPlay / Zlink / CarPlay / OBD / widgets behaviour preserved.

## Build (local / CI)

```bash
# requires Android SDK + Java 17
gradle --no-daemon clean assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

Debug keystore: `app/dock-debug.p12` (alias `dock`, password `dockdock`).

## GitHub Actions

Workflow: `.github/workflows/build-release.yml`

- Java 17, Gradle 8.9, AGP 8.5.2  
- platform android-35 / build-tools 35.0.0  
- Builds debug APK, uploads artifact, creates GitHub Release `v1.0.<run_number>`

## Project layout

```
CarplayLaucher/
  settings.gradle
  build.gradle
  gradle.properties
  app/
    build.gradle
    dock-debug.p12
    src/main/java/pl/dock/widget/
      … existing launcher …
      MediaCenterActivity.java
      DockDiagnosticsActivity.java
      media/          # MediaSnapshot, MediaCommand, MediaCapabilities
      ui/             # DockTheme, DockColors, DockMotion
      core/           # DockTaskManager
  docs/
  .github/workflows/build-release.yml
```

## Media protocol (ContentProvider)

Authority: `pl.dock.widget.state`

| Method | Role |
|--------|------|
| get_media_snapshot | Current track state |
| get_media_capabilities | Supported actions |
| media_play / media_pause / media_toggle | Playback |
| media_next / media_previous | Skip |
| media_seek | `position_ms` in extras |
| media_select_source | preferred package |

Trusted callers only for control commands (Prefs.trustedCallers + same package).

## Roadmap (from full Dock 2.0 plan)

1. Split Media Center into separate APK `pl.dock.media` (same signing cert for signature permission).
2. Liquid Glass materials + adaptive quality profile.
3. Automotive Center screen.
4. Full dashboard redesign with independent repositories.
5. R8 + release keystore via GitHub Secrets.

Existing preferences, AppWidgetHost IDs, CarPlay launch paths and OBD bridge are intentionally unchanged.




