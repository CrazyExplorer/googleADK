package com.zucc.g3.hzy.myapplication.bth.exception.hanlder;

import com.zucc.g3.hzy.myapplication.bth.exception.BlueToothNotEnableException;
import com.zucc.g3.hzy.myapplication.bth.exception.ConnectException;
import com.zucc.g3.hzy.myapplication.bth.exception.GattException;
import com.zucc.g3.hzy.myapplication.bth.exception.NotFoundDeviceException;
import com.zucc.g3.hzy.myapplication.bth.exception.OtherException;
import com.zucc.g3.hzy.myapplication.bth.exception.ScanFailedException;
import com.zucc.g3.hzy.myapplication.bth.exception.TimeoutException;
import com.zucc.g3.hzy.myapplication.bth.utils.BleLog;

public class DefaultBleExceptionHandler extends BleExceptionHandler {

    private static final String TAG = "BleExceptionHandler";

    public DefaultBleExceptionHandler() {

    }

    @Override
    protected void onConnectException(ConnectException e) {
        BleLog.e(TAG, e.getDescription());
    }

    @Override
    protected void onGattException(GattException e) {
        BleLog.e(TAG, e.getDescription());
    }

    @Override
    protected void onTimeoutException(TimeoutException e) {
        BleLog.e(TAG, e.getDescription());
    }

    @Override
    protected void onNotFoundDeviceException(NotFoundDeviceException e) {
        BleLog.e(TAG, e.getDescription());
    }

    @Override
    protected void onBlueToothNotEnableException(BlueToothNotEnableException e) {
        BleLog.e(TAG, e.getDescription());
    }

    @Override
    protected void onScanFailedException(ScanFailedException e) {
        BleLog.e(TAG, e.getDescription());
    }

    @Override
    protected void onOtherException(OtherException e) {
        BleLog.e(TAG, e.getDescription());
    }
}
