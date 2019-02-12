package com.example.splash_try;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class Home extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_home);

        Thread xyz = new Thread(){
            public void run(){
                try{
                    sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace ();
                }
                finally{
                    Intent intent = new Intent(Home.this,MainActivity.class);
                    startActivity (intent);
                }
            }
        };
    }

    @Override
    protected  void onPause(){
        super.onPause ();
        finish ();
    }
}
