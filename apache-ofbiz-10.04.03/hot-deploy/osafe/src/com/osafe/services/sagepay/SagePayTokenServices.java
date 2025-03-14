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

package com.osafe.services.sagepay;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

public class SagePayTokenServices
{
    public static final String module = SagePayTokenServices.class.getName();

    private static Map<String, String> buildSagePayProperties(Map<String, Object> context, Delegator delegator) {

        Map<String, String> sagePayConfig = new HashMap<String, String>();

        String paymentGatewayConfigId = (String) context.get("paymentGatewayConfigId");

        if (UtilValidate.isNotEmpty(paymentGatewayConfigId)) {
            try {
                GenericValue sagePay = delegator.findOne("PaymentGatewaySagePayToken", UtilMisc.toMap("paymentGatewayConfigId", paymentGatewayConfigId), false);
                if (UtilValidate.isNotEmpty(sagePay)) {
                    Map<String, Object> tmp = sagePay.getAllFields();
                    Set<String> keys = tmp.keySet();
                    for (String key : keys) {
                        Object keyValue = tmp.get(key);
                        String value="";
                        if (UtilValidate.isNotEmpty(keyValue))
                        {
                        	value = keyValue.toString();
                        }
                        sagePayConfig.put(key, value);
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }

        Debug.logInfo("SagePay Token Configuration : " + sagePayConfig.toString(), module);
        return sagePayConfig;
    }

    public static Map<String, Object> paymentRegistration(DispatchContext ctx, Map<String, Object> context)
    {
        Debug.logInfo("SagePay Token -  Entered paymentRegistration", module);
        Debug.logInfo("SagePay Token - paymentRegistration context : " + context, module);

        Delegator delegator = ctx.getDelegator();
        Map<String, Object> resultMap = new HashMap<String, Object>();

        Map<String, String> props = buildSagePayProperties(context, delegator);

        String vendorTxCode = (String)context.get("vendorTxCode");
        String currency = (String) context.get("currency");
        String cardHolder = (String) context.get("cardHolder");
        String cardNumber = (String) context.get("cardNumber");
        String expiryDate = (String) context.get("expiryDate");
        String cv2 = (String) context.get("cv2");
        String cardType = (String) context.get("cardType");
        String paymentMethodId = (String) context.get("paymentMethodId");
        String contactMechId = (String) context.get("contactMechId");


        String startDate = (String) context.get("startDate");
        String issueNumber = (String) context.get("issueNumber");
        String clientIPAddress = (String) context.get("clientIPAddress");

        HttpClient httpClient = SagePayUtil.getHttpClient();
        HttpHost host = SagePayUtil.getHost(props);

        //start - authentication parameters
        Map<String, String> parameters = new HashMap<String, String>();

        String vpsProtocol = props.get("protocolVersion");
        String vendor = props.get("vendor");
        String txType = props.get("registrationTransType");

        //start - required parameters
        parameters.put("VPSProtocol", vpsProtocol);
        parameters.put("TxType", txType);
        parameters.put("Vendor", vendor);

        if (currency != null) { parameters.put("Currency", currency); } //GBP/USD
        if (cardHolder != null) { parameters.put("CardHolder", cardHolder); }
        if (cardNumber != null) { parameters.put("CardNumber", cardNumber); }
        if (expiryDate != null) { parameters.put("ExpiryDate", expiryDate); }
        if (cardType != null) { parameters.put("CardType", cardType); }


        //start - optional parameters
        if (cv2 != null) { parameters.put("CV2", cv2); }
        if (startDate != null) { parameters.put("StartDate", startDate); }
        if (issueNumber != null) { parameters.put("IssueNumber", issueNumber); }
        if (clientIPAddress != null) { parameters.put("ClientIPAddress", clientIPAddress); }
        
        //end - optional parameters
        //end - authentication parameters

        try {

            String successMessage = null;
            HttpPost httpPost = SagePayUtil.getHttpPost(props.get("registrationUrl"), parameters);
            HttpResponse response = httpClient.execute(host, httpPost);
            Map<String, String> responseData = SagePayUtil.getResponseData(response);

            String status = responseData.get("Status");
            String statusDetail = responseData.get("StatusDetail");

            resultMap.put("status", status);
            resultMap.put("statusDetail", statusDetail);

            //returning the below details back to the calling code, as it not returned back by the payment gateway
            resultMap.put("vendorTxCode", vendorTxCode);
            resultMap.put("transactionType", txType);
            resultMap.put("token", null);
            //start - transaction registered
            if ("OK".equals(status)) {
                resultMap.put("token", responseData.get("Token"));
                successMessage = "Payment Token registered";
            }
            //end - transaction authorized

            if ("MALFORMED".equals(status)) {
                //request not formed properly or parameters missing
            }

            if ("INVALID".equals(status)) {
                //invalid information in request
            }

            resultMap.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            resultMap.put(ModelService.SUCCESS_MESSAGE, successMessage);

        } catch(UnsupportedEncodingException uee) {
            //exception in encoding parameters in httpPost
            String errorMsg = "Error occured in encoding parameters for HttpPost (" + uee.getMessage() + ")";
            Debug.logError(uee, errorMsg, module);
            resultMap = ServiceUtil.returnError(errorMsg);
        } catch(ClientProtocolException cpe) {
            //from httpClient execute
            String errorMsg = "Error occured in HttpClient execute(" + cpe.getMessage() + ")";
            Debug.logError(cpe, errorMsg, module);
            resultMap = ServiceUtil.returnError(errorMsg);
        } catch(IOException ioe) {
            //from httpClient execute or getResponsedata
            String errorMsg = "Error occured in HttpClient execute or getting response (" + ioe.getMessage() + ")";
            Debug.logError(ioe, errorMsg, module);
            resultMap = ServiceUtil.returnError(errorMsg);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
        return resultMap;
    }
    
    public static Map<String, Object> paymentAuthentication(DispatchContext ctx, Map<String, Object> context)
    {
        Debug.logInfo("SagePay Token - Entered paymentAuthentication", module);
        Debug.logInfo("SagePay Token paymentAuthentication context : " + context, module);

        Delegator delegator = ctx.getDelegator();
        Map<String, Object> resultMap = new HashMap<String, Object>();

        Map<String, String> props = buildSagePayProperties(context, delegator);

        String vendorTxCode = (String)context.get("vendorTxCode");
        String token = (String) context.get("token");
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

        HttpClient httpClient = SagePayUtil.getHttpClient();
        HttpHost host = SagePayUtil.getHost(props);

        //start - authentication parameters
        Map<String, String> parameters = new HashMap<String, String>();

        String vpsProtocol = props.get("protocolVersion");
        String vendor = props.get("vendor");
        String txType = props.get("authenticationTransType");
        String storeToken = props.get("storeToken");
        if (UtilValidate.isEmpty(storeToken))
        {
        	storeToken="0";
        }
        	

        //start - required parameters
        parameters.put("VPSProtocol", vpsProtocol);
        parameters.put("TxType", txType);
        parameters.put("Vendor", vendor);
//        parameters.put("StoreToken", storeToken);

        if (vendorTxCode != null) { parameters.put("VendorTxCode", vendorTxCode); }
        if (amount != null) { parameters.put("Amount", amount); }
        if (currency != null) { parameters.put("Currency", currency); } //GBP/USD
        if (description != null) { parameters.put("Description", description); }
        if (token != null) { parameters.put("Token", token); }
        if (storeToken != null) { parameters.put("StoreToken", storeToken); }

        //start - billing details
        if (billingSurname != null) { parameters.put("BillingSurname", billingSurname); }
        if (billingFirstnames != null) { parameters.put("BillingFirstnames", billingFirstnames); }
        if (billingAddress != null) { parameters.put("BillingAddress1", billingAddress); }
        if (billingAddress2 != null) { parameters.put("BillingAddress2", billingAddress2); }
        if (billingCity != null) { parameters.put("BillingCity", billingCity); }
        if (billingPostCode != null) { parameters.put("BillingPostCode", billingPostCode); }
        if (billingCountry != null) { parameters.put("BillingCountry", billingCountry); }
//        if (UtilValidate.isNotEmpty(billingState)) {parameters.put("BillingState", billingState);}
        if (billingPhone != null) { parameters.put("BillingPhone", billingPhone); }
        //end - billing details

        //start - delivery details
        if (isBillingSameAsDelivery != null && isBillingSameAsDelivery) {
            if (billingSurname != null) { parameters.put("DeliverySurname", billingSurname); }
            if (billingFirstnames != null) { parameters.put("DeliveryFirstnames", billingFirstnames); }
            if (billingAddress != null) { parameters.put("DeliveryAddress1", billingAddress); }
            if (billingAddress2 != null) { parameters.put("DeliveryAddress2", billingAddress2); }
            if (billingCity != null) { parameters.put("DeliveryCity", billingCity); }
            if (billingPostCode != null) { parameters.put("DeliveryPostCode", billingPostCode); }
            if (billingCountry != null) { parameters.put("DeliveryCountry", billingCountry); }
            if (billingState != null) { parameters.put("DeliveryState", billingState); }
            if (billingPhone != null) { parameters.put("DeliveryPhone", billingPhone); }
        } else {
            if (deliverySurname != null) { parameters.put("DeliverySurname", deliverySurname); }
            if (deliveryFirstnames != null) { parameters.put("DeliveryFirstnames", deliveryFirstnames); }
            if (deliveryAddress != null) { parameters.put("DeliveryAddress1", deliveryAddress); }
            if (deliveryAddress2 != null) { parameters.put("DeliveryAddress2", deliveryAddress2); }
            if (deliveryCity != null) { parameters.put("DeliveryCity", deliveryCity); }
            if (deliveryPostCode != null) { parameters.put("DeliveryPostCode", deliveryPostCode); }
            if (deliveryCountry != null) { parameters.put("DeliveryCountry", deliveryCountry); }
//            if (UtilValidate.isNotEmpty(deliveryState)) { parameters.put("DeliveryState", deliveryState); }
            if (deliveryPhone != null) {parameters.put("DeliveryPhone", deliveryPhone); }
        }
        //end - delivery details
        //end - required parameters

        //start - optional parameters
        if (cv2 != null) { parameters.put("CV2", cv2); }
        if (startDate != null) { parameters.put("StartDate", startDate); }
        if (issueNumber != null) { parameters.put("IssueNumber", issueNumber); }
        if (basket != null) { parameters.put("Basket", basket); }
        if (clientIPAddress != null) { parameters.put("ClientIPAddress", clientIPAddress); }
        
        //end - optional parameters
        //end - authentication parameters

        try {

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

        } catch(UnsupportedEncodingException uee) {
            //exception in encoding parameters in httpPost
            String errorMsg = "Error occured in encoding parameters for HttpPost (" + uee.getMessage() + ")";
            Debug.logError(uee, errorMsg, module);
            resultMap = ServiceUtil.returnError(errorMsg);
        } catch(ClientProtocolException cpe) {
            //from httpClient execute
            String errorMsg = "Error occured in HttpClient execute(" + cpe.getMessage() + ")";
            Debug.logError(cpe, errorMsg, module);
            resultMap = ServiceUtil.returnError(errorMsg);
        } catch(IOException ioe) {
            //from httpClient execute or getResponsedata
            String errorMsg = "Error occured in HttpClient execute or getting response (" + ioe.getMessage() + ")";
            Debug.logError(ioe, errorMsg, module);
            resultMap = ServiceUtil.returnError(errorMsg);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
        return resultMap;
    }

    public static Map<String, Object> paymentAuthorisation(DispatchContext ctx, Map<String, Object> context)
    {
        Debug.logInfo("SagePay Token -  Entered paymentAuthorisation", module);
        Debug.logInfo("SagePay Token - paymentAuthorisation context : " + context, module);

        Delegator delegator = ctx.getDelegator();
        Map<String, Object> resultMap = new HashMap<String, Object>();

        Map<String, String> props = buildSagePayProperties(context, delegator);

        String vendorTxCode = (String)context.get("vendorTxCode");
        String vpsTxId = (String) context.get("vpsTxId");
        String securityKey = (String) context.get("securityKey");
        String txAuthNo = (String) context.get("txAuthNo");
        String amount = (String) context.get("amount");
        String token = (String) context.get("token");
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

        HttpClient httpClient = SagePayUtil.getHttpClient();
        HttpHost host = SagePayUtil.getHost(props);

        //start - authorization parameters
        Map<String, String> parameters = new HashMap<String, String>();

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
        parameters.put("Amount", amount);
        parameters.put("Token", token);
        parameters.put("Currency", currency);
        
        if (description != null) { parameters.put("Description", description); }
        if (billingSurname != null) { parameters.put("BillingSurname", billingSurname); }
        if (billingFirstnames != null) { parameters.put("BillingFirstnames", billingFirstnames); }
        if (billingAddress != null) { parameters.put("BillingAddress1", billingAddress); }
        if (billingAddress2 != null) { parameters.put("BillingAddress2", billingAddress2); }
        if (billingCity != null) { parameters.put("BillingCity", billingCity); }
        if (billingPostCode != null) { parameters.put("BillingPostCode", billingPostCode); }
        if (billingCountry != null) { parameters.put("BillingCountry", billingCountry); }
        if (billingState != null) { parameters.put("BillingState", billingState); }
        if (billingPhone != null) { parameters.put("BillingPhone", billingPhone); }

        Debug.logInfo("authorization parameters -> " + parameters, module);
        //end - authorization parameters

        try {
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
                successMessage = "Payment Authorised";
            }
            //end - payment refunded

            //start - refund request not formed properly or parameters missing
            if ("MALFORMED".equals(status)) {
                successMessage = "Released request not formed properly or parameters missing";
            }
            //end - refund request not formed properly or parameters missing

            //start - invalid information passed in parameters
            if ("INVALID".equals(status)) {
                successMessage = "Invalid information passed in parameters";
            }
            //end - invalid information passed in parameters

            //start - problem at Sagepay
            if ("ERROR".equals(status)) {
                successMessage = "Problem at SagePay";
            }
            //end - problem at Sagepay

            resultMap.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            resultMap.put(ModelService.SUCCESS_MESSAGE, successMessage);

        } catch(UnsupportedEncodingException uee) {
            //exception in encoding parameters in httpPost
            String errorMsg = "Error occured in encoding parameters for HttpPost (" + uee.getMessage() + ")";
            Debug.logError(uee, errorMsg, module);
            resultMap = ServiceUtil.returnError(errorMsg);
        } catch(ClientProtocolException cpe) {
            //from httpClient execute
            String errorMsg = "Error occured in HttpClient execute(" + cpe.getMessage() + ")";
            Debug.logError(cpe, errorMsg, module);
            resultMap = ServiceUtil.returnError(errorMsg);
        } catch(IOException ioe) {
            //from httpClient execute or getResponsedata
            String errorMsg = "Error occured in HttpClient execute or getting response (" + ioe.getMessage() + ")";
            Debug.logError(ioe, errorMsg, module);
            resultMap = ServiceUtil.returnError(errorMsg);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
        return resultMap;
    }

    public static Map<String, Object> paymentRelease(DispatchContext ctx, Map<String, Object> context)
    {
        Debug.logInfo("SagePay Token -  Entered paymentRelease", module);
        Debug.logInfo("SagePay Token - paymentRelease context : " + context, module);

        Delegator delegator = ctx.getDelegator();
        Map<String, Object> resultMap = new HashMap<String, Object>();

        Map<String, String> props = buildSagePayProperties(context, delegator);

        String vendorTxCode = (String)context.get("vendorTxCode");
        String vpsTxId = (String) context.get("vpsTxId");
        String securityKey = (String) context.get("securityKey");
        String txAuthNo = (String) context.get("txAuthNo");
        String amount = (String) context.get("amount");
        String token = (String) context.get("token");
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


        HttpClient httpClient = SagePayUtil.getHttpClient();
        HttpHost host = SagePayUtil.getHost(props);

        //start - release parameters
        Map<String, String> parameters = new HashMap<String, String>();

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
        parameters.put("Amount", amount);
        parameters.put("Description", description);
        parameters.put("Token", token);
        parameters.put("Currency", currency);
        
        if (description != null) { parameters.put("Description", description); }
        if (billingSurname != null) { parameters.put("BillingSurname", billingSurname); }
        if (billingFirstnames != null) { parameters.put("BillingFirstnames", billingFirstnames); }
        if (billingAddress != null) { parameters.put("BillingAddress1", billingAddress); }
        if (billingAddress2 != null) { parameters.put("BillingAddress2", billingAddress2); }
        if (billingCity != null) { parameters.put("BillingCity", billingCity); }
        if (billingPostCode != null) { parameters.put("BillingPostCode", billingPostCode); }
        if (billingCountry != null) { parameters.put("BillingCountry", billingCountry); }
        if (billingState != null) { parameters.put("BillingState", billingState); }
        if (billingPhone != null) { parameters.put("BillingPhone", billingPhone); }
        //end - release parameters

        try {

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
                successMessage = "Payment Released";
            }
            //end - payment released

            //start - release request not formed properly or parameters missing
            if ("MALFORMED".equals(status)) {
                successMessage = "Release request not formed properly or parameters missing";
            }
            //end - release request not formed properly or parameters missing

            //start - invalid information passed in parameters
            if ("INVALID".equals(status)) {
                successMessage = "Invalid information passed in parameters";
            }
            //end - invalid information passed in parameters

            //start - problem at Sagepay
            if ("ERROR".equals(status)) {
                successMessage = "Problem at SagePay";
            }
            //end - problem at Sagepay

            resultMap.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            resultMap.put(ModelService.SUCCESS_MESSAGE, successMessage);

        }  catch(UnsupportedEncodingException uee) {
            //exception in encoding parameters in httpPost
            String errorMsg = "Error occured in encoding parameters for HttpPost (" + uee.getMessage() + ")";
            Debug.logError(uee, errorMsg, module);
            resultMap = ServiceUtil.returnError(errorMsg);
        } catch(ClientProtocolException cpe) {
            //from httpClient execute
            String errorMsg = "Error occured in HttpClient execute(" + cpe.getMessage() + ")";
            Debug.logError(cpe, errorMsg, module);
            resultMap = ServiceUtil.returnError(errorMsg);
        } catch(IOException ioe) {
            //from httpClient execute or getResponsedata
            String errorMsg = "Error occured in HttpClient execute or getting response (" + ioe.getMessage() + ")";
            Debug.logError(ioe, errorMsg, module);
            resultMap = ServiceUtil.returnError(errorMsg);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
        return resultMap;
    }

    public static Map<String, Object> paymentVoid(DispatchContext ctx, Map<String, Object> context)
    {
        Debug.logInfo("SagePay Token -  Entered paymentVoid", module);
        Debug.logInfo("SagePay Token - paymentVoid context : " + context, module);

        Delegator delegator = ctx.getDelegator();
        Map<String, Object> resultMap = new HashMap<String, Object>();

        Map<String, String> props = buildSagePayProperties(context, delegator);

        String vendorTxCode = (String)context.get("vendorTxCode");
        String vpsTxId = (String) context.get("vpsTxId");
        String securityKey = (String) context.get("securityKey");
        String txAuthNo = (String) context.get("txAuthNo");
        String amount = (String) context.get("amount");
        String token = (String) context.get("token");
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

        HttpClient httpClient = SagePayUtil.getHttpClient();
        HttpHost host = SagePayUtil.getHost(props);

        //start - void parameters
        Map<String, String> parameters = new HashMap<String, String>();

        String vpsProtocol = props.get("protocolVersion");
        String vendor = props.get("vendor");
        String txType = props.get("voidTransType");

        parameters.put("VPSProtocol", vpsProtocol);
        parameters.put("TxType", txType);
        parameters.put("Vendor", vendor);
        parameters.put("VendorTxCode", vendorTxCode);
        parameters.put("VPSTxId", vpsTxId);
        parameters.put("SecurityKey", securityKey);
        parameters.put("TxAuthNo", txAuthNo);
        parameters.put("Amount", amount);
        parameters.put("Description", description);
        parameters.put("Token", token);
        parameters.put("Currency", currency);
        
        if (description != null) { parameters.put("Description", description); }
        if (billingSurname != null) { parameters.put("BillingSurname", billingSurname); }
        if (billingFirstnames != null) { parameters.put("BillingFirstnames", billingFirstnames); }
        if (billingAddress != null) { parameters.put("BillingAddress1", billingAddress); }
        if (billingAddress2 != null) { parameters.put("BillingAddress2", billingAddress2); }
        if (billingCity != null) { parameters.put("BillingCity", billingCity); }
        if (billingPostCode != null) { parameters.put("BillingPostCode", billingPostCode); }
        if (billingCountry != null) { parameters.put("BillingCountry", billingCountry); }
        if (billingState != null) { parameters.put("BillingState", billingState); }
        if (billingPhone != null) { parameters.put("BillingPhone", billingPhone); }
        //end - void parameters

        try {
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
                successMessage = "Payment Voided";
            }
            //end - payment void

            //start - void request not formed properly or parameters missing
            if ("MALFORMED".equals(status)) {
                successMessage = "Void request not formed properly or parameters missing";
            }
            //end - void request not formed properly or parameters missing

            //start - invalid information passed in parameters
            if ("INVALID".equals(status)) {
                successMessage = "Invalid information passed in parameters";
            }
            //end - invalid information passed in parameters

            //start - problem at Sagepay
            if ("ERROR".equals(status)) {
                successMessage = "Problem at SagePay";
            }
            //end - problem at Sagepay

            resultMap.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            resultMap.put(ModelService.SUCCESS_MESSAGE, successMessage);

        }  catch(UnsupportedEncodingException uee) {
            //exception in encoding parameters in httpPost
            String errorMsg = "Error occured in encoding parameters for HttpPost (" + uee.getMessage() + ")";
            Debug.logError(uee, errorMsg, module);
            resultMap = ServiceUtil.returnError(errorMsg);
        } catch(ClientProtocolException cpe) {
            //from httpClient execute
            String errorMsg = "Error occured in HttpClient execute(" + cpe.getMessage() + ")";
            Debug.logError(cpe, errorMsg, module);
            resultMap = ServiceUtil.returnError(errorMsg);
        } catch(IOException ioe) {
            //from httpClient execute or getResponsedata
            String errorMsg = "Error occured in HttpClient execute or getting response (" + ioe.getMessage() + ")";
            Debug.logError(ioe, errorMsg, module);
            resultMap = ServiceUtil.returnError(errorMsg);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
        return resultMap;
    }

    public static Map<String, Object> paymentRefund(DispatchContext ctx, Map<String, Object> context)
    {
        Debug.logInfo("SagePay Token -  Entered paymentRefund", module);
        Debug.logInfo("SagePay Token - paymentRefund context : " + context, module);

        Delegator delegator = ctx.getDelegator();
        Map<String, Object> resultMap = new HashMap<String, Object>();

        Map<String, String> props = buildSagePayProperties(context, delegator);

        String vendorTxCode = (String)context.get("vendorTxCode");
        String relatedVPSTxId = (String) context.get("relatedVPSTxId");
        String relatedVendorTxCode = (String) context.get("relatedVendorTxCode");
        String relatedSecurityKey = (String) context.get("relatedSecurityKey");
        String relatedTxAuthNo = (String) context.get("relatedTxAuthNo");

        String amount = (String) context.get("amount");
        String token = (String) context.get("token");
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

        HttpClient httpClient = SagePayUtil.getHttpClient();
        HttpHost host = SagePayUtil.getHost(props);

        //start - refund parameters
        Map<String, String> parameters = new HashMap<String, String>();

        String vpsProtocol = props.get("protocolVersion");
        String vendor = props.get("vendor");
        String txType = props.get("refundTransType");

        parameters.put("VPSProtocol", vpsProtocol);
        parameters.put("TxType", txType);
        parameters.put("Vendor", vendor);
        parameters.put("VendorTxCode", vendorTxCode);
        parameters.put("Amount", amount);
        parameters.put("Description", description);
        parameters.put("RelatedVPSTxId", relatedVPSTxId);
        parameters.put("RelatedVendorTxCode", relatedVendorTxCode);
        parameters.put("RelatedSecurityKey", relatedSecurityKey);
        parameters.put("RelatedTxAuthNo", relatedTxAuthNo);
        parameters.put("Token", token);
        parameters.put("Currency", currency);
        
        if (description != null) { parameters.put("Description", description); }
        if (billingSurname != null) { parameters.put("BillingSurname", billingSurname); }
        if (billingFirstnames != null) { parameters.put("BillingFirstnames", billingFirstnames); }
        if (billingAddress != null) { parameters.put("BillingAddress1", billingAddress); }
        if (billingAddress2 != null) { parameters.put("BillingAddress2", billingAddress2); }
        if (billingCity != null) { parameters.put("BillingCity", billingCity); }
        if (billingPostCode != null) { parameters.put("BillingPostCode", billingPostCode); }
        if (billingCountry != null) { parameters.put("BillingCountry", billingCountry); }
        if (billingState != null) { parameters.put("BillingState", billingState); }
        if (billingPhone != null) { parameters.put("BillingPhone", billingPhone); }

        //end - refund parameters

        try {
            String successMessage = null;

            HttpPost httpPost = SagePayUtil.getHttpPost(props.get("refundUrl"), parameters);
            HttpResponse response = httpClient.execute(host, httpPost);
            Map<String, String> responseData = SagePayUtil.getResponseData(response);

            Debug.logInfo("response data -> " + responseData, module);

            String status = responseData.get("Status");
            String statusDetail = responseData.get("StatusDetail");

            resultMap.put("status", status);
            resultMap.put("statusDetail", statusDetail);

            //start - payment refunded
            if ("OK".equals(status)) {
                resultMap.put("vpsTxId", responseData.get("VPSTxId"));
                resultMap.put("txAuthNo", responseData.get("TxAuthNo"));
                successMessage = "Payment Refunded";
            }
            //end - payment refunded

            //start - refund not authorized by the acquiring bank
            if ("NOTAUTHED".equals(status)) {
                successMessage = "Refund not authorized by the acquiring bank";
            }
            //end - refund not authorized by the acquiring bank

            //start - refund request not formed properly or parameters missing
            if ("MALFORMED".equals(status)) {
                successMessage = "Refund request not formed properly or parameters missing";
            }
            //end - refund request not formed properly or parameters missing

            //start - invalid information passed in parameters
            if ("INVALID".equals(status)) {
                successMessage = "Invalid information passed in parameters";
            }
            //end - invalid information passed in parameters

            //start - problem at Sagepay
            if ("ERROR".equals(status)) {
                successMessage = "Problem at SagePay";
            }
            //end - problem at Sagepay

            resultMap.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
            resultMap.put(ModelService.SUCCESS_MESSAGE, successMessage);

        }  catch(UnsupportedEncodingException uee) {
            //exception in encoding parameters in httpPost
            String errorMsg = "Error occured in encoding parameters for HttpPost (" + uee.getMessage() + ")";
            Debug.logError(uee, errorMsg, module);
            resultMap = ServiceUtil.returnError(errorMsg);
        } catch(ClientProtocolException cpe) {
            //from httpClient execute
            String errorMsg = "Error occured in HttpClient execute(" + cpe.getMessage() + ")";
            Debug.logError(cpe, errorMsg, module);
            resultMap = ServiceUtil.returnError(errorMsg);
        } catch(IOException ioe) {
            //from httpClient execute or getResponsedata
            String errorMsg = "Error occured in HttpClient execute or getting response (" + ioe.getMessage() + ")";
            Debug.logError(ioe, errorMsg, module);
            resultMap = ServiceUtil.returnError(errorMsg);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }

        return resultMap;
    }
}
