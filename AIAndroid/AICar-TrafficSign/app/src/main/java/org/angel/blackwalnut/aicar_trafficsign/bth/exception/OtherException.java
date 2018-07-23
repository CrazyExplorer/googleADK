package org.angel.blackwalnut.aicar_trafficsign.bth.exception;


public class OtherException extends BleException {
    public OtherException(String description) {
        super(ERROR_CODE_OTHER, description);
    }
}
