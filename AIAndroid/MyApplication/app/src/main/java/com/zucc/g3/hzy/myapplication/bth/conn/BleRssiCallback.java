package com.zucc.g3.hzy.myapplication.bth.conn;


public abstract class BleRssiCallback extends BleCallback {
    public abstract void onSuccess(int rssi);
}