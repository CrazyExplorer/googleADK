package org.blackwalnutlabs.hzy.aicar.activity;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;


import org.blackwalnutlabs.hzy.aicar.R;
import org.blackwalnutlabs.hzy.aicar.bth.BluetoothService;
import org.blackwalnutlabs.hzy.aicar.bth.conn.BleCharacterCallback;
import org.blackwalnutlabs.hzy.aicar.bth.exception.BleException;

import java.util.Timer;
import java.util.TimerTask;

public class GravityActivity  extends AppCompatActivity {

    public int mode;
    public int leftSpeed;
    public int rightSpeed;

    TextView test=null;
    TextView accelerometer=null; //加速度
    TextView orientation=null; //方向
    TextView gravity=null; //纯重力
    TextView rotation_vector=null; //旋转矢量

    SensorManager smanger;
    Sensor sensor_gravity;
    Sensor sensor_accelerometer;
    Sensor sensor_orientation;
    Sensor sensor_rotation_vector;



    private SensorEventListener myAccelerometerListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            float X = sensorEvent.values[0];
            float Y = sensorEvent.values[1];
            float Z = sensorEvent.values[2];
            accelerometer.setText("X:"+X+"Y:"+Y+"Z:"+Z);
            double sqr = Math.sqrt(X*X+Y*Y+Z*Z);
            double alpha = Math.toDegrees(Math.atan(Math.sqrt(Math.pow(X,2) + Math.pow(Z,2))/Y ));//左右旋转角
            double beta = Math.toDegrees(Math.atan(Math.sqrt(Math.pow(Y,2) + Math.pow(Z,2))/X ));//上下旋转角
            double alpha_factor=1;
            if(sqr<11 ){

                if( (beta<89&&beta>-89) || (alpha<89&&alpha>-89)){

                    if(0<=alpha){
                        alpha=(90-alpha);

                    }else {
                        alpha=(-90-alpha);
                    }

                    if(alpha<-45){
                        alpha_factor=-1;
                    }else if(-45<=alpha&&alpha<=0){
                        alpha_factor=alpha/45;
                    }else if(0<alpha&&alpha<=45){
                        alpha_factor=alpha/45;
                    }else if(45<alpha){
                        alpha_factor=1;
                    }

                    if(beta<=0){
                        mode=5;
                        leftSpeed=0;
                        rightSpeed=0;
                    }
                    if(70<=beta&&beta<89){
                        mode=5;
                        if(alpha_factor>0){
                            leftSpeed=150;
                            rightSpeed=(int)(150-150*alpha_factor);
                        }else if(alpha_factor<=0){
                            leftSpeed=(int)(150+150*alpha_factor);
                            rightSpeed=150;
                        }
                    }
                    if(45<=beta&&beta<70){
                        mode=5;
                        if(alpha_factor>0){
                            leftSpeed=(int)((beta-45)*150/25);
                            rightSpeed=(int)((beta-45)*150/25)-(int)(((beta-45)*150/25)*alpha_factor);
                        }else if(alpha_factor<=0){
                            leftSpeed=(int)((beta-45)*150/25)+(int)(((beta-45)*150/25)*alpha_factor);
                            rightSpeed=(int)((beta-45)*150/25);
                        }
                    }
                    if(10<beta&&beta<45){

                        mode=4;

                        if(alpha_factor>0){
                            leftSpeed=(int)((1-1*alpha_factor)*(45-beta)*150/35);
                            rightSpeed=(int)((45-beta)*150/35);
                        }else if(alpha_factor<=0){
                            leftSpeed=(int)((45-beta)*150/35);
                            rightSpeed=(int)((1+1*alpha_factor)*(45-beta)*150/35);
                        }
                    }
                    if(0<beta&&beta<=10){
                        mode=4;
                        if(alpha_factor>0){
                            leftSpeed=150;
                            rightSpeed=(int)(150-150*alpha_factor);
                        }else if(alpha_factor<=0){

                            leftSpeed=(int)(150+150*alpha_factor);
                            rightSpeed=150;
                        }

                    }
                    test.setText("leftSpeed"+leftSpeed+"rightSpeed"+rightSpeed);

                }
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private SensorEventListener myGravityListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            float X = sensorEvent.values[0];
            float Y = sensorEvent.values[1];
            float Z = sensorEvent.values[2];
            gravity.setText("X:"+X+"Y:"+Y+"Z:"+Z);

        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            //精度发生改变
        }
    };

    private SensorEventListener myOrientationListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            float Azimuth = sensorEvent.values[0];
            float Pitch = sensorEvent.values[1];
            float Roll = sensorEvent.values[2];
            orientation.setText("Azimuth:"+Azimuth+"Pitch:"+Pitch+"Roll:"+Roll);
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private SensorEventListener myRotationVectorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            float X = sensorEvent.values[0];
            float Y = sensorEvent.values[1];
            float Z = sensorEvent.values[2];
            float C = sensorEvent.values[3];
            rotation_vector.setText("X*sin(θ/2):"+X+"Y*sin(θ/2):"+Y+"Z*sin(θ/2):"+Z+"cos(θ/2)"+C);
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_gravity);
        mode=0;
        leftSpeed=0;
        rightSpeed=0;
        test = (TextView) findViewById(R.id.textView);
        rotation_vector=(TextView) findViewById(R.id.ROTATION_VECTOR);
        gravity=(TextView) findViewById(R.id.GRAVITY);
        orientation=(TextView) findViewById(R.id.ORIENTATION);
        accelerometer=(TextView) findViewById(R.id.ACCELEROMETER);

        smanger = (SensorManager) getSystemService(SENSOR_SERVICE);

        sensor_accelerometer = smanger.getDefaultSensor(Sensor.TYPE_ACCELEROMETER );
        sensor_gravity = smanger.getDefaultSensor(Sensor.TYPE_GRAVITY );
        sensor_orientation = smanger.getDefaultSensor(Sensor.TYPE_ORIENTATION); //这个现在已经不怎么使用了
        sensor_rotation_vector = smanger.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR );

        smanger.registerListener(myAccelerometerListener,sensor_accelerometer,100000);
        smanger.registerListener(myGravityListener,sensor_gravity,100000);
        smanger.registerListener(myOrientationListener,sensor_orientation,100000);
        smanger.registerListener(myRotationVectorListener,sensor_rotation_vector,100000);

        initBlueTooth();
    }



    @Override
    protected void onPause() {
        smanger.unregisterListener(myAccelerometerListener,sensor_gravity);
        Log.i("12345671", "结果: "+"onPause");
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.i("12345671", "结果: "+"onResume");
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mode=0;leftSpeed=0;rightSpeed=0;
        writer(mode,leftSpeed,rightSpeed);
        unbindService();
        Log.i("12345671", "结果: "+"onDestroy");
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
                        StartWriteing(100);
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
                        GravityActivity.this.runOnUiThread(() -> {
                        });
                    }

                    @Override
                    public void onFailure(final BleException exception) {
                        GravityActivity.this.runOnUiThread(() -> {
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
