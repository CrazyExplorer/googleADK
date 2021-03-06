package org.angel.blackwalnut.aicar_trafficsign.bth.exception.hanlder;

import org.angel.blackwalnut.aicar_trafficsign.bth.exception.BlueToothNotEnableException;
import org.angel.blackwalnut.aicar_trafficsign.bth.exception.ConnectException;
import org.angel.blackwalnut.aicar_trafficsign.bth.exception.GattException;
import org.angel.blackwalnut.aicar_trafficsign.bth.exception.NotFoundDeviceException;
import org.angel.blackwalnut.aicar_trafficsign.bth.exception.OtherException;
import org.angel.blackwalnut.aicar_trafficsign.bth.exception.ScanFailedException;
import org.angel.blackwalnut.aicar_trafficsign.bth.exception.TimeoutException;
import org.angel.blackwalnut.aicar_trafficsign.bth.utils.BleLog;

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
