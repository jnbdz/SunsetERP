/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.sitenetsoft.sunseterp.framework.minilang.method.envops;

import org.sitenetsoft.sunseterp.framework.base.util.string.FlexibleStringExpander;
import org.sitenetsoft.sunseterp.framework.minilang.MiniLangException;
import org.sitenetsoft.sunseterp.framework.minilang.MiniLangValidate;
import org.sitenetsoft.sunseterp.framework.minilang.SimpleMethod;
import org.sitenetsoft.sunseterp.framework.minilang.method.MethodContext;
import org.sitenetsoft.sunseterp.framework.minilang.method.MethodOperation;
import org.w3c.dom.Element;

import java.util.List;

/**
 * Implements the &lt;check-errors&gt; element.
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Referenc</a>
 */
public final class CheckErrors extends MethodOperation {

    private final FlexibleStringExpander errorCodeFse;
    private final FlexibleStringExpander errorListNameFse;

    public CheckErrors(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "error-code", "error-list-name");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        this.errorCodeFse = FlexibleStringExpander.getInstance(element.getAttribute("error-code"));
        this.errorListNameFse = FlexibleStringExpander.getInstance(MiniLangValidate.checkAttribute(element.getAttribute("error-list-name"),
                "error_list"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        SimpleMethod simpleMethod = getSimpleMethod();
        if (methodContext.isTraceOn()) {
            outputTraceMessage(methodContext, "Begin check-errors.");
        }
        List<Object> messages = methodContext.getEnv(this.errorListNameFse.expandString(methodContext.getEnvMap()));
        if (messages != null && !messages.isEmpty()) {
            if (methodContext.getMethodType() == MethodContext.EVENT) {
                methodContext.putEnv(simpleMethod.getEventErrorMessageListName(), messages);
                methodContext.putEnv(this.getSimpleMethod().getEventResponseCodeName(), getErrorCode(methodContext));
            } else {
                methodContext.putEnv(simpleMethod.getServiceErrorMessageListName(), messages);
                methodContext.putEnv(this.getSimpleMethod().getServiceResponseMessageName(), getErrorCode(methodContext));
            }
            if (methodContext.isTraceOn()) {
                outputTraceMessage(methodContext, "Found error messages. Setting error status and halting script execution.");
                outputTraceMessage(methodContext, "End check-errors.");
            }
            return false;
        }
        if (methodContext.isTraceOn()) {
            outputTraceMessage(methodContext, "No error messages found. Continuing script execution.");
            outputTraceMessage(methodContext, "End check-errors.");
        }
        return true;
    }

    private String getErrorCode(MethodContext methodContext) {
        String errorCode = this.errorCodeFse.expandString(methodContext.getEnvMap());
        if (errorCode.isEmpty()) {
            errorCode = this.getSimpleMethod().getDefaultErrorCode();
        }
        return errorCode;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<check-errors ");
        if (!this.errorCodeFse.isEmpty()) {
            sb.append("error-code=\"").append(this.errorCodeFse).append("\" ");
        }
        if (!"error_list".equals(this.errorListNameFse.getOriginal())) {
            sb.append("error-list-name=\"").append(this.errorListNameFse).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;check-errors&gt; element.
     */
    public static final class CheckErrorsFactory implements Factory<CheckErrors> {
        @Override
        public CheckErrors createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new CheckErrors(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "check-errors";
        }
    }
}
