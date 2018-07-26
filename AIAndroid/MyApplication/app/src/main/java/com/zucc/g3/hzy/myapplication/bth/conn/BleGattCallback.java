
package com.zucc.g3.hzy.myapplication.bth.conn;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;

import com.zucc.g3.hzy.myapplication.bth.data.ScanResult;
import com.zucc.g3.hzy.myapplication.bth.exception.BleException;


public abstract class BleGattCallback extends BluetoothGattCallback {

    public void onFoundDevice(ScanResult scanResult) {
    }

    public void onConnecting(BluetoothGatt gatt, int status) {
    }

    public abstract void onConnectError(BleException exception);

    public abstract void onConnectSuccess(BluetoothGatt gatt, int status);

    public abstract void onDisConnected(BluetoothGatt gatt, int status, BleException exception);

}