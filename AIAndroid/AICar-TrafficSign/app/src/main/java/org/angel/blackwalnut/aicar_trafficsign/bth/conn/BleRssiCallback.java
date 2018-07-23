package org.angel.blackwalnut.aicar_trafficsign.bth.conn;


public abstract class BleRssiCallback extends BleCallback {
    public abstract void onSuccess(int rssi);
}