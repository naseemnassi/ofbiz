<script type="text/javascript">
    function setDateRange(dateFrom,dateTo) {
    var form = document.updatePeriodForm;
    form.elements['periodFrom'].value=dateFrom;
    form.elements['periodTo'].value=dateTo;
    form.submit();
    
    }
    function setCheckboxes(formName,checkBoxName) {
        // This would be clearer with camelCase variable names
        var allCheckbox = document.forms[formName].elements[checkBoxName + "all"];
        for(i = 0;i < document.forms[formName].elements.length;i++) {
            var elem = document.forms[formName].elements[i];
            if (elem.name.indexOf(checkBoxName) == 0 && elem.name.indexOf("_") < 0 && elem.type == "checkbox") {
                elem.checked = allCheckbox.checked;
            }
        }
    }
</script>

<!-- start displayBox -->
<div class="periodBox">
    <div class="boxBody period">
        <form action="<@ofbizUrl>${periodRequest}</@ofbizUrl>" method="post" name="updatePeriodForm">
         <input type="hidden" name="productStoreId" value="${globalContext.productStoreId!""}" />
         <input type="hidden" name="browseRootProductCategoryId" value="${globalContext.rootProductCategoryId!""}" />
         <input type="hidden" name="preferredDateFormat" value="${globalContext.preferredDateFormat!""}" />
         <input type="hidden" name="preferredDateTimeFormat" value="${globalContext.preferredDateTimeFormat!""}" />
         <div class="entryRow">
            <div class="entry daterange">
             <label>${uiLabelMap.PeriodCaption}</label>
             <div class="entryInput from">
                    <input class="dateEntry" type="text" id="" name="periodFrom" maxlength="40" value="${periodFrom!request.getParameter('periodFrom')!""}"/>
             </div>
             <label class="tolabel">${uiLabelMap.ToLabel}</label>
             <div class="entryInput to">
                    <input class="dateEntry" type="text" name="periodTo" maxlength="40" value="${periodTo!request.getParameter('periodTo')!""}"/>
             </div>
            </div>
            <div class="entryButtons">
                <input type="submit" class="standardBtn action" name="periodBtn" value="${uiLabelMap.SearchBtn}" />
                <#assign nowTimestamp=Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp()>
                <#assign yesterday=Static["org.ofbiz.base.util.UtilDateTime"].getDayStart(nowTimestamp, -1?int)/>
                <#assign weekStart=Static["org.ofbiz.base.util.UtilDateTime"].getWeekStart(nowTimestamp)>
                <#assign weekStart=Static["org.ofbiz.base.util.UtilDateTime"].addDaysToTimestamp(weekStart,-1?int)>
                <#assign monthStart=Static["org.ofbiz.base.util.UtilDateTime"].getMonthStart(nowTimestamp)>
                <#assign yearStart=Static["org.ofbiz.base.util.UtilDateTime"].getYearStart(nowTimestamp)>
                <#assign productionDate=Static["org.ofbiz.base.util.UtilProperties"].getPropertyValue('osafeAdmin','production-date')>
                <#if productionDate?has_content>
                   <#assign productDateStart=Static["org.ofbiz.base.util.UtilDateTime"].toTimestamp(productionDate)>
                <#else>
                   <#assign productDateStart=Static["org.ofbiz.base.util.UtilDateTime"].toTimestamp('06/02/2011 00:00:00')>
                </#if>
            </div>
	            <div class="dateSelectButtons">
	                <input type="button" class="standardBtn dateSelect" name="TodayBtn" value="${uiLabelMap.TodayBtn}" onClick="setDateRange('${nowTimestamp?string(preferredDateFormat)}','${nowTimestamp?string(preferredDateFormat)}');"/>
	                <input type="button" class="standardBtn dateSelect" name="YesterdayBtn" value="${uiLabelMap.YesterdayBtn}" onClick="setDateRange('${yesterday?string(preferredDateFormat)}','${yesterday?string(preferredDateFormat)}');"/>
					<input type="button" class="standardBtn dateSelect" name="ThisWeekBtn" value="${uiLabelMap.ThisWeekBtn}" onClick="setDateRange('${weekStart?string(preferredDateFormat)}','${nowTimestamp?string(preferredDateFormat)}');"/>
	                <input type="button" class="standardBtn dateSelect" name="MonthToDateBtn" value="${uiLabelMap.MonthToDateBtn}" onClick="setDateRange('${monthStart?string(preferredDateFormat)}','${nowTimestamp?string(preferredDateFormat)}');"/>
	                <input type="button" class="standardBtn dateSelect" name="YearToDateBtn" value="${uiLabelMap.YearToDateBtn}" onClick="setDateRange('${yearStart?string(preferredDateFormat)}','${nowTimestamp?string(preferredDateFormat)}');"/>
	                <input type="button" class="standardBtn dateSelect" name="DateAllBtn" value="${uiLabelMap.DateAllBtn}" onClick="setDateRange('${productDateStart?string(preferredDateFormat)}','${nowTimestamp?string(preferredDateFormat)}');"/>
	            </div>
        <#if showDeliveryOption?has_content && showDeliveryOption =="Y">
             <div class="entryRow">
             <div class="entry medium">
                 <label class="smallLabel">${uiLabelMap.DeliveryOptionCaption}</label>
                 <div class="entryInput checkbox medium">
                     <input type="checkbox" class="checkBoxEntry" name="srchall" id="srchall" value="Y" onclick="javascript:setCheckboxes('updatePeriodForm','srch')" <#if parameters.srchall?has_content>checked</#if> />${uiLabelMap.AllLabel}
                     <input type="checkbox" class="checkBoxEntry" name="srchShipTo" id="srchShipTo" value="Y" <#if parameters.srchShipTo?has_content>checked</#if>/>${uiLabelMap.ShipToDeliveryLabel}
                     <input type="checkbox" class="checkBoxEntry" name="srchStorePickup" id="srchStorePickup" value="Y" <#if parameters.srchStorePickup?has_content>checked</#if>/>${uiLabelMap.StorePickupDeliveryLabel}
                 </div>
             </div>
             </div>
        </#if> 
         </div>
        </form>
    </div>
</div>
<!-- end displayBox -->
