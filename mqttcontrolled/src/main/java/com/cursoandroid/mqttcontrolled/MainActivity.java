package com.cursoandroid.mqttcontrolled;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.IOException;

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
public class MainActivity extends Activity implements MqttCallback {
    private static final String TAG = "Things";
    private final String PIN_LED = "BCM17"; // "BCM18";
    public Gpio mLedGpio;
    private static final String topic_gestion = "isgrator/gestion";  //***************************
    private static final String topic_led = "isgrator/led";          //***************************
    static final String hello = "Hello world! Android Things conectada.";
    private static final int qos = 1;
    private static final String broker = "tcp://broker.hivemq.com:1883";//"tcp://iot.eclipse.org:1883";  //**********************
    MqttClient client;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PeripheralManager service = PeripheralManager.getInstance();
        try {
            mLedGpio = service.openGpio(PIN_LED);
            mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        } catch (IOException e) {
            Log.e(TAG, "Error en el API PeripheralIO", e);
        }
        try {
            String clientId = MqttClient.generateClientId();
            client = new MqttClient(broker, clientId, new MemoryPersistence());
            client.setCallback(this);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setKeepAliveInterval(60);
            connOpts.setWill(topic_gestion, "Android Things desconectada!"
                    .getBytes(), qos, false);
            Log.i(TAG, "Conectando al broker " + broker);
            client.connect(connOpts);
            Log.i(TAG, "Conectado");
            Log.i(TAG, "Publicando mensaje: " + hello);
            MqttMessage message = new MqttMessage(hello.getBytes());
            message.setQos(qos);
            client.publish(topic_gestion, message);
            Log.i(TAG, "Mensaje publicado");
            client.subscribe(topic_led, qos);
            Log.i(TAG, "Suscrito a " + topic_led);
        } catch (MqttException e) {
            Log.e(TAG, "Error en MQTT.", e);
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
            }
        } catch (MqttException e) {
            Log.e(TAG, "Error en MQTT.", e);
        }
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

    //se invoca cuando se pierde la conexión con el broker
    @Override public void connectionLost(Throwable cause) {
        Log.d(TAG, "Conexión perdida...");
        //Indicar una acción de aviso de la desconexión, como encender el Led
    }

    //Se invoca cuando la publicación de un mensaje se completa con éxito
    @Override public void messageArrived(String topic, MqttMessage message)
            throws Exception {
        String payload = new String(message.getPayload());
        Log.d(TAG, payload);
        switch (payload) {
            case "ON":
                mLedGpio.setValue(true);
                Log.d(TAG, "LED ON!");
                break;
            case "OFF":
                mLedGpio.setValue(false);
                Log.d(TAG, "LED OFF!");
                break;
            default:
                Log.d(TAG, "Comando no soportado");
                break;
        }
    }

    // gestionará la recepción de un mensaje MQTT al que estemos suscritos.
    @Override public void deliveryComplete(IMqttDeliveryToken token) {
        Log.d(TAG, "Entrega completa!");
    }
}
