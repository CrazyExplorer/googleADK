package com.zucc.g3.hzy.myapplication.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;
import android.widget.Button;
import com.zucc.g3.hzy.myapplication.R;

public class MainActivity extends AppCompatActivity  implements View.OnClickListener {

    private Button bluetooth;
    private Button mqtt;
    private Button collect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

    }

    @Override
    public void onClick(View v) {
        if(v==bluetooth){
            startActivity(new Intent(MainActivity.this, AnyScanActivity.class));
        }

        if(v==mqtt){
            startActivity(new Intent(MainActivity.this, MqttActivity.class));
        }

        if(v==collect){
            startActivity(new Intent(MainActivity.this, CollectActivity.class));
        }
    }

    private void initView() {


        bluetooth = (Button) findViewById(R.id.bluetooth);
        bluetooth.setOnClickListener(this);


        mqtt = (Button) findViewById(R.id.mqtt);
        mqtt.setOnClickListener(this);

        collect = (Button) findViewById(R.id.collect);
        collect.setOnClickListener(this);
    }
}

