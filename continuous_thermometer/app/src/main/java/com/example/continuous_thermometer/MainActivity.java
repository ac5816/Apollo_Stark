package com.example.continuous_thermometer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.Notification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;


public class MainActivity extends AppCompatActivity {

    EditText mEditText;
    double threshold = 40.0;

    String temperature;
    Button publish;
    NotificationCompat.Builder mBuilder;
    NotificationManager mNotificationManager;               // for < OREO
    private NotificationManagerCompat notificationManager;  // for >= OREO
    PendingIntent mResultPendingIntent;
    TaskStackBuilder mTaskStackBuilder;
    Intent mResultIntent;
    public static final String CHANNEL_1_ID = "channel1";
    public static final String CHANNEL_2_ID = "channel2";

    MqttHelper mqttHelper;

    TextView TempReceived, Temperature;
    TextView TimeReceived, Time;

    Notification notification;
    ConstraintLayout CL;

    final String DEGREE = "\u2103";

    //Graph variables
    private static final Random RANDOM = new Random();

    private LineGraphSeries<DataPoint> seriesLine;
    private PointsGraphSeries<DataPoint> series;
    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
    //private double lastX = 0;
    double lastY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startMqtt();

        // we get a graph view instance
        GraphView graph = (GraphView) findViewById(R.id.graph);
        //graph.setPadding(2,2,2,100);

        Viewport viewport = graph.getViewport();
        //viewport.setDrawBorder(true);
        viewport.setYAxisBoundsManual(true);
        viewport.setMinY(15);
        viewport.setMaxY(50);
        viewport.setScrollable(true);
        viewport.setScrollable(true); // enables horizontal scrolling
        viewport.setScalable(true); // enables horizontal zooming and scrolling

        //------------------------------------------------------------------------------------------

        String title = "Hot Babe!";
        String message = "Critical Temperature!";
        //Log.w("Temperature", temperature);

        // ------------------------------------------------------------------------------------------------------
        // BUILD<OREO
        //mShowNotificationButton = findViewById(R.id.btnShowNotification);

        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setSmallIcon(R.drawable.hot_babe_notif2); //set notification icon
        mBuilder.setContentTitle(title); //set notification title
        mBuilder.setContentText(message); //set notification content

        mResultIntent = new Intent(this, MainActivity.class);
        mTaskStackBuilder = TaskStackBuilder.create(this);
        mTaskStackBuilder.addParentStack(MainActivity.this);

        //Add the intent that will start the activity to the top stack
        mTaskStackBuilder.addNextIntent(mResultIntent);
        mResultPendingIntent = mTaskStackBuilder.getPendingIntent(0,PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(mResultPendingIntent);
        mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        // --------------------------------------------------------------------------------------------------------
        //BUILD>=OREO
        createNotificationChannels();
        notificationManager = NotificationManagerCompat.from(this);
        notification = new NotificationCompat.Builder(this, CHANNEL_1_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        if (lastY>15){
            notificationManager.notify(1,notification);
        }
        // --------------------------------------------------------------------------------------------------------

        seriesLine = new LineGraphSeries<DataPoint>();
        seriesLine = new LineGraphSeries<>(getDataPoint());

        series = new PointsGraphSeries<DataPoint>();
        series = new PointsGraphSeries<>(getDataPoint());

        seriesLine.setColor(Color.rgb(0,22,165));
        series.setColor(Color.rgb(0,22,165));

        graph.addSeries(series);
        graph.addSeries(seriesLine);

        graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter(){
            @Override
            public String formatLabel(double value, boolean isValueX){
                if (isValueX)
                {
                    return sdf.format(new Date((long)value));
                }else
                {
                    return super.formatLabel(value, isValueX);
                }
            }
        });

        //graph.getGridLabelRenderer().setHumanRounding(false);
        graph.getGridLabelRenderer().setNumHorizontalLabels(10);
        graph.getGridLabelRenderer().setNumVerticalLabels(25);
        graph.getGridLabelRenderer().setTextSize(10f);
        //graph.getGridLabelRenderer().setHorizontalAxisTitle("Time");
        graph.getGridLabelRenderer().setVerticalAxisTitle("Temperature");

        series.setSize(10);

        TempReceived = findViewById(R.id.Temp);
        TimeReceived = findViewById(R.id.Time);
        mEditText = findViewById(R.id.editText);
        //publish = findViewById(R.id.button2);

        CL = findViewById(R.id.Layout);
        Temperature = findViewById(R.id.LabelTemp);
        Time = findViewById(R.id.LabelTime);

        setTitle("Cool Babe");
    }

    private DataPoint[] getDataPoint()
    {
        DataPoint[] dp = new DataPoint[]
                {
                        new DataPoint(new Date().getTime(),lastY),
                        //new DataPoint(new Date().getTime(),lastY),
                        //new DataPoint(new Date().getTime(),lastY),
                        //new DataPoint(new Date().getTime(),lastY),
                        //new DataPoint(new Date().getTime(),lastY),

                };
        return dp;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // we're going to simulate real time with thread that append data to the graph
        new Thread(new Runnable() {

            @Override
            public void run() {
                // we add 100 new entries
                //for (int i = 0; i < 200; i++) {
                while (true){
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {

                            //lastX = 0;
                            //lastY = lastY+1;

                            addEntry();
                            //series.resetData(DataPoint);
                        }
                    });

                    // sleep to slow down the add of entries
                    try {
                        Thread.sleep(2200);     //sampling every 1.1s
                    } catch (InterruptedException e) {
                        // manage error ...
                    }
                }
            }
        }).start();
    }

    // add random data to graph
    private void addEntry() {
        // here, we choose to display max 10 points on the viewport and we scroll to end
        series.appendData(new DataPoint(new Date().getTime(),lastY), true, 3600);
        seriesLine.appendData(new DataPoint(new Date().getTime(),lastY), true, 3600);
        //temperature = Double.toString(lastY);
    }

    private void startMqtt(){
        mqttHelper = new MqttHelper(getApplicationContext());
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                Toast.makeText(MainActivity.this, "Connected!", Toast.LENGTH_LONG).show();
            }

            @Override
            public void connectionLost(Throwable throwable) {
                Toast.makeText(MainActivity.this, "Connection Lost!", Toast.LENGTH_LONG).show();
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) {
                Log.w("message", mqttMessage.toString());
                        if(topic.equals("IC.embedded/apollostark/temperature")) {
                            Log.w("mqtt_message_received", mqttMessage.toString());
                            Log.w("topic", topic);
                            String temperature = mqttMessage.toString();
                            TimeReceived.setText(temperature + DEGREE);
                            Calendar calendar = Calendar.getInstance();
                            SimpleDateFormat mdformat = new SimpleDateFormat("HH:mm:ss");
                            TempReceived.setText(mdformat.format(calendar.getTime()));
                            lastY = Double.parseDouble(temperature);
                            //threshold = Double.parseDouble(mEditText.getText().toString());
                            //mEditText.setText(mEditText.getText().toString()+ DEGREE);
                            //lastY = 41;
                            if (lastY > threshold) {
                                CL.setBackgroundColor(Color.parseColor("#FFC4B7"));
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#CB3517")));
                                Temperature.setTextColor(Color.rgb(97, 16, 0));
                                Time.setTextColor(Color.rgb(97, 16, 0));
                                setTitle("Hot Babe");
                                seriesLine.setColor(Color.rgb(198, 0, 0));
                                series.setColor(Color.rgb(198, 0, 0));
                                //TimeReceived.setTextColor(0x520E00);
                                //send notification for builds equal greater than OREO
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    notificationManager.notify(1, notification);
                                }
                                //send notification for builds less than OREO
                                else {
                                    mNotificationManager.notify(1, mBuilder.build());
                                }
                            } else {
                                CL.setBackgroundColor(Color.parseColor("#D6E2F0"));
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#4578B2")));
                                Temperature.setTextColor(Color.rgb(4, 25, 50));
                                Time.setTextColor(Color.rgb(4, 25, 50));
                                setTitle("Cool Babe");
                                seriesLine.setColor(Color.rgb(0, 22, 165));
                                series.setColor(Color.rgb(0, 22, 165));
                                //TimeReceived.setTextColor(0x520E00);
                            }
                        } else {
                            threshold = Double.parseDouble(mEditText.getText().toString());
                        }
                    }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
}

        });
    }

    public void createNotificationChannels(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel1 = new NotificationChannel(
                    CHANNEL_1_ID,
                    "Channel 1",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationChannel channel2 = new NotificationChannel(
                    CHANNEL_2_ID,
                    "Channel 2",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel1);
            manager.createNotificationChannel(channel2);
        }
    }

    //Setting the threshold

    public void pub(View v) {
        String topic_publish = "IC.embedded/apollostark/threshold";
        String tmessage = mEditText.getText().toString();
        //byte[] encodedPayload = new byte[0];
        try {
            mqttHelper.mqttAndroidClient.publish(topic_publish, tmessage.getBytes(),0, false);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void connect(View v) throws MqttException {
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
        //mqttConnectOptions.setUserName(username);
        //mqttConnectOptions.setPassword(password.toCharArray());

        try {

            mqttHelper.mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttHelper.mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    mqttHelper.subscribeToTopic();
                    Toast.makeText(MainActivity.this, "Connected!", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    //Log.w("Mqtt", "Failed to connect to: " + serverUri + exception.toString());
                    Toast.makeText(MainActivity.this, "unable to Connect!", Toast.LENGTH_LONG).show();
                }
            });


        } catch (MqttException ex){
            ex.printStackTrace();
        }
    }

    public void disconnect (View v){
        try {
            IMqttToken token = mqttHelper.mqttAndroidClient.disconnect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Toast.makeText(MainActivity.this, "Successfully Disconnected!", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Toast.makeText(MainActivity.this, "Disconnection failed", Toast.LENGTH_LONG).show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

}
