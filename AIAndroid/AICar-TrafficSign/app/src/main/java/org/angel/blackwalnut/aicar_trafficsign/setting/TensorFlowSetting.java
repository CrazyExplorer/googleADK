package org.angel.blackwalnut.aicar_trafficsign.setting;

public class TensorFlowSetting {
    public static final String INPUTNAME = "image_tensor";
    public static final String[] OUTPUTNAMES = {"detection_boxes", "detection_scores", "detection_classes", "num_detections"};
    public static final String MODELFILE = "file:///android_asset/retrained_graph.pb";
    public static final String HANDMAP[] = {"limit40", "pedestrian", "gostraight", "mute", "turnaround", "banturnright", "sidewalk", "banturnleft", "turnright", "slow"};
}
