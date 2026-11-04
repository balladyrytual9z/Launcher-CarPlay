


# Dock – współpraca z innymi aplikacjami (instrukcja dla projektów OBD2 Dashboard, DiPlay, mapy)

Ten dokument można wkleić do osobnego projektu jako polecenie. Dock to launcher na tablet
ESSGOO (Android 9, API 28). Poniżej trzy rzeczy, które inna aplikacja może zrobić.

## 1. Własne ustawienia w oknie Ustawienia Docka

Użytkownik nie musi nic włączać. Dock sam znajduje aplikacje z odpowiednim providerem
i pokazuje ich ustawienia jako osobną kategorię (nazwa = nazwa aplikacji).

### Deklaracja w AndroidManifest.xml aplikacji

```xml
<provider
    android:name=".DockSettingsProvider"
    android:authorities="com.twojaaplikacja.docksettings"
    android:exported="true">
    <intent-filter>
        <action android:name="pl.dock.widget.SETTINGS_PROVIDER" />
    </intent-filter>
</provider>
```

### Provider (Kotlin)

Dock używa wyłącznie `ContentResolver.call(...)`. Trzy metody:

| method   | arg   | extras          | wynik (Bundle)            |
|----------|-------|-----------------|---------------------------|
| `schema` | null  | –               | `json` = opis ustawień    |
| `set`    | klucz | `value` (bool lub String) | –               |
| `action` | klucz | –               | –                         |

```kotlin
class DockSettingsProvider : ContentProvider() {
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? = when (method) {
        "schema" -> Bundle().apply { putString("json", schemaJson()) }
        "set" -> { /* zapisz extras.get("value") pod kluczem arg */ null }
        "action" -> { /* wykonaj akcję arg */ null }
        else -> null
    }
    // pozostałe metody ContentProvider: zwróć null / 0
}
```

### Format JSON (`schema`)

```json
{
  "rows": [
    {"type": "toggle", "key": "show_volt", "label": "Wysyłaj napięcie do Docka", "value": true},
    {"type": "select", "key": "unit", "label": "Jednostka", "options": ["V", "mV"], "value": "V"},
    {"type": "action", "key": "reconnect", "label": "Połącz z adapterem OBD"},
    {"type": "info", "label": "Adapter", "value": "Połączono"}
  ]
}
```

Zasady podpisów: krótko, po polsku, wielka litera tylko na początku, bez kropki na końcu,
czasownik w trybie rozkazującym dla akcji („Połącz…”), bez skrótów technicznych.
Dock odświeża kategorię po każdej zmianie, więc `schema` musi zwracać aktualne wartości.

## 2. Napięcie instalacji (OBD2 Dashboard → Dock)

Broadcast wysyłany co ~1 s, gdy aplikacja ma odczyt:

```kotlin
sendBroadcast(Intent("pl.dock.widget.OBD_VOLTAGE").apply {
    setPackage("pl.dock.widget")
    putExtra("voltage", 13.8f)   // Float, wolty
})
```

Dock pokazuje wartość w pasku statusu; od 13,3 V pokazuje ikonę ładowania zamiast liczby,
poniżej 13,0 V wraca do liczby (histereza).

## 3. Stan CarPlay i bateria telefonu (DiPlay → Dock)

Dock sam odczytuje powiadomienie i sesję multimedialną DiPlay (pakiet `com.shihab.diplay`).
Jeśli fork DiPlay ma wysyłać stan wprost, proponowany kontrakt:

```kotlin
sendBroadcast(Intent("pl.dock.widget.CARPLAY_STATE").apply {
    setPackage("pl.dock.widget")
    putExtra("state", "connected")   // disconnected | connecting | connected | error
    putExtra("phone_battery", 82)    // 0..100, opcjonalnie
})
```

## 4. Otwieranie okna CarPlay

Samo uruchomienie pakietu (`getLaunchIntentForPackage`) otwiera ekran startowy DiPlay.
Okno z obrazem z telefonu to osobna aktywność (w kodzie DiPlay: `CarPlayHostActivity`).
Dock wykrywa ją automatycznie, a w Ustawienia → CarPlay (DiPlay) można wybrać ją ręcznie.
Uwaga: w przesłanej instrukcji pojawiał się pakiet `com.carplay.dipower` – na tym tablecie
DiPlay to `com.shihab.diplay`, więc ten pakiet nie jest używany.



## 5. Wybór aplikacji w siatce Docka

Dock trzyma listę wybranych pakietów w `Prefs.selectedPkgs`. Pusta lista = wszystkie
aplikacje widoczne. Odczyt przez ContentProvider:

```kotlin
val b = contentResolver.call(
    Uri.parse("content://pl.dock.widget.state"), "get_selected", null, null)
val json = b?.getString("pkgs")
```

## 6. Stan strony Docka

Metody ContentResolver.call na `content://pl.dock.widget.state`:

| method | arg | wynik |
|--------|-----|-------|
| get_state | – | page, page_index, home_index, apps_index, pages_total, last_page, external_caller |
| navigate | widgets/home/apps | ok (tylko zaufane pakiety, launcher aktywny) |
| set_page | extras.page | zapisuje ostatnią stronę |
| get_selected | – | pkgs (JSON array) |

Broadcasty (gdy launcher na wierzchu): `pl.dock.widget.NAVIGATE`, `pl.dock.widget.STATE_QUERY` → odpowiedź `pl.dock.widget.STATE`.




