package org.sitenetsoft.sunseterp.exception;

public class ApiError {
    public int status;
    public String message;

    public ApiError(int status, String message) {
        this.status = status;
        this.message = message;
    }
}