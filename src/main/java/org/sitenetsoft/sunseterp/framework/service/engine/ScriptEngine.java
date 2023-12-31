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

import org.sitenetsoft.sunseterp.framework.base.util.Assert;
import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.base.util.ScriptUtil;
import org.sitenetsoft.sunseterp.framework.service.*;

import javax.script.ScriptContext;
import javax.script.ScriptException;
import java.util.*;

import static org.sitenetsoft.sunseterp.framework.base.util.UtilGenerics.cast;

/**
 * Generic Script Service Engine. This service engine uses the javax.script package (JSR-223) to invoke scripts or script functions.
 * <p>The script service engine will put the following artifacts in the script engine's bindings:</p>
 * <ul>
 *   <li><code>parameters</code> - the service attributes <code>Map</code></li>
 *   <li><code>dctx</code> - a <code>DispatchContext</code> instance</li>
 *   <li><code>dispatcher</code> - a <code>LocalDispatcher</code> instance</li>
 *   <li><code>delegator</code> - a <code>Delegator</code> instance</li>
 * </ul>
 * <p>If the service definition includes an invoke attribute, then the matching script function/method will be called
 * with a single argument - the bindings <code>Map</code>.</p>
 */
public final class ScriptEngine extends GenericAsyncEngine {

    private static final String MODULE = ScriptEngine.class.getName();
    private static final Set<String> PROTECTED_KEYS = createProtectedKeys();

    private static Set<String> createProtectedKeys() {
        Set<String> newSet = new HashSet<>();
        /* Commenting out for now because some scripts write to the parameters Map - which should not be allowed.
        newSet.add(ScriptUtil.PARAMETERS_KEY);
        */
        newSet.add("dctx");
        newSet.add("dispatcher");
        newSet.add("delegator");
        return Collections.unmodifiableSet(newSet);
    }

    public ScriptEngine(ServiceDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public Map<String, Object> runSync(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        Assert.notNull("localName", localName, "modelService.location", modelService.getLocation(), "context", context);
        Map<String, Object> params = new HashMap<>();
        params.putAll(context);
        context.put(ScriptUtil.PARAMETERS_KEY, params);
        DispatchContext dctx = getDispatcher().getLocalContext(localName);
        context.put("dctx", dctx);
        context.put("dispatcher", dctx.getDispatcher());
        context.put("delegator", getDispatcher().getDelegator());
        try {
            ScriptContext scriptContext = ScriptUtil.createScriptContext(context, PROTECTED_KEYS);
            Object resultObj = ScriptUtil.executeScript(getLocation(modelService), modelService.getInvoke(), scriptContext, null);
            if (resultObj == null) {
                resultObj = scriptContext.getAttribute(ScriptUtil.RESULT_KEY);
            }
            if (resultObj != null && resultObj instanceof Map<?, ?>) {
                return cast(resultObj);
            }
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.putAll(modelService.makeValid(scriptContext.getBindings(ScriptContext.ENGINE_SCOPE), ModelService.OUT_PARAM));
            return result;
        } catch (ScriptException se) {
            return ServiceUtil.returnError(se.getMessage());
        } catch (Exception e) {
            Debug.logWarning(e, "Error invoking service " + modelService.getName() + ": ", MODULE);
            throw new GenericServiceException(e);
        }
    }

    @Override
    public void runSyncIgnore(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        runSync(localName, modelService, context);
    }
}
