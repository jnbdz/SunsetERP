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
package org.sitenetsoft.sunseterp.framework.service.job;

import org.sitenetsoft.sunseterp.framework.base.util.Assert;
import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.service.DispatchContext;
import org.sitenetsoft.sunseterp.framework.service.GenericRequester;
import org.sitenetsoft.sunseterp.framework.service.LocalDispatcher;
import org.sitenetsoft.sunseterp.framework.service.ServiceUtil;
import org.sitenetsoft.sunseterp.framework.service.semaphore.SemaphoreFailException;
import org.sitenetsoft.sunseterp.framework.service.semaphore.SemaphoreWaitException;

import java.io.Serializable;
import java.util.Map;
/**
 * A generic async-service job.
 */
@SuppressWarnings("serial")
public class GenericServiceJob extends AbstractJob implements Serializable {

    private static final String MODULE = GenericServiceJob.class.getName();

    private final transient GenericRequester requester;
    private final transient DispatchContext dctx;
    private final String service;
    private final Map<String, Object> context;
    /**
     * Gets dctx.
     * @return the dctx
     */
    public DispatchContext getDctx() {
        return dctx;
    }

    public GenericServiceJob(DispatchContext dctx, String jobId, String jobName, String service, Map<String, Object> context, GenericRequester req) {
        super(jobId, jobName);
        Assert.notNull("dctx", dctx);
        this.dctx = dctx;
        this.service = service;
        this.context = context;
        this.requester = req;
    }

    /**
     * Invokes the service.
     */
    @Override
    public void exec() throws InvalidJobException {
        if (getCurrentState() != State.QUEUED) {
            throw new InvalidJobException("Illegal state change");
        }
        setCurrentState(State.RUNNING);
        init();
        Throwable thrown = null;
        Map<String, Object> result = null;
        // no transaction is necessary since runSync handles this
        try {
            // get the dispatcher and invoke the service via runSync -- will run all ECAs
            LocalDispatcher dispatcher = dctx.getDispatcher();
            result = dispatcher.runSync(getServiceName(), getContext());
            // check for a failure
            if (ServiceUtil.isError(result)) {
                thrown = new Exception(ServiceUtil.getErrorMessage(result));
            }
            if (requester != null) {
                requester.receiveResult(result);
            }
        } catch (Throwable t) {
            if (requester != null) {
                // pass the exception back to the requester.
                requester.receiveThrowable(t);
            }
            thrown = t;
        }
        if (thrown == null) {
            finish(result);
        } else {
            failed(thrown);
        }
    }

    /**
     * Method is called prior to running the service.
     */
    protected void init() throws InvalidJobException {
        if (Debug.verboseOn()) {
            Debug.logVerbose("Async-Service initializing.", MODULE);
        }
    }

    /**
     * Method is called after the service has finished successfully.
     */
    protected void finish(Map<String, Object> result) throws InvalidJobException {
        if (getCurrentState() != State.RUNNING) {
            throw new InvalidJobException("Illegal state change");
        }
        setCurrentState(State.FINISHED);
        if (Debug.verboseOn()) {
            Debug.logVerbose("Async-Service finished.", MODULE);
        }
    }

    /**
     * Method is called when the service fails.
     * @param t Throwable
     */
    protected void failed(Throwable t) throws InvalidJobException {
        if (t instanceof SemaphoreWaitException || t instanceof SemaphoreFailException) {
            Debug.logError("Async-Service failed due to " + t, MODULE);
        } else {
            Debug.logError(t, "Async-Service failed.", MODULE);
        }
        setCurrentState(State.FAILED);
    }

    /**
     * Gets the context for the service invocation.
     * @return Map of name value pairs making up the service context.
     */
    protected Map<String, Object> getContext() throws InvalidJobException {
        return context;
    }

    /**
     * Gets the name of the service as defined in the definition file.
     * @return The name of the service to be invoked.
     */
    protected String getServiceName() {
        return service;
    }

    @Override
    public boolean isValid() {
        return getCurrentState() == State.CREATED;
    }

    @Override
    public void deQueue() throws InvalidJobException {
        super.deQueue();
        throw new InvalidJobException("Unable to queue job [" + getJobId() + "]");
    }
}
