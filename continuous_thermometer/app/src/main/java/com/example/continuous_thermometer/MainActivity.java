package com.example.continuous_thermometer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String clientId = MqttClient.generateClientId();

        //The URL of the Mosquitto Broker is 192.168.9.100:1883
        final  MqttAndroidClient client = new MqttAndroidClient(this.getApplicationContext(), "tcp://test.mosquitto.org:1883", clientId);

        client.setCallback(new MqttCallbackHandler(client));//This is here for when a message is received

        MqttConnectOptions options = new MqttConnectOptions();

        try {
            IMqttToken token = client.connect(options);

            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Log.d("mqtt", "onSuccess");
//-----------------------------------------------------------------------------------------------
                    //PUBLISH THE MESSAGE
                    MqttMessage message = new MqttMessage("Hello, I am an Android Mqtt Client.".getBytes());
                    message.setQos(2);
                    message.setRetained(false);

                    String topic = "IC.embedded/apollostark/test";

                    try {
                        client.publish(topic, message);
                        Log.i("mqtt", "Message published");

                        // client.disconnect();
                        //Log.i("mqtt", "client disconnected");

                    } catch (MqttPersistenceException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();

                    } catch (MqttException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
//-----------------------------------------------------------------------------------------------

                    String subtopic = "IC.embedded/apollostark/#";
                    int qos = 1;
                    try {
                        IMqttToken subToken = client.subscribe(subtopic, qos);
                        subToken.setActionCallback(new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                // The message was published
                                Log.i("mqtt", "subscription success");
                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken,
                                                  Throwable exception) {
                                // The subscription could not be performed, maybe the user was not
                                // authorized to subscribe on the specified topic e.g. using wildcards
                                Log.i("mqtt", "subscription failed");

                            }
                        });



                    } catch (MqttException e) {
                        e.printStackTrace();
                    }

//---------------------------------------------------------------------------

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d("mqtt", "onFailure");

                }

            });


        } catch (MqttException e) {
            e.printStackTrace();
        }

    }


}//End of Activity class


