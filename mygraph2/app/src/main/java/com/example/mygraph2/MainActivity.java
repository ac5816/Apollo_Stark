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
import java.util.ArrayList;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;

import java.util.Random;

public class MainActivity extends AppCompatActivity
{
    private static final Random RANDOM = new Random();
    private PointsGraphSeries<DataPoint> series;
    private int lastX = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // we get a graph view instance
        GraphView graph = (GraphView) findViewById(R.id.graph);

        //data
        series = new PointsGraphSeries<DataPoint>();
        graph.addSeries(series);

        // customize a little bit viewport
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
        series.appendData(new DataPoint(lastX++, RANDOM.nextDouble() * 10d), true, 3600);
    }

}
