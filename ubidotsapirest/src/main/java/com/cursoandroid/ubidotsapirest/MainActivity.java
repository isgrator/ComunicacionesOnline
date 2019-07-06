package com.cursoandroid.ubidotsapirest;

import android.app.Activity;
import android.os.Handler;
import android.os.Bundle;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

public class MainActivity extends Activity {

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
    //******************************************************************

    // IDs Ubidots
    private final String token = "A1E-X01DsBUBNDFdRPrvtr0UShuOrHhhmx";
    private final String idIluminacion = "5d192f0fc03f970976fdd5ea";
    private final String idBoton = "5d192f45c03f970976fdd5fa";

    private final String PIN_BUTTON = "BCM16"; //"BCM23";
    private Gpio mButtonGpio;
    private Double buttonstatus = 0.0;
    private Handler handler = new Handler();
    private Runnable runnable = new UpdateRunner();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //I2C *****************************************************
        PeripheralManager manager = PeripheralManager.getInstance();
        List<String> listaDispositivos = manager.getI2cBusList();
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
            Log.d(TAG, s);                     // mostramos salida: 0 si 0V; 255 si 5V

            i2c.close();                       // cerramos i2c
            i2c = null;                        // liberamos memoria
        } catch (IOException e) {
            Log.e(TAG, "Error en al acceder a dispositivo I2C", e);
        }
        //*********************************************************

        PeripheralManager service = PeripheralManager.getInstance();
        try {
            mButtonGpio = service.openGpio(PIN_BUTTON);
            mButtonGpio.setDirection(Gpio.DIRECTION_IN);
            mButtonGpio.setActiveType(Gpio.ACTIVE_LOW);
            mButtonGpio.setEdgeTriggerType(Gpio.EDGE_FALLING);
            mButtonGpio.registerGpioCallback(mCallback);
        } catch (IOException e) {
            Log.e(TAG, "Error en PeripheralIO API", e);
        }
        handler.post(runnable);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        handler = null;
        runnable = null;
        if (mButtonGpio != null) {
            mButtonGpio.unregisterGpioCallback(mCallback);
            try {
                mButtonGpio.close();
            } catch (IOException e) {
                Log.e(TAG, "Error en PeripheralIO API", e);
            }
        }
    }

    // Callback para envío asíncrono de pulsación de botón
    private GpioCallback mCallback = new GpioCallback() {
        @Override public boolean onGpioEdge(Gpio gpio) {
            Log.i(TAG, "Botón pulsado!");
            if (buttonstatus == 0.0) buttonstatus = 1.0;
            else buttonstatus = 0.0;
            //Por cada variable se crea un objeto Data
            final Data boton = new Data();
            boton.setVariable(idBoton);
            boton.setValue(buttonstatus);
            ArrayList<Data> message = new ArrayList<Data>() {{add(boton);}};
            UbiClient.getClient().sendData(message, token);
            return true;  // Mantenemos el callback activo
        }
    };

    // Envío síncrono (5 segundos) del valor del fotorresistor
    private class UpdateRunner implements Runnable {
        @Override public void run() {
            readLDR();
            Log.i(TAG, "Ejecución de acción periódica");
            handler.postDelayed(this, 5000);
        }
    }
    private void readLDR() {
        Data iluminacion = new Data();
        ArrayList<Data> message = new ArrayList<Data>();
        Random rand = new Random();
        float valor = rand.nextFloat() * 5.0f;
        iluminacion.setVariable(idIluminacion);
        iluminacion.setValue((double) valor);
        message.add(iluminacion);
        UbiClient.getClient().sendData(message, token);
    }
}


