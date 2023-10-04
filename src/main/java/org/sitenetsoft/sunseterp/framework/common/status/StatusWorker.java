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
package org.sitenetsoft.sunseterp.framework.common.status;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityQuery;

//import jakarta.servlet.jsp.PageContext;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * StatusWorker
 */
@ApplicationScoped
public final class StatusWorker {

    private static final String MODULE = StatusWorker.class.getName();

    @Inject
    Delegator delegator;

    @Context
    UriInfo uriInfo;

    private StatusWorker() { }

    public List<GenericValue> getStatusItems(String statusTypeId) {
        try {
            return EntityQuery.use(delegator)
                    .from("StatusItem")
                    .where("statusTypeId", statusTypeId)
                    .orderBy("sequenceId")
                    .cache(true)
                    .queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return Collections.emptyList(); // or handle this exception as suitable for your app
        }
    }

    /*public static void getStatusItems(String attributeName, String statusTypeId) {
        //Delegator delegator = (Delegator) pageContext.getRequest().getAttribute("delegator");

        try {
            List<GenericValue> statusItems = EntityQuery.use(this.delegator)
                                                        .from("StatusItem")
                                                        .where("statusTypeId", statusTypeId)
                                                        .orderBy("sequenceId")
                                                        .cache(true)
                                                        .queryList();
            //if (statusItems != null) {
            //    pageContext.setAttribute(attributeName, statusItems);
            //}
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
    }*/

    /*public static void getStatusItems(PageContext pageContext, String attributeName, String statusTypeIdOne, String statusTypeIdTwo) {
        Delegator delegator = (Delegator) pageContext.getRequest().getAttribute("delegator");
        List<GenericValue> statusItems = new LinkedList<>();

        try {
            List<GenericValue> calItems = EntityQuery.use(delegator)
                                                      .from("StatusItem")
                                                      .where("statusTypeId", statusTypeIdOne)
                                                      .orderBy("sequenceId")
                                                      .cache(true)
                                                      .queryList();
            if (calItems != null) {
                statusItems.addAll(calItems);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        try {
            List<GenericValue> taskItems = EntityQuery.use(delegator)
                                                      .from("StatusItem")
                                                      .where("statusTypeId", statusTypeIdTwo)
                                                      .orderBy("sequenceId")
                                                      .cache(true)
                                                      .queryList();
            if (taskItems != null) {
                statusItems.addAll(taskItems);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }

        if (!statusItems.isEmpty()) {
            pageContext.setAttribute(attributeName, statusItems);
        }
    }*/

    public List<GenericValue> getStatusItems(String statusTypeIdOne, String statusTypeIdTwo) {
        List<GenericValue> statusItems = new LinkedList<>();

        try {
            List<GenericValue> calItems = EntityQuery.use(delegator)
                    .from("StatusItem")
                    .where("statusTypeId", statusTypeIdOne)
                    .orderBy("sequenceId")
                    .cache(true)
                    .queryList();
            if (calItems != null) {
                statusItems.addAll(calItems);
            }

            List<GenericValue> taskItems = EntityQuery.use(delegator)
                    .from("StatusItem")
                    .where("statusTypeId", statusTypeIdTwo)
                    .orderBy("sequenceId")
                    .cache(true)
                    .queryList();
            if (taskItems != null) {
                statusItems.addAll(taskItems);
            }

        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return Collections.emptyList();  // or handle the exception as per your application needs
        }

        return statusItems;
    }

    /*public static void getStatusValidChangeToDetails(PageContext pageContext, String attributeName, String statusId) {
        Delegator delegator = (Delegator) pageContext.getRequest().getAttribute("delegator");
        List<GenericValue> statusValidChangeToDetails = null;

        try {
            statusValidChangeToDetails = EntityQuery.use(delegator)
                                                    .from("StatusValidChangeToDetail")
                                                    .where("statusId", statusId)
                                                    .orderBy("sequenceId")
                                                    .cache(true)
                                                    .queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }

        if (statusValidChangeToDetails != null) {
            pageContext.setAttribute(attributeName, statusValidChangeToDetails);
        }
    }*/

    public List<GenericValue> getStatusValidChangeToDetails(String statusId) {
        List<GenericValue> statusValidChangeToDetails;

        try {
            statusValidChangeToDetails = EntityQuery.use(delegator)
                    .from("StatusValidChangeToDetail")
                    .where("statusId", statusId)
                    .orderBy("sequenceId")
                    .cache(true)
                    .queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return Collections.emptyList();  // or handle the exception as per your application needs
        }

        return statusValidChangeToDetails;
    }
}
