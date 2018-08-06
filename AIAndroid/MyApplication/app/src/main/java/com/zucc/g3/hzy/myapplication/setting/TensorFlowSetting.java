package com.zucc.g3.hzy.myapplication.setting;

public class TensorFlowSetting {
    public static final String INPUTNAME = "image_tensor";
    public static final String[] OUTPUTNAMES = {"detection_boxes", "detection_scores", "detection_classes", "num_detections"};
    public static final String MODELFILE = "file:///android_asset/retrained_graph.pb";
    public static final String HANDMAP[] = {"gostraightturnright", "gostraight", "hornplease", "turnaround", "turnright", "sidewalk", "pedestrian", "slow", "turnleft", "vehicles"};
}
