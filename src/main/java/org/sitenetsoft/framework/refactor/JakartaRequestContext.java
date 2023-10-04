package org.sitenetsoft.framework.refactor;

import org.apache.commons.fileupload.servlet.ServletRequestContext;

public class JakartaRequestContext extends ServletRequestContext {

    private final jakarta.servlet.http.HttpServletRequest jakartaRequest;

    public JakartaRequestContext(jakarta.servlet.http.HttpServletRequest request) {
        super(null);  // Pass null since we're overriding the necessary methods
        this.jakartaRequest = request;
    }

    @Override
    public String getCharacterEncoding() {
        return jakartaRequest.getCharacterEncoding();
    }

}
