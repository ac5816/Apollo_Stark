package com.example.mygraph2;

import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

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

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;

import java.util.Random;
import java.util.*;
import java.text.SimpleDateFormat;

//public class MainActivity extends Activity
public class MainActivity extends AppCompatActivity
{
    Button mShowNotificationButton;
    NotificationCompat.Builder mBuilder;
    NotificationManager mNotificationManager;               // for < OREO
    private NotificationManagerCompat notificationManager;  // for >= OREO
    PendingIntent mResultPendingIntent;
    TaskStackBuilder mTaskStackBuilder;
    Intent mResultIntent;
    public static final String CHANNEL_1_ID = "channel1";
    public static final String CHANNEL_2_ID = "channel2";

    private static final Random RANDOM = new Random();
    private PointsGraphSeries<DataPoint> series;
    SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
    private double lastX = 0;
    private double lastY = 10;
    public Notification notification;





    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //-------------------------------------------------------------------------------------------
        //Graph setup
        // we get a graph view instance
        GraphView graph = (GraphView) findViewById(R.id.graph);
        series = new PointsGraphSeries<DataPoint>();
        graph.addSeries(series);
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
        Viewport viewport = graph.getViewport();
        viewport.setYAxisBoundsManual(true);
        viewport.setMinY(0);
        viewport.setMaxY(50);
        viewport.setMinX(0);
        viewport.setMaxX(10);
        viewport.setScrollable(true);
        viewport.setScrollable(true); // enables horizontal scrolling
        viewport.setScrollable(true); // enables vertical scrolling
        viewport.setScalable(true); // enables horizontal zooming and scrolling
        viewport.setScalableY(true); // enables vertical zooming and scrolling
        //------------------------------------------------------------------------------------------

        String title = "Hot Babe";
        String message = "Critical Temperature!";

        // ------------------------------------------------------------------------------------------------------
        // BUILD<OREO
        mShowNotificationButton = findViewById(R.id.btnShowNotification);
        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher); //set notification icon
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
        final Notification notification = new NotificationCompat.Builder(this, CHANNEL_1_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        if (lastY>15){
            notificationManager.notify(1,notification);
        }
        // --------------------------------------------------------------------------------------------------------
        //show notification on button click (need to change this to Boolean)
        mShowNotificationButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                //send notification for builds equal greater than OREO
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    notificationManager.notify(1,notification);
                }
                //send notification for builds less than OREO
                else{
                    mNotificationManager.notify(1,mBuilder.build());
                }
            }
        });
    }
    // ------------------------------------------------------------------------------------
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
                        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                        @Override
                        public void run() {
                            lastY = lastY+1;
                            addEntry();


                        }
                    });
                    // sleep to slow down the add of entries
                    try {
                        Thread.sleep(1100);     //sampling every 1.1s
                    } catch (InterruptedException e) {
                    }
                }
            }
        }).start();
    }
    // -------------------------------------------------------------------------------------------

    // add random data to graph
    private void addEntry() {
        // here, we choose to display max 10 points on the viewport and we scroll to end
        series.appendData(new DataPoint(lastX++,lastY), true, 3600);
    }

    private DataPoint[] getDataPoint()
    {
        DataPoint[] dp = new DataPoint[]{
                new DataPoint(new Date().getTime(),lastY)
        };
        return dp;
    }
    // function to create notification channels when Build is OREO or greater
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

}