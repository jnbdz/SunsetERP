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

package org.sitenetsoft.sunseterp.applications.accounting.thirdparty.sagepay;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.base.util.UtilMisc;
import org.sitenetsoft.sunseterp.framework.base.util.UtilProperties;
import org.sitenetsoft.sunseterp.framework.base.util.UtilValidate;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityQuery;
import org.sitenetsoft.sunseterp.framework.service.DispatchContext;
import org.sitenetsoft.sunseterp.framework.service.ModelService;
import org.sitenetsoft.sunseterp.framework.service.ServiceUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

public class SagePayServices {
    private static final String MODULE = SagePayServices.class.getName();
    private static final String RESOURCE = "AccountingUiLabels";

    private static Map<String, String> buildSagePayProperties(Map<String, Object> context, Delegator delegator) {

        Map<String, String> sagePayConfig = new HashMap<>();

        String paymentGatewayConfigId = (String) context.get("paymentGatewayConfigId");

        if (UtilValidate.isNotEmpty(paymentGatewayConfigId)) {
            try {
                GenericValue sagePay = EntityQuery.use(delegator).from("PaymentGatewaySagePay").where("paymentGatewayConfigId",
                        paymentGatewayConfigId).queryOne();
                if (sagePay != null) {
                    for (Entry<String, Object> set : sagePay.entrySet()) {
                        if (set.getValue() == null) {
                            sagePayConfig.put(set.getKey(), null);
                        } else {
                            sagePayConfig.put(set.getKey(), set.getValue().toString());
                        }
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
        }

        Debug.logInfo("SagePay Configuration : " + sagePayConfig.toString(), MODULE);
        return sagePayConfig;
    }

    public static Map<String, Object> paymentAuthentication(DispatchContext ctx, Map<String, Object> context) {
        Debug.logInfo("SagePay - Entered paymentAuthentication", MODULE);
        Debug.logInfo("SagePay paymentAuthentication context : " + context, MODULE);

        Delegator delegator = ctx.getDelegator();
        Map<String, Object> resultMap = new HashMap<>();

        Map<String, String> props = buildSagePayProperties(context, delegator);

        String vendorTxCode = (String) context.get("vendorTxCode");
        String cardHolder = (String) context.get("cardHolder");
        String cardNumber = (String) context.get("cardNumber");
        String expiryDate = (String) context.get("expiryDate");
        String cardType = (String) context.get("cardType");
        String cv2 = (String) context.get("cv2");
        String amount = (String) context.get("amount");
        String currency = (String) context.get("currency");
        String description = (String) context.get("description");

        String billingSurname = (String) context.get("billingSurname");
        String billingFirstnames = (String) context.get("billingFirstnames");
        String billingAddress = (String) context.get("billingAddress");
        String billingAddress2 = (String) context.get("billingAddress2");
        String billingCity = (String) context.get("billingCity");
        String billingPostCode = (String) context.get("billingPostCode");
        String billingCountry = (String) context.get("billingCountry");
        String billingState = (String) context.get("billingState");
        String billingPhone = (String) context.get("billingPhone");

        Boolean isBillingSameAsDelivery = (Boolean) context.get("isBillingSameAsDelivery");

        String deliverySurname = (String) context.get("deliverySurname");
        String deliveryFirstnames = (String) context.get("deliveryFirstnames");
        String deliveryAddress = (String) context.get("deliveryAddress");
        String deliveryAddress2 = (String) context.get("deliveryAddress2");
        String deliveryCity = (String) context.get("deliveryCity");
        String deliveryPostCode = (String) context.get("deliveryPostCode");
        String deliveryCountry = (String) context.get("deliveryCountry");
        String deliveryState = (String) context.get("deliveryState");
        String deliveryPhone = (String) context.get("deliveryPhone");

        String startDate = (String) context.get("startDate");
        String issueNumber = (String) context.get("issueNumber");
        String basket = (String) context.get("basket");
        String clientIPAddress = (String) context.get("clientIPAddress");
        Locale locale = (Locale) context.get("locale");

        HttpHost host = SagePayUtil.getHost(props);

        //start - authentication parameters
        Map<String, String> parameters = new HashMap<>();

        String vpsProtocol = props.get("protocolVersion");
        String vendor = props.get("vendor");
        String txType = props.get("authenticationTransType");
        //start - required parameters
        StringBuilder errorRequiredParameters = new StringBuilder();
        if (vpsProtocol == null) {
            errorRequiredParameters.append("Required transaction parameter 'protocolVersion' is missing. ");
        }
        if (vendor == null) {
            errorRequiredParameters.append("Required transaction parameter 'vendor' is missing. ");
        }
        if (txType == null) {
            errorRequiredParameters.append("Required transaction parameter 'authenticationsTransType' is missing. ");
        }
        if (errorRequiredParameters.length() > 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "AccountingSagePayPaymentAuthorisationException",
                    UtilMisc.toMap("errorString", errorRequiredParameters), locale));
        }
        parameters.put("VPSProtocol", vpsProtocol);
        parameters.put("TxType", txType);
        parameters.put("Vendor", vendor);

        if (vendorTxCode != null) {
            parameters.put("VendorTxCode", vendorTxCode);
        }
        if (amount != null) {
            parameters.put("Amount", amount);
        }
        if (currency != null) {
            parameters.put("Currency", currency);
        } //GBP/USD
        if (description != null) {
            parameters.put("Description", description);
        }
        if (cardHolder != null) {
            parameters.put("CardHolder", cardHolder);
        }
        if (cardNumber != null) {
            parameters.put("CardNumber", cardNumber);
        }
        if (cardType != null) {
            parameters.put("CardType", cardType);
        }
        if (expiryDate != null) {
            parameters.put("ExpiryDate", expiryDate);
        }

        //start - billing details
        if (billingSurname != null) {
            parameters.put("BillingSurname", billingSurname);
        }
        if (billingFirstnames != null) {
            parameters.put("BillingFirstnames", billingFirstnames);
        }
        if (billingAddress != null) {
            parameters.put("BillingAddress", billingAddress);
        }
        if (billingAddress2 != null) {
            parameters.put("BillingAddress2", billingAddress2);
        }
        if (billingCity != null) {
            parameters.put("BillingCity", billingCity);
        }
        if (billingPostCode != null) {
            parameters.put("BillingPostCode", billingPostCode);
        }
        if (billingCountry != null) {
            parameters.put("BillingCountry", billingCountry);
        }
        if (billingState != null) {
            parameters.put("BillingState", billingState);
        }
        if (billingPhone != null) {
            parameters.put("BillingPhone", billingPhone);
        }
        //end - billing details

        //start - delivery details
        if (isBillingSameAsDelivery != null && isBillingSameAsDelivery) {
            if (billingSurname != null) {
                parameters.put("DeliverySurname", billingSurname);
            }
            if (billingFirstnames != null) {
                parameters.put("DeliveryFirstnames", billingFirstnames);
            }
            if (billingAddress != null) {
                parameters.put("DeliveryAddress", billingAddress);
            }
            if (billingAddress2 != null) {
                parameters.put("DeliveryAddress2", billingAddress2);
            }
            if (billingCity != null) {
                parameters.put("DeliveryCity", billingCity);
            }
            if (billingPostCode != null) {
                parameters.put("DeliveryPostCode", billingPostCode);
            }
            if (billingCountry != null) {
                parameters.put("DeliveryCountry", billingCountry);
            }
            if (billingState != null) {
                parameters.put("DeliveryState", billingState);
            }
            if (billingPhone != null) {
                parameters.put("DeliveryPhone", billingPhone);
            }
        } else {
            if (deliverySurname != null) {
                parameters.put("DeliverySurname", deliverySurname);
            }
            if (deliveryFirstnames != null) {
                parameters.put("DeliveryFirstnames", deliveryFirstnames);
            }
            if (deliveryAddress != null) {
                parameters.put("DeliveryAddress", deliveryAddress);
            }
            if (deliveryAddress2 != null) {
                parameters.put("DeliveryAddress2", deliveryAddress2);
            }
            if (deliveryCity != null) {
                parameters.put("DeliveryCity", deliveryCity);
            }
            if (deliveryPostCode != null) {
                parameters.put("DeliveryPostCode", deliveryPostCode);
            }
            if (deliveryCountry != null) {
                parameters.put("DeliveryCountry", deliveryCountry);
            }
            if (deliveryState != null) {
                parameters.put("DeliveryState", deliveryState);
            }
            if (deliveryPhone != null) {
                parameters.put("DeliveryPhone", deliveryPhone);
            }
        }
        //end - delivery details
        //end - required parameters

        //start - optional parameters
        if (cv2 != null) {
            parameters.put("CV2", cv2);
        }
        if (startDate != null) {
            parameters.put("StartDate", startDate);
        }
        if (issueNumber != null) {
            parameters.put("IssueNumber", issueNumber);
        }
        if (basket != null) {
            parameters.put("Basket", basket);
        }
        if (clientIPAddress != null) {
            parameters.put("ClientIPAddress", clientIPAddress);
        }
        //end - optional parameters
        //end - authentication parameters

        try (CloseableHttpClient httpClient = SagePayUtil.getHttpClient()) {

            String successMessage = null;
            HttpPost httpPost = SagePayUtil.getHttpPost(props.get("authenticationUrl"), parameters);
            HttpResponse response = httpClient.execute(host, httpPost);
            Map<String, String> responseData = SagePayUtil.getResponseData(response);

            String status = responseData.get("Status");
            String statusDetail = responseData.get("StatusDetail");

            resultMap.put("status", status);
            resultMap.put("statusDetail", statusDetail);

            //returning the below details back to the calling code, as it not returned back by the payment gateway
            resultMap.put("vendorTxCode", vendorTxCode);
            resultMap.put("amount", amount);
            resultMap.put("transactionType", txType);

            //start - transaction authorized
            if ("OK".equals(status)) {
                resultMap.put("vpsTxId", responseData.get("VPSTxId"));
                resultMap.put("securityKey", responseData.get("SecurityKey"));
                resultMap.put("txAuthNo", responseData.get("TxAuthNo"));
                resultMap.put("avsCv2", responseData.get("AVSCV2"));
                resultMap.put("addressResult", responseData.get("AddressResult"));
                resultMap.put("postCodeResult", responseData.get("PostCodeResult"));
                resultMap.put("cv2Result", responseData.get("CV2Result"));
                successMessage = "Payment authorized";
            }
            //end - transaction authorized

            if ("NOTAUTHED".equals(status)) {
                resultMap.put("vpsTxId", responseData.get("VPSTxId"));
                resultMap.put("securityKey", responseData.get("SecurityKey"));
                resultMap.put("avsCv2", responseData.get("AVSCV2"));
                resultMap.put("addressResult", responseData.get("AddressResult"));
                resultMap.put("postCodeResult", responseData.get("PostCodeResult"));
                resultMap.put("cv2Result", responseData.get("CV2Result"));
                successMessage = "Payment not authorized";
            }

            if ("MALFORMED".equals(status)) {
                //request not formed properly or parameters missing
                resultMap.put("vpsTxId", responseData.get("VPSTxId"));
                resultMap.put("securityKey", responseData.get("SecurityKey"));
                resultMap.put("avsCv2", responseData.get("AVSCV2"));
                resultMap.put("addressResult", responseData.get("AddressResult"));
                resultMap.put("postCodeResult", responseData.get("PostCodeResult"));
                resultMap.put("cv2Result", responseData.get("CV2Result"));
            }

            if ("INVALID".equals(status)) {
                //invalid information in request
                resultMap.put("vpsTxId", responseData.get("VPSTxId"));
                resultMap.put("securityKey", responseData.get("SecurityKey"));
                resultMap.put("avsCv2", responseData.get("AVSCV2"));
                resultMap.put("addressResult", responseData.get("AddressResult"));
                resultMap.put("postCodeResult", responseData.get("PostCodeResult"));
                resultMap.put("cv2Result", responseData.get("CV2Result"));
            }

            if ("REJECTED".equals(status)) {
                //invalid information in request
                resultMap.put("vpsTxId", responseData.get("VPSTxId"));
                resultMap.put("securityKey", responseData.get("SecurityKey"));
                resultMap.put("avsCv2", responseData.get("AVSCV2"));
                resultMap.put("addressResult", responseData.get("AddressResult"));
                resultMap.put("postCodeResult", responseData.get("PostCodeResult"));
                resultMap.put("cv2Result", responseData.get("CV2Result"));
            }

            resultMap.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            resultMap.put(ModelService.SUCCESS_MESSAGE, successMessage);

        } catch (UnsupportedEncodingException uee) {
            //exception in encoding parameters in httpPost
            Debug.logError(uee, "Error occurred in encoding parameters for HttpPost (" + uee.getMessage() + ")", MODULE);
            resultMap = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "AccountingSagePayErrorEncodingParameters",
                    UtilMisc.toMap("errorString", uee.getMessage()), locale));
        } catch (ClientProtocolException cpe) {
            //from httpClient execute
            Debug.logError(cpe, "Error occurred in HttpClient execute(" + cpe.getMessage() + ")", MODULE);
            resultMap = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "AccountingSagePayErrorHttpClientExecute",
                    UtilMisc.toMap("errorString", cpe.getMessage()), locale));
        } catch (IOException ioe) {
            //from httpClient execute or getResponsedata
            Debug.logError(ioe, "Error occurred in HttpClient execute or getting response (" + ioe.getMessage() + ")", MODULE);
            resultMap = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "AccountingSagePayErrorHttpClientExecuteOrGettingResponse",
                    UtilMisc.toMap("errorString", ioe.getMessage()), locale));
        }
        return resultMap;
    }

    public static Map<String, Object> paymentAuthorisation(DispatchContext ctx, Map<String, Object> context) {
        Debug.logInfo("SagePay - Entered paymentAuthorisation", MODULE);
        Debug.logInfo("SagePay paymentAuthorisation context : " + context, MODULE);

        Delegator delegator = ctx.getDelegator();
        Map<String, Object> resultMap = new HashMap<>();

        Map<String, String> props = buildSagePayProperties(context, delegator);

        String vendorTxCode = (String) context.get("vendorTxCode");
        String vpsTxId = (String) context.get("vpsTxId");
        String securityKey = (String) context.get("securityKey");
        String txAuthNo = (String) context.get("txAuthNo");
        String amount = (String) context.get("amount");
        Locale locale = (Locale) context.get("locale");

        HttpHost host = SagePayUtil.getHost(props);

        //start - authorization parameters
        Map<String, String> parameters = new HashMap<>();

        String vpsProtocol = props.get("protocolVersion");
        String vendor = props.get("vendor");
        String txType = props.get("authoriseTransType");

        parameters.put("VPSProtocol", vpsProtocol);
        parameters.put("TxType", txType);
        parameters.put("Vendor", vendor);
        parameters.put("VendorTxCode", vendorTxCode);
        parameters.put("VPSTxId", vpsTxId);
        parameters.put("SecurityKey", securityKey);
        parameters.put("TxAuthNo", txAuthNo);
        parameters.put("ReleaseAmount", amount);

        Debug.logInfo("authorization parameters -> " + parameters, MODULE);
        //end - authorization parameters

        try (CloseableHttpClient httpClient = SagePayUtil.getHttpClient()) {
            String successMessage = null;
            HttpPost httpPost = SagePayUtil.getHttpPost(props.get("authoriseUrl"), parameters);
            HttpResponse response = httpClient.execute(host, httpPost);

            Map<String, String> responseData = SagePayUtil.getResponseData(response);
            String status = responseData.get("Status");
            String statusDetail = responseData.get("StatusDetail");

            resultMap.put("status", status);
            resultMap.put("statusDetail", statusDetail);

            //start - payment refunded
            if ("OK".equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, "AccountingSagePayPaymentReleased", locale);
            }
            //end - payment refunded

            //start - refund request not formed properly or parameters missing
            if ("MALFORMED".equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, "AccountingSagePayPaymentReleaseRequestMalformed", locale);
            }
            //end - refund request not formed properly or parameters missing

            //start - invalid information passed in parameters
            if ("INVALID".equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, "AccountingSagePayPaymentInvalidInformationPassed", locale);
            }
            //end - invalid information passed in parameters

            //start - problem at Sagepay
            if ("ERROR".equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, "AccountingSagePayPaymentError", locale);
            }
            //end - problem at Sagepay

            resultMap.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            resultMap.put(ModelService.SUCCESS_MESSAGE, successMessage);

        } catch (UnsupportedEncodingException uee) {
            //exception in encoding parameters in httpPost
            Debug.logError(uee, "Error occurred in encoding parameters for HttpPost (" + uee.getMessage() + ")", MODULE);
            resultMap = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "AccountingSagePayErrorEncodingParameters",
                    UtilMisc.toMap("errorString", uee.getMessage()), locale));
        } catch (ClientProtocolException cpe) {
            //from httpClient execute
            Debug.logError(cpe, "Error occurred in HttpClient execute(" + cpe.getMessage() + ")", MODULE);
            resultMap = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "AccountingSagePayErrorHttpClientExecute",
                    UtilMisc.toMap("errorString", cpe.getMessage()), locale));
        } catch (IOException ioe) {
            //from httpClient execute or getResponsedata
            Debug.logError(ioe, "Error occurred in HttpClient execute or getting response (" + ioe.getMessage() + ")", MODULE);
            resultMap = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "AccountingSagePayErrorHttpClientExecuteOrGettingResponse",
                    UtilMisc.toMap("errorString", ioe.getMessage()), locale));
        }
        return resultMap;
    }

    public static Map<String, Object> paymentRelease(DispatchContext ctx, Map<String, Object> context) {
        Debug.logInfo("SagePay - Entered paymentRelease", MODULE);
        Debug.logInfo("SagePay paymentRelease context : " + context, MODULE);

        Delegator delegator = ctx.getDelegator();
        Map<String, Object> resultMap = new HashMap<>();

        Map<String, String> props = buildSagePayProperties(context, delegator);

        String vendorTxCode = (String) context.get("vendorTxCode");
        String vpsTxId = (String) context.get("vpsTxId");
        String securityKey = (String) context.get("securityKey");
        String txAuthNo = (String) context.get("txAuthNo");
        Locale locale = (Locale) context.get("locale");

        HttpHost host = SagePayUtil.getHost(props);

        //start - release parameters
        Map<String, String> parameters = new HashMap<>();

        String vpsProtocol = props.get("protocolVersion");
        String vendor = props.get("vendor");
        String txType = props.get("releaseTransType");

        parameters.put("VPSProtocol", vpsProtocol);
        parameters.put("TxType", txType);
        parameters.put("Vendor", vendor);
        parameters.put("VendorTxCode", vendorTxCode);
        parameters.put("VPSTxId", vpsTxId);
        parameters.put("SecurityKey", securityKey);
        parameters.put("TxAuthNo", txAuthNo);
        //end - release parameters

        try (CloseableHttpClient httpClient = SagePayUtil.getHttpClient()) {
            String successMessage = null;
            HttpPost httpPost = SagePayUtil.getHttpPost(props.get("releaseUrl"), parameters);
            HttpResponse response = httpClient.execute(host, httpPost);

            Map<String, String> responseData = SagePayUtil.getResponseData(response);

            String status = responseData.get("Status");
            String statusDetail = responseData.get("StatusDetail");

            resultMap.put("status", status);
            resultMap.put("statusDetail", statusDetail);

            //start - payment released
            if ("OK".equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, "AccountingSagePayPaymentReleased", locale);
            }
            //end - payment released

            //start - release request not formed properly or parameters missing
            if ("MALFORMED".equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, "AccountingSagePayPaymentReleaseRequestMalformed", locale);
            }
            //end - release request not formed properly or parameters missing

            //start - invalid information passed in parameters
            if ("INVALID".equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, "AccountingSagePayPaymentInvalidInformationPassed", locale);
            }
            //end - invalid information passed in parameters

            //start - problem at Sagepay
            if ("ERROR".equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, "AccountingSagePayPaymentError", locale);
            }
            //end - problem at Sagepay

            resultMap.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            resultMap.put(ModelService.SUCCESS_MESSAGE, successMessage);

        } catch (UnsupportedEncodingException uee) {
            //exception in encoding parameters in httpPost
            Debug.logError(uee, "Error occurred in encoding parameters for HttpPost (" + uee.getMessage() + ")", MODULE);
            resultMap = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "AccountingSagePayErrorEncodingParameters",
                    UtilMisc.toMap("errorString", uee.getMessage()), locale));
        } catch (ClientProtocolException cpe) {
            //from httpClient execute
            Debug.logError(cpe, "Error occurred in HttpClient execute(" + cpe.getMessage() + ")", MODULE);
            resultMap = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "AccountingSagePayErrorHttpClientExecute",
                    UtilMisc.toMap("errorString", cpe.getMessage()), locale));
        } catch (IOException ioe) {
            //from httpClient execute or getResponsedata
            Debug.logError(ioe, "Error occurred in HttpClient execute or getting response (" + ioe.getMessage() + ")", MODULE);
            resultMap = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "AccountingSagePayErrorHttpClientExecuteOrGettingResponse",
                    UtilMisc.toMap("errorString", ioe.getMessage()), locale));
        }
        return resultMap;
    }

    public static Map<String, Object> paymentVoid(DispatchContext ctx, Map<String, Object> context) {
        Debug.logInfo("SagePay - Entered paymentVoid", MODULE);
        Debug.logInfo("SagePay paymentVoid context : " + context, MODULE);

        Delegator delegator = ctx.getDelegator();
        Map<String, Object> resultMap = new HashMap<>();

        Map<String, String> props = buildSagePayProperties(context, delegator);

        String vendorTxCode = (String) context.get("vendorTxCode");
        String vpsTxId = (String) context.get("vpsTxId");
        String securityKey = (String) context.get("securityKey");
        String txAuthNo = (String) context.get("txAuthNo");
        Locale locale = (Locale) context.get("locale");

        HttpHost host = SagePayUtil.getHost(props);

        //start - void parameters
        Map<String, String> parameters = new HashMap<>();

        String vpsProtocol = props.get("protocolVersion");
        String vendor = props.get("vendor");

        parameters.put("VPSProtocol", vpsProtocol);
        parameters.put("TxType", "VOID");
        parameters.put("Vendor", vendor);
        parameters.put("VendorTxCode", vendorTxCode);
        parameters.put("VPSTxId", vpsTxId);
        parameters.put("SecurityKey", securityKey);
        parameters.put("TxAuthNo", txAuthNo);
        //end - void parameters

        try (CloseableHttpClient httpClient = SagePayUtil.getHttpClient()) {
            String successMessage = null;
            HttpPost httpPost = SagePayUtil.getHttpPost(props.get("voidUrl"), parameters);
            HttpResponse response = httpClient.execute(host, httpPost);
            Map<String, String> responseData = SagePayUtil.getResponseData(response);

            String status = responseData.get("Status");
            String statusDetail = responseData.get("StatusDetail");

            resultMap.put("status", status);
            resultMap.put("statusDetail", statusDetail);

            //start - payment void
            if ("OK".equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, "AccountingSagePayPaymentVoided", locale);
            }
            //end - payment void

            //start - void request not formed properly or parameters missing
            if ("MALFORMED".equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, "AccountingSagePayPaymentVoidRequestMalformed", locale);
            }
            //end - void request not formed properly or parameters missing

            //start - invalid information passed in parameters
            if ("INVALID".equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, "AccountingSagePayPaymentInvalidInformationPassed", locale);
            }
            //end - invalid information passed in parameters

            //start - problem at Sagepay
            if ("ERROR".equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, "AccountingSagePayPaymentError", locale);
            }
            //end - problem at Sagepay

            resultMap.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            resultMap.put(ModelService.SUCCESS_MESSAGE, successMessage);
        } catch (UnsupportedEncodingException uee) {
            //exception in encoding parameters in httpPost
            Debug.logError(uee, "Error occurred in encoding parameters for HttpPost (" + uee.getMessage() + ")", MODULE);
            resultMap = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "AccountingSagePayErrorEncodingParameters",
                    UtilMisc.toMap("errorString", uee.getMessage()), locale));
        } catch (ClientProtocolException cpe) {
            //from httpClient execute
            Debug.logError(cpe, "Error occurred in HttpClient execute(" + cpe.getMessage() + ")", MODULE);
            resultMap = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "AccountingSagePayErrorHttpClientExecute",
                    UtilMisc.toMap("errorString", cpe.getMessage()), locale));
        } catch (IOException ioe) {
            //from httpClient execute or getResponsedata
            Debug.logError(ioe, "Error occurred in HttpClient execute or getting response (" + ioe.getMessage() + ")", MODULE);
            resultMap = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "AccountingSagePayErrorHttpClientExecuteOrGettingResponse",
                    UtilMisc.toMap("errorString", ioe.getMessage()), locale));
        }
        return resultMap;
    }

    public static Map<String, Object> paymentRefund(DispatchContext ctx, Map<String, Object> context) {
        Debug.logInfo("SagePay - Entered paymentRefund", MODULE);
        Debug.logInfo("SagePay paymentRefund context : " + context, MODULE);

        Delegator delegator = ctx.getDelegator();
        Map<String, Object> resultMap = new HashMap<>();

        Map<String, String> props = buildSagePayProperties(context, delegator);

        String vendorTxCode = (String) context.get("vendorTxCode");
        String amount = (String) context.get("amount");
        String currency = (String) context.get("currency");
        String description = (String) context.get("description");

        String relatedVPSTxId = (String) context.get("relatedVPSTxId");
        String relatedVendorTxCode = (String) context.get("relatedVendorTxCode");
        String relatedSecurityKey = (String) context.get("relatedSecurityKey");
        String relatedTxAuthNo = (String) context.get("relatedTxAuthNo");
        Locale locale = (Locale) context.get("locale");

        HttpHost host = SagePayUtil.getHost(props);

        //start - refund parameters
        Map<String, String> parameters = new HashMap<>();

        String vpsProtocol = props.get("protocolVersion");
        String vendor = props.get("vendor");

        parameters.put("VPSProtocol", vpsProtocol);
        parameters.put("TxType", "REFUND");
        parameters.put("Vendor", vendor);
        parameters.put("VendorTxCode", vendorTxCode);
        parameters.put("Amount", amount);
        parameters.put("Currency", currency);
        parameters.put("Description", description);
        parameters.put("RelatedVPSTxId", relatedVPSTxId);
        parameters.put("RelatedVendorTxCode", relatedVendorTxCode);
        parameters.put("RelatedSecurityKey", relatedSecurityKey);
        parameters.put("RelatedTxAuthNo", relatedTxAuthNo);
        //end - refund parameters

        try (CloseableHttpClient httpClient = SagePayUtil.getHttpClient()) {
            String successMessage = null;
            HttpPost httpPost = SagePayUtil.getHttpPost(props.get("refundUrl"), parameters);
            HttpResponse response = httpClient.execute(host, httpPost);
            Map<String, String> responseData = SagePayUtil.getResponseData(response);

            Debug.logInfo("response data -> " + responseData, MODULE);

            String status = responseData.get("Status");
            String statusDetail = responseData.get("StatusDetail");

            resultMap.put("status", status);
            resultMap.put("statusDetail", statusDetail);

            //start - payment refunded
            if ("OK".equals(status)) {
                resultMap.put("vpsTxId", responseData.get("VPSTxId"));
                resultMap.put("txAuthNo", responseData.get("TxAuthNo"));
                successMessage = UtilProperties.getMessage(RESOURCE, "AccountingSagePayPaymentRefunded", locale);
            }
            //end - payment refunded

            //start - refund not authorized by the acquiring bank
            if ("NOTAUTHED".equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, "AccountingSagePayPaymentRefundNotAuthorized", locale);
            }
            //end - refund not authorized by the acquiring bank

            //start - refund request not formed properly or parameters missing
            if ("MALFORMED".equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, "AccountingSagePayPaymentRefundRequestMalformed", locale);
            }
            //end - refund request not formed properly or parameters missing

            //start - invalid information passed in parameters
            if ("INVALID".equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, "AccountingSagePayPaymentInvalidInformationPassed", locale);
            }
            //end - invalid information passed in parameters

            //start - problem at Sagepay
            if ("ERROR".equals(status)) {
                successMessage = UtilProperties.getMessage(RESOURCE, "AccountingSagePayPaymentError", locale);
            }
            //end - problem at Sagepay

            resultMap.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            resultMap.put(ModelService.SUCCESS_MESSAGE, successMessage);

        } catch (UnsupportedEncodingException uee) {
            //exception in encoding parameters in httpPost
            Debug.logError(uee, "Error occurred in encoding parameters for HttpPost (" + uee.getMessage() + ")", MODULE);
            resultMap = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "AccountingSagePayErrorEncodingParameters",
                    UtilMisc.toMap("errorString", uee.getMessage()), locale));
        } catch (ClientProtocolException cpe) {
            //from httpClient execute
            Debug.logError(cpe, "Error occurred in HttpClient execute(" + cpe.getMessage() + ")", MODULE);
            resultMap = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "AccountingSagePayErrorHttpClientExecute",
                    UtilMisc.toMap("errorString", cpe.getMessage()), locale));
        } catch (IOException ioe) {
            //from httpClient execute or getResponsedata
            Debug.logError(ioe, "Error occurred in HttpClient execute or getting response (" + ioe.getMessage() + ")", MODULE);
            resultMap = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "AccountingSagePayErrorHttpClientExecuteOrGettingResponse",
                    UtilMisc.toMap("errorString", ioe.getMessage()), locale));
        }

        return resultMap;
    }
}
