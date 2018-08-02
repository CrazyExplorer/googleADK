package com.zucc.g3.hzy.myapplication.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.zucc.g3.hzy.myapplication.R;


public class MidActivity extends AppCompatActivity  implements View.OnClickListener {


    private Button object_detection;
    private Button handle;
    private Button gravity_handle;
    private Button developer_mode;
    private Button  objgame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mid);
        initView();
    }

    @Override
    public void onClick(View v) {
        if(v==handle){
            startActivity(new Intent(MidActivity.this, HandleActivity.class));
        }

        if(v==object_detection){
            startActivity(new Intent(MidActivity.this, ObjActivity.class));
        }

        if(v==developer_mode){
            startActivity(new Intent(MidActivity.this, DevelopeActivity.class));
        }

        if(v==gravity_handle){
            startActivity(new Intent(MidActivity.this, GravityActivity.class));
        }

        if(v==objgame){
            startActivity(new Intent(MidActivity.this, ObjGameSettingActivity.class));
        }
    }

    private void initView() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        object_detection = (Button) findViewById(R.id.object_detection);
        object_detection.setOnClickListener(this);

        handle = (Button) findViewById(R.id.handle);
        handle.setOnClickListener(this);

        gravity_handle = (Button) findViewById(R.id.gravity_handle);
        gravity_handle.setOnClickListener(this);

        developer_mode = (Button) findViewById(R.id.developer_mode);
        developer_mode.setOnClickListener(this);

        objgame = (Button) findViewById(R.id.objgame);
        objgame.setOnClickListener(this);


    }
}

