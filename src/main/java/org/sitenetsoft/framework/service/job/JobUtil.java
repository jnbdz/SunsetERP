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
package org.sitenetsoft.framework.service.job;

import org.sitenetsoft.framework.base.util.Debug;
import org.sitenetsoft.framework.entity.GenericEntityException;
import org.sitenetsoft.framework.entity.GenericValue;
import org.sitenetsoft.framework.entity.transaction.GenericTransactionException;
import org.sitenetsoft.framework.entity.transaction.TransactionUtil;

import javax.transaction.Transaction;
import java.util.List;

public final class JobUtil {

    private static final String MODULE = JobUtil.class.getName();
    protected JobUtil() { }

    public static void removeJob(GenericValue jobValue) {
        // always suspend the current transaction; use the one internally
        boolean beganTransaction = false;
        Transaction parent = null;
        try {
            if (TransactionUtil.getStatus() != TransactionUtil.STATUS_NO_TRANSACTION) {
                parent = TransactionUtil.suspend();
            }
            beganTransaction = TransactionUtil.begin(60);
            jobValue.remove();
            GenericValue relatedValue = jobValue.getRelatedOne("RecurrenceInfo", false);
            if (relatedValue != null) {
                List<GenericValue> valueList = relatedValue.getRelated("JobSandbox", null, null, false);
                if (valueList.isEmpty()) {
                    relatedValue.remove();
                    relatedValue.removeRelated("RecurrenceRule");
                }
            }
            relatedValue = jobValue.getRelatedOne("RuntimeData", false);
            if (relatedValue != null) {
                List<GenericValue> valueList = relatedValue.getRelated("JobSandbox", null, null, false);
                if (valueList.isEmpty()) {
                    relatedValue.remove();
                }
            }
            TransactionUtil.commit(beganTransaction);
            if (Debug.infoOn()) {
                Debug.logInfo("Purged job " + jobValue.get("jobId"), MODULE);
            }
        } catch (Throwable t) {
            String errMsg = "Exception thrown while purging job: ";
            try {
                TransactionUtil.rollback(beganTransaction, errMsg, t);
            } catch (GenericEntityException e) {
                Debug.logWarning(e, "Exception thrown while rolling back transaction: ", MODULE);
            }
            Debug.logWarning(errMsg, MODULE);
        } finally {
            if (parent != null) {
                try {
                    TransactionUtil.resume(parent);
                } catch (GenericTransactionException e) {
                    Debug.logWarning(e, "Exception thrown while resume transaction: ", MODULE);
                }
            }
        }
    }
}
