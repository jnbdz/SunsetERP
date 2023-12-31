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
package org.sitenetsoft.sunseterp.framework.service.engine;

import org.sitenetsoft.sunseterp.framework.base.util.*;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.entity.finder.PrimaryKeyFinder;
import org.sitenetsoft.sunseterp.framework.entity.model.ModelEntity;
import org.sitenetsoft.sunseterp.framework.entity.model.ModelField;
import org.sitenetsoft.sunseterp.framework.entity.model.ModelUtil;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityQuery;
import org.sitenetsoft.sunseterp.framework.security.SecuredUpload;
import org.sitenetsoft.sunseterp.framework.service.*;

import java.io.IOException;
import java.util.*;

/**
 * Standard Java Static Method Service Engine
 */
public final class EntityAutoEngine extends GenericAsyncEngine {

    private static final String MODULE = EntityAutoEngine.class.getName();
    private static final String RESOURCE = "ServiceErrorUiLabels";
    private static final List<String> AVAIL_INVOKE_ACTION_NAMES = UtilMisc.toList("create", "update", "delete", "expire");

    public EntityAutoEngine(ServiceDispatcher dispatcher) {
        super(dispatcher);
    }

    /**
     * @see org.sitenetsoft.sunseterp.framework.service.engine.GenericEngine#runSyncIgnore(String, org.sitenetsoft.sunseterp.framework.service.ModelService, Map)
     */
    @Override
    public void runSyncIgnore(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        runSync(localName, modelService, context);
    }

    /**
     * @see org.sitenetsoft.sunseterp.framework.service.engine.GenericEngine#runSync(String, org.sitenetsoft.sunseterp.framework.service.ModelService, Map)
     */
    @Override
    public Map<String, Object> runSync(String localName, ModelService modelService, Map<String, Object> parameters) throws GenericServiceException {
        // static java service methods should be: public Map<String, Object> methodName(DispatchContext dctx, Map<String, Object> context)
        if (!isValidText(parameters)) {
            return ServiceUtil.returnError("Not saved for security reason!");
        }
        DispatchContext dctx = getDispatcher().getLocalContext(localName);
        Locale locale = (Locale) parameters.get("locale");
        Map<String, Object> result = ServiceUtil.returnSuccess();

        // check the package and method names
        if (modelService.getInvoke() == null || !AVAIL_INVOKE_ACTION_NAMES.contains(modelService.getInvoke())) {
            throw new GenericServiceException("In Service [" + modelService.getName()
                    + "] the invoke value must be create, update, or delete for entity-auto engine");
        }

        if (UtilValidate.isEmpty(modelService.getDefaultEntityName())) {
            throw new GenericServiceException("In Service [" + modelService.getName()
                    + "] you must specify a default-entity-name for entity-auto engine");
        }

        ModelEntity modelEntity = dctx.getDelegator().getModelEntity(modelService.getDefaultEntityName());
        if (modelEntity == null) {
            throw new GenericServiceException("In Service [" + modelService.getName() + "] the specified default-entity-name ["
                    + modelService.getDefaultEntityName() + "] is not valid");
        }

        try {
            boolean allPksInOnly = true;
            List<String> pkFieldNameOutOnly = null;
            /* Check for each pk if it's :
             * 1. part IN
             * 2. or part IN and OUT, but without value but present on parameters map
             * Help the engine to determinate the operation to realize for a create call or validate that
             * any pk is present for update/delete call.
             */
            for (ModelField pkField: modelEntity.getPkFieldsUnmodifiable()) {
                ModelParam pkParam = modelService.getParam(pkField.getName());
                boolean pkValueInParameters = pkParam.isIn() && UtilValidate.isNotEmpty(parameters.get(pkParam.getFieldName()));
                if (pkParam.isOut() && !pkValueInParameters) {
                    if (pkFieldNameOutOnly == null) {
                        pkFieldNameOutOnly = new LinkedList<>();
                        allPksInOnly = false;
                    }
                    pkFieldNameOutOnly.add(pkField.getName());
                }
            }

            switch (modelService.getInvoke()) {
            case "create":
                result = invokeCreate(dctx, parameters, modelService, modelEntity, allPksInOnly, pkFieldNameOutOnly);
                break;
            case "update":
                result = invokeUpdate(dctx, parameters, modelService, modelEntity, allPksInOnly);
                break;
            case "delete":
                result = invokeDelete(dctx, parameters, modelService, modelEntity, allPksInOnly);
                break;
            case "expire":
                result = invokeExpire(dctx, parameters, modelService, modelEntity, allPksInOnly);
                if (ServiceUtil.isSuccess(result)) {
                    result = invokeUpdate(dctx, parameters, modelService, modelEntity, allPksInOnly);
                }
                break;
            default:
                break;
            }
            GenericValue crudValue = (GenericValue) result.get("crudValue");
            if (crudValue != null) {
                result.remove("crudValue");
                result.putAll(modelService.makeValid(crudValue, ModelService.OUT_PARAM));
            }
        } catch (GeneralException e) {
            Debug.logError(e, "Error doing entity-auto operation for entity [" + modelEntity.getEntityName() + "] in service ["
                    + modelService.getName() + "]: " + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ServiceEntityAutoOperation",
                    UtilMisc.toMap("entityName", modelEntity.getEntityName(), "serviceName", modelService.getName(), "errorString",
                            e.toString()), locale));
        }
        result.put(ModelService.SUCCESS_MESSAGE, ServiceUtil.makeSuccessMessage(result, "", "", "", ""));
        return result;
    }

    private static Map<String, Object> invokeCreate(DispatchContext dctx, Map<String, Object> parameters, ModelService modelService,
                                                    ModelEntity modelEntity, boolean allPksInOnly, List<String> pkFieldNameOutOnly)
            throws GeneralException {
        Locale locale = (Locale) parameters.get("locale");

        GenericValue newEntity = dctx.getDelegator().makeValue(modelEntity.getEntityName());

        boolean isSinglePk = modelEntity.getPksSize() == 1;
        boolean isDoublePk = modelEntity.getPksSize() == 2;
        Iterator<ModelField> pksIter = modelEntity.getPksIterator();

        ModelField singlePkModeField = isSinglePk ? pksIter.next() : null;
        ModelParam singlePkModelParam = isSinglePk ? modelService.getParam(singlePkModeField.getName()) : null;
        boolean isSinglePkIn = isSinglePk ? singlePkModelParam.isIn() : false;
        boolean isSinglePkOut = isSinglePk ? singlePkModelParam.isOut() : false;

        ModelParam doublePkPrimaryInParam = null;
        ModelParam doublePkSecondaryOutParam = null;
        ModelField doublePkSecondaryOutField = null;
        if (isDoublePk) {
            ModelField firstPkField = pksIter.next();
            ModelParam firstPkParam = modelService.getParam(firstPkField.getName());
            ModelField secondPkField = pksIter.next();
            ModelParam secondPkParam = modelService.getParam(secondPkField.getName());
            if (firstPkParam.isIn() && secondPkParam.isOut()) {
                doublePkPrimaryInParam = firstPkParam;
                doublePkSecondaryOutParam = secondPkParam;
                doublePkSecondaryOutField = secondPkField;
            } else if (firstPkParam.isOut() && secondPkParam.isIn()) {
                doublePkPrimaryInParam = secondPkParam;
                doublePkSecondaryOutParam = firstPkParam;
                doublePkSecondaryOutField = firstPkField;
                // } else {
                // we don't have an IN and an OUT... so do nothing and leave them null
            }
        }

        if (isSinglePk && isSinglePkOut && !isSinglePkIn) {
            /*
             **** primary sequenced primary key ****
             *
            <auto-attributes include="pk" mode="OUT" optional="false"/>
             *
            <make-value entity-name="Example" value-name="newEntity"/>
            <sequenced-id-to-env sequence-name="Example" env-name="newEntity.exampleId"/> <!-- get the next sequenced ID -->
            <field-to-result field-name="newEntity.exampleId" result-name="exampleId"/>
            <set-nonpk-fields map-name="parameters" value-name="newEntity"/>
            <create-value value-name="newEntity"/>
             *
             */

            String sequencedId = dctx.getDelegator().getNextSeqId(modelEntity.getEntityName());
            newEntity.set(singlePkModeField.getName(), sequencedId);
        } else if (isSinglePk && isSinglePkOut && isSinglePkIn) {
            /*
             **** primary sequenced key with optional override passed in ****
             *
            <auto-attributes include="pk" mode="INOUT" optional="true"/>
             *
            <make-value value-name="newEntity" entity-name="Product"/>
            <set-nonpk-fields map-name="parameters" value-name="newEntity"/>
            <set from-field="parameters.productId" field="newEntity.productId"/>
            <if-empty field="newEntity.productId">
                <sequenced-id-to-env sequence-name="Product" env-name="newEntity.productId"/>
            <else>
                <check-id field-name="productId" map-name="newEntity"/>
                <check-errors/>
            </else>
            </if-empty>
            <field-to-result field-name="productId" map-name="newEntity" result-name="productId"/>
            <create-value value-name="newEntity"/>
             *
             */

            Object pkValue = parameters.get(singlePkModelParam.getName());
            if (UtilValidate.isEmpty(pkValue)) {
                pkValue = dctx.getDelegator().getNextSeqId(modelEntity.getEntityName());
            } else {
                // pkValue passed in, check and if there are problems return an error

                if (pkValue instanceof String) {
                    StringBuffer errorDetails = new StringBuffer();
                    if (!UtilValidate.isValidDatabaseId((String) pkValue, errorDetails)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ServiceParameterValueNotValid",
                                UtilMisc.toMap("parameterName", singlePkModelParam.getName(), "errorDetails", errorDetails), locale));
                    }
                }
            }
            newEntity.set(singlePkModeField.getName(), pkValue);
            GenericValue lookedUpValue = PrimaryKeyFinder.runFind(modelEntity, parameters, dctx.getDelegator(), false, true, null, null);
            if (lookedUpValue != null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ServiceValueFound",
                        UtilMisc.toMap("pkFields", newEntity.getPkShortValueString()), locale));
            }
        } else if (isDoublePk && doublePkPrimaryInParam != null && doublePkSecondaryOutParam != null) {
            /*
             **** secondary sequenced primary key ****
             *
            <auto-attributes include="pk" mode="IN" optional="false"/>
            <override name="exampleItemSeqId" mode="OUT"/> <!-- make this OUT rather than IN, we will automatically generate the
            * next sub-sequence ID -->
             *
            <make-value entity-name="ExampleItem" value-name="newEntity"/>
            <set-pk-fields map-name="parameters" value-name="newEntity"/>
            <make-next-seq-id value-name="newEntity" seq-field-name="exampleItemSeqId"/> <!-- this finds the next sub-sequence ID -->
            <field-to-result field-name="newEntity.exampleItemSeqId" result-name="exampleItemSeqId"/>
            <set-nonpk-fields map-name="parameters" value-name="newEntity"/>
            <create-value value-name="newEntity"/>
             */

            newEntity.setPKFields(parameters, true);
            dctx.getDelegator().setNextSubSeqId(newEntity, doublePkSecondaryOutField.getName(), 5, 1);
        } else if (allPksInOnly) {
            /*
             **** plain specified primary key ****
             *
            <auto-attributes include="pk" mode="IN" optional="false"/>
             *
            <make-value entity-name="Example" value-name="newEntity"/>
            <set-pk-fields map-name="parameters" value-name="newEntity"/>
            <set-nonpk-fields map-name="parameters" value-name="newEntity"/>
            <create-value value-name="newEntity"/>
             *
             */
            newEntity.setPKFields(parameters, true);
            //with all pks present on parameters, check if the entity is not already exists.
            GenericValue lookedUpValue = PrimaryKeyFinder.runFind(modelEntity, parameters, dctx.getDelegator(), false, true, null, null);
            if (lookedUpValue != null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ServiceValueFound",
                        UtilMisc.toMap("pkFields", newEntity.getPkShortValueString()), locale));
            }
        } else {
            /* We haven't all Pk and their are 3 or more, now check if isn't a associate entity with own sequence
            <set-pk-fields map="parameters" value-field="newEntity"/>
            <sequenced-id sequence-name="ExempleItemAssoc" field="newEntity.exempleItemAssocId"/>
            <create-value value-field="newEntity"/>
             */
            if (pkFieldNameOutOnly != null && pkFieldNameOutOnly.size() == 1) {
                newEntity.setPKFields(parameters, true);
                String pkFieldName = pkFieldNameOutOnly.get(0);
                //if it's a fromDate, don't update it now, it's will be done next step
                if (!"fromDate".equals(pkFieldName)) {
                    String pkValue = dctx.getDelegator().getNextSeqId(modelEntity.getEntityName());
                    newEntity.set(pkFieldName, pkValue);
                }
            } else {
                throw new GenericServiceException("In Service [" + modelService.getName()
                        + "] which uses the entity-auto engine with the create invoke option: "
                        + "could not find a valid combination of primary key settings to do a known create operation; options include: "
                        + "1. a single OUT pk for primary auto-sequencing, "
                        + "2. a single INOUT pk for primary auto-sequencing with optional override, "
                        + "3. a 2-part pk with one part IN (existing primary pk) and one part OUT (the secondary pk to sub-sequence), "
                        + "4. a N-part pk with N-1 part IN and one party OUT only (missing pk is a sub-sequence mainly for entity assoc), "
                        + "5. all pk fields are IN for a manually specified primary key");
            }
        }

        // handle the case where there is a fromDate in the pk of the entity, and it is optional or undefined in the service def,
        // populate automatically
        ModelField fromDateField = modelEntity.getField("fromDate");
        if (fromDateField != null && fromDateField.getIsPk()) {
            ModelParam fromDateParam = modelService.getParam("fromDate");
            if (fromDateParam == null || parameters.get("fromDate") == null) {
                newEntity.set("fromDate", UtilDateTime.nowTimestamp());
            }
        }

        newEntity.setNonPKFields(parameters, true);
        if (modelEntity.getField("createdDate") != null) {
            newEntity.set("createdDate", UtilDateTime.nowTimestamp());
            if (modelEntity.getField("createdByUserLogin") != null) {
                GenericValue userLogin = (GenericValue) parameters.get("userLogin");
                if (userLogin != null) {
                    newEntity.set("createdByUserLogin", userLogin.get("userLoginId"));
                    if (modelEntity.getField("lastModifiedByUserLogin") != null) {
                        newEntity.set("lastModifiedByUserLogin", userLogin.get("userLoginId"));
                    } else if (modelEntity.getField("changedByUserLogin") != null) {
                        newEntity.set("changedByUserLogin", userLogin.get("userLoginId"));
                    }
                }
            }
            if (modelEntity.getField("lastModifiedDate") != null) {
                newEntity.set("lastModifiedDate", UtilDateTime.nowTimestamp());
            } else if (modelEntity.getField("changedDate") != null) {
                newEntity.set("changedDate", UtilDateTime.nowTimestamp());
            }
        }

        if (modelEntity.getField("changeByUserLoginId") != null) {
            GenericValue userLogin = (GenericValue) parameters.get("userLogin");
            if (userLogin != null) {
                newEntity.set("changeByUserLoginId", userLogin.get("userLoginId"));
            } else {
                throw new GenericServiceException("You call a creation on entity that require the userLogin to track the activity,"
                        + " please control that your service definition has auth='true'");
            }

            //Oh changeByUserLoginId detected, check if an EntityStatus concept
            if (modelEntity.getEntityName().endsWith("Status")) {
                if (modelEntity.getField("statusDate") != null && parameters.get("statusDate") == null) {
                    newEntity.set("statusDate", UtilDateTime.nowTimestamp());

                    //if a statusEndDate is present, resolve the last EntityStatus to store this value on the previous element
                    if (modelEntity.getField("statusEndDate") != null) {
                        ModelEntity relatedEntity = dctx.getDelegator().getModelEntity(modelEntity.getEntityName().replaceFirst("Status", ""));
                        if (relatedEntity != null) {
                            Map<String, Object> conditionRelatedPkFieldMap = new HashMap<>();
                            for (String pkRelatedField : relatedEntity.getPkFieldNames()) {
                                conditionRelatedPkFieldMap.put(pkRelatedField, parameters.get(pkRelatedField));
                            }
                            GenericValue previousStatus = EntityQuery.use(newEntity.getDelegator()).from(modelEntity.getEntityName())
                                    .where(conditionRelatedPkFieldMap).orderBy("-statusDate").queryFirst();
                            if (previousStatus != null) {
                                previousStatus.put("statusEndDate", newEntity.get("statusDate"));
                                previousStatus.store();
                            }
                        }
                    }
                }
            }
        }
        newEntity.create();
        Map<String, Object> result = ServiceUtil.returnSuccess(UtilProperties.getMessage("ServiceUiLabels", "EntityCreatedSuccessfully",
                UtilMisc.toMap("label", modelEntity.getTitle()), locale));
        result.put("crudValue", newEntity);
        return result;
    }

    private static Map<String, Object> invokeUpdate(DispatchContext dctx, Map<String, Object> parameters, ModelService modelService,
                                                    ModelEntity modelEntity, boolean allPksInOnly)
            throws GeneralException {
        Locale locale = (Locale) parameters.get("locale");
        Map<String, Object> localContext = new HashMap<>();
        localContext.put("parameters", parameters);
        Map<String, Object> result = ServiceUtil.returnSuccess();
        /*
         <auto-attributes include="pk" mode="IN" optional="false"/>
         <entity-one entity-name="ExampleItem" value-name="lookedUpValue"/>
         <set-nonpk-fields value-name="lookedUpValue" map-name="parameters"/>
         <store-value value-name="lookedUpValue"/>
         */

        // check to make sure that all primary key fields are defined as IN attributes
        if (!allPksInOnly) {
            throw new GenericServiceException("In Service [" + modelService.getName() + "] which uses the entity-auto engine with the update"
                    + " invoke option not all pk fields have the mode IN");
        }

        GenericValue lookedUpValue = PrimaryKeyFinder.runFind(modelEntity, parameters, dctx.getDelegator(), false, true, null, null);
        if (lookedUpValue == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ServiceValueNotFound", locale));
        }

        localContext.put("lookedUpValue", lookedUpValue);

        // populate the oldStatusId or oldItemStatusId out if there is a service parameter for it, and before we do the set non-pk fields
        /*
        <auto-attributes include="pk" mode="IN" optional="false"/>
        <attribute name="oldStatusId" type="String" mode="OUT" optional="false"/>
        <field-to-result field-name="lookedUpValue.statusId" result-name="oldStatusId"/>
        OR
        <auto-attributes include="pk" mode="IN" optional="false"/>
        <attribute name="oldItemStatusId" type="String" mode="OUT" optional="false"/>
        <field-to-result field-name="lookedUpValue.itemStatusId" result-name="oldItemStatusId"/>
         */
        for (String statusField: UtilMisc.toList("statusId", "itemStatusId")) {
            ModelParam statusIdParam = modelService.getParam(statusField);
            ModelField statusIdModelField = modelEntity.getField(statusField);
            String oldStatusField = "old" + ModelUtil.upperFirstChar(statusField);
            ModelParam oldStatusIdParam = modelService.getParam(oldStatusField);
            if (statusIdParam != null && statusIdParam.isIn()
                    && oldStatusIdParam != null && oldStatusIdParam.isOut()
                    && statusIdModelField != null) {
                result.put(oldStatusField, lookedUpValue.get(statusField));
            }

            // do the StatusValidChange check
            /*
             <if-compare-field field="lookedUpValue.statusId" operator="not-equals" to-field="parameters.statusId">
                 <!-- if the record exists there should be a statusId, but just in case make it so it won't blow up -->
                 <if-not-empty field="lookedUpValue.statusId">
                     <!-- if statusId change is not in the StatusValidChange list, complain... -->
                          <entity-one entity-name="StatusValidChange" value-name="statusValidChange" auto-field-map="false">
                         <field-map field-name="statusId" env-name="lookedUpValue.statusId"/>
                         <field-map field-name="statusIdTo" env-name="parameters.statusId"/>
                     </entity-one>
                     <if-empty field="statusValidChange">
                         <!-- no valid change record found? return an error... -->
                              <add-error><fail-property resource="CommonUiLabels" property="CommonErrorNoStatusValidChange"/></add-error>
                         <check-errors/>
                     </if-empty>
                 </if-not-empty>
             </if-compare-field>
             */
            String statusIdParamValue = (String) parameters.get(statusField);
            if (statusIdParam != null && statusIdParam.isIn()
                    && UtilValidate.isNotEmpty(statusIdParamValue) && statusIdModelField != null) {
                String lookedUpStatusId = (String) lookedUpValue.get(statusField);
                if (UtilValidate.isNotEmpty(lookedUpStatusId) && !statusIdParamValue.equals(lookedUpStatusId)) {
                    // there was an old status, and in this call we are trying to change it, so do the StatusValidChange check
                    GenericValue statusValidChange = dctx.getDelegator().findOne("StatusValidChange", true, "statusId",
                            lookedUpStatusId, "statusIdTo", statusIdParamValue);
                    if (statusValidChange == null) {
                        // uh-oh, no valid change...
                        return ServiceUtil.returnError(UtilProperties.getMessage("CommonUiLabels", "CommonErrorNoStatusValidChange",
                                localContext, locale));
                    }
                }
            }
            // NOTE: nothing here to maintain the status history, that should be done with a custom service called by SECA rule
        }

        lookedUpValue.setNonPKFields(parameters, true);
        if (modelEntity.getField("lastModifiedDate") != null
                || modelEntity.getField("changedDate") != null) {
            if (modelEntity.getField("lastModifiedDate") != null) {
                lookedUpValue.set("lastModifiedDate", UtilDateTime.nowTimestamp());
            } else {
                lookedUpValue.set("changedDate", UtilDateTime.nowTimestamp());
            }
            if (modelEntity.getField("lastModifiedByUserLogin") != null
                    || modelEntity.getField("changedByUserLogin") != null) {
                GenericValue userLogin = (GenericValue) parameters.get("userLogin");
                if (userLogin != null) {
                    if (modelEntity.getField("lastModifiedByUserLogin") != null) {
                        lookedUpValue.set("lastModifiedByUserLogin", userLogin.get("userLoginId"));
                    } else {
                        lookedUpValue.set("changedByUserLogin", userLogin.get("userLoginId"));
                    }
                }
            }
        }

        if (modelEntity.getField("changeByUserLoginId") != null) {
            if (modelEntity.getEntityName().endsWith("Status")) {
                //Oh update on EntityStatus concept detected ... not possible, return invalid request
                throw new GenericServiceException("You call a updating operation on entity that track the activity, sorry I can't do that,"
                        + "please amazing developer check your service definition;)");
            }
            GenericValue userLogin = (GenericValue) parameters.get("userLogin");
            if (userLogin != null) {
                lookedUpValue.set("changeByUserLoginId", userLogin.get("userLoginId"));
            } else {
                throw new GenericServiceException("You call a updating operation on entity that track the activity, sorry I can't do that,"
                        + "please amazing developer check your service definition;)");
            }
        }

        lookedUpValue.store();
        result.put("crudValue", lookedUpValue);
        result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage("ServiceUiLabels", "EntityUpdatedSuccessfully",
                UtilMisc.toMap("label", modelEntity.getTitle()), locale));
        return result;
    }

    private static Map<String, Object> invokeDelete(DispatchContext dctx, Map<String, Object> parameters, ModelService modelService,
                                                    ModelEntity modelEntity, boolean allPksInOnly)
            throws GeneralException {
        Locale locale = (Locale) parameters.get("locale");
        /*
        <auto-attributes include="pk" mode="IN" optional="false"/>
        <entity-one entity-name="ExampleItem" value-name="lookedUpValue"/>
        <remove-value value-name="lookedUpValue"/>
         */

        // check to make sure that all primary key fields are defined as IN attributes
        if (!allPksInOnly) {
            throw new GenericServiceException("In Service [" + modelService.getName() + "] which uses the entity-auto engine with the delete"
                    + "invoke option not all pk fields have the mode IN");
        }

        if (modelEntity.getField("changeByUserLoginId") != null) {
            if (modelEntity.getEntityName().endsWith("Status")) {
                //Oh update on EntityStatus concept detected ... not possible, return invalid request
                throw new GenericServiceException("You call a deleting operation on entity that track the activity, sorry I can't do that,"
                        + "please amazing developer check your service definition;)");
            }
        }

        GenericValue lookedUpValue = PrimaryKeyFinder.runFind(modelEntity, parameters, dctx.getDelegator(), false, true, null, null);
        if (lookedUpValue == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ServiceValueNotFoundForRemove", locale));
        }
        lookedUpValue.remove();
        Map<String, Object> result = ServiceUtil.returnSuccess(UtilProperties.getMessage("ServiceUiLabels", "EntityDeletedSuccessfully",
                UtilMisc.toMap("label", modelEntity.getTitle()), locale));
        return result;
    }

    /**
     * Analyse the entity, service and parameter to resolve the field to update with what value
     * @param dctx
     * @param parameters
     * @param modelService
     * @param modelEntity
     * @param allPksInOnly
     * @return
     * @throws GeneralException
     */
    private static Map<String, Object> invokeExpire(DispatchContext dctx, Map<String, Object> parameters, ModelService modelService,
                                                    ModelEntity modelEntity, boolean allPksInOnly)
            throws GeneralException {
        Locale locale = (Locale) parameters.get("locale");
        List<String> fieldThruDates = new LinkedList<>();
        boolean thruDatePresent = false;
        String fieldDateNameIn = null;

        // check to make sure that all primary key fields are defined as IN attributes
        if (!allPksInOnly) {
            throw new GenericServiceException("In Service [" + modelService.getName() + "] which uses the entity-auto engine with the update"
                    + "invoke option not all pk fields have the mode IN");
        }
        GenericValue lookedUpValue = PrimaryKeyFinder.runFind(modelEntity, parameters, dctx.getDelegator(), false, true, null, null);
        if (lookedUpValue == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ServiceValueNotFound", locale));
        }

        //check if a non pk date field is present on parameters
        for (String fieldDateName : modelEntity.getNoPkFieldNames()) {
            if ("thruDate".equals(fieldDateName)) {
                thruDatePresent = true;
            } else if (fieldDateName.endsWith("ThruDate")) {
                fieldThruDates.add(fieldDateName);
            } else if (fieldDateName.startsWith("thru") && fieldDateName.endsWith("Date")) {
                fieldThruDates.add(fieldDateName);
            } else if (fieldDateNameIn == null && modelService.getParam(fieldDateName) != null
                    && modelEntity.getField(fieldDateName).getType().contains("date")) {
                fieldDateNameIn = fieldDateName;
            }
        }

        if (Debug.infoOn()) {
            Debug.logInfo(" FIELD FOUND : " + fieldDateNameIn + " ## # " + fieldThruDates + " ### " + thruDatePresent, MODULE);
        }

        if (Debug.infoOn()) {
            Debug.logInfo(" parameters IN  : " + parameters, MODULE);
        }
        // Resolve the field without value to expire and check if the value is present on parameters or use now
        if (fieldDateNameIn != null) {
            if (parameters.get(fieldDateNameIn) == null) {
                parameters.put(fieldDateNameIn, UtilDateTime.nowTimestamp());
            }
        }
        // Expire thruDate fields
        if (thruDatePresent && UtilValidate.isEmpty(lookedUpValue.getTimestamp("thruDate"))) {
            if (UtilValidate.isEmpty(parameters.get("thruDate"))) {
                parameters.put("thruDate", UtilDateTime.nowTimestamp());
            }
        } else {
            for (String fieldDateName: fieldThruDates) {
                if (UtilValidate.isEmpty(lookedUpValue.getTimestamp(fieldDateName))) {
                    if (UtilValidate.isEmpty(parameters.get(fieldDateName))) {
                        parameters.put(fieldDateName, UtilDateTime.nowTimestamp());
                    }
                    break;
                }
            }
        }
        if (Debug.infoOn()) {
            Debug.logInfo(" parameters OUT  : " + parameters, MODULE);
        }
        Map<String, Object> result = ServiceUtil.returnSuccess(UtilProperties.getMessage("ServiceUiLabels", "EntityExpiredSuccessfully",
                UtilMisc.toMap("label", modelEntity.getTitle()), locale));
        return result;
    }

    private static boolean isValidText(Map<String, Object> parameters) {
        // TODO maybe more parameters will be needed in future...
        String parameter = (String) parameters.get("webappPath");
        if (parameter != null) {
            try {
                if (!SecuredUpload.isValidText(parameter, Collections.emptyList())) {
                    Debug.logError("================== Not saved for security reason ==================", MODULE);
                    return false;
                }
            } catch (IOException e) {
                Debug.logError("================== Not saved for security reason ==================", MODULE);
                return false;
            }
        }
        return true;
    }
}
