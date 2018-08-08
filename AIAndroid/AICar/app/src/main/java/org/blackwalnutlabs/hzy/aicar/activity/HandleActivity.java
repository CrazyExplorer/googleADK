package org.blackwalnutlabs.hzy.aicar.activity;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import org.blackwalnutlabs.hzy.aicar.R;
import org.blackwalnutlabs.hzy.aicar.bth.BluetoothService;
import org.blackwalnutlabs.hzy.aicar.bth.conn.BleCharacterCallback;
import org.blackwalnutlabs.hzy.aicar.bth.exception.BleException;
import org.blackwalnutlabs.hzy.aicar.view.HandleView;

import java.util.Timer;
import java.util.TimerTask;

public class HandleActivity extends AppCompatActivity {
    public int mode;
    public int leftSpeed;
    public int rightSpeed;

    private HandleView rocter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_handle);
        mode=0;
        leftSpeed=0;
        rightSpeed=0;
        initRocter();
        initBlueTooth();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService();
    }


/**
 * 手柄设置
 */
private void initRocter() {
    rocter=(HandleView)findViewById(R.id.picker);
    rocter.setTXT("");
    rocter.setTxtColor("#0099cc");
    rocter.setLowerBound(0);
    rocter.setUpperBound(362);
    rocter.setTextSize(18);
    rocter.setInnerColor("#cccc66");
    rocter.setValueSetColor("#666699");
}

    private void hand_control(int angle,int maxsped){
        if(0<angle&&angle<=90){
            float factor= (float) ((90-angle)/90.0);
            mode=5;
            leftSpeed=maxsped;
            rightSpeed= (int)(maxsped*factor);
        }
        if(90<angle&&angle<=180){
            float factor=(float)((angle-90)/90.0);
            mode=4;
            leftSpeed=maxsped;
            rightSpeed=(int)(maxsped*factor);
        }
        if(180<angle&&angle<=270){
            float factor=(float)((angle-180)/90.0);
            mode=4;
            leftSpeed=(int)(maxsped*factor);
            rightSpeed=maxsped;
        }
        if(270<angle&&angle<=360){
            float factor=(float)((angle-270)/90.0);
            mode=5;
            leftSpeed= (int) (maxsped*factor);
            rightSpeed=maxsped;
        }
        if(angle<=-1){
            leftSpeed=0;
            rightSpeed=0;
            mode=5;
        }
    }


    /**
     * BlueTooth
     */

    private BluetoothService mBluetoothService;
    private BluetoothGatt gatt;
    private BluetoothGattService service;

    private void initBlueTooth() {
        bindService();
    }

    private Handler bthHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                getService();
                BLE_start_listener();
            } else if (msg.what == 2) {
                BLE_start_writer();
            }
            else if (msg.what == 3) {
                writer(mode,leftSpeed,rightSpeed);
            }
            super.handleMessage(msg);
        }
    };

    public String protocol(int protocol_data) { //自定义协议
        String protocol_msg = "";
        if (protocol_data >= 0 && 100 > protocol_data) {
            protocol_msg = Integer.toHexString(30) + Integer.toHexString(protocol_data + 30);
        } else if (protocol_data < 10000 && protocol_data >= 100) {
            protocol_msg = Integer.toHexString(protocol_data / 100 + 30) + Integer.toHexString(protocol_data % 100 + 30);
        }
        return protocol_msg;
    }

    private void writer(int controlMode, int targetL, int targetR) { //仅仅用于发送小车动作控制命令
        final BluetoothGattCharacteristic characteristic = mBluetoothService.getCharacteristic();
        mBluetoothService.write(
                characteristic.getService().getUuid().toString(),
                characteristic.getUuid().toString(),
                String.valueOf(protocol(controlMode) + protocol(targetL) + protocol(targetR) + protocol(0) + protocol(0) + protocol(0) + protocol(0) + protocol(0) + protocol(0) + protocol(4321)),//发送10进制比特
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(final BluetoothGattCharacteristic characteristic) {
                        //成功写入操作
                        StartWriteing(200);
                    }

                    @Override
                    public void onFailure(final BleException exception) {
                        StartBLEWriterAfter(150);
                    }

                    @Override
                    public void onInitiatedResult(boolean result) {

                    }
                });
    }

    private void StartBLEListenerAfter(int time) {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            public void run() {
                Message msg = new Message();
                msg.what = 1;
                bthHandler.sendMessage(msg);
            }
        };
        timer.schedule(task, time);
    }

    private void StartBLEWriterAfter(int time) {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            public void run() {
                Message msg = new Message();
                msg.what = 2;
                bthHandler.sendMessage(msg);
            }
        };
        timer.schedule(task, time);
    }

    private void getService() {
        gatt = mBluetoothService.getGatt();
        mBluetoothService.setService(gatt.getServices().get(gatt.getServices().size() - 1));
    }

    private void BLE_start_writer() {
        service = mBluetoothService.getService();
        mBluetoothService.setCharacteristic((service.getCharacteristics().get(service.getCharacteristics().size() - 2)));
        mBluetoothService.setCharaProp(1);
        Message msg = new Message();
        msg.what = 3;
        bthHandler.sendMessage(msg);
        rocter.setRockerChangeListener(new HandleView.RockerChangeListener() {
            @Override
            public void report(float value) {

                hand_control(rocter.angleResult,130);
            }
        });


    }

    private void BLE_start_listener() {
        service = mBluetoothService.getService();
        mBluetoothService.setCharacteristic((service.getCharacteristics().get(service.getCharacteristics().size() - 1)));
        mBluetoothService.setCharaProp(2);
        final BluetoothGattCharacteristic characteristic = mBluetoothService.getCharacteristic();
        mBluetoothService.notify(
                characteristic.getService().getUuid().toString(),
                characteristic.getUuid().toString(),
                new BleCharacterCallback() {

                    @Override
                    public void onSuccess(final BluetoothGattCharacteristic characteristic) {
                        HandleActivity.this.runOnUiThread(() -> {
                        });
                    }

                    @Override
                    public void onFailure(final BleException exception) {
                        HandleActivity.this.runOnUiThread(() -> {
                            StartBLEListenerAfter(100);//重新开始监听
                        });
                    }

                    @Override
                    public void onInitiatedResult(boolean result) {

                    }
                });
    }

    private void bindService() {
        Intent bindIntent = new Intent(this, BluetoothService.class);
        this.bindService(bindIntent, mFhrSCon, Context.BIND_AUTO_CREATE);
        StartBLEListenerAfter(100);
        StartBLEWriterAfter(150);
    }

    private ServiceConnection mFhrSCon = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothService = ((BluetoothService.BluetoothBinder) service).getService();
            mBluetoothService.setConnectCallback(callback);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBluetoothService = null;
        }
    };

    private BluetoothService.Callback2 callback = this::finish;

    private void unbindService() {
        this.unbindService(mFhrSCon);
    }

    private void StartWriteing(int time ) {
        Timer timer=new Timer();
        TimerTask task=new TimerTask(){
            public void run(){
                Message msg=new Message();
                msg.what=3;
                bthHandler.sendMessage(msg);
            }
        };
        timer.schedule(task, time);
    }

}
