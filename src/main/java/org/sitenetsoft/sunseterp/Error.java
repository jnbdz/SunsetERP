package org.sitenetsoft.sunseterp;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "An error response")
public class Error {

    @Schema(description = "The error message")
    private String error;

    public Error(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
