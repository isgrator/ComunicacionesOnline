package com.cursoandroid.comunicacionesonline;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;
import java.util.List;

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

public class MainActivity extends Activity implements WebServer.WebserverListener {

    //I2C **************************************************************
    private static final byte ACTIVA_SALIDA  = 0x40; // 0100 00 00
    private static final byte AUTOINCREMENTO = 0x04; // 0000 01 00
    private static final byte ENTRADA_0      = 0x00; // 0000 00 00
    private static final byte ENTRADA_1      = 0x01; // 0000 00 01
    private static final byte ENTRADA_2      = 0x02; // 0000 00 10
    private static final byte ENTRADA_3      = 0x03; // 0000 00 11
    private static final String IN_I2C_NOMBRE = "I2C1"; // Puerto de entrada
    private static final int IN_I2C_DIRECCION = 0x48; // Dirección de entrada
    private I2cDevice i2c;

    PeripheralManager manager;
    //******************************************************************

    private WebServer server;
    private final String PIN_LED = "BCM17";
    public Gpio mLedGpio;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        server = new WebServer(8180, this, this);

        //I2C *****************************************************
        manager = PeripheralManager.getInstance();
        /*try {
            i2c = manager.openI2cDevice(IN_I2C_NOMBRE, IN_I2C_DIRECCION);

            byte[] config = new byte[2];
            config[0] = (byte) ACTIVA_SALIDA + ENTRADA_3; // byte de control:activamos la salida OUT + lectura de la línea que queremos
            // valor de salida (128/255): Lo que convertimos de analógico a digital
            config[1] = (byte) 0x80;
            //config[1] = (byte) 0x00;
            //config[1] = (byte) 0x70;
            i2c.write(config, config.length);          // escribimos 2 bytes

            byte[] buffer = new byte[5];
            i2c.read(buffer, buffer.length);           // leemos 5 bytes para la entrada analógica Byte 0 no se sabe qué es. Resto: 4 lecturas consecutivas de la entrada
            String s ="";
            for (int i=0; i<buffer.length; i++) {
                s += " byte "+i+": " + (buffer[i]&0xFF);
            }
            lectura = ""+(buffer[1]&0xFF);
            Log.d(TAG, s);                     // mostramos salida: 0 si 0V; 255 si 5V

            i2c.close();                       // cerramos i2c
            i2c = null;                        // liberamos memoria
        } catch (IOException e) {
            Log.e(TAG, "Error en al acceder a dispositivo I2C", e);
        }*/
        //*********************************************************

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

    @Override public String getPotentiometerValue() {
        float lectura = 0;
        try {
            i2c = manager.openI2cDevice(IN_I2C_NOMBRE, IN_I2C_DIRECCION);

            byte[] config = new byte[2];
            config[0] = (byte) ACTIVA_SALIDA + ENTRADA_3; // byte de control:activamos la salida OUT + lectura de la línea que queremos
            // valor de salida (128/255): Lo que convertimos de analógico a digital
            config[1] = (byte) 0x80;
            //config[1] = (byte) 0x00;
            //config[1] = (byte) 0x70;
            i2c.write(config, config.length);          // escribimos 2 bytes

            byte[] buffer = new byte[5];
            i2c.read(buffer, buffer.length);           // leemos 5 bytes para la entrada analógica Byte 0 no se sabe qué es. Resto: 4 lecturas consecutivas de la entrada
            String s ="";
            for (int i=0; i<buffer.length; i++) {
                s += " byte "+i+": " + (buffer[i]&0xFF);
            }
            lectura = convertToVolts(buffer[1]&0xFF);
            Log.d(TAG, s);                     // mostramos salida: 0 si 0V; 255 si 5V

            i2c.close();                       // cerramos i2c
            i2c = null;                        // liberamos memoria

        } catch (IOException e) {
            Log.e(TAG, "Error en la lectura del potenciómetro", e);
        }
        //Transformación a voltios

        return ""+lectura;
    }

    private float convertToVolts(float reading){
        return reading*5/255;
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
