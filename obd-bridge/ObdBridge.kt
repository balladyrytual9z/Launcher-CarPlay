


package com.miskibin.obd2dashboard   // <- dopasuj do pakietu aplikacji OBD2 Dashboard

import android.content.Context
import android.content.Intent

/**
 * Wysyła napięcie z OBD do Docka (broadcast). Dodaj ten plik do projektu OBD2 Dashboard
 * i wywołaj ObdBridge.publish(context, volts) w miejscu, gdzie aplikacja odczytuje
 * napięcie (PID 0x42 "Control module voltage" albo odpowiedź na ATRV).
 *
 * Umowa z Dockiem (ustawiana w Dock → Źródło napięcia → "Z aplikacji OBD2 Dashboard"):
 *   akcja:  pl.dock.widget.OBD_VOLTAGE
 *   extra:  "voltage" (Float, wolty)
 * Broadcast jest wysyłany tylko do aplikacji Dock (setPackage), bez uprawnień.
 */
object ObdBridge {
    private const val ACTION = "pl.dock.widget.OBD_VOLTAGE"
    private const val DOCK_PACKAGE = "pl.dock.widget"
    private var lastSent = 0L
    private var lastValue = -1f

    fun publish(context: Context, volts: Float) {
        if (volts <= 0f || volts > 40f) return          // odrzuć wartości z błędnych odczytów
        val now = System.currentTimeMillis()
        // wysyłaj najwyżej raz na 2 s, chyba że napięcie się zmieniło o >= 0,1 V
        if (now - lastSent < 2000 && Math.abs(volts - lastValue) < 0.1f) return
        lastSent = now
        lastValue = volts
        try {
            context.applicationContext.sendBroadcast(
                Intent(ACTION).setPackage(DOCK_PACKAGE).putExtra("voltage", volts)
            )
        } catch (_: Throwable) {
        }
    }
}





