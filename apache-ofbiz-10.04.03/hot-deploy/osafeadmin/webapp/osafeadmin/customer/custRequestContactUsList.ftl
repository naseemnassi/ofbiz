<!-- start listBox -->
<thead>
  <tr class="heading">
    <th class="idCol firstCol">${uiLabelMap.IdLabel}</th>
    <th class="nameCol">${uiLabelMap.LastNameLabel}</th>
    <th class="nameCol">${uiLabelMap.FirstNameLabel}</th>
    <th class="descCol">${uiLabelMap.ContactReasonLabel}</th>
    <th class="addrCol">${uiLabelMap.EmailLabel}</th>
    <th class="dateCol">${uiLabelMap.RequestDateLabel}</th>
    <th class="statusCol">${uiLabelMap.ExportStatusLabel}</th>
    <th class="actionCol"></th>
  </tr>
</thead>
  <#if resultList?exists && resultList?has_content>
    <#assign rowClass = "1"/>
    
    <#list resultList as contactUs>
      <#assign hasNext = contactUs_has_next>
      <tr class="dataRow <#if rowClass?if_exists == "2">even<#else>odd</#if>">
        <td class="idCol <#if !contactUs_has_next?if_exists>lastRow</#if> firstCol" ><a href="<@ofbizUrl>custRequestContactUsDetail?custReqId=${contactUs.custRequestId?if_exists}</@ofbizUrl>">${contactUs.custRequestId?if_exists}</a></td>
        <#assign custReqAttributeList = delegator.findByAnd("CustRequestAttribute",Static["org.ofbiz.base.util.UtilMisc"].toMap("custRequestId", contactUs.custRequestId))>
        <#assign comment =""/>
        <#if custReqAttributeList?exists && custReqAttributeList?has_content>
          <#list custReqAttributeList as custReqAttribute>
            <#if custReqAttribute.attrName == 'LAST_NAME'>
              <#assign lname = custReqAttribute.attrValue!""/>
            </#if>
            <#if custReqAttribute.attrName == 'FIRST_NAME'>
              <#assign fname = custReqAttribute.attrValue!""/>
            </#if>
            <#if custReqAttribute.attrName == 'EMAIL_ADDRESS'>
              <#assign email = custReqAttribute.attrValue!""/>
            </#if>
            <#if custReqAttribute.attrName == 'REASON_FOR_CONTACT'>
              <#assign contactUsReason = custReqAttribute.attrValue!""/>
            </#if>
            <#if custReqAttribute.attrName == 'IS_DOWNLOADED'>
              <#assign exported = custReqAttribute.attrValue!""/>
            </#if>
            <#if custReqAttribute.attrName == 'COMMENT'>
              <#assign comment = custReqAttribute.attrValue!""/>
            </#if>
          </#list>
        </#if>
        <td class="nameCol">
          ${lname!}
        </td>
        <td class="nameCol">
          ${fname!}
        </td>
        <td class="descCol">
          ${contactUsReason!}
        </td>
        <td class="addrCol">
          ${email!}
        </td>
        <td class="dateCol">
          ${(Static["com.osafe.util.OsafeAdminUtil"].convertDateTimeFormat(contactUs.createdDate, preferredDateFormat).toLowerCase())!"N/A"}
        </td>
        <td class="statusCol">
          <#if exported == 'Y'>
            ${uiLabelMap.ExportStatusInfo}
          <#else>
            ${uiLabelMap.DownloadNewInfo}
          </#if>
        </td>
        <td class="actionCol">
          <#if comment != ''>
              <#assign comment = Static["com.osafe.util.OsafeAdminUtil"].formatToolTipText(comment, ADM_TOOLTIP_MAX_CHAR!)/>
              <a href="javascript:void(0);" onMouseover="showTooltip(event,'${comment!""}');" onMouseout="hideTooltip()"><span class="descIcon"></span></a>
          </#if>
        </td>
      </tr>
      <#if rowClass == "2">
        <#assign rowClass = "1">
      <#else>
        <#assign rowClass = "2">
      </#if>
    </#list>
  </#if>
<!-- end listBox -->