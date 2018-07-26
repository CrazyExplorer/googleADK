package com.zucc.g3.hzy.myapplication.bth.exception;


public class OtherException extends BleException {
    public OtherException(String description) {
        super(ERROR_CODE_OTHER, description);
    }
}
