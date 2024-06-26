/*
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
*/
package org.sitenetsoft.sunseterp.applications.accounting.invoice

import org.sitenetsoft.sunseterp.framework.base.util.Debug
import org.sitenetsoft.sunseterp.framework.base.util.UtilDateTime
import org.sitenetsoft.sunseterp.framework.base.util.UtilValidate
import org.sitenetsoft.sunseterp.framework.entity.GenericValue
import org.sitenetsoft.sunseterp.framework.service.ServiceUtil

import java.sql.Timestamp

Map getNextInvoiceId() {
    result = success()

    // try to find PartyAcctgPreference for parameters.partyId, see if we need any special invoice number sequencing
    GenericValue partyAcctgPreference = from('PartyAcctgPreference').where(parameters).queryOne()
    if (Debug.infoOn()) {
        logInfo("In getNextInvoiceId partyId is [${parameters.partyId}], partyAcctgPreference: ${partyAcctgPreference}")
    }

    String customMethodName = null
    String invoiceIdPrefix = ''
    if (partyAcctgPreference) {
        invoiceIdPrefix = partyAcctgPreference.invoiceIdPrefix ?: ''
        //see OFBIZ-3765 beware of OFBIZ-3557
        GenericValue customMethod = partyAcctgPreference.getRelatedOne('InvoiceCustomMethod', true)
        if (customMethod) {
            customMethodName = customMethod.customMethodName
        } else {
            //retrieve service from deprecated enumeration see OFBIZ-3765 beware of OFBIZ-3557
            if (partyAcctgPreference.oldInvoiceSequenceEnumId == 'INVSQ_ENF_SEQ') {
                customMethodName = 'invoiceSequenceEnforced'
            }
            if (partyAcctgPreference.oldInvoiceSequenceEnumId == 'INVSQ_RESTARTYR') {
                customMethodName = 'invoiceSequenceRestart'
            }
        }
    } else {
        logWarning("Acctg preference not defined for partyId [${parameters.partyId}]")
    }

    String invoiceIdTemp = ''
    if (customMethodName) {
        parameters.partyAcctgPreference = partyAcctgPreference
        Map serviceResult = run service: customMethodName, with: parameters
        if (ServiceUtil.isError(serviceResult)) {
            return serviceResult
        }
        invoiceIdTemp = serviceResult.invoiceId
    } else {
        logInfo('In createInvoice sequence enum Standard')
        //default to the default sequencing: INVSQ_STANDARD
        invoiceIdTemp = parameters.invoiceId
        if (invoiceIdTemp) {
            //check the provided ID
            errorMsg = UtilValidate.checkValidDatabaseId(invoiceIdTemp)
            if (errorMsg != null) {
                return error("In getNextInvoiceId ${errorMsg}")
            }
        } else {
            invoiceIdTemp = delegator.getNextSeqId('Invoice', 1)
        }
    }

    // use invoiceIdTemp along with the invoiceIdPrefix to create the real ID
    result.invoiceId = invoiceIdPrefix + invoiceIdTemp
    return result
}

Map invoiceSequenceEnforced() {
    result = success()

    logInfo('In createInvoice sequence enum Enforced')
    GenericValue partyAcctgPreference = parameters.partyAcctgPreference
    //this is sequential sequencing, we can't skip a number, also it must be a unique sequence per partyIdFrom

    Long lastInvoiceNumber = 1
    if (partyAcctgPreference.lastInvoiceNumber) {
        lastInvoiceNumber = partyAcctgPreference.lastInvoiceNumber + 1
    }

    partyAcctgPreference.lastInvoiceNumber = lastInvoiceNumber
    delegator.store(partyAcctgPreference)
    result.invoiceId = lastInvoiceNumber
    return result
}

Map invoiceSequenceRestart() {
    result = success()

    logInfo('In createInvoice sequence enum Enforced')
    GenericValue partyAcctgPreference = parameters.partyAcctgPreference
    //this is sequential sequencing, we can't skip a number, also it must be a unique sequence per partyIdFrom

    Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
    if (partyAcctgPreference.lastInvoiceRestartDate) {
        //first figure out if we need to reset the lastInvoiceNumber; is the lastInvoiceRestartDate after the fiscalYearStartMonth/Day for this year?
        curYearFiscalStartDate = UtilDateTime.getYearStart(nowTimestamp,
                partyAcctgPreference.fiscalYearStartDay, partyAcctgPreference.fiscalYearStartMonth, 0L)
        if (partyAcctgPreference.lastInvoiceRestartDate < curYearFiscalStartDate && nowTimestamp >= curYearFiscalStartDate) {
            //less than fiscal year start, we need to reset it
            partyAcctgPreference.lastInvoiceNumber = 1L
            partyAcctgPreference.lastInvoiceRestartDate = nowTimestamp
        } else {
            //greater than or equal to fiscal year start or nowTimestamp hasn't yet hit the current year fiscal start date, we're okay, just increment
            partyAcctgPreference.lastInvoiceNumber += 1L
        }
    } else {
        //if no lastInvoiceRestartDate then it's easy, just start now with 1
        partyAcctgPreference.lastInvoiceNumber = 1L
        partyAcctgPreference.lastInvoiceRestartDate = nowTimestamp
    }
    delegator.store(partyAcctgPreference)

    //get the current year string for prefix, etc; simple 4 digit year date string (using system defaults)
    Integer curYearString = UtilDateTime.getYear(partyAcctgPreference.lastInvoiceRestartDate, timeZone, locale)
    return success(invoiceId: "${curYearString}-${partyAcctgPreference.lastInvoiceNumber}")
}

