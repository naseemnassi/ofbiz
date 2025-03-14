<!-- start displayBox -->
<#if Static["com.osafe.util.Util"].isProductStoreParmTrue(CHECKOUT_STORE_PICKUP!"")>
<div class="displayBox dashboardSummary">
    <div class="header"><h2>${uiLabelMap.DashboardStorePickupHeading}</h2></div>
    <div class="boxBody">
        <table class="osafe">
            <tr class="heading">
                <th class="boxCaption firstCol">${uiLabelMap.SummaryLabel}</th>
                <th class="boxDollar">&nbsp;</th>
                <th class="boxNumber">${uiLabelMap.OrdLabel}</th>
                <th class="boxDollar lastCol">${uiLabelMap.SalesLabel}${globalContext.currencySymbol!}</th>
            </tr>
            <tr class="odd">
                <td class="boxCaption firstCol">${uiLabelMap.PeriodSummaryCaption}</td>
                <td class="boxNumber">&nbsp;</td>
                <td class="boxNumber">
                    <#if (storePickupOrderCount!0) != 0>
                        <#assign defaultStatusList = "viewcreated" + "=" + "Y">
                        <#assign defaultStatusList = defaultStatusList + "&" + "viewprocessing" + "=" + "Y">
                        <#assign defaultStatusList = defaultStatusList + "&" + "viewapproved" + "=" + "Y">
                        <#assign defaultStatusList = defaultStatusList + "&" + "viewhold" + "=" + "Y">
                        <#assign defaultStatusList = defaultStatusList + "&" + "viewcompleted" + "=" + "Y">

                        <#assign orderDateFrom = "&orderDateFrom" + "=" + (periodFrom!parameters.periodFrom!"")>
                        <#assign orderDateTo = "&orderDateTo" + "=" + (periodTo!parameters.periodTo!"")>
                        <#assign initializedCB = "&initializedCB" + "=" + "Y">
                        <#assign preRetrieved = "&preRetrieved" + "=" + "Y">
                        <#assign srchStorePickup = "&srchStorePickup" + "=" + "Y">

                        <#assign orderManagementParams = defaultStatusList + orderDateFrom + orderDateTo+ initializedCB+ preRetrieved+ srchStorePickup>

                        <a href="<@ofbizUrl>orderManagement?${orderManagementParams}</@ofbizUrl>">${storePickupOrderCount}</a>
                    <#else>
                        ${storePickupOrderCount?default('0')}
                    </#if>
                </td>
                <td class="boxDollar lastCol"><#if storePickupTotalRevenue?has_content && storePickupTotalRevenue &gt; 0> <@ofbizCurrency amount=storePickupTotalRevenue!0 rounding=0 isoCode=globalContext.defaultCurrencyUomId /><#else>${storePickupTotalRevenue?default(0)}</#if></td>
            </tr>
            <tr class="even">
                <td class="boxCaption firstCol">${uiLabelMap.AverageOrderCaption}</td>
                <td class="boxNumber">&nbsp;</td>
                <td class="boxNumber">&nbsp;</td>
                <td class="boxDollar lastCol"><#if storePickupAverageRevenue?has_content && storePickupAverageRevenue &gt; 0> <@ofbizCurrency amount=storePickupAverageRevenue!0 rounding=0 isoCode=globalContext.defaultCurrencyUomId /><#else>${storePickupAverageRevenue?default(0)}</#if></td>
            </tr>
            <tr class="odd">
                <td class="boxCaption firstCol">${uiLabelMap.DailyAverageCaption} (${diffDays!"0"} ${uiLabelMap.DaysLabel}):</td>
                <td class="boxNumber">&nbsp;</td>
                <td class="boxNumber">${storePickupDailyAvgOrderCount?default('0')}</td>
                <td class="boxDollar lastCol"><#if storePickupDailyAvgRevenue?has_content && storePickupDailyAvgRevenue &gt; 0> <@ofbizCurrency amount=storePickupDailyAvgRevenue!0 rounding=0 isoCode=globalContext.defaultCurrencyUomId /><#else>${storePickupDailyAvgRevenue?default(0)}</#if></td>
            </tr>
            <tr class="even">
              <#if periodRecTrendRange?exists>
                <#assign rangeListperiodRec = Static["org.ofbiz.base.util.StringUtil"].split(periodRecTrendRange,"to")/>
              </#if>
                <#if rangeListperiodRec?has_content && rangeListperiodRec?size gt 1>
                  <#assign periodFrom = rangeListperiodRec.get(0)?trim/>
                  <#assign periodTo = rangeListperiodRec.get(1)?trim/>
                </#if>
                <#if rangeListperiodRec?has_content && rangeListperiodRec?size == 1>
                  <#assign periodFrom = rangeListperiodRec.get(0)?trim/>
                  <#assign periodTo = rangeListperiodRec.get(0)?trim/>
                </#if>
                <td class="boxCaption firstCol">${uiLabelMap.RecentTrendCaption}<span class="trendRange">
                   <#if periodRecTrendRange?exists>
                     (<a href="<@ofbizUrl>updatePeriodDashboardSummary?periodFrom=${periodFrom}&periodTo=${periodTo}</@ofbizUrl>">${periodRecTrendRange}</a>):
                   <#else>
                     ${uiLabelMap.NaCaption}
                   </#if>
                </span></td>
                <#assign recentTrendClass="noTrend">
                <#if storePickupRecentTrendRevenue?has_content>
                   <#if (storePickupRecentTrendRevenue < 0)>
                     <#assign recentTrendClass="negativeTrend">
                     <#assign recentTrendColorClass="negativeTrendColor">
                     <#assign storePickupRecentTrendRevenue=-(storePickupRecentTrendRevenue)>
                   <#elseif (storePickupRecentTrendRevenue > 0)>
                     <#assign recentTrendClass="positiveTrend">
                     <#assign recentTrendColorClass="positiveTrendColor">
                   </#if>
                </#if>
                <td class="boxDollar ${recentTrendClass!""}">${storePickupRecentTrendRevenue!"N/A"}<#if storePickupRecentTrendRevenue?has_content>%</#if></td>
                <td class="boxNumber">${storePickupRecentOrderCount?default('0')}</td>
                <td class="boxDollar lastRow lastCol ${recentTrendColorClass!""}"><#if storePickupRecentTrendTotalRevenue?has_content && storePickupRecentTrendTotalRevenue &gt; 0> <@ofbizCurrency amount=storePickupRecentTrendTotalRevenue!0 rounding=0 isoCode=globalContext.defaultCurrencyUomId /><#else>${storePickupRecentTrendTotalRevenue?default(0)}</#if></td>
            </tr>
            <tr class="odd">
              <#if periodPriorTrendRange?exists>
                <#assign rangeListperiodPrior = Static["org.ofbiz.base.util.StringUtil"].split(periodPriorTrendRange,"to")/>
              </#if>
                <#if rangeListperiodPrior?has_content && rangeListperiodPrior?size gt 1>
                  <#assign periodFrom = rangeListperiodPrior.get(0)?trim/>
                  <#assign periodTo = rangeListperiodPrior.get(1)?trim/>
                </#if>
                <#if rangeListperiodPrior?has_content && rangeListperiodPrior?size == 1>
                  <#assign periodFrom = rangeListperiodPrior.get(0)?trim/>
                  <#assign periodTo = rangeListperiodPrior.get(0)?trim/>
                </#if>
                <td class="boxCaption firstCol">${uiLabelMap.PriorTrendCaption}<span class="trendRange">
                   <#if periodPriorTrendRange?exists>
                     (<a href="<@ofbizUrl>updatePeriodDashboardSummary?periodFrom=${periodFrom}&periodTo=${periodTo}</@ofbizUrl>">${periodPriorTrendRange}</a>):
                   <#else>
                     ${uiLabelMap.NaCaption}
                   </#if>
                </span></td>
                <#assign priorTrendClass="noTrend">
                <#if storePickupPriorTrendRevenue?has_content>
                   <#if (storePickupPriorTrendRevenue < 0)>
                     <#assign priorTrendClass="negativeTrend">
                     <#assign priorTrendColorClass="negativeTrendColor">
                     <#assign storePickupPriorTrendRevenue=-(storePickupPriorTrendRevenue)>
                   <#elseif (storePickupPriorTrendRevenue > 0)>
                     <#assign priorTrendClass="positiveTrend">
                     <#assign priorTrendColorClass="positiveTrendColor">
                   </#if>
                </#if>
                <td class="boxDollar ${priorTrendClass!""}">${storePickupPriorTrendRevenue!"N/A"}<#if storePickupPriorTrendRevenue?has_content>%</#if></td>
                <td class="boxNumber">${storePickupPriorOrderCount?default('0')}</td>
                <td class="boxDollar lastRow lastCol ${priorTrendColorClass!""}"><#if storePickupPriorTrendTotalRevenue?has_content && storePickupPriorTrendTotalRevenue &gt; 0>  <@ofbizCurrency amount=storePickupPriorTrendTotalRevenue!0 rounding=0 isoCode=globalContext.defaultCurrencyUomId /><#else>${storePickupPriorTrendTotalRevenue?default(0)}</#if></td>
            </tr>
        </table>
    </div>
</div>
</#if>
<!-- end displayBox -->

