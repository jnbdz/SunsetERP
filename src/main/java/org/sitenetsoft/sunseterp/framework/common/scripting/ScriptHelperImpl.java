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
package org.sitenetsoft.sunseterp.framework.common.scripting;

import org.sitenetsoft.sunseterp.framework.base.util.*;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;
import org.sitenetsoft.sunseterp.framework.entity.GenericPK;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.entity.model.ModelEntity;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityQuery;
import org.sitenetsoft.sunseterp.framework.service.GenericServiceException;
import org.sitenetsoft.sunseterp.framework.service.ModelService;
import org.sitenetsoft.sunseterp.framework.service.ServiceUtil;

import javax.script.ScriptContext;
import javax.script.ScriptException;
import java.util.*;

/**
 * An implementation of the <code>ScriptHelper</code> interface.
 */
public final class ScriptHelperImpl implements ScriptHelper {

    private static final String MODULE = ScriptHelperImpl.class.getName();
    private static final Map<String, ? extends Object> EMPTY_ARGS = Collections.unmodifiableMap(new HashMap<>());

    private static GenericValue runFindByPrimaryKey(ModelEntity modelEntity, ContextHelper ctxHelper, boolean useCache, boolean autoFieldMap,
            Map<String, ? extends Object> fieldMap, List<String> selectFieldList) throws ScriptException {
        Map<String, Object> entityContext = new HashMap<>();
        Delegator delegator = ctxHelper.getDelegator();
        Map<String, Object> context = ctxHelper.getBindings();
        if (autoFieldMap) {
            GenericValue tempVal = delegator.makeValue(modelEntity.getEntityName());
            Object parametersObj = context.get("parameters");
            if (parametersObj != null && parametersObj instanceof Map<?, ?>) {
                tempVal.setAllFields(UtilGenerics.cast(parametersObj), true, null, Boolean.TRUE);
            }
            tempVal.setAllFields(context, true, null, Boolean.TRUE);
            entityContext.putAll(tempVal);
        }
        if (fieldMap != null) {
            entityContext.putAll(fieldMap);
        }
        entityContext.put("locale", context.get("locale"));
        entityContext.put("timeZone", context.get("timeZone"));
        modelEntity.convertFieldMapInPlace(entityContext, delegator);
        entityContext.remove("locale");
        entityContext.remove("timeZone");
        Set<String> fieldsToSelect = null;
        if (selectFieldList != null) {
            fieldsToSelect = new HashSet<>(selectFieldList);
        }
        if (fieldsToSelect != null && useCache) {
            String errMsg = "Error running script " + ctxHelper.getScriptName() + ": Problem invoking the findOne method:"
                    + "Cannot specify selectFieldList argument when useCache is set to true ";
            Debug.logWarning(errMsg, MODULE);
            throw new ScriptException(errMsg);
        }
        GenericValue valueOut = null;
        GenericPK entityPK = delegator.makePK(modelEntity.getEntityName(), entityContext);
        if (entityPK.containsPrimaryKey(true)) {
            try {
                if (useCache) {
                    valueOut = EntityQuery.use(delegator).from(entityPK.getEntityName()).where(entityPK).cache(true).queryOne();
                } else {
                    if (fieldsToSelect != null) {
                        valueOut = delegator.findByPrimaryKeyPartial(entityPK, fieldsToSelect);
                    } else {
                        valueOut = EntityQuery.use(delegator).from(entityPK.getEntityName()).where(entityPK).queryOne();
                    }
                }
            } catch (GenericEntityException e) {
                String errMsg = "Error running script " + ctxHelper.getScriptName() + ": Problem invoking the findOne method: " + e.getMessage();
                Debug.logWarning(e, errMsg, MODULE);
                throw new ScriptException(errMsg);
            }
        } else {
            if (Debug.warningOn()) {
                Debug.logWarning("Error running script " + ctxHelper.getScriptName() + ": Returning null because found incomplete"
                        + "primary key in find: " + entityPK, MODULE);
            }
        }
        return valueOut;
    }

    private final ContextHelper ctxHelper;

    public ScriptHelperImpl(ScriptContext context) {
        this.ctxHelper = new ContextHelper(context);
    }

    @Override
    public Map<String, ? extends Object> createServiceMap(String serviceName, Map<String, ? extends Object> inputMap) throws ScriptException {
        Assert.notNull("serviceName", serviceName, "inputMap", inputMap);
        Map<String, Object> toMap = new HashMap<>();
        ModelService modelService = null;
        try {
            modelService = ctxHelper.getDispatcher().getDispatchContext().getModelService(serviceName);
        } catch (GenericServiceException e) {
            String errMsg = "Error running script " + ctxHelper.getScriptName() + ": Problem invoking the createServiceMap method: get service"
                    + "definition for service name [" + serviceName + "]: " + e.getMessage();
            Debug.logWarning(e, errMsg, MODULE);
            throw new ScriptException(errMsg);
        }
        toMap.putAll(modelService.makeValid(inputMap, ModelService.IN_PARAM, true, UtilGenerics.cast(ctxHelper.getErrorMessages()),
                ctxHelper.getTimeZone(), ctxHelper.getLocale()));
        return toMap;
    }

    @Override
    public void error(String message) {
        if (ctxHelper.isEvent()) {
            ctxHelper.putResult("_error_message_", ctxHelper.expandString(message));
            ctxHelper.putResult("_response_code_", "error");
        } else if (ctxHelper.isService()) {
            ctxHelper.putResults(ServiceUtil.returnError(ctxHelper.expandString(message)));
        }
    }

    @Override
    public String evalString(String original) {
        return ctxHelper.expandString(original);
    }

    @Override
    public void failure(String message) {
        if (ctxHelper.isEvent()) {
            ctxHelper.putResult("_error_message_", ctxHelper.expandString(message));
            ctxHelper.putResult("_response_code_", "fail");
        } else if (ctxHelper.isService()) {
            ctxHelper.putResults(ServiceUtil.returnFailure(ctxHelper.expandString(message)));
        }
    }

    @Override
    public List<Map<String, Object>> findList(String entityName, Map<String, ? extends Object> fields) throws ScriptException {
        try {
            return UtilGenerics.cast(ctxHelper.getDelegator().findByAnd(entityName, fields, null, false));
        } catch (GenericEntityException e) {
            String errMsg = "Error running script " + ctxHelper.getScriptName() + ": Problem invoking the findList method: " + e.getMessage();
            Debug.logWarning(e, errMsg, MODULE);
            throw new ScriptException(errMsg);
        }
    }

    @Override
    public Map<String, Object> findOne(String entityName) throws ScriptException {
        return findOne(entityName, null, EMPTY_ARGS);
    }

    @Override
    public Map<String, Object> findOne(String entityName, Map<String, ? extends Object> fields, Map<String, ? extends Object> args)
            throws ScriptException {
        Assert.notNull("entityName", entityName);
        if (args == null) {
            args = EMPTY_ARGS;
        }
        boolean useCache = "true".equals(args.get("useCache"));
        boolean autoFieldMap = !"false".equals(args.get("autoFieldMap"));
        List<String> selectFieldList = UtilGenerics.cast(args.get("selectFieldList"));
        ModelEntity modelEntity = ctxHelper.getDelegator().getModelEntity(entityName);
        if (modelEntity == null) {
            throw new ScriptException("Error running script " + ctxHelper.getScriptName() + " - no entity definition found for entity name ["
                    + entityName + "]");
        }
        return runFindByPrimaryKey(modelEntity, ctxHelper, useCache, autoFieldMap, fields, selectFieldList);
    }

    @Override
    public void logError(String message) {
        String expandedMessage = ctxHelper.expandString(message);
        Debug.logError("[".concat(ctxHelper.getScriptName()).concat("] ").concat(expandedMessage), MODULE);
    }

    @Override
    public void logInfo(String message) {
        String expandedMessage = ctxHelper.expandString(message);
        Debug.logInfo("[".concat(ctxHelper.getScriptName()).concat("] ").concat(expandedMessage), MODULE);
    }

    @Override
    public void logWarning(String message) {
        String expandedMessage = ctxHelper.expandString(message);
        Debug.logWarning("[".concat(ctxHelper.getScriptName()).concat("] ").concat(expandedMessage), MODULE);
    }

    @Override
    public Map<String, Object> makeValue(String entityName) throws ScriptException {
        return ctxHelper.getDelegator().makeValidValue(entityName);
    }

    @Override
    public Map<String, Object> makeValue(String entityName, Map<String, Object> fields) throws ScriptException {
        return ctxHelper.getDelegator().makeValidValue(entityName, fields);
    }

    @Override
    public Map<String, ? extends Object> runService(String serviceName, Map<String, ? extends Object> inputMap) throws ScriptException {
        return runService(serviceName, inputMap, EMPTY_ARGS);
    }

    @Override
    public Map<String, ? extends Object> runService(String serviceName, Map<String, ? extends Object> inputMap, Map<String, ? extends Object> args)
            throws ScriptException {
        Assert.notNull("serviceName", serviceName, "args", args);
        boolean includeUserLogin = !"false".equals(args.get("includeUserLoginStr"));
        String requireNewTransactionStr = (String) args.get("requireNewTransaction");
        int transactionTimeout = -1;
        if (UtilValidate.isNotEmpty(requireNewTransactionStr)) {
            String timeoutStr = (String) args.get("transactionTimout");
            if (UtilValidate.isNotEmpty(timeoutStr)) {
                try {
                    transactionTimeout = Integer.parseInt(timeoutStr);
                } catch (NumberFormatException e) {
                    Debug.logWarning(e, "Setting timeout to 0 (default)", MODULE);
                    transactionTimeout = 0;
                }
            }
        }
        Map<String, Object> inMap = new HashMap<>(inputMap);
        if (includeUserLogin && !inMap.containsKey("userLogin")) {
            GenericValue userLogin = ctxHelper.getUserLogin();
            if (userLogin != null) {
                inMap.put("userLogin", userLogin);
            }
        }
        if (!inMap.containsKey("locale") && ctxHelper.getLocale() != null) {
            inMap.put("locale", ctxHelper.getLocale());
        }
        if (!inMap.containsKey("timeZone") && ctxHelper.getTimeZone() != null) {
            inMap.put("timeZone", ctxHelper.getTimeZone());
        }
        Map<String, Object> result = null;
        try {
            if (UtilValidate.isEmpty(requireNewTransactionStr) && transactionTimeout < 0) {
                result = ctxHelper.getDispatcher().runSync(serviceName, inMap);
            } else {
                ModelService modelService = ctxHelper.getDispatcher().getDispatchContext().getModelService(serviceName);
                boolean requireNewTransaction = modelService.isRequireNewTransaction();
                int timeout = modelService.getTransactionTimeout();
                if (UtilValidate.isNotEmpty(requireNewTransactionStr)) {
                    requireNewTransaction = "true".equals(requireNewTransactionStr);
                }
                if (transactionTimeout >= 0) {
                    timeout = transactionTimeout;
                }
                result = ctxHelper.getDispatcher().runSync(serviceName, inMap, timeout, requireNewTransaction);
            }
        } catch (GenericServiceException e) {
            String errMsg = "Error running script " + ctxHelper.getScriptName() + " [problem invoking the [" + serviceName + "] service: "
                    + e.getMessage();
            Debug.logWarning(e, errMsg, MODULE);
            throw new ScriptException(errMsg);
        }
        return result;
    }

    @Override
    public void success() {
        if (ctxHelper.isEvent()) {
            ctxHelper.putResult("_response_code_", "success");
        } else if (ctxHelper.isService()) {
            ctxHelper.putResults(ServiceUtil.returnSuccess());
        }
    }

    @Override
    public void success(String message) {
        if (ctxHelper.isEvent()) {
            ctxHelper.putResult("_event_message_", ctxHelper.expandString(message));
            ctxHelper.putResult("_response_code_", "success");
        } else if (ctxHelper.isService()) {
            ctxHelper.putResults(ServiceUtil.returnSuccess(ctxHelper.expandString(message)));
        }
    }
}
