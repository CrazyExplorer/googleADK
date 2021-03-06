package com.zucc.g3.hzy.myapplication.activity;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.zucc.g3.hzy.myapplication.R;
import com.zucc.g3.hzy.myapplication.model.HandDetection;
import com.zucc.g3.hzy.myapplication.setting.ImageSetting;
import com.zucc.g3.hzy.myapplication.util.PermissionUtils;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

import java.util.HashMap;

import java.util.Map;

import java.util.List;




import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import com.zucc.g3.hzy.myapplication.bth.BluetoothService;
import com.zucc.g3.hzy.myapplication.bth.conn.BleCharacterCallback;
import com.zucc.g3.hzy.myapplication.bth.exception.BleException;

import java.util.Timer;
import java.util.TimerTask;

import static com.zucc.g3.hzy.myapplication.setting.ImageSetting.MAXHEIGHT;
import static com.zucc.g3.hzy.myapplication.setting.ImageSetting.MAXWIDTH;

public class ObjActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    /**
     * Initial & Debug
     */
    private static final String TAG = "ObjGameActivity";


    private HandDetection handDetection;
    private TextView displayDebugMsg;
    public int mode;
    public int leftSpeed;
    public int rightSpeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_obj);
        mode=0;
        leftSpeed=0;
        rightSpeed=0;
        initDebug();
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
                    Toast.makeText(ObjActivity.this, "Result Permission Grant CODE_CAMERA", Toast.LENGTH_SHORT).show();
                    break;
                case PermissionUtils.CODE_READ_EXTERNAL_STORAGE:
                    Toast.makeText(ObjActivity.this, "Result Permission Grant CODE_READ_EXTERNAL_STORAGE", Toast.LENGTH_SHORT).show();
                    break;
                case PermissionUtils.CODE_WRITE_EXTERNAL_STORAGE:
                    Toast.makeText(ObjActivity.this, "Result Permission Grant CODE_WRITE_EXTERNAL_STORAGE", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(ObjActivity.this, "Result Permission Grant CODE_MULTI_PERMISSION", Toast.LENGTH_SHORT).show();
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
                        ObjActivity.this.runOnUiThread(() -> {
                        });
                    }

                    @Override
                    public void onFailure(final BleException exception) {
                        ObjActivity.this.runOnUiThread(() -> {
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

        Map<String, Object> resultObj = handDetection.detectTrafficSign(rgbaMat);
        Core.rectangle(rgbaMat, new Point(MAXWIDTH - MAXHEIGHT, 0), new Point(MAXWIDTH, MAXHEIGHT), new Scalar(255), 3);

        List<Float> boxes = (List<Float>) resultObj.get("boxes");
        List<String> classes = (List<String>) resultObj.get("classes");
        if (classes.size() != 0) {
            StringBuilder handString = new StringBuilder();
            for (int i = 0; i < classes.size(); i++) {
                float ymin = boxes.get(i * 4) * MAXHEIGHT;
                float xmin = boxes.get(i * 4 + 1) * MAXHEIGHT;
                float ymax = boxes.get(i * 4 + 2) * MAXHEIGHT;
                float xmax = boxes.get(i * 4 + 3) * MAXHEIGHT;

                Mat rightMat = new Mat(rgbaMat, new Rect(new Point(MAXWIDTH - MAXHEIGHT, 0), new Size(MAXHEIGHT, MAXHEIGHT)));
                Core.rectangle(rightMat, new Point(xmin, ymin), new Point(xmax, ymax), new Scalar(255), 3);
                handString.append("").append(classes.get(i));
            }
            Bundle bundle = new Bundle();
            bundle.putString("TrafficSign", handString.toString());
            Message message = new Message();
            message.setData(bundle);
            message.what = 0;
            controlHandler.sendMessage(message);

        } else {
            controlHandler.sendEmptyMessage(1);
        }

        return rgbaMat;
    }

    private Handler controlHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                displayDebugMsg.setText(msg.getData().getString("TrafficSign"));
                leftSpeed=0;rightSpeed=0;mode=0;
            } else if (msg.what == 1) {
                displayDebugMsg.setText("直行");
                mode=5;leftSpeed=50;rightSpeed=50;

            }
            super.handleMessage(msg);
        }
    };
}

