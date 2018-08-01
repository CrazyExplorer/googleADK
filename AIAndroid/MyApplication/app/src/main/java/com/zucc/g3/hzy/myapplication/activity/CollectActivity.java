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
import android.widget.Toast;
import com.zucc.g3.hzy.myapplication.R;
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
import android.graphics.Bitmap;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import org.opencv.android.Utils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


public class CollectActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    /**
     * Initial & Debug
     */
    private static final String TAG = "MainActivity";
    private boolean ReadToTakePhoto;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_collect);
        ReadToTakePhoto=true;
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (openCvCameraView != null) {
            openCvCameraView.disableView();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        doRequestPermission();
    }

    private void doRequestPermission() {
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
    }

    private void initCamera() {
        openCvCameraView = findViewById(R.id.HelloOpenCvView);
        openCvCameraView.setVisibility(SurfaceView.VISIBLE);
        openCvCameraView.setCvCameraViewListener(this);
        openCvCameraView.setMaxFrameSize(ImageSetting.MAXWIDTH, ImageSetting.MAXHEIGHT);
        openCvCameraView.enableFpsMeter();
        openCvCameraView.enableView();
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
                    Toast.makeText(CollectActivity.this, "Result Permission Grant CODE_CAMERA", Toast.LENGTH_SHORT).show();
                    break;
                case PermissionUtils.CODE_READ_EXTERNAL_STORAGE:
                    Toast.makeText(CollectActivity.this, "Result Permission Grant CODE_READ_EXTERNAL_STORAGE", Toast.LENGTH_SHORT).show();
                    break;
                case PermissionUtils.CODE_WRITE_EXTERNAL_STORAGE:
                    Toast.makeText(CollectActivity.this, "Result Permission Grant CODE_WRITE_EXTERNAL_STORAGE", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(CollectActivity.this, "Result Permission Grant CODE_MULTI_PERMISSION", Toast.LENGTH_SHORT).show();
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

    private boolean canSave;

    @Override
    public void onCameraViewStarted(int width, int height) {
        canSave = false;
        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                canSave = true;

            }
        });
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgbaMat = inputFrame.rgba();

        if (canSave) {
            canSave = false;
            if(ReadToTakePhoto){
                ReadToTakePhoto=false;
                photoDelay(1000);
                handler.sendEmptyMessage(0);
                Mat centerMat = new Mat(rgbaMat, new Rect(new Point(ImageSetting.MAXWIDTH / 2 - ImageSetting.MAXHEIGHT / 2, 0), new Size(ImageSetting.MAXHEIGHT, ImageSetting.MAXHEIGHT)));
                mat2PngFile(centerMat, new Date().getTime());
            }else{
                Message msg=new Message();
                msg.what=1;
                handler.sendMessage(msg);
            }

        }

        Core.rectangle(rgbaMat, new Point(ImageSetting.MAXWIDTH / 2 - ImageSetting.MAXHEIGHT / 2, 0), new Point(ImageSetting.MAXWIDTH / 2 + ImageSetting.MAXHEIGHT / 2, ImageSetting.MAXHEIGHT), new Scalar(255), 1);

        return rgbaMat;
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            if(msg.what==2){
                ReadToTakePhoto=true;
            }
            if(msg.what==1){
                Toast.makeText(CollectActivity.this, "请不要连拍", Toast.LENGTH_SHORT).show();
            }
            super.handleMessage(msg);
            Toast.makeText(CollectActivity.this, "Saved Photo", Toast.LENGTH_SHORT).show();
        }
    };

    private void photoDelay(int time ) {
        Timer timer=new Timer();
        TimerTask task=new TimerTask(){
            public void run(){
                Message msg=new Message();
                msg.what=2;
                handler.sendMessage(msg);
            }
        };
        timer.schedule(task, time);
    }

    private static File mat2PngFile(Mat mat, long fileName) {
        Bitmap bmp = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bmp);
        Log.e(TAG, "Transform: finish! ");

        File file = null;
        FileOutputStream fos;
        try {
            file = new File(Environment.getExternalStorageDirectory() + "/AICar/" + fileName + ".jpg");
            if (!file.getParentFile().exists()) {
                boolean isCreated = file.getParentFile().mkdir();
                if (!isCreated) {
                    return file;
                }
            }
            fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }
}