package com.example.continuous_thermometer;

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
import android.widget.TextView;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;


public class MainActivity extends AppCompatActivity {

    MqttHelper mqttHelper;

    TextView TempReceived, Temperature;
    TextView TimeReceived, Time;

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
        //dat

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
                            lastY = lastY+1;

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
    }

    private void startMqtt(){
        mqttHelper = new MqttHelper(getApplicationContext());
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {

            }

            @Override
            public void connectionLost(Throwable throwable) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                Log.w("mqtt_message_received",mqttMessage.toString());
                String temperature = mqttMessage.toString();
                lastY = Double.parseDouble(temperature);
                //lastY = 41;
                TimeReceived.setText(temperature+ DEGREE);
                if (lastY > 40) {
                    CL.setBackgroundColor(Color.parseColor("#FFC4B7"));
                    getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#CB3517")));
                    Temperature.setTextColor(Color.rgb(97,16,0));
                    Time.setTextColor(Color.rgb(97,16,0));
                    setTitle ("Hot Babe");
                    seriesLine.setColor(Color.rgb(198,0,0));
                    series.setColor(Color.rgb(198,0,0));
                    //TimeReceived.setTextColor(0x520E00);
                }

                else {
                    CL.setBackgroundColor(Color.parseColor("#D6E2F0"));
                    getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#4578B2")));
                    Temperature.setTextColor(Color.rgb(4,25,50));
                    Time.setTextColor(Color.rgb(4,25,50));
                    setTitle ("Cool Babe");
                    seriesLine.setColor(Color.rgb(0,22,165));
                    series.setColor(Color.rgb(0,22,165));
                    //TimeReceived.setTextColor(0x520E00);
                }
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat mdformat = new SimpleDateFormat("HH:mm:ss");
                TempReceived.setText(mdformat.format(calendar.getTime()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }
}
