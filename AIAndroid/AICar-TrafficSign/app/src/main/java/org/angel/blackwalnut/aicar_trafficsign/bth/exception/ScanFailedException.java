package org.angel.blackwalnut.aicar_trafficsign.bth.exception;


public class ScanFailedException extends BleException {
    public ScanFailedException() {
        super(ERROR_CODE_SCAN_FAILED, "Scan Failed Exception Occurred!");
    }
}
