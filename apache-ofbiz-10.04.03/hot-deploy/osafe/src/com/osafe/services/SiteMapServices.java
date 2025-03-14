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

package com.osafe.services;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.ofbiz.product.category.CategoryContentWrapper;
import org.ofbiz.product.category.CategoryWorker;
import javolution.util.FastMap;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.base.util.UtilMisc; 
import org.ofbiz.base.util.UtilURL;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.product.product.ProductContentWrapper;
import org.ofbiz.product.product.ProductWorker;
import org.ofbiz.service.ServiceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javolution.util.FastList;
import com.osafe.util.Util;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import org.ofbiz.base.util.string.FlexibleStringExpander;

public class SiteMapServices {

    public static final String module = SiteMapServices.class.getName();
    private static Document document = null;
    private static Element rootElement = null;
    public static String PRODUCT_DETAIL_URL = null;
    public static String PRODUCT_LIST_URL = null;
    public static String PRODUCT_CATEGORY_LIST_URL = null;
    public static String STATIC_PAGE_URL = null;
    public static String CATALOG_URL_MOUNT_POINT = "shop";
    public static String SITEMAP_VARIANT_FEATURES = null;
    private static ResourceBundleMapWrapper OSAFE_FRIENDLY_URL = null;
    private static final ResourceBundle OSAFE_PROP = UtilProperties.getResourceBundle("osafe.properties", Locale.getDefault());
    
    public static Map buildSiteMap(DispatchContext dctx, Map<String, ?> context) 
    {

        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");
        String browseRootProductCategoryId = (String) context.get("browseRootProductCategoryId");
        String siteMapOutputDir = (String) context.get("siteMapOutputDir");
        CATALOG_URL_MOUNT_POINT=OSAFE_PROP.getString("url.catalog.prefix");
        OSAFE_FRIENDLY_URL = (ResourceBundleMapWrapper) UtilProperties.getResourceBundleMap("OSafeSeoUrlMap", Locale.getDefault());        	
        
        PRODUCT_DETAIL_URL = (String) context.get("productDetailUrl");
        if (UtilValidate.isEmpty(PRODUCT_DETAIL_URL)) {
            PRODUCT_DETAIL_URL = Util.getProductStoreParm(delegator, productStoreId, "SITEMAP_PDP_URL");
        }
        
        PRODUCT_LIST_URL = (String) context.get("productListUrl");
        if (UtilValidate.isEmpty(PRODUCT_LIST_URL)) {
            PRODUCT_LIST_URL = Util.getProductStoreParm(delegator, productStoreId, "SITEMAP_PLP_URL");
        }
        PRODUCT_CATEGORY_LIST_URL = (String) context.get("productCategoryListUrl");
        if (UtilValidate.isEmpty(PRODUCT_CATEGORY_LIST_URL)) {
            PRODUCT_CATEGORY_LIST_URL = Util.getProductStoreParm(delegator, productStoreId, "SITEMAP_CLP_URL");
        }
        
        STATIC_PAGE_URL = (String) context.get("staticPageUrl");
        if (UtilValidate.isEmpty(STATIC_PAGE_URL)) {
            STATIC_PAGE_URL = Util.getProductStoreParm(delegator, productStoreId, "SITEMAP_STATIC_URL");
        }
        
        SITEMAP_VARIANT_FEATURES = (String) context.get("siteMapVariantFeatures");
        if (UtilValidate.isEmpty(SITEMAP_VARIANT_FEATURES)) {
        	SITEMAP_VARIANT_FEATURES = Util.getProductStoreParm(delegator, productStoreId, "SITEMAP_VARIANT_FEATURES");
        }
        document = UtilXml.makeEmptyXmlDocument();
        rootElement = document.createElement("urlset");
        document.appendChild(rootElement);
        rootElement.setAttribute("xmlns", "http://www.sitemaps.org/schemas/sitemap/0.9");
        
        String url=null;

        try 
        {
            List<String> rootUrlList = StringUtil.split(PRODUCT_LIST_URL, "/");
            if (rootUrlList.size() > 1) {
                String rootUrl = rootUrlList.get(0)+"//"+rootUrlList.get(1);
                createSiteMapNode(rootUrl);
            }
           
            // Get all unexpired Product Categories (Top Level Catalog Category)
            List<Map<String, Object>> allUnexpiredCategories = CategoryServices.getRelatedCategories(delegator, browseRootProductCategoryId, null, true, false, true);
            GenericValue workingCategory = null;
            String productCategoryIdPath = null;
            int categoryLevel = 0;
            List<String> categoryTrail = null;
            for (Map<String, Object> workingCategoryMap : allUnexpiredCategories) 
            {
                workingCategory = (GenericValue) workingCategoryMap.get("ProductCategory");
                if ("CATALOG_CATEGORY".equals(workingCategory.getString("productCategoryTypeId"))) 
                {
                    String productCategoryId = (String) workingCategory.getString("productCategoryId");
                    String parentCategoryId = (String) workingCategory.getString("primaryParentCategoryId");
                    if (UtilValidate.isNotEmpty(parentCategoryId) && parentCategoryId.equals(browseRootProductCategoryId))
                    {
                        List<Map<String, Object>> relatedCategories = CategoryServices.getRelatedCategories(delegator, productCategoryId, null, true, false, true);
                        if (UtilValidate.isNotEmpty(relatedCategories))
                        {
                            url = makeCatalogUrl(null,null,null,productCategoryId, null,null);
                        }
                        else
                        {
                            url = makeCatalogUrl(null,null,productCategoryId,null, null,null);
                        	
                        }
                        createSiteMapNode(url);
                    	
                    }
                    else
                    {
                        url = makeCatalogUrl(null,null,productCategoryId,null, null,null);
                        createSiteMapNode(url);
                    	
                    }
                    
                    categoryTrail = (List<String>) workingCategoryMap.get("categoryTrail");
                    categoryLevel = categoryTrail.size() - 1;
                    
                    List<GenericValue> productCategoryMembers = workingCategory.getRelated("ProductCategoryMember");
                    // Remove any expired
                    productCategoryMembers = EntityUtil.filterByDate(productCategoryMembers, true);
                    for (GenericValue productCategoryMember : productCategoryMembers) 
                    {
                        GenericValue product = productCategoryMember.getRelatedOne("Product");
                        if (UtilValidate.isNotEmpty(product)) 
                        {
                            String isVariant = product.getString("isVariant");
                            if (UtilValidate.isEmpty(isVariant)) 
                            {
                                isVariant = "N";
                            }
                            // All Non-Variant Products
                            if ("N".equals(isVariant)) 
                            {

                                if (ProductWorker.isSellable(product)) 
                                {
                                    url = makeCatalogUrl(product.getString("productId"),productCategoryId, null, null, null,null);
                                    createSiteMapNode(url);
                                    if (UtilValidate.isNotEmpty(SITEMAP_VARIANT_FEATURES))
                                    {
                                       List<GenericValue> lProductFeatureAndAppl = delegator.findByAnd("ProductFeatureAndAppl",UtilMisc.toMap("productId", product.getString("productId"),"productFeatureTypeId",SITEMAP_VARIANT_FEATURES.toUpperCase(),"productFeatureApplTypeId","SELECTABLE_FEATURE"));
                                        lProductFeatureAndAppl = EntityUtil.filterByDate(lProductFeatureAndAppl);
                                        if (UtilValidate.isNotEmpty(lProductFeatureAndAppl))
                                        {
                                            for (GenericValue productFeatureAndAppl : lProductFeatureAndAppl)
                                            {
                                                String featureDescription = productFeatureAndAppl.getString("description"); 
                                                url = makeCatalogUrl(product.getString("productId"),productCategoryId, null, null, null,SITEMAP_VARIANT_FEATURES.toUpperCase() + ":" + featureDescription);
                                                createSiteMapNode(url);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                	
                }
            }
            
            //Added static page url in sitemap
            List<GenericValue> xContentXrefs = null;
            xContentXrefs = delegator.findByAndCache("XContentXref", UtilMisc.toMap("contentTypeId", "BF_STATIC_PAGE","productStoreId", productStoreId), null);
            if (UtilValidate.isNotEmpty(xContentXrefs))
            {
                for (GenericValue xContentXref : xContentXrefs)
                {
                    GenericValue content = xContentXref.getRelatedOne("Content");
                    if (UtilValidate.isNotEmpty(content) && "CTNT_PUBLISHED".equals(content.getString("statusId")))
                    {
                        url = makeCatalogUrl(null, null, null, null, xContentXref.getString("bfContentId"),null);
                        createSiteMapNode(url);
                    }
                }
            }
            File siteMapFile = null;
           if (UtilValidate.isEmpty(siteMapOutputDir))
           {
               siteMapFile = createDocument(System.getProperty("ofbiz.home"),"sitemap.xml");
           }
           else
           {
        	   siteMapFile = createDocument(siteMapOutputDir,"sitemap.xml");
           }

           if (UtilValidate.isNotEmpty(siteMapFile)) {
        	   result.put("siteMapFile", siteMapFile);
           }
        }
          catch (Exception e)
          {
              Debug.logError("Error generating SiteMap: " + e.getMessage(), module);
              ServiceUtil.returnError("Error generating SiteMap: " + e.getMessage());
        	  
          }
          
       return result;
    }
    	
    public static Map buildFriendlyUrlMap(DispatchContext dctx, Map<String, ?> context) 
    {

        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");
        String browseRootProductCategoryId = (String) context.get("browseRootProductCategoryId");
        String friendlyMapOutputDir = (String) context.get("friendlyMapOutputDir");
        ProductContentWrapper productContentWrapper = null;
        CategoryContentWrapper categoryContentWrapper = null;
        CategoryContentWrapper parentCategoryContentWrapper = null;
        
        String configPath = FlexibleStringExpander.expandString(UtilProperties.getPropertyValue("osafeAdmin.properties", "ecommerce-config-path"), context);
        String deploymentConfigPath = FlexibleStringExpander.expandString(UtilProperties.getPropertyValue("osafeAdmin.properties", "ecommerce-deployment-config-path"), context);

        
        PRODUCT_DETAIL_URL = Util.getProductStoreParm(delegator, productStoreId, "SITEMAP_PDP_URL");
        PRODUCT_LIST_URL = Util.getProductStoreParm(delegator, productStoreId, "SITEMAP_PLP_URL");
        PRODUCT_CATEGORY_LIST_URL = Util.getProductStoreParm(delegator, productStoreId, "SITEMAP_CLP_URL");
        STATIC_PAGE_URL = Util.getProductStoreParm(delegator, productStoreId, "SITEMAP_STATIC_URL");
      	SITEMAP_VARIANT_FEATURES = Util.getProductStoreParm(delegator, productStoreId, "SITEMAP_VARIANT_FEATURES");
        
        document = UtilXml.makeEmptyXmlDocument();
        rootElement = document.createElement("resource");
        document.appendChild(rootElement);
        rootElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        
        String url=null;

        try 
        {
            // Get all unexpired Product Categories (Top Level Catalog Category)
            List<Map<String, Object>> allUnexpiredCategories = CategoryServices.getRelatedCategories(delegator, browseRootProductCategoryId, null, true, false, true);
            GenericValue workingCategory = null;
            String productCategoryIdPath = null;
            int categoryLevel = 0;
            List<String> categoryTrail = null;
            for (Map<String, Object> workingCategoryMap : allUnexpiredCategories) 
            {
            	
                productContentWrapper = null;
                categoryContentWrapper = null;
                parentCategoryContentWrapper = null;
            	
                workingCategory = (GenericValue) workingCategoryMap.get("ProductCategory");
                categoryContentWrapper = new CategoryContentWrapper(dispatcher, workingCategory, locale, "text/html");
                if ("CATALOG_CATEGORY".equals(workingCategory.getString("productCategoryTypeId"))) 
                {
                    String productCategoryId = (String) workingCategory.getString("productCategoryId");
                    String parentCategoryId = (String) workingCategory.getString("primaryParentCategoryId");
                    if (UtilValidate.isNotEmpty(parentCategoryId) && parentCategoryId.equals(browseRootProductCategoryId))
                    {
                        List<Map<String, Object>> relatedCategories = CategoryServices.getRelatedCategories(delegator, productCategoryId, null, true, false, true);
                        if (UtilValidate.isNotEmpty(relatedCategories))
                        {
                            url = makeCatalogUrl(null,null,null,productCategoryId, null,null);
                        }
                        else
                        {
                            url = makeCatalogUrl(null,null,productCategoryId,null, null,null);
                        	
                        }
                        createFriendlyMapNode(url, productContentWrapper, categoryContentWrapper,parentCategoryContentWrapper,null, null);
                    	
                    }
                    else
                    {
                    	if (UtilValidate.isNotEmpty(parentCategoryId))
                    	{
                    		GenericValue parentCategory =  delegator.findOne("ProductCategory", UtilMisc.toMap("productCategoryId", parentCategoryId), true);
                    		parentCategoryContentWrapper = new CategoryContentWrapper(dispatcher, parentCategory, locale, "text/html");
                    		
                    	}
                        url = makeCatalogUrl(null,null,productCategoryId,null, null,null);
                        createFriendlyMapNode(url, productContentWrapper, categoryContentWrapper,parentCategoryContentWrapper,null, null);
                    	
                    }
                    
                    categoryTrail = (List<String>) workingCategoryMap.get("categoryTrail");
                    categoryLevel = categoryTrail.size() - 1;
                    
                    List<GenericValue> productCategoryMembers = workingCategory.getRelated("ProductCategoryMember");
                    // Remove any expired
                    productCategoryMembers = EntityUtil.filterByDate(productCategoryMembers, true);
                    for (GenericValue productCategoryMember : productCategoryMembers) 
                    {
                        GenericValue product = productCategoryMember.getRelatedOne("Product");
                        if (UtilValidate.isNotEmpty(product)) 
                        {
                            String isVariant = product.getString("isVariant");
                            if (UtilValidate.isEmpty(isVariant)) 
                            {
                                isVariant = "N";
                            }
                            // All Non-Variant Products
                            if ("N".equals(isVariant)) 
                            {

                                if (ProductWorker.isSellable(product)) 
                                {
                                    url = makeCatalogUrl(product.getString("productId"),productCategoryId, null, null, null,null);
                                    productContentWrapper = new ProductContentWrapper(dispatcher, product, locale, "text/html");
                                    createFriendlyMapNode(url, productContentWrapper, categoryContentWrapper,null,null, null);
                                    if (UtilValidate.isNotEmpty(SITEMAP_VARIANT_FEATURES))
                                    {
                                       List<GenericValue> lProductFeatureAndAppl = delegator.findByAnd("ProductFeatureAndAppl",UtilMisc.toMap("productId", product.getString("productId"),"productFeatureTypeId",SITEMAP_VARIANT_FEATURES.toUpperCase(),"productFeatureApplTypeId","SELECTABLE_FEATURE"));
                                        lProductFeatureAndAppl = EntityUtil.filterByDate(lProductFeatureAndAppl);
                                        if (UtilValidate.isNotEmpty(lProductFeatureAndAppl))
                                        {
                                            for (GenericValue productFeatureAndAppl : lProductFeatureAndAppl)
                                            {
                                                String featureDescription = productFeatureAndAppl.getString("description"); 
                                                url = makeCatalogUrl(product.getString("productId"),productCategoryId, null, null, null,SITEMAP_VARIANT_FEATURES.toUpperCase() + ":" + featureDescription);
                                                createFriendlyMapNode(url, productContentWrapper, categoryContentWrapper,null,featureDescription, null);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                	
                }
            }

            //Added static page url in sitemap
            List<GenericValue> xContentXrefs = null;
            xContentXrefs = delegator.findByAndCache("XContentXref", UtilMisc.toMap("contentTypeId", "BF_STATIC_PAGE","productStoreId", productStoreId), null);
            if (UtilValidate.isNotEmpty(xContentXrefs))
            {
                for (GenericValue xContentXref : xContentXrefs)
                {
                    GenericValue content = xContentXref.getRelatedOne("Content");
                    if (UtilValidate.isNotEmpty(content) && "CTNT_PUBLISHED".equals(content.getString("statusId")))
                    {
                        String seoUrlValue = content.getString("contentName");
                        GenericValue contentAttribute = delegator.findOne("ContentAttribute", UtilMisc.toMap("contentId",content.getString("contentId"),"attrName","SEO_FRIENDLY_URL"), false);
                        if (UtilValidate.isNotEmpty(contentAttribute))
                        {
                            seoUrlValue = contentAttribute.getString("attrValue");
                        }
                        url = makeCatalogUrl(null, null, null, null, xContentXref.getString("bfContentId"),null);
                        createFriendlyMapNode(url, null, null, null, null, seoUrlValue);
                    }
                }
            }
           File friendlyMapFile = null;
           friendlyMapFile = createDocument(deploymentConfigPath,"OSafeSeoUrlMap.xml");
           FileUtils.copyFile(new File(deploymentConfigPath + "/" + "OSafeSeoUrlMap.xml"), new File(configPath + "/" + "OSafeSeoUrlMap.xml"));
        }
          catch (Exception e)
          {
              Debug.logError("Error generating SiteMap: " + e.getMessage(), module);
              ServiceUtil.returnError("Error generating SiteMap: " + e.getMessage());
        	  
          }
          
       return result;
    }
    
    private static void createSiteMapNode (String URL)
    {
        StringBuilder urlBuilder = new StringBuilder();
    	
    	try {
            List<String> pathElements = StringUtil.split(URL, "/");
            String sUrlTarget=pathElements.get(pathElements.size() - 1);
        	String friendlyKey=StringUtil.replaceString(sUrlTarget,"&","~");
        	friendlyKey=StringUtil.replaceString(friendlyKey,"=","-");
        	String friendlyUrl =null;
        	
        	if (OSAFE_FRIENDLY_URL.containsKey(friendlyKey))
        	{
            	friendlyUrl =(String)OSAFE_FRIENDLY_URL.get(friendlyKey);
        		
        	}
        	if (UtilValidate.isNotEmpty(friendlyUrl))
        	{
            	int idxControl = URL.indexOf("control/");
            	if (idxControl > -1)
            	{
                    urlBuilder.append(URL.substring(0,idxControl));
                    urlBuilder.append(CATALOG_URL_MOUNT_POINT + "/");
            	}
                urlBuilder.append(friendlyUrl.toLowerCase());
        	}
        	else
        	{
                urlBuilder.append(URL);
        		
        	}
        	
            Element newElement = document.createElement("url");
            Element newChildElement = document.createElement("loc");
            newChildElement.appendChild(document.createTextNode(urlBuilder.toString()));
            newElement.appendChild(newChildElement);
            rootElement.appendChild(newElement);
    		
    		
    	} catch (Exception e) {
    		
    	}
    	
    }
    
    private static void createFriendlyMapNode (String URL,ProductContentWrapper productContentWrapper,CategoryContentWrapper categoryContentWrapper,CategoryContentWrapper parentCategoryContentWrapper,String featureDescription, String contentSeoFriendlyName) {
    	
        StringBuilder urlBuilder = new StringBuilder();
        String productName=null;
        String parentCategoryName=null;
        String categoryName=null;
        String friendlyKey=null;
        String friendlyValue=null;
        StringBuilder friendlyKeyValue=new StringBuilder();
    	
    	try {
            List<String> pathElements = StringUtil.split(URL, "/");
            String sUrlTarget=pathElements.get(pathElements.size() - 1);
            
        	friendlyKey=StringUtil.replaceString(sUrlTarget,"&","~");
        	friendlyKey=StringUtil.replaceString(friendlyKey,"=","-");
            
        	if (productContentWrapper != null)
        	{
        		productName=productContentWrapper.get("PRODUCT_NAME").toString();
        	}

        	if (parentCategoryContentWrapper !=null)
        	{
        		parentCategoryName=parentCategoryContentWrapper.get("CATEGORY_NAME").toString();
        	}

        	if (categoryContentWrapper !=null)
        	{
        		categoryName=categoryContentWrapper.get("CATEGORY_NAME").toString();
        	}
        	
        	if (UtilValidate.isNotEmpty(parentCategoryName))
        	{
        		friendlyKeyValue.append(parentCategoryName + "/");
        	}
        	
        	if (UtilValidate.isNotEmpty(categoryName))
        	{
        		friendlyKeyValue.append(categoryName);
        	}

        	if (UtilValidate.isNotEmpty(productName))
        	{
            	if (UtilValidate.isNotEmpty(featureDescription))
            	{
            		productName = productName + " " + featureDescription;
            	}
        		friendlyKeyValue.append("/" + productName);
        	}
        	if (UtilValidate.isNotEmpty(contentSeoFriendlyName))
        	{
        		friendlyKeyValue.append(contentSeoFriendlyName);
        	}
        	friendlyValue=friendlyKeyValue.toString();
        	friendlyValue=StringUtil.replaceString(friendlyValue,"&apos;","");
        	friendlyValue=StringUtil.replaceString(friendlyValue,"'","");
        	friendlyValue=StringUtil.replaceString(friendlyValue,"&quot;","");
        	friendlyValue=StringUtil.replaceString(friendlyValue,"\"","");
        	friendlyValue=StringUtil.replaceString(friendlyValue,"&amp;","And");
        	friendlyValue=StringUtil.replaceString(friendlyValue,"&","And");
        	friendlyValue=StringUtil.replaceString(friendlyValue,",","");
        	friendlyValue=StringUtil.replaceString(friendlyValue,"-","");
        	friendlyValue=StringUtil.replaceString(friendlyValue,".","");
        	friendlyValue=StringUtil.replaceString(friendlyValue," ","-");
        	
        	Element newElement = document.createElement("property");
            newElement.setAttribute("key", friendlyKey);
            Element newChildElement = document.createElement("value");
            newChildElement.setAttribute("xml:lang", "en");
            newChildElement.appendChild(document.createTextNode(friendlyValue.toLowerCase()));
            newElement.appendChild(newChildElement);
            rootElement.appendChild(newElement);    
    		
    	}
    	catch (Exception e) {
            Debug.logError(e, module);
    		
    	}
    }

    private static File createDocument(String xmlFilePath,String sFileName) 
    {
        OutputStream os = null;
        try 
        {
            if (document != null) 
            {
            	if (xmlFilePath!= null)
            	{
            		sFileName = xmlFilePath + File.separator + sFileName;
            	}

            	File siteMapFile = new File(sFileName);
                // ################################################
                // save the DOM document  
                // ################################################
                document.normalize();
                os = new FileOutputStream(siteMapFile);
                UtilXml.writeXmlDocument(os, document);
                if (os != null) 
                {
                    os.close();
                }
                return siteMapFile;
            }
        } catch (IOException ioe) {
            Debug.logError(ioe, module);
        } catch (Exception exc) {
            Debug.logError(exc, module);
        }
        return null;
    }
    
    public static String makeCatalogUrl(String productId, String productCategoryId, String currentCategoryId, String topCategoryId, String contentId,String productFeatureType) {
        StringBuilder urlBuilder = new StringBuilder();

        if (UtilValidate.isNotEmpty(topCategoryId)) 
        {
            urlBuilder.append(PRODUCT_CATEGORY_LIST_URL);
            urlBuilder.append("?productCategoryId=" + topCategoryId);
        }
        if (UtilValidate.isNotEmpty(currentCategoryId)) 
        {
            urlBuilder.append(PRODUCT_LIST_URL);
            urlBuilder.append("?productCategoryId=" + currentCategoryId);
        }

        if (UtilValidate.isNotEmpty(productId)) 
        {
            urlBuilder.append(PRODUCT_DETAIL_URL);
            urlBuilder.append("?productId=" + productId);
            if (UtilValidate.isNotEmpty(productCategoryId)) 
            {
                urlBuilder.append("&productCategoryId=");
                urlBuilder.append(productCategoryId);
            }
            if (UtilValidate.isNotEmpty(productFeatureType)) 
            {
                urlBuilder.append("&productFeatureType=");
                urlBuilder.append(productFeatureType);
            }
        }
        
        if (UtilValidate.isNotEmpty(contentId)) 
        {
            urlBuilder.append(STATIC_PAGE_URL);
            urlBuilder.append("?contentId=" + contentId);
        }

        return urlBuilder.toString();
    }
    
}
