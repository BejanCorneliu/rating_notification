package com.themejunky.module.notifyrating;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.example.tj_notifyrating.Module_NotifyRating;

public class Main extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getIntent().hasExtra(getResources().getString(com.example.tj_notifyrating.R.string.intent_key_notification))) {
            Log.d("debugMode_NotifyRating","VENIT DIN CLICK ");
            new Module_NotifyRating(this,false);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                new Module_NotifyRating(Main.this,Main.class,"com.keyboard").set_TextAndIcon("Title","Subtite",R.drawable.app_logo).set_DebugMode(true).set_HoursAndRepeateTimes(30000,10,20000).start();
            }
        }).start();

    }
}
