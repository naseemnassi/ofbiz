
<#-- variable setup and worker calls -->
<#if resultList?exists && resultList?has_content><#assign topLevelList = resultList></#if>

<#-- looping macro -->
<#macro categories parentCategory category levelUrl levelValue listIndex listSize rowClass>
  <#assign categoryName = category.categoryName?if_exists>
  <#assign categoryDescription = category.description?if_exists>
  <#if listIndex =1>
    <#assign itemIndexClass="firstitem">
  <#else>
    <#if listIndex = listSize>
      <#assign itemIndexClass="lastitem">
    <#else>
      <#assign itemIndexClass="">
    </#if>
  </#if>

  <#local macroLevelUrl = levelUrl>

  <#assign levelClass = "">
  <#if levelValue?has_content && levelValue="1">
    <#assign levelClass = "topLevel">
  <#elseif levelValue?has_content && levelValue="2">
    <#assign levelClass = "subLevel">
  </#if>
  <#if subCatRollUpMap?has_content>
    <#assign subCatList = subCatRollUpMap.get(category.productCategoryId)!/>
  </#if> 
      
  <#if !(subCatList?has_content)>
    <#assign subListSize=0/>
    <#if subListSize == 0>
      <#local macroLevelUrl = "categoryDetail">
    </#if>
  </#if>

    <#if levelValue?has_content && levelValue="1">
      <tr class="dataRow ${levelClass} ${itemIndexClass} <#if rowClass == "2">even<#else>odd</#if>">
	    <td class="seqCol firstCol">${category.sequenceNum?if_exists}</td>
	    <td class="descCol"><a class="${levelClass}" href="<@ofbizUrl>categoryDetail?productCategoryId=${category.productCategoryId}&amp;parentProductCategoryId=${category.parentProductCategoryId?if_exists}&amp;activeFromDate=${category.fromDate?if_exists}</@ofbizUrl>"><#if categoryName?has_content>${categoryName}<#else>${categoryDescription?default("")}</#if></a></td>
	    <td class="seqCol">&nbsp;</td>
	    <td class="descCol" colspan="0">&nbsp;</td>
        <td class="dateCol">${(category.fromDate?string(preferredDateFormat))!""}</td>
        <td class="dateCol">${(category.thruDate?string(preferredDateFormat))!""}</td>
        <td class="actionCol">
          <span class="noAction"></span>
          <#assign categoryMembers = delegator.findByAnd("ProductCategoryMember",Static["org.ofbiz.base.util.UtilMisc"].toMap("productCategoryId", category.productCategoryId))>
          <#assign categoryMembers = Static["org.ofbiz.entity.util.EntityUtil"].filterByDate(categoryMembers)>
	      <#if categoryMembers?has_content> 
            <a href="<@ofbizUrl>productManagement?categoryId=${category.productCategoryId!}&preRetrieved='Y'</@ofbizUrl>" onMouseover="showTooltip(event,'${uiLabelMap.ProductCategoryTooltip}');" onMouseout="hideTooltip()"><span class="productIcon"></span></a>
            <a href="<@ofbizUrl>plpSequence?productCategoryId=${category.productCategoryId!}&preRetrieved='Y'</@ofbizUrl>" onMouseover="showTooltip(event,'${uiLabelMap.ProductSequenceTooltip}');" onMouseout="hideTooltip()"><span class="sequenceIcon"></span></a>
          <#else>
            <span class="noAction"></span>
            <span class="noAction"></span>
          </#if>
          <a href="<@ofbizUrl>categoryMetatag?productCategoryId=${category.productCategoryId!}</@ofbizUrl>" onMouseover="showTooltip(event,'${uiLabelMap.HtmlMetatagTooltip}');" onMouseout="hideTooltip()"><span class="metatagIcon"></span></a>
        </td>
	  </tr>
	</#if>
    <#if levelValue?has_content && levelValue="2">
	  <tr class="dataRow ${levelClass} ${itemIndexClass} <#if rowClass == "2">even<#else>odd</#if>">
	    <td class="seqCol firstCol">&nbsp;</td>
	    <td class="descCol">&nbsp;</td>
	    <td class="seqCol">${category.sequenceNum?if_exists}</td>
	    <td class="descCol" colspan="0"><a class="${levelClass}" href="<@ofbizUrl>categoryDetail?productCategoryId=${category.productCategoryId}&amp;parentProductCategoryId=${category.parentProductCategoryId?if_exists}&amp;activeFromDate=${category.fromDate?if_exists}</@ofbizUrl>"><#if categoryName?has_content>${categoryName}<#else>${categoryDescription?default("")}</#if></a></td>
        <td class="dateCol">${(category.fromDate?string(preferredDateFormat))!""}</td>
        <td class="dateCol">${(category.thruDate?string(preferredDateFormat))!""}</td>
        <#assign productIds = Static["com.osafe.util.OsafeAdminUtil"].getAssociatedProductFromCategory(request,category.productCategoryId)/>
        <td class="actionCol">
          <a href="<@ofbizUrl>categoryImageDetail?productCategoryId=${category.productCategoryId?if_exists}</@ofbizUrl>" onMouseover="<#if category.categoryImageUrl?has_content>showTooltipImage(event,'${uiLabelMap.CategoryImageTooltip}','${category.categoryImageUrl!}?${nowTimestamp!}');<#else>showTooltip(event,'${uiLabelMap.CategoryImageTooltip}');</#if>" onMouseout="hideTooltip()"><span class="imageIcon"></span></a>
	      <#if productIds?has_content> 
            <a href="<@ofbizUrl>productManagement?categoryId=${category.productCategoryId!}&preRetrieved='Y'</@ofbizUrl>" onMouseover="showTooltip(event,'${uiLabelMap.ProductCategoryTooltip}');" onMouseout="hideTooltip()"><span class="productIcon"></span></a>
          <#else>
            <span class="noAction"></span>
          </#if>
          <#if productIds?has_content> 
            <a href="<@ofbizUrl>plpSequence?productCategoryId=${category.productCategoryId!}&preRetrieved='Y'</@ofbizUrl>" onMouseover="showTooltip(event,'${uiLabelMap.ProductSequenceTooltip}');" onMouseout="hideTooltip()"><span class="sequenceIcon"></span></a>
          <#else>
            <span class="noAction"></span>
          </#if>
          <a href="<@ofbizUrl>categoryMetatag?productCategoryId=${category.productCategoryId!}</@ofbizUrl>" onMouseover="showTooltip(event,'${uiLabelMap.HtmlMetatagTooltip}');" onMouseout="hideTooltip()"><span class="metatagIcon"></span></a>
        </td>
	  </tr>
	</#if>
    <#if subCatList?has_content>
      <#assign idx=1/>
      <#assign subListSize=subCatList.size()/>
      <#list subCatList as subCat>
          <@categories parentCategory=category category=subCat levelUrl="eCommerceProductList" levelValue="2" listIndex=idx listSize=subListSize rowClass=rowClass/>
          <#assign idx= idx + 1/>
      </#list>
    </#if>
</#macro>

<#--
    Current nav bar is genrated as a single level menu
    http://htmldog.com/articles/suckerfish/dropdowns/
    -->

  <tr class="heading">
    <th class="seqCol firstCol">${uiLabelMap.SeqNumberLabel}</th>
    <th class="descCol">${uiLabelMap.TopNavigationLabel}</th>
    <th class="seqCol">${uiLabelMap.SeqNumberLabel}</th>
    <th class="descCol">${uiLabelMap.SubItemLabel}</th>
    <th class="dateCol">${uiLabelMap.ActiveFromLabel}</th>
    <th class="dateCol">${uiLabelMap.ActiveThruLabel}</th>
    <th class="actionCol">${uiLabelMap.ActionsLabel}</th>
  </tr>
  <#if topLevelList?has_content>
    <#assign rowClass = "1">
    <#assign parentIdx=1/>
    <#assign listSize=topLevelList.size()/>
    <#list topLevelList as category>
      <@categories parentCategory="" category=category levelUrl="categoryDetail" levelValue="1" listIndex=parentIdx listSize=listSize rowClass=rowClass/>
      <#assign parentIdx= parentIdx + 1/>
      <#-- toggle the row color -->
      <#if rowClass == "2">
        <#assign rowClass = "1">
      <#else>
        <#assign rowClass = "2">
      </#if>
    </#list>
  <#else>
    <tr><td class="boxNumber" colspan="5">${uiLabelMap.NoMatchingDataInfo}</td></tr>
  </#if>
