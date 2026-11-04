


# Napięcie z OBD2 Dashboard do Docka

OBD2 Dashboard (github.com/miskibin/obd2-dashboard) czyta dane z adaptera ELM327 przez BLE
i **nie udostępnia ich innym aplikacjom** (README nie opisuje żadnego API). Adapter BLE
obsługuje zwykle jedno połączenie naraz, więc Dock nie może czytać go równolegle.
Najpewniejsza droga: mały dodatek do forka OBD2 Dashboard, który wysyła napięcie broadcastem.

1. W swoim forku dodaj plik `ObdBridge.kt` (obok).
2. Znajdź w kodzie miejsce, gdzie dekodowane jest napięcie: szukaj `0x42`, `"42"`,
   `ControlModuleVoltage`, `voltage` albo komendy `ATRV`.
3. Tam, gdzie wartość w woltach jest już policzona, dodaj jedną linię:
   `ObdBridge.publish(context, voltsFloat)`.
   Jeśli pod ręką nie ma `Context`, przekaż `applicationContext` przy starcie sesji OBD.
4. Zbuduj fork przez GitHub Actions i zainstaluj zamiast oryginału.
5. W Docku: Ustawienia → Pasek stanu → Źródło napięcia → "Z aplikacji OBD2 Dashboard
   (broadcast Docka)" i włącz "Pokaż napięcie".

Dock uznaje napięcie za nieaktualne po 2 minutach bez broadcastu (pokazuje "-- V"),
więc nie pokaże starej wartości, gdy aplikacja OBD jest zamknięta lub adapter odłączony.





