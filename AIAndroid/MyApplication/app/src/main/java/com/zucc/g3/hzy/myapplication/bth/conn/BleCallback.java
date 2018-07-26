
package com.zucc.g3.hzy.myapplication.bth.conn;

import android.bluetooth.BluetoothGattCallback;

import com.zucc.g3.hzy.myapplication.bth.exception.BleException;


public abstract class BleCallback {

    private BluetoothGattCallback bluetoothGattCallback;

    public BleCallback setBluetoothGattCallback(BluetoothGattCallback bluetoothGattCallback) {
        this.bluetoothGattCallback = bluetoothGattCallback;
        return this;
    }

    public BluetoothGattCallback getBluetoothGattCallback() {
        return bluetoothGattCallback;
    }

    public abstract void onFailure(BleException exception);

    public abstract void onInitiatedResult(boolean result);
}