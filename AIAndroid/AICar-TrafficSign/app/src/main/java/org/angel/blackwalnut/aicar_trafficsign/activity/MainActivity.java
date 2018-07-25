package org.angel.blackwalnut.aicar_trafficsign.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import org.angel.blackwalnut.aicar_trafficsign.R;
import org.angel.blackwalnut.aicar_trafficsign.model.HandDetection;
import org.angel.blackwalnut.aicar_trafficsign.setting.ImageSetting;
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

import static org.angel.blackwalnut.aicar_trafficsign.setting.ImageSetting.MAXHEIGHT;
import static org.angel.blackwalnut.aicar_trafficsign.setting.ImageSetting.MAXWIDTH;


public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    /**
     * Initial & Debug
     */
    private static final String TAG = "MainActivity";

    private HandDetection handDetection;
    private TextView displayDebugMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);


//        initBlueTooth();
    }








    /**
     * Permission
     */


    /**
     * BlueTooth
     */










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
        Core.rectangle(rgbaMat, new Point(ImageSetting.MAXWIDTH - ImageSetting.MAXHEIGHT, 0), new Point(ImageSetting.MAXWIDTH, ImageSetting.MAXHEIGHT), new Scalar(255), 3);

        List<Float> boxes = (List<Float>) resultObj.get("boxes");
        List<String> classes = (List<String>) resultObj.get("classes");
        if (classes.size() != 0) {
            StringBuilder handString = new StringBuilder();
            for (int i = 0; i < classes.size(); i++) {
                float ymin = boxes.get(i * 4) * ImageSetting.MAXHEIGHT;
                float xmin = boxes.get(i * 4 + 1) * ImageSetting.MAXHEIGHT;
                float ymax = boxes.get(i * 4 + 2) * ImageSetting.MAXHEIGHT;
                float xmax = boxes.get(i * 4 + 3) * ImageSetting.MAXHEIGHT;

                Mat rightMat = new Mat(rgbaMat, new Rect(new Point(ImageSetting.MAXWIDTH - ImageSetting.MAXHEIGHT, 0), new Size(ImageSetting.MAXHEIGHT, ImageSetting.MAXHEIGHT)));
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
//                writer(0, 0, 0);
            } else if (msg.what == 1) {
                displayDebugMsg.setText("直行");
//                writer(5, 50, 50);
            }
            super.handleMessage(msg);
        }
    };
}
