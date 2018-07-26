package com.zucc.g3.hzy.myapplication.bth.exception;


public class TimeoutException extends BleException {
    public TimeoutException() {
        super(ERROR_CODE_TIMEOUT, "Timeout Exception Occurred!");
    }
}
