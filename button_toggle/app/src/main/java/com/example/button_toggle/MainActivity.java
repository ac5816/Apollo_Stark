package com.example.button_toggle;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    TextView temp_text;
    Switch temp_toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_main);
        temp_toggle = findViewById (R.id.myswitch);
        temp_toggle.setChecked (true);//set current state of switch to true
        temp_toggle.setTextOff ("\u2109");
        temp_toggle.setTextOn ("\2103");

        temp_toggle.setOnCheckedChangeListener (new CompoundButton.OnCheckedChangeListener () {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    temp_text.setText ("\u2103");

                }
                else{
                    temp_text.setText("u\2109");
                }
            }
        });
    }
}
