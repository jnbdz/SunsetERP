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
package org.sitenetsoft.framework.service.engine;

import org.sitenetsoft.framework.base.config.GenericConfigException;
import org.sitenetsoft.framework.base.util.Debug;
import org.sitenetsoft.framework.base.util.UtilDateTime;
import org.sitenetsoft.framework.base.util.UtilMisc;
import org.sitenetsoft.framework.base.util.UtilValidate;
import org.sitenetsoft.framework.entity.GenericEntityException;
import org.sitenetsoft.framework.entity.GenericValue;
import org.sitenetsoft.framework.entity.serialize.SerializeException;
import org.sitenetsoft.framework.entity.serialize.XmlSerializer;
import org.sitenetsoft.framework.service.*;
import org.sitenetsoft.framework.service.config.ServiceConfigUtil;
import org.sitenetsoft.framework.service.job.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

/**
 * Generic Asynchronous Engine
 */
public abstract class GenericAsyncEngine extends AbstractEngine {

    private static final String MODULE = GenericAsyncEngine.class.getName();

    protected GenericAsyncEngine(ServiceDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public abstract Map<String, Object> runSync(String localName, ModelService modelService, Map<String, Object> context)
            throws GenericServiceException;

    @Override
    public abstract void runSyncIgnore(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException;

    @Override
    public void runAsync(String localName, ModelService modelService, Map<String, Object> context, boolean persist) throws GenericServiceException {
        runAsync(localName, modelService, context, null, persist);
    }

    @Override
    public void runAsync(String localName, ModelService modelService, Map<String, Object> context, GenericRequester requester, boolean persist)
            throws GenericServiceException {
        DispatchContext dctx = getDispatcher().getLocalContext(localName);
        Job job = null;

        if (persist) {
            // check for a delegator
            if (getDispatcher().getDelegator() == null) {
                throw new GenericServiceException("No reference to delegator; cannot run persisted services.");
            }

            GenericValue jobV = null;
            // Build the value object(s).
            try {
                // Create the runtime data
                String dataId = getDispatcher().getDelegator().getNextSeqId("RuntimeData");

                GenericValue runtimeData = getDispatcher().getDelegator().makeValue("RuntimeData", "runtimeDataId", dataId);

                runtimeData.set("runtimeInfo", XmlSerializer.serialize(context));
                runtimeData.create();

                // Get the userLoginId out of the context
                String authUserLoginId = null;
                if (context.get("userLogin") != null) {
                    GenericValue userLogin = (GenericValue) context.get("userLogin");
                    authUserLoginId = userLogin.getString("userLoginId");
                }

                // Create the job info
                String jobId = getDispatcher().getDelegator().getNextSeqId("JobSandbox");
                String jobName = Long.toString(System.currentTimeMillis());

                Map<String, Object> jFields = UtilMisc.toMap("jobId", jobId, "jobName", jobName, "runTime", UtilDateTime.nowTimestamp());
                jFields.put("poolId", ServiceConfigUtil.getServiceEngine().getThreadPool().getSendToPool());
                jFields.put("statusId", "SERVICE_PENDING");
                jFields.put("serviceName", modelService.getName());
                jFields.put("loaderName", localName);
                jFields.put("maxRetry", (long) modelService.getMaxRetry());
                jFields.put("runtimeDataId", dataId);
                jFields.put("priority", JobPriority.NORMAL);
                if (UtilValidate.isNotEmpty(authUserLoginId)) {
                    jFields.put("authUserLoginId", authUserLoginId);
                }

                jobV = getDispatcher().getDelegator().makeValue("JobSandbox", jFields);
                jobV.create();
            } catch (GenericEntityException e) {
                throw new GenericServiceException("Unable to create persisted job", e);
            } catch (FileNotFoundException e) {
                throw new GenericServiceException("Problem serializing service attributes", e);
            } catch (SerializeException | GenericConfigException | IOException e) {
                throw new GenericServiceException("Problem serializing service attributes", e);
            }

            Debug.logInfo("Persisted job queued : " + jobV.getString("jobName"), MODULE);
        } else {
            JobManager jMgr = getDispatcher().getJobManager();
            if (jMgr != null) {
                String name = Long.toString(System.currentTimeMillis());
                String jobId = modelService.getName() + "." + name;
                job = new GenericServiceJob(dctx, jobId, name, modelService.getName(), context, requester);
                try {
                    getDispatcher().getJobManager().runJob(job);
                } catch (JobManagerException jse) {
                    throw new GenericServiceException("Cannot run job.", jse);
                }
            } else {
                throw new GenericServiceException("Cannot get JobManager instance to invoke the job");
            }
        }
    }

    @Override
    protected boolean allowCallbacks(ModelService model, Map<String, Object> context, int mode) throws GenericServiceException {
        return mode == GenericEngine.SYNC_MODE;
    }
}
