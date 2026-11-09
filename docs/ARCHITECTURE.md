# Dock 2.0 / CarPlay Launcher – Architecture (Android 9 Automotive)

Rygorystyczna specyfikacja zgodna z raportem refaktoryzacji dla jednostek OEM (UIS7862, SC9863, Spreadtrum/Unisoc, Topway).

## Filozofia Apple-like

- Spring Physics / damped harmonic motion zamiast LinearInterpolator
- Liquid Glass: software Gaussian (box-blur passes) + półprzezroczysty overlay, Z-order Dock nad treścią
- Squircles / precyzyjne ikony wektorowe (BuiltinIcons)
- Haptic-like feedback i natychmiastowa responsywność
- Trwałość Docka podczas nawigacji ustawień (Push/Pop stack)

## Sensor Fusion – CarPlay state (CarplayState)

Agregator deterministyczny czterech wektorów:

| Wektor              | API                          | Trigger / stan                  |
|---------------------|------------------------------|---------------------------------|
| Notification        | NotificationListenerService  | DiPlaySessionService (bez "error") → CONNECTING / CONNECTED |
| Media session       | MediaSessionManager          | STATE_PLAYING/PAUSED pakietu DiPlay → CONNECTED |
| UsageStats polling  | UsageStatsManager            | ACTIVITY_RESUMED CarPlayHost* → CONNECTED |
| USB                 | UsbManager                   | Apple VID 0x05AC → PHONE        |

Dodatkowo Prefs.cpbVal (broadcast override). Stany: OFF / PHONE / CONNECTED. Kolory iOS-green / amber.

## Inteligentny HOME / Quick switch

LauncherActivity singleTask + overridePendingTransition(0,0) + ForegroundTracker (Accessibility / task monitoring).  
Przełączenie HOME ↔ CarPlayHost bez pełnego przeładowania okna.

## Silnik graficzny i Z-Order

- Pager / treść: MATCH_PARENT, Dock dodawany ostatni (Z-order)
- DockView: hit-test propaguje zdarzenia w obszarach nie-interaktywnych (przezroczysty glass nie blokuje swipe stron)
- Blur: wieloprzebiegowy box filter na downsampled bitmapie (API 28 – brak RenderEffect)

## Centrum Sterowania

- Bottom swipe (overlay TYPE_APPLICATION_OVERLAY / cienki pasek) → panel
- Flex / ConstraintLayout + ItemTouchHelper (drag-and-drop, EditMode, Wobble)
- EditModeController – globalny singleton z animacją sinusoidy

## Night Shift – trójstanowy

day / night / time (astronomiczny auto).  
CircadianBridge + NightShiftService (overlay filtr ciepła).  
Długie przytrzymanie → suwak warmth.

## HAL / OEM fallbacki

- Hotspot: setClassName → TetherSettings / Tether → fallback WIRELESS_SETTINGS
- Bluetooth: po timeout enable() → com.syu.bt / com.ts.bt
- Volume HUD: wyłącznie STREAM_MUSIC + ContentObserver na Settings.System (SWC reaction)

## Nawigacja i trwałość Docka

ConstraintLayout + zakotwiczony Dock (Prefs.dockSide).  
FragmentContainerView / ArrayDeque push-pop dla ustawień – Dock zawsze widoczny.

## Dot Indicators

Kalkulacja liczby stron przed pierwszym rysowaniem (brak race z async adapterem).  
Mutable-like aktualizacja z onScroll + initial set po assemblePages.

## Moduły kodu

- `pl.dock.widget` – LauncherActivity, DockView, CarplayState, EditModeController, …
- `pl.dock.widget.core` – DockTaskManager, motion, theme
- `pl.dock.widget.circadian` – CircadianBridge
- `pl.dock.widget.media` – MediaSnapshot / Command
- `pl.dock.widget.ui` – design tokens

## Uprawnienia kluczowe

PACKAGE_USAGE_STATS, SYSTEM_ALERT_WINDOW, WRITE_SETTINGS, Notification Listener, Accessibility (opcjonalnie dla trackera).

## Build

Gradle 8.9 + Java 17, compileSdk/target zgodny z workflow (android-35 build-tools).  
Workflow: `.github/workflows/build-release.yml` – assembleDebug + GitHub Release.

