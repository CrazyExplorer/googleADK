package com.zucc.g3.hzy.myapplication.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.zucc.g3.hzy.myapplication.R;


public class ObjGameSettingActivity extends Activity  implements Button.OnClickListener{

    private EditText userName,broker;
    private Button pubButton;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_obj_game_setting);
        broker=(EditText)findViewById(R.id.broker);
        userName=(EditText)findViewById(R.id.username);
        pubButton=(Button)findViewById(R.id.pubButton);
        pubButton.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {

        if (v == pubButton) {
            if (userName.getText().toString().length() > 0&&broker.getText().toString().length()>0) {
                Intent intent = new Intent();
                intent.putExtra("UserName", ""+userName.getText().toString());
                intent.putExtra("Broker", ""+broker.getText().toString());
                intent.setClass(ObjGameSettingActivity.this, ObjGameActivity.class);
                ObjGameSettingActivity.this.startActivity(intent);

            }
        }
    }

}

