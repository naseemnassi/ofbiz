package common;
import org.apache.commons.lang.StringUtils;

import org.ofbiz.base.util.*;
import org.ofbiz.order.shoppingcart.ShoppingCartEvents;
import org.ofbiz.product.catalog.*;
import org.ofbiz.product.category.*;
import javolution.util.FastMap;
import javolution.util.FastList;

// Get the Cart and Prepare Size
shoppingCart = ShoppingCartEvents.getCartObject(request);
context.shoppingCart = shoppingCart;

CategoryWorker.getRelatedCategories(request, "topLevelList", CatalogWorker.getCatalogTopCategoryId(request, CatalogWorker.getCurrentCatalogId(request)), true);
curCategoryId = parameters.productCategoryId ?: parameters.CATEGORY_ID ?: context.productCategoryId ?: "";
curTopMostCategoryId = parameters.topMostProductCategoryId ?: parameters.TOP_MOST_CATEGORY_ID ?: "";

request.setAttribute("curCategoryId", curCategoryId);
request.setAttribute("curTopMostCategoryId", curTopMostCategoryId);

CategoryWorker.setTrail(request, curCategoryId);
topCategoryList = request.getAttribute("topLevelList");
if (topCategoryList)
{
    catContentWrappers = FastMap.newInstance();
    CategoryWorker.getCategoryContentWrappers(catContentWrappers, topCategoryList, request);
    context.catContentWrappers = catContentWrappers;
}

searchText = com.osafe.util.Util.stripHTML(parameters.searchText) ?: "";
context.searchText = searchText;

context.topMostProductCategoryIdFacet = "N";
context.productCategoryIdFacet = "N";
filterGroup = parameters.filterGroup ?: "";
if (UtilValidate.isNotEmpty(filterGroup))
{
  facetGroups = FastList.newInstance();
  filterGroupValues = FastList.newInstance();
  filterGroupArr = StringUtil.split(filterGroup, "|");
  for (int i = 0; i < filterGroupArr.size(); i++)
  {
        facetGroupName = filterGroupArr[i];
        if(facetGroupName.indexOf("productCategoryId") > -1){
            context.productCategoryIdFacet = "Y";
        } else if (facetGroupName.indexOf("topMostProductCategoryId") > -1 ){
            context.topMostProductCategoryIdFacet = "Y";
        }else{
            // Only replace "_" underscores for facetGroups that are not productCategoryId
            // This is because the productCategoryId facetGroup value needs to be looked up
            // the actual "Product Category" information and we do not want to change the productCategoryId value.
            // ex. productCategoryId:SOME_PROD_CAT_ID needs to stay that value. In all other cases the
            // underscores can be replaced to get the description used for the breadcrumb
            facetGroupName =StringUtils.replace(facetGroupName, "_", " ");
        }

        facetGroups.add(facetGroupName);

        filterGroupValue = StringUtil.join(filterGroupArr.subList(0,i), "|");
        filterGroupValues.add(filterGroupValue);
  }

  context.facetGroups = facetGroups;
  context.filterGroupValues = filterGroupValues;
}

