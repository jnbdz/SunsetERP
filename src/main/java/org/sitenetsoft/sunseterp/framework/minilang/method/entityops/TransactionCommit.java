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
package org.sitenetsoft.sunseterp.framework.minilang.method.entityops;

import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.base.util.collections.FlexibleMapAccessor;
import org.sitenetsoft.sunseterp.framework.entity.transaction.GenericTransactionException;
import org.sitenetsoft.sunseterp.framework.entity.transaction.TransactionUtil;
import org.sitenetsoft.sunseterp.framework.minilang.MiniLangException;
import org.sitenetsoft.sunseterp.framework.minilang.MiniLangValidate;
import org.sitenetsoft.sunseterp.framework.minilang.SimpleMethod;
import org.sitenetsoft.sunseterp.framework.minilang.method.MethodContext;
import org.sitenetsoft.sunseterp.framework.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;transaction-commit&gt; element.
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
 */
public final class TransactionCommit extends MethodOperation {

    private static final String MODULE = TransactionCommit.class.getName();

    private final FlexibleMapAccessor<Boolean> beganTransactionFma;

    public TransactionCommit(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "began-transaction-name");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "began-transaction-name");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        beganTransactionFma = FlexibleMapAccessor.getInstance(MiniLangValidate.checkAttribute(element.getAttribute("began-transaction-name"),
                "beganTransaction"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        boolean beganTransaction = false;
        Boolean beganTransactionBoolean = beganTransactionFma.get(methodContext.getEnvMap());
        if (beganTransactionBoolean != null) {
            beganTransaction = beganTransactionBoolean;
        }
        try {
            TransactionUtil.commit(beganTransaction);
        } catch (GenericTransactionException e) {
            String errMsg = "Exception thrown while committing transaction: " + e.getMessage();
            Debug.logWarning(e, errMsg, MODULE);
            getSimpleMethod().addErrorMessage(methodContext, errMsg);
            return false;
        }
        beganTransactionFma.remove(methodContext.getEnvMap());
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<transaction-commit ");
        sb.append("began-transaction-name=\"").append(this.beganTransactionFma).append("\" />");
        return sb.toString();
    }

    /**
     * A factory for the &lt;transaction-commit&gt; element.
     */
    public static final class TransactionCommitFactory implements Factory<TransactionCommit> {
        @Override
        public TransactionCommit createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new TransactionCommit(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "transaction-commit";
        }
    }
}
