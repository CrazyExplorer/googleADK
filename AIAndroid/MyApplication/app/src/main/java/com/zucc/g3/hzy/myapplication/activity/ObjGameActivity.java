package com.zucc.g3.hzy.myapplication.activity;


import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.zucc.g3.hzy.myapplication.R;
import com.zucc.g3.hzy.myapplication.bth.BluetoothService;
import com.zucc.g3.hzy.myapplication.bth.conn.BleCharacterCallback;
import com.zucc.g3.hzy.myapplication.bth.exception.BleException;
import com.zucc.g3.hzy.myapplication.model.HandDetection;
import com.zucc.g3.hzy.myapplication.util.PermissionUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import static com.zucc.g3.hzy.myapplication.setting.ImageSetting.MAXHEIGHT;
import static com.zucc.g3.hzy.myapplication.setting.ImageSetting.MAXWIDTH;

public class ObjGameActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    /**
     * Initial & Debug
     */
    private static final String TAG = "ObjGameActivity";

    private final static String username="";
    private final static String password="";

    private HandDetection handDetection;
    private TextView displayDebugMsg;
    public int mode;
    public int leftSpeed;
    public int rightSpeed;
    private MqttAsyncClient mqttClient;

    private String Broker="";
    private String UserName="";
    private boolean trigger=false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        UserName = intent.getStringExtra("UserName");
        Broker = intent.getStringExtra("Broker");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_obj_game);
        mode=0;
        leftSpeed=0;
        rightSpeed=0;
        initDebug();
        connectBroker();
        initBlueTooth();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (openCvCameraView != null) {
            openCvCameraView.disableView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermission();
        } else {
            initCamera();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (openCvCameraView != null) {
            openCvCameraView.disableView();
        }
        unbindService();
    }

    private void initDebug() {
        displayDebugMsg = findViewById(R.id.displayDebugMsg);
    }

    /**
     * Permission
     */
    private void requestPermission() {
        PermissionUtils.requestMultiPermissions(this, mPermissionGrant);
    }


    private PermissionUtils.PermissionGrant mPermissionGrant = new PermissionUtils.PermissionGrant() {
        @Override
        public void onPermissionGranted(int requestCode) {
            switch (requestCode) {
                case PermissionUtils.CODE_CAMERA:
                    Toast.makeText(ObjGameActivity.this, "Result Permission Grant CODE_CAMERA", Toast.LENGTH_SHORT).show();
                    break;
                case PermissionUtils.CODE_READ_EXTERNAL_STORAGE:
                    Toast.makeText(ObjGameActivity.this, "Result Permission Grant CODE_READ_EXTERNAL_STORAGE", Toast.LENGTH_SHORT).show();
                    break;
                case PermissionUtils.CODE_WRITE_EXTERNAL_STORAGE:
                    Toast.makeText(ObjGameActivity.this, "Result Permission Grant CODE_WRITE_EXTERNAL_STORAGE", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(ObjGameActivity.this, "Result Permission Grant CODE_MULTI_PERMISSION", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        PermissionUtils.requestPermissionsResult(this, requestCode, permissions, grantResults, mPermissionGrant);
        initCamera();
    }

    /**
     * MQTT
     */

    private Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg){
            if(msg.what==1){
                Toast.makeText(ObjGameActivity.this,"连接成功",Toast.LENGTH_SHORT).show();
                try {
                    mqttClient.subscribe("InnoCamp18/TSD/sign", 2);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }else if(msg.what==2){
                Toast.makeText(ObjGameActivity.this,"连接丢失，进行重连",Toast.LENGTH_SHORT).show();
            }else if(msg.what==3){
                Toast.makeText(ObjGameActivity.this,"连接失败",Toast.LENGTH_SHORT).show();
            }else if(msg.what==4){
                //MQTT收到数据操作
                trigger=true;
                Toast.makeText(ObjGameActivity.this,"ok",Toast.LENGTH_SHORT).show();

            }
            super.handleMessage(msg);
        }
    };

    private IMqttActionListener mqttActionListener=new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            //连接成功处理
            Message msg=new Message();
            msg.what=1;
            handler.sendMessage(msg);
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            exception.printStackTrace();
            //连接失败处理
            Message msg=new Message();
            msg.what=3;
            handler.sendMessage(msg);
        }
    };

    private MqttCallback callback=new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {
            //连接断开
            Message msg=new Message();
            msg.what=2;
            handler.sendMessage(msg);
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            //消息到达
//            subMsg.append(new String(message.getPayload())+"\n"); //不能直接修改,需要在UI线程中操作
            Message msg=new Message();
            msg.what=4;
            msg.obj=new String(message.getPayload())+"\n";
            handler.sendMessage(msg);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            //消息发送完成
        }
    };

    private void connectBroker(){
        try {
            mqttClient=new MqttAsyncClient("tcp://"+Broker,"ClientID"+Math.random(),new MemoryPersistence());
//            mqttClient.connect(getOptions());
            mqttClient.connect(getOptions(),null,mqttActionListener);
            mqttClient.setCallback(callback);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private MqttConnectOptions getOptions(){

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);//重连不保持状态
        if(username!=null&&username.length()>0&&password!=null&&password.length()>0){
            options.setUserName(username);//设置服务器账号密码
            options.setPassword(password.toCharArray());
        }
        options.setConnectionTimeout(10);//设置连接超时时间
        options.setKeepAliveInterval(30);//设置保持活动时间，超过时间没有消息收发将会触发ping消息确认
        return options;
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
                        ObjGameActivity.this.runOnUiThread(() -> {
                        });
                    }

                    @Override
                    public void onFailure(final BleException exception) {
                        ObjGameActivity.this.runOnUiThread(() -> {
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
            mBluetoothService.setConnectCallback(callbackbth);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBluetoothService = null;
        }
    };

    private BluetoothService.Callback2 callbackbth = this::finish;

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


    /**
     * OpenCV
     */
    private CameraBridgeViewBase openCvCameraView;

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV not loaded");
        } else {
            Log.d(TAG, "OpenCV loaded");
        }
    }

    private void initCamera() {
        openCvCameraView = findViewById(R.id.HelloOpenCvView);
        openCvCameraView.setVisibility(SurfaceView.VISIBLE);
        openCvCameraView.setCvCameraViewListener(this);
        openCvCameraView.setMaxFrameSize(MAXWIDTH, MAXHEIGHT);
        openCvCameraView.enableFpsMeter();
        openCvCameraView.enableView();
    }

    private void initModel() {
        Map<String, Object> tmpMap = new HashMap<>();

        Map<String, Object> funMap = new HashMap<>();

        Map<String, SeekBar> debugMap = new HashMap<>();

        Map<String, Object> othersMap = new HashMap<>();
        othersMap.put("assets", getAssets());

        handDetection = new HandDetection(tmpMap, funMap, debugMap, othersMap);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        initModel();
    }

    @Override
    public void onCameraViewStopped() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgbaMat = inputFrame.rgba();
        if(trigger)
        {
            Map<String, Object> resultObj = handDetection.detectTrafficSign(rgbaMat);
            Core.rectangle(rgbaMat, new Point(MAXWIDTH - MAXHEIGHT, 0), new Point(MAXWIDTH, MAXHEIGHT), new Scalar(255), 3);
            List<Float> boxes = (List<Float>) resultObj.get("boxes");
            List<String> classes = (List<String>) resultObj.get("classes");

            if (classes.size() != 0)
                { //如果有物体
                    StringBuilder handString = new StringBuilder();

                    String Sign="";
                    float probability=0;
                    for (int i = 0; i < classes.size(); i++) {
                        float ymin = boxes.get(i * 4) * MAXHEIGHT;
                        float xmin = boxes.get(i * 4 + 1) * MAXHEIGHT;
                        float ymax = boxes.get(i * 4 + 2) * MAXHEIGHT;
                        float xmax = boxes.get(i * 4 + 3) * MAXHEIGHT;

                        Mat rightMat = new Mat(rgbaMat, new Rect(new Point(MAXWIDTH - MAXHEIGHT, 0), new Size(MAXHEIGHT, MAXHEIGHT)));
                        Core.rectangle(rightMat, new Point(xmin, ymin), new Point(xmax, ymax), new Scalar(255), 3);


                        if(probability<Float.parseFloat(classes.get(i).split("\\:")[1])){//获取概率最高的一次
                            Sign=classes.get(i).split("\\:")[0];
                            probability=Float.parseFloat(classes.get(i).split("\\:")[1]);
                        }


                        handString.append("").append(classes.get(i));
                    }
                    if(probability>=0.70){//概率大于0.70认为合格可以发送
                        JSONObject obj = new JSONObject();
                        try {
                            obj.put("UserName", UserName);
                            obj.put("Answer", Sign+":"+probability);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Bundle bundle = new Bundle();
                        bundle.putString("TrafficSign",obj.toString());
                        trigger=false;

                        Message message = new Message();
                        message.setData(bundle);
                        message.what = 0;
                        controlHandler.sendMessage(message);
                    }
                }
        }
        else//如果没有物体
        {
        controlHandler.sendEmptyMessage(1);
        }
        return rgbaMat;
    }

    private Handler controlHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                String ans=msg.getData().getString("TrafficSign");
                displayDebugMsg.setText(ans);

                try {
                    mqttClient.publish("InnoCamp18/TSD/correct", ans.getBytes(), 2, false);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                leftSpeed=0;rightSpeed=0;mode=0;
            } else if (msg.what == 1) {

                mode=5;leftSpeed=50;rightSpeed=50;

            }
            super.handleMessage(msg);
        }
    };
}

