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
package org.sitenetsoft.sunseterp.framework.service.engine;

import groovy.lang.Script;
import org.sitenetsoft.sunseterp.framework.base.util.GroovyUtil;
import org.sitenetsoft.sunseterp.framework.base.util.ScriptHelper;
import org.sitenetsoft.sunseterp.framework.base.util.ScriptUtil;
import org.sitenetsoft.sunseterp.framework.base.util.UtilValidate;
import org.sitenetsoft.sunseterp.framework.service.*;
import org.codehaus.groovy.runtime.InvokerHelper;

import javax.script.ScriptContext;
import java.util.*;

import static org.sitenetsoft.sunseterp.framework.base.util.UtilGenerics.cast;

/**
 * Groovy Script Service Engine
 */
public final class GroovyEngine extends GenericAsyncEngine {

    private static final String MODULE = GroovyEngine.class.getName();
    private static final Object[] EMPTY_ARGS = {};
    private static final Set<String> PROTECTED_KEYS = createProtectedKeys();

    private static Set<String> createProtectedKeys() {
        Set<String> newSet = new HashSet<>();
        /* Commenting out for now because some scripts write to the parameters Map - which should not be allowed.
        newSet.add(ScriptUtil.PARAMETERS_KEY);
        */
        newSet.add("dctx");
        newSet.add("dispatcher");
        newSet.add("delegator");
        newSet.add("visualTheme");
        return Collections.unmodifiableSet(newSet);
    }

    public GroovyEngine(ServiceDispatcher dispatcher) {
        super(dispatcher);
    }

    /**
     * @see org.sitenetsoft.sunseterp.framework.service.engine.GenericEngine#runSyncIgnore(String, org.sitenetsoft.sunseterp.framework.service.ModelService, Map)
     */
    @Override
    public void runSyncIgnore(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        runSync(localName, modelService, context);
    }

    @Override
    public Map<String, Object> runSync(String localName, ModelService modelService, Map<String, Object> context)
            throws GenericServiceException {
        if (UtilValidate.isEmpty(modelService.getLocation())) {
            throw new GenericServiceException("Cannot run Groovy service with empty location");
        }
        Map<String, Object> params = new HashMap<>();
        params.putAll(context);

        Map<String, Object> gContext = new HashMap<>();
        gContext.putAll(context);
        gContext.put(ScriptUtil.PARAMETERS_KEY, params);

        DispatchContext dctx = getDispatcher().getLocalContext(localName);
        gContext.put("dctx", dctx);
        gContext.put("security", dctx.getSecurity());
        gContext.put("dispatcher", dctx.getDispatcher());
        gContext.put("delegator", getDispatcher().getDelegator());
        try {
            ScriptContext scriptContext = ScriptUtil.createScriptContext(gContext, PROTECTED_KEYS);
            ScriptHelper scriptHelper = (ScriptHelper) scriptContext.getAttribute(ScriptUtil.SCRIPT_HELPER_KEY);
            if (scriptHelper != null) {
                gContext.put(ScriptUtil.SCRIPT_HELPER_KEY, scriptHelper);
            }

            Script script = InvokerHelper.createScript(
                    GroovyUtil.getScriptClassFromLocation(getLocation(modelService)),
                    GroovyUtil.getBinding(gContext));

            // Groovy services can either be implemented as a stand-alone script or with a method inside a script.
            Object resultObj = UtilValidate.isEmpty(modelService.getInvoke())
                    ? script.run()
                    : script.invokeMethod(modelService.getInvoke(), EMPTY_ARGS);

            if (resultObj == null) {
                resultObj = scriptContext.getAttribute(ScriptUtil.RESULT_KEY);
            }
            if (resultObj != null && resultObj instanceof Map<?, ?>) {
                return cast(resultObj);
            }
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.putAll(modelService.makeValid(
                    scriptContext.getBindings(ScriptContext.ENGINE_SCOPE),
                    ModelService.OUT_PARAM));
            return result;
        } catch (Exception e) {
            // When throwing ExecutionServiceException in Groovy DSL run Service method
            // since we are dependent on Groovy MetaClassImpl that throws InvokerInvocationException
            // we need to check nested exception to return the embedded service error message.
            Throwable nested = e.getCause();
            if (nested instanceof ExecutionServiceException) {
                return ServiceUtil.returnError(nested.getMessage());
            }
            if (UtilValidate.isEmpty(modelService.getInvoke())) {
                throw new GenericServiceException("Error running Groovy script file ["
                        + modelService.getLocation() + "]: ", e);
            }
            // detailMessage can be null.  If it is null, the exception won't be properly returned and logged, and that will
            // make spotting problems very difficult.  Disabling this for now in favor of returning a proper exception.
            throw new GenericServiceException("Error running Groovy method [" + modelService.getInvoke() + "]"
                    + " in Groovy file [" + modelService.getLocation() + "]: ", e);
        }
    }
}
