package com.zucc.g3.hzy.myapplication.bth.exception;


public class BlueToothNotEnableException extends BleException {
    public BlueToothNotEnableException() {
        super(ERROR_CODE_BLUETOOTH_NOT_ENABLE, "BlueTooth Not Enable Exception Occurred!");
    }
}
