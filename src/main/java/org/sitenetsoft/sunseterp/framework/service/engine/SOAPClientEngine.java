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

import org.apache.axiom.om.*;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.base.util.UtilGenerics;
import org.sitenetsoft.sunseterp.framework.base.util.UtilValidate;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.service.GenericServiceException;
import org.sitenetsoft.sunseterp.framework.service.ModelParam;
import org.sitenetsoft.sunseterp.framework.service.ModelService;
import org.sitenetsoft.sunseterp.framework.service.ServiceDispatcher;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic Service SOAP Interface
 */
public final class SOAPClientEngine extends GenericAsyncEngine {

    private static final String MODULE = SOAPClientEngine.class.getName();

    public SOAPClientEngine(ServiceDispatcher dispatcher) {
        super(dispatcher);
    }

    /**
     * @see org.sitenetsoft.sunseterp.framework.service.engine.GenericEngine#runSyncIgnore(String, ModelService, Map)
     */
    @Override
    public void runSyncIgnore(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        runSync(localName, modelService, context);
    }

    /**
     * @see org.sitenetsoft.sunseterp.framework.service.engine.GenericEngine#runSync(String, ModelService, Map)
     */
    @Override
    public Map<String, Object> runSync(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        Map<String, Object> result = serviceInvoker(modelService, context);

        if (result == null) {
            throw new GenericServiceException("Service did not return expected result");
        }
        return result;
    }

    // Invoke the remote SOAP service
    private Map<String, Object> serviceInvoker(ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        Delegator delegator = getDispatcher().getDelegator();
        if (modelService.getLocation() == null || modelService.getInvoke() == null) {
            throw new GenericServiceException("Cannot locate service to invoke");
        }

        ServiceClient client = null;
        QName serviceName = null;
        String axis2Repo = "/framework/service/config/axis2";
        String axis2RepoLocation = System.getProperty("ofbiz.home") + axis2Repo;
        String axis2XmlFile = "/framework/service/config/axis2/conf/axis2.xml";
        String axis2XmlFileLocation = System.getProperty("ofbiz.home") + axis2XmlFile;

        try {
            ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(axis2RepoLocation,
                    axis2XmlFileLocation);
            client = new ServiceClient(configContext, null);
            Options options = new Options();
            EndpointReference endPoint = new EndpointReference(this.getLocation(modelService));
            options.setTo(endPoint);
            client.setOptions(options);
        } catch (AxisFault e) {
            throw new GenericServiceException("RPC service error", e);
        }

        List<ModelParam> inModelParamList = modelService.getInModelParamList();

        if (Debug.infoOn()) {
            Debug.logInfo("[SOAPClientEngine.invoke] : Parameter length - " + inModelParamList.size(), MODULE);
        }

        if (UtilValidate.isNotEmpty(modelService.getNameSpace())) {
            serviceName = new QName(modelService.getNameSpace(), modelService.getInvoke());
        } else {
            serviceName = new QName(modelService.getInvoke());
        }

        int i = 0;

        Map<String, Object> parameterMap = new HashMap<>();
        for (ModelParam p: inModelParamList) {
            if (Debug.infoOn()) {
                Debug.logInfo("[SOAPClientEngine.invoke} : Parameter: " + p.getName() + " (" + p.getMode() + ") - " + i, MODULE);
            }

            // exclude params that ModelServiceReader insert into (internal params)
            if (!p.getInternal()) {
                parameterMap.put(p.getName(), context.get(p.getName()));
            }
            i++;
        }

        OMElement parameterSer = null;

        try {
            String xmlParameters = SoapSerializer.serialize(parameterMap);
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(xmlParameters));
            OMXMLParserWrapper builder = OMXMLBuilderFactory.createStAXOMBuilder(reader);
            parameterSer = builder.getDocumentElement();
        } catch (Exception e) {
            Debug.logError(e, MODULE);
        }

        Map<String, Object> results = null;
        try {
            OMFactory factory = OMAbstractFactory.getOMFactory();
            OMElement payload = factory.createOMElement(serviceName);
            payload.addChild(parameterSer.getFirstElement());
            OMElement respOMElement = client.sendReceive(payload);
            client.cleanupTransport();
            results = UtilGenerics.cast(SoapSerializer.deserialize(respOMElement.toString(), delegator));
        } catch (Exception e) {
            Debug.logError(e, MODULE);
        }
        return results;
    }
}
