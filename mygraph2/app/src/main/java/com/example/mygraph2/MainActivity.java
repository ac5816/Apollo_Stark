package com.example.mygraph2;

import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;

import java.util.Random;
import java.util.*;

public class MainActivity extends AppCompatActivity
{
    private static final Random RANDOM = new Random();
    private PointsGraphSeries<DataPoint> series;
    SimpleDateFormat sdf = new SimpleDateFormat("mm:ss:");
    private double lastX = 0;
    private double lastY = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // we get a graph view instance
        GraphView graph = (GraphView) findViewById(R.id.graph);

        //data
        series = new PointsGraphSeries<DataPoint>();
        series = new PointsGraphSeries<>(getDataPoint());
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

    }

    private DataPoint[] getDataPoint()
    {
        DataPoint[] dp = new DataPoint[]
        {
                new DataPoint(new Date().getTime(),lastY),
                new DataPoint(new Date().getTime(),lastY),
                new DataPoint(new Date().getTime(),lastY),
                new DataPoint(new Date().getTime(),lastY),
                new DataPoint(new Date().getTime(),lastY),

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
                        Thread.sleep(1100);     //sampling every 1.1s
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
        //series.appendData(new DataPoint(lastX++, lastY++), true, 3600);
    }

}
