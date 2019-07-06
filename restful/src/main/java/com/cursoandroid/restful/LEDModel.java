package com.cursoandroid.restful;

import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;

import static android.content.ContentValues.TAG;

public class LEDModel {
    private static LEDModel instance = null;
    PeripheralManager service;
    private Gpio mLedGpio;
    private final String PIN_LED = "BCM17";

    public static LEDModel getInstance() {
        if (instance == null) {
            instance = new LEDModel();
        }
        return instance;
    }

    private LEDModel() {
        service = PeripheralManager.getInstance();
        try {
            mLedGpio = service.openGpio(PIN_LED);
            mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        } catch (Exception e) {
            Log.e(TAG, "Error en el API PeripheralIO", e);
        }
    }

    static Boolean setState(boolean state) {
        try {
            getInstance().mLedGpio.setValue(state);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error en el API PeripheralIO", e);
            return false;
        }
    }

    public static boolean getState() {
        boolean value = false;
        try {
            value = getInstance().mLedGpio.getValue();
        } catch (IOException e) {
            Log.e(TAG, "Error en el API PeripheralIO", e);
        }
        return value;
    }
}

/*
NOTA: El LED interactuará con
el mundo externo mediante dos métodos:
getState(), que devolverá un valor de
true o false según si está encendido o apagado; y
setState(), al que le indicaremos
el valor al que queremos que cambie.
 */
