package com.cursoandroid.comunicacionesonline;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;

import static android.content.ContentValues.TAG;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */

/*
Desde aquí podremos iniciar y parar el servidor web, e implementar la lógica del listener para tomar
acciones cuando el usuario nos envía datos a través de la página HTML.
 */
/* NOTAS:
    - WebserverListener es la interfaz de callback.
    - Al arrancar la app (onCreate()) se crea e inicia el servidor web en el puerto
    8180, y pasamos nuestro listener como parámetro.
    - Vemos la implementación de tres métodos del callback: switchLEDon, switchLEDoff y getLedStatus.
 */

public class MainActivity extends Activity implements WebServer
        .WebserverListener {

    private WebServer server;
    private final String PIN_LED = "BCM17";
    public Gpio mLedGpio;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        server = new WebServer(8180, this, this);
        PeripheralManager service = PeripheralManager.getInstance();
        try {
            mLedGpio = service.openGpio(PIN_LED);
            mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        } catch (IOException e) {
            Log.e(TAG, "Error en el API PeripheralIO", e);
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        server.stop();
        if (mLedGpio != null) {
            try {
                mLedGpio.close();
            } catch (IOException e) {
                Log.e(TAG, "Error en el API PeripheralIO", e);
            } finally {
                mLedGpio = null;
            }
        }
    }

    @Override public void switchLEDon() {
        try {
            mLedGpio.setValue(true);
            Log.i(TAG, "LED switched ON");
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        }
    }

    @Override public void switchLEDoff() {
        try {
            mLedGpio.setValue(false);
            Log.i(TAG, "LED switched OFF");
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        }
    }

    @Override public Boolean getLedStatus() {
        try {
            return mLedGpio.getValue();
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
            return false;
        }
    }
}
