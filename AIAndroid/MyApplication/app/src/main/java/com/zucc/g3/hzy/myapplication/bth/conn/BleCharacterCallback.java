
package com.zucc.g3.hzy.myapplication.bth.conn;
import android.bluetooth.BluetoothGattCharacteristic;


public abstract class BleCharacterCallback extends BleCallback {
    public abstract void onSuccess(BluetoothGattCharacteristic characteristic);
}