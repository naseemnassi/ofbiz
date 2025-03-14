/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

package com.osafe.services;

import javolution.util.FastList;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.*;
import org.ofbiz.entity.model.DynamicViewEntity;
import org.ofbiz.entity.model.ModelKeyMap;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * OrderLookupServices
 */
public class OrderLookupServices {

    public static final String module = OrderLookupServices.class.getName();

    public static Map findOrders(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Integer viewIndex = (Integer) context.get("viewIndex");
        Integer viewSize = (Integer) context.get("viewSize");
        String showAll = (String) context.get("showAll");
        String useEntryDate = (String) context.get("useEntryDate");
        if (showAll == null) {
            showAll = "N";
        }

        // list of fields to select (initial list)
        List fieldsToSelect = FastList.newInstance();
        fieldsToSelect.add("orderId");
        fieldsToSelect.add("orderName");
        fieldsToSelect.add("statusId");
        fieldsToSelect.add("orderTypeId");
        fieldsToSelect.add("orderDate");
        fieldsToSelect.add("currencyUom");
        fieldsToSelect.add("grandTotal");
        fieldsToSelect.add("remainingSubTotal");

        // sorting by order date newest first
        List orderBy = UtilMisc.toList("-orderDate", "-orderId");

        // list to hold the parameters
        List paramList = FastList.newInstance();

        // list of conditions
        List conditions = FastList.newInstance();

        // check security flag for purchase orders
        boolean canViewPo = security.hasEntityPermission("ORDERMGR", "_PURCHASE_VIEW", userLogin);
        if (!canViewPo) {
            conditions.add(EntityCondition.makeCondition("orderTypeId", EntityOperator.NOT_EQUAL, "PURCHASE_ORDER"));
        }

        // dynamic view entity
        DynamicViewEntity dve = new DynamicViewEntity();
        dve.addMemberEntity("OH", "OrderHeader");
        dve.addAliasAll("OH", ""); // no prefix
        dve.addRelation("one-nofk", "", "OrderType", UtilMisc.toList(new ModelKeyMap("orderTypeId", "orderTypeId")));
        dve.addRelation("one-nofk", "", "StatusItem", UtilMisc.toList(new ModelKeyMap("statusId", "statusId")));

        // start the lookup
        String orderId = (String) context.get("orderId");
        if (UtilValidate.isNotEmpty(orderId)) {
            paramList.add("orderId=" + orderId);
            conditions.add(makeExpr("orderId", orderId, false, true));
        }

        // the base order header fields
        List orderTypeList = (List) context.get("orderTypeId");
        if (orderTypeList != null) {
            Iterator i = orderTypeList.iterator();
            List orExprs = FastList.newInstance();
            while (i.hasNext()) {
                String orderTypeId = (String) i.next();
                paramList.add("orderTypeId=" + orderTypeId);

                if (!"PURCHASE_ORDER".equals(orderTypeId) || ("PURCHASE_ORDER".equals(orderTypeId) && canViewPo)) {
                    orExprs.add(EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, orderTypeId));
                }
            }
            conditions.add(EntityCondition.makeCondition(orExprs, EntityOperator.OR));
        }

        String orderName = (String) context.get("orderName");
        if (UtilValidate.isNotEmpty(orderName)) {
            paramList.add("orderName=" + orderName);
            conditions.add(makeExpr("orderName", orderName, true));
        }

        List orderStatusList = (List) context.get("orderStatusId");
        if (orderStatusList != null) {
            Iterator i = orderStatusList.iterator();
            List orExprs = FastList.newInstance();
            while (i.hasNext()) {
                String orderStatusId = (String) i.next();
                paramList.add("orderStatusId=" + orderStatusId);
                if ("PENDING".equals(orderStatusId)) {
                    List pendExprs = FastList.newInstance();
                    pendExprs.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ORDER_CREATED"));
                    pendExprs.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ORDER_PROCESSING"));
                    pendExprs.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ORDER_APPROVED"));
                    orExprs.add(EntityCondition.makeCondition(pendExprs, EntityOperator.OR));
                } else {
                    orExprs.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, orderStatusId));
                }
            }
            conditions.add(EntityCondition.makeCondition(orExprs, EntityOperator.OR));
        }

        List productStoreList = (List) context.get("productStoreId");
        if (productStoreList != null) {
            Iterator i = productStoreList.iterator();
            List orExprs = FastList.newInstance();
            while (i.hasNext()) {
                String productStoreId = (String) i.next();
                paramList.add("productStoreId=" + productStoreId);
                orExprs.add(EntityCondition.makeCondition("productStoreId", EntityOperator.EQUALS, productStoreId));
            }
            conditions.add(EntityCondition.makeCondition(orExprs, EntityOperator.OR));
        }

        List webSiteList = (List) context.get("orderWebSiteId");
        if (webSiteList != null) {
            Iterator i = webSiteList.iterator();
            List orExprs = FastList.newInstance();
            while (i.hasNext()) {
                String webSiteId = (String) i.next();
                paramList.add("webSiteId=" + webSiteId);
                orExprs.add(EntityCondition.makeCondition("webSiteId", EntityOperator.EQUALS, webSiteId));
            }
            conditions.add(EntityCondition.makeCondition(orExprs, EntityOperator.OR));
        }

        List saleChannelList = (List) context.get("salesChannelEnumId");
        if (saleChannelList != null) {
            Iterator i = saleChannelList.iterator();
            List orExprs = FastList.newInstance();
            while (i.hasNext()) {
                String salesChannelEnumId = (String) i.next();
                paramList.add("salesChannelEnumId=" + salesChannelEnumId);
                orExprs.add(EntityCondition.makeCondition("salesChannelEnumId", EntityOperator.EQUALS, salesChannelEnumId));
            }
            conditions.add(EntityCondition.makeCondition(orExprs, EntityOperator.OR));
        }

        String createdBy = (String) context.get("createdBy");
        if (UtilValidate.isNotEmpty(createdBy)) {
            paramList.add("createdBy=" + createdBy);
            conditions.add(makeExpr("createdBy", createdBy));
        }

        String terminalId = (String) context.get("terminalId");
        if (UtilValidate.isNotEmpty(terminalId)) {
            paramList.add("terminalId=" + terminalId);
            conditions.add(makeExpr("terminalId", terminalId));
        }

        String transactionId = (String) context.get("transactionId");
        if (UtilValidate.isNotEmpty(transactionId)) {
            paramList.add("transactionId=" + transactionId);
            conditions.add(makeExpr("transactionId", transactionId));
        }

        String externalId = (String) context.get("externalId");
        if (UtilValidate.isNotEmpty(externalId)) {
            paramList.add("externalId=" + externalId);
            conditions.add(makeExpr("externalId", externalId));
        }

        String internalCode = (String) context.get("internalCode");
        if (UtilValidate.isNotEmpty(internalCode)) {
            paramList.add("internalCode=" + internalCode);
            conditions.add(makeExpr("internalCode", internalCode));
        }

        String dateField = "Y".equals(useEntryDate) ? "entryDate" : "orderDate";
        String minDate = (String) context.get("minDate");
        if (UtilValidate.isNotEmpty(minDate) && minDate.length() >= 8) {
            minDate = minDate.trim();
            if (minDate.length() < 14) minDate = minDate + " " + "00:00:00.000";
            paramList.add("minDate=" + minDate);

            try {
                Object converted = ObjectType.simpleTypeConvert(minDate, "Timestamp", null, null);
                if (converted != null) {
                    converted = UtilDateTime.getDayStart((Timestamp)converted);
                    conditions.add(EntityCondition.makeCondition(dateField, EntityOperator.GREATER_THAN_EQUAL_TO, converted));
                }
            } catch (GeneralException e) {
                Debug.logWarning(e.getMessage(), module);
            }
        }

        String maxDate = (String) context.get("maxDate");
        if (UtilValidate.isNotEmpty(maxDate) && maxDate.length() >= 8) {
            maxDate = maxDate.trim();
            if (maxDate.length() < 14) maxDate = maxDate + " " + "23:59:59.999";
            paramList.add("maxDate=" + maxDate);

            try {
                Object converted = ObjectType.simpleTypeConvert(maxDate, "Timestamp", null, null);
                if (converted != null) {
                    converted = UtilDateTime.getDayEnd((Timestamp)converted);
                    conditions.add(EntityCondition.makeCondition("orderDate", EntityOperator.LESS_THAN_EQUAL_TO, converted));
                }
            } catch (GeneralException e) {
                Debug.logWarning(e.getMessage(), module);
            }
        }

/*        //Download Status
        String isDownloaded= (String) context.get("isDownloaded");
        if (UtilValidate.isNotEmpty(isDownloaded)) {

            if ("Y".equals(isDownloaded))
            {
                paramList.add("isDownloaded=" + isDownloaded);
                conditions.add(makeExpr("isDownloaded", isDownloaded));

            }
            else
            {
                conditions.add(EntityCondition.makeCondition("isDownloaded", EntityOperator.EQUALS,null));

            }

        }*/

        String minDownloadDate = (String) context.get("minDownloadDate");
        if (UtilValidate.isNotEmpty(minDownloadDate) && minDownloadDate.length() >= 8) {
            minDownloadDate = minDownloadDate.trim();
            if (minDownloadDate.length() < 14) minDownloadDate = minDownloadDate + " " + "00:00:00.000";
            paramList.add("minDownloadDate=" + minDownloadDate);

            try {
                Object converted = ObjectType.simpleTypeConvert(minDownloadDate, "Timestamp", null, null);
                if (converted != null) {
                    converted = UtilDateTime.getDayStart((Timestamp)converted);
                    conditions.add(EntityCondition.makeCondition("downloadDate", EntityOperator.GREATER_THAN_EQUAL_TO, converted));
                }
            } catch (GeneralException e) {
                Debug.logWarning(e.getMessage(), module);
            }
        }

        String maxDownloadDate = (String) context.get("maxDownloadDate");
        if (UtilValidate.isNotEmpty(maxDownloadDate) && maxDownloadDate.length() >= 8) {
            maxDownloadDate = maxDownloadDate.trim();
            if (maxDownloadDate.length() < 14) maxDownloadDate = maxDownloadDate + " " + "23:59:59.999";
            paramList.add("maxDownloadDate=" + maxDownloadDate);

            try {
                Object converted = ObjectType.simpleTypeConvert(maxDate, "Timestamp", null, null);
                if (converted != null) {
                    converted = UtilDateTime.getDayEnd((Timestamp)converted);
                    conditions.add(EntityCondition.makeCondition("downloadDate", EntityOperator.LESS_THAN_EQUAL_TO, converted));
                }
            } catch (GeneralException e) {
                Debug.logWarning(e.getMessage(), module);
            }
        }


        // party (role) fields
        String userLoginId = (String) context.get("userLoginId");
        String partyId = (String) context.get("partyId");
        List roleTypeList = (List) context.get("roleTypeId");

        if (UtilValidate.isNotEmpty(userLoginId) && UtilValidate.isEmpty(partyId)) {
            GenericValue ul = null;
            try {
                ul = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", userLoginId));
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), module);
            }
            if (ul != null) {
                partyId = ul.getString("partyId");
            }
        }

        String isViewed = (String) context.get("isViewed");
        if (UtilValidate.isNotEmpty(isViewed)) {
            paramList.add("isViewed=" + isViewed);
            conditions.add(makeExpr("isViewed", isViewed));
        }

        //Order adjustment with promotions
        String productPromoCodeId = (String) context.get("productPromoCodeId");
        if (UtilValidate.isNotEmpty(productPromoCodeId)) {
            dve.addMemberEntity("OPPC", "OrderProductPromoCode");
            dve.addAlias("OPPC", "productPromoCodeId");
            dve.addViewLink("OH", "OPPC", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("orderId", "orderId")));
        }

        // Order attirbute for downloadable stuff
        String attrName = (String) context.get("attrName");
        String attrValue = (String) context.get("attrValue");
        String isDownloaded = (String) context.get("isDownloaded");
        
        if ((UtilValidate.isNotEmpty(attrName) && UtilValidate.isNotEmpty(attrValue)) || UtilValidate.isNotEmpty(isDownloaded)) {
            dve.addMemberEntity("OA", "OrderAttribute");
            dve.addAlias("OA", "attrName");
            dve.addAlias("OA", "attrValue");
            dve.addViewLink("OH", "OA", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("orderId", "orderId")));
        }
        //Download Status
        if (UtilValidate.isNotEmpty(attrName) && UtilValidate.isNotEmpty(attrValue)) 
        {

            List attrExprs = FastList.newInstance();
        	paramList.add("attrName=" + attrName);
        	paramList.add("attrValue=" + attrValue);
            attrExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("attrName"), EntityOperator.LIKE, EntityFunction.UPPER("%"+attrName+"%")));
            attrExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("attrValue"), EntityOperator.LIKE, EntityFunction.UPPER(attrValue)));
        	conditions.add(EntityCondition.makeCondition(attrExprs, EntityOperator.AND));
        }
        
        if (UtilValidate.isNotEmpty(isDownloaded)) 
        {

            List attrExprs = FastList.newInstance();
            paramList.add("isDownloaded=" + isDownloaded);
            if ("Y".equals(isDownloaded))
            {
                attrExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("attrName"), EntityOperator.LIKE, EntityFunction.UPPER("IS_DOWNLOADED")));
                attrExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("attrValue"), EntityOperator.LIKE, EntityFunction.UPPER("Y")));
            	conditions.add(EntityCondition.makeCondition(attrExprs, EntityOperator.AND));
            }
            else
            {
                attrExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("attrName"), EntityOperator.LIKE, EntityFunction.UPPER("IS_DOWNLOADED")));
                List orExprs = FastList.newInstance();
                orExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("attrValue"), EntityOperator.LIKE, EntityFunction.UPPER("N")));
                orExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("attrValue"), EntityOperator.EQUALS, null));
                attrExprs.add(EntityCondition.makeCondition(orExprs, EntityOperator.OR));
            	conditions.add(EntityCondition.makeCondition(attrExprs, EntityOperator.AND));
            }

        }
        // Shipment Method
        String shipmentMethod = (String) context.get("shipmentMethod");
        if (UtilValidate.isNotEmpty(shipmentMethod)) {
            String carrierPartyId = (String) shipmentMethod.substring(0, shipmentMethod.indexOf("@"));
            String ShippingMethodTypeId = (String) shipmentMethod.substring(shipmentMethod.indexOf("@")+1);
            dve.addMemberEntity("OISG", "OrderItemShipGroup");
            dve.addAlias("OISG", "shipmentMethodTypeId");
            dve.addAlias("OISG", "carrierPartyId");
            dve.addViewLink("OH", "OISG", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("orderId", "orderId")));

            if (UtilValidate.isNotEmpty(carrierPartyId)) {
                paramList.add("carrierPartyId=" + carrierPartyId);
                conditions.add(makeExpr("carrierPartyId", carrierPartyId));
            }

            if (UtilValidate.isNotEmpty(ShippingMethodTypeId)) {
                paramList.add("ShippingMethodTypeId=" + ShippingMethodTypeId);
                conditions.add(makeExpr("shipmentMethodTypeId", ShippingMethodTypeId));
            }
        }
        // PaymentGatewayResponse
        String gatewayAvsResult = (String) context.get("gatewayAvsResult");
        String gatewayScoreResult = (String) context.get("gatewayScoreResult");
        if (UtilValidate.isNotEmpty(gatewayAvsResult) || UtilValidate.isNotEmpty(gatewayScoreResult)) {
            dve.addMemberEntity("OPP", "OrderPaymentPreference");
            dve.addMemberEntity("PGR", "PaymentGatewayResponse");
            dve.addAlias("OPP", "orderPaymentPreferenceId");
            dve.addAlias("PGR", "gatewayAvsResult");
            dve.addAlias("PGR", "gatewayScoreResult");
            dve.addViewLink("OH", "OPP", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("orderId", "orderId")));
            dve.addViewLink("OPP", "PGR", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("orderPaymentPreferenceId", "orderPaymentPreferenceId")));
        }

        if (UtilValidate.isNotEmpty(gatewayAvsResult)) {
            paramList.add("gatewayAvsResult=" + gatewayAvsResult);
            conditions.add(EntityCondition.makeCondition("gatewayAvsResult", gatewayAvsResult));
        }

        if (UtilValidate.isNotEmpty(productPromoCodeId)) {
            paramList.add("productPromoCodeId=" + productPromoCodeId);
            conditions.add(makeExpr("productPromoCodeId", productPromoCodeId));
        }

        if (UtilValidate.isNotEmpty(gatewayScoreResult)) {
            paramList.add("gatewayScoreResult=" + gatewayScoreResult);
            conditions.add(EntityCondition.makeCondition("gatewayScoreResult", gatewayScoreResult));
        }

        // add the role data to the view
        if (roleTypeList != null || partyId != null) {
            dve.addMemberEntity("OT", "OrderRole");
            dve.addAlias("OT", "partyId");
            dve.addAlias("OT", "roleTypeId");
            dve.addViewLink("OH", "OT", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("orderId", "orderId")));
        }

        if (UtilValidate.isNotEmpty(partyId)) {
            paramList.add("partyId=" + partyId);
            fieldsToSelect.add("partyId");
            conditions.add(makeExpr("partyId", partyId));
        }

        if (roleTypeList != null) {
            fieldsToSelect.add("roleTypeId");
            Iterator i = roleTypeList.iterator();
            List orExprs = FastList.newInstance();
            while (i.hasNext()) {
                String roleTypeId = (String) i.next();
                paramList.add("roleTypeId=" + roleTypeId);
                orExprs.add(makeExpr("roleTypeId", roleTypeId));
            }
            conditions.add(EntityCondition.makeCondition(orExprs, EntityOperator.OR));
        }

        // order item fields
        String correspondingPoId = (String) context.get("correspondingPoId");
        String subscriptionId = (String) context.get("subscriptionId");
        String productId = (String) context.get("productId");
        String budgetId = (String) context.get("budgetId");
        String quoteId = (String) context.get("quoteId");

        if (correspondingPoId != null || subscriptionId != null || productId != null || budgetId != null || quoteId != null) {
            dve.addMemberEntity("OI", "OrderItem");
            dve.addAlias("OI", "correspondingPoId");
            dve.addAlias("OI", "subscriptionId");
            dve.addAlias("OI", "productId");
            dve.addAlias("OI", "budgetId");
            dve.addAlias("OI", "quoteId");
            dve.addViewLink("OH", "OI", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("orderId", "orderId")));
        }

        if (UtilValidate.isNotEmpty(correspondingPoId)) {
            paramList.add("correspondingPoId=" + correspondingPoId);
            conditions.add(makeExpr("correspondingPoId", correspondingPoId));
        }

        if (UtilValidate.isNotEmpty(subscriptionId)) {
            paramList.add("subscriptionId=" + subscriptionId);
            conditions.add(makeExpr("subscriptionId", subscriptionId));
        }

        if (UtilValidate.isNotEmpty(productId)) {
            paramList.add("productId=" + productId);
            if (productId.startsWith("%") || productId.startsWith("*") || productId.endsWith("%") || productId.endsWith("*")) {
                conditions.add(makeExpr("productId", productId));
            } else {
                GenericValue product = null;
                try {
                    product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));
                } catch (GenericEntityException e) {
                    Debug.logWarning(e.getMessage(), module);
                }
                if (product != null) {
                    String isVirtual = product.getString("isVirtual");
                    if (isVirtual != null && "Y".equals(isVirtual)) {
                        List orExprs = FastList.newInstance();
                        orExprs.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId));

                        Map varLookup = null;
                        try {
                            varLookup = dispatcher.runSync("getAllProductVariants", UtilMisc.toMap("productId", productId));
                        } catch (GenericServiceException e) {
                            Debug.logWarning(e.getMessage(), module);
                        }
                        List variants = (List) varLookup.get("assocProducts");
                        if (variants != null) {
                            Iterator i = variants.iterator();
                            while (i.hasNext()) {
                                GenericValue v = (GenericValue) i.next();
                                orExprs.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, v.getString("productIdTo")));
                            }
                        }
                        conditions.add(EntityCondition.makeCondition(orExprs, EntityOperator.OR));
                    } else {
                        conditions.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId));
                    }
                }
            }
        }

        if (UtilValidate.isNotEmpty(budgetId)) {
            paramList.add("budgetId=" + budgetId);
            conditions.add(makeExpr("budgetId", budgetId));
        }

        if (UtilValidate.isNotEmpty(quoteId)) {
            paramList.add("quoteId=" + quoteId);
            conditions.add(makeExpr("quoteId", quoteId));
        }

        // payment preference fields
        String billingAccountId = (String) context.get("billingAccountId");
        String finAccountId = (String) context.get("finAccountId");
        String cardNumber = (String) context.get("cardNumber");
        String accountNumber = (String) context.get("accountNumber");

        if (finAccountId != null || cardNumber != null || accountNumber != null) {
            dve.addMemberEntity("OP", "OrderPaymentPreference");
            dve.addAlias("OP", "finAccountId");
            dve.addAlias("OP", "paymentMethodId");
            dve.addViewLink("OH", "OP", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("orderId", "orderId")));
        }

        // search by billing account ID
        if (UtilValidate.isNotEmpty(billingAccountId)) {
            paramList.add("billingAccountId=" + billingAccountId);
            conditions.add(makeExpr("billingAccountId", billingAccountId));
        }

        // search by fin account ID
        if (UtilValidate.isNotEmpty(finAccountId)) {
            paramList.add("finAccountId=" + finAccountId);
            conditions.add(makeExpr("finAccountId", finAccountId));
        }

        // search by card number
        if (UtilValidate.isNotEmpty(cardNumber)) {
            dve.addMemberEntity("CC", "CreditCard");
            dve.addAlias("CC", "cardNumber");
            dve.addViewLink("OP", "CC", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("paymentMethodId", "paymentMethodId")));

            paramList.add("cardNumber=" + cardNumber);
            conditions.add(makeExpr("cardNumber", cardNumber));
        }

        // search by eft account number
        if (UtilValidate.isNotEmpty(accountNumber)) {
            dve.addMemberEntity("EF", "EftAccount");
            dve.addAlias("EF", "accountNumber");
            dve.addViewLink("OP", "EF", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("paymentMethodId", "paymentMethodId")));

            paramList.add("accountNumber=" + accountNumber);
            conditions.add(makeExpr("accountNumber", accountNumber));
        }

        // shipment/inventory item
        String inventoryItemId = (String) context.get("inventoryItemId");
        String softIdentifier = (String) context.get("softIdentifier");
        String serialNumber = (String) context.get("serialNumber");
        String shipmentId = (String) context.get("shipmentId");

        if (shipmentId != null || inventoryItemId != null || softIdentifier != null || serialNumber != null) {
            dve.addMemberEntity("II", "ItemIssuance");
            dve.addAlias("II", "shipmentId");
            dve.addAlias("II", "inventoryItemId");
            dve.addViewLink("OH", "II", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("orderId", "orderId")));

            if (softIdentifier != null || serialNumber != null) {
                dve.addMemberEntity("IV", "InventoryItem");
                dve.addAlias("IV", "softIdentifier");
                dve.addAlias("IV", "serialNumber");
                dve.addViewLink("II", "IV", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("inventoryItemId", "inventoryItemId")));
            }
        }

        if (UtilValidate.isNotEmpty(inventoryItemId)) {
            paramList.add("inventoryItemId=" + inventoryItemId);
            conditions.add(makeExpr("inventoryItemId", inventoryItemId));
        }

        if (UtilValidate.isNotEmpty(softIdentifier)) {
            paramList.add("softIdentifier=" + softIdentifier);
            conditions.add(makeExpr("softIdentifier", softIdentifier, true));
        }

        if (UtilValidate.isNotEmpty(serialNumber)) {
            paramList.add("serialNumber=" + serialNumber);
            conditions.add(makeExpr("serialNumber", serialNumber, true));
        }

        if (UtilValidate.isNotEmpty(shipmentId)) {
            paramList.add("shipmentId=" + shipmentId);
            conditions.add(makeExpr("shipmentId", shipmentId));
        }

        // back order checking
        String hasBackOrders = (String) context.get("hasBackOrders");
        if (UtilValidate.isNotEmpty(hasBackOrders)) {
            dve.addMemberEntity("IR", "OrderItemShipGrpInvRes");
            dve.addAlias("IR", "quantityNotAvailable");
            dve.addViewLink("OH", "IR", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("orderId", "orderId")));

            paramList.add("hasBackOrders=" + hasBackOrders);
            if ("Y".equals(hasBackOrders)) {
                conditions.add(EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.NOT_EQUAL, null));
                conditions.add(EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.GREATER_THAN, BigDecimal.ZERO));
            } else if ("N".equals(hasBackOrders)) {
                List orExpr = FastList.newInstance();
                orExpr.add(EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.EQUALS, null));
                orExpr.add(EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.EQUALS, BigDecimal.ZERO));
                conditions.add(EntityCondition.makeCondition(orExpr, EntityOperator.OR));
            }
        }
           
        // Get all orders according to specific ship to country with "Only Include" or "Do not Include".
        List<String> orderContactMechIds = (List) context.get("orderContactMechIds");
        String countryGeoId = (String) context.get("countryGeoId");
        String includeCountry = (String) context.get("includeCountry");
        
        List orCon = FastList.newInstance();

        if (orderContactMechIds != null || (UtilValidate.isNotEmpty(countryGeoId) && UtilValidate.isNotEmpty(includeCountry))) {
            dve.addMemberEntity("OCM", "OrderContactMech");
            dve.addAlias("OCM", "contactMechId");
            dve.addAlias("OCM", "contactMechPurposeTypeId");
            dve.addViewLink("OH", "OCM", Boolean.FALSE, ModelKeyMap.makeKeyMapList("orderId"));
        }
        if (orderContactMechIds != null) {
            paramList.add("orderContactMechId=" + countryGeoId);
            for(String orderContactMechId : orderContactMechIds){
            	if(UtilValidate.isNotEmpty(orderContactMechId)){
            		orCon.add(EntityCondition.makeCondition("contactMechId", EntityOperator.EQUALS, orderContactMechId));
            	}
            }
            conditions.add(EntityCondition.makeCondition(orCon, EntityOperator.OR));
        }
         
        if (UtilValidate.isNotEmpty(countryGeoId) && UtilValidate.isNotEmpty(includeCountry)) {
            paramList.add("countryGeoId=" + countryGeoId);
            paramList.add("includeCountry=" + includeCountry);
            // add condition to dynamic view
            dve.addMemberEntity("PA", "PostalAddress");
            dve.addAlias("PA", "countryGeoId");
            dve.addViewLink("OCM", "PA", Boolean.FALSE, ModelKeyMap.makeKeyMapList("contactMechId"));

            EntityConditionList exprs = null;
            if ("Y".equals(includeCountry)) {
                exprs = EntityCondition.makeCondition(UtilMisc.toList(
                            EntityCondition.makeCondition("contactMechPurposeTypeId", "SHIPPING_LOCATION"),
                            EntityCondition.makeCondition("countryGeoId", countryGeoId)), EntityOperator.AND);
            } else {
                exprs = EntityCondition.makeCondition(UtilMisc.toList(
                            EntityCondition.makeCondition("contactMechPurposeTypeId", "SHIPPING_LOCATION"),
                            EntityCondition.makeCondition("countryGeoId", EntityOperator.NOT_EQUAL, countryGeoId)), EntityOperator.AND);
            }
            conditions.add(exprs);
        }

        // set distinct on so we only get one row per order
        EntityFindOptions findOpts = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true);

        // create the main condition
        EntityCondition cond = null;
        if (conditions.size() > 0 || showAll.equalsIgnoreCase("Y")) {
            cond = EntityCondition.makeCondition(conditions, EntityOperator.AND);
        }

        if (Debug.verboseOn()) {
            Debug.log("Find order query: " + cond.toString());
        }

        List orderList = FastList.newInstance();
        List completeOrderList = FastList.newInstance();
        int orderCount = 0;

        // get the index for the partial list
        int lowIndex = (((viewIndex.intValue() - 1) * viewSize.intValue()) + 1);
        int highIndex = viewIndex.intValue() * viewSize.intValue();
//        findOpts.setMaxRows(highIndex);

        if (cond != null) {
            EntityListIterator eli = null;
            try {
                // do the lookup
                eli = delegator.findListIteratorByCondition(dve, cond, null, fieldsToSelect, orderBy, findOpts);

                orderCount = eli.getResultsSizeAfterPartialList();

                // Always return the complete list
                // The next branch of logic will factor in the view size
                if (orderCount > 0)
                {
                    completeOrderList = eli.getCompleteList();
                }

                // get the partial list for this page
                eli.beforeFirst();
                if (orderCount > viewSize.intValue()) {
                    orderList = eli.getPartialList(lowIndex, viewSize.intValue());
                } else if (orderCount > 0) {
                    orderList = eli.getCompleteList();
                }

                if (highIndex > orderCount) {
                    highIndex = orderCount;
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            } finally {
                if (eli != null) {
                    try {
                        eli.close();
                    } catch (GenericEntityException e) {
                        Debug.logWarning(e, e.getMessage(), module);
                    }
                }
            }
        }

        // create the result map
        Map result = ServiceUtil.returnSuccess();

        // format the param list
        String paramString = StringUtil.join(paramList, "&amp;");

        result.put("highIndex", Integer.valueOf(highIndex));
        result.put("lowIndex", Integer.valueOf(lowIndex));
        result.put("viewIndex", viewIndex);
        result.put("viewSize", viewSize);
        result.put("showAll", showAll);

        result.put("paramList", (paramString != null? paramString: ""));
        result.put("orderList", orderList);
        result.put("completeOrderList", completeOrderList);
        result.put("orderListSize", Integer.valueOf(orderCount));

        return result;
    }

    protected static EntityExpr makeExpr(String fieldName, String value) {
        return makeExpr(fieldName, value, false, false);
    }

    protected static EntityExpr makeExpr(String fieldName, String value, boolean forceLike) {
        return makeExpr(fieldName, value, forceLike, false);
    }

    protected static EntityExpr makeExpr(String fieldName, String value, boolean forceLike, boolean caseInsensitive) {
        EntityComparisonOperator op = forceLike ? EntityOperator.LIKE : EntityOperator.EQUALS;

        if (value.startsWith("*")) {
            op = EntityOperator.LIKE;
            value = "%" + value.substring(1);
        }
        else if (value.startsWith("%")) {
            op = EntityOperator.LIKE;
        }

        if (value.endsWith("*")) {
            op = EntityOperator.LIKE;
            value = value.substring(0, value.length() - 1) + "%";
        }
        else if (value.endsWith("%")) {
            op = EntityOperator.LIKE;
        }

        if (forceLike) {
            if (!value.startsWith("%")) {
                value = "%" + value;
            }
            if (!value.endsWith("%")) {
                value = value + "%";
            }
        }

        EntityExpr retExpr = EntityCondition.makeCondition(fieldName, op, value);
        if(caseInsensitive){
            retExpr = EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(fieldName), op, EntityFunction.UPPER(value));
        }

        return retExpr;
    }
}
