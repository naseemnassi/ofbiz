<#if mode?has_content>
  <#assign statusId = "CTNT_PUBLISHED" />
    <#if pixelTrack?has_content>
        <#assign pixelId = pixelTrack.pixelId!"" />
        <#assign pixelScope = pixelTrack.pixelScope!"" />
        <#assign description = pixelTrack.description!"" />
        <#if pixelContent?has_content>
            <#assign contentId = pixelContent.contentId?if_exists />
            <#assign statusId = pixelContent.statusId!"CTNT_DEACTIVATED" />
            <#if statusId != "CTNT_PUBLISHED">
                <#assign statusId = "CTNT_DEACTIVATED">
            </#if>
            <input type="hidden" name="dataResourceId" value=${pixelContent.dataResourceId!""} />
            <#assign createdDate = pixelContent.createdDate!"" />
            <#assign lastModifiedDate = pixelContent.lastModifiedDate!"" />
        </#if>
    </#if>
    <#assign statusItem = delegator.findOne("StatusItem", {"statusId" : statusId}, false) />
    <#assign statusDesc = statusItem.description!statusItem.get("description",locale)!statusItem.statusId /> 
       <input type="hidden" name="statusId" id="statusId" value="${statusId!""}" />
       <#if !(createdDate?has_content)>
           <#assign createdDate = Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp() />
       </#if>
       <input type="hidden" name="createdDate" value="${createdDate}" />
       <div class="infoRow">
       <#-- ==== Content ID === -->
         <div class="infoEntry">
           <div class="infoCaption"><label>${uiLabelMap.PixelIdCaption}</label></div>
           <#-- ===== Content ID ==== -->
           <div class="infoValue">
             <#if mode?has_content && mode == "add">
               <input name="pixelId" type="text" id="pixelId" value="${parameters.pixelId?default("")}"/>
             <#elseif mode?has_content && mode == "edit">
               <input type="hidden" name="pixelId" value="${pixelId!""}" />${pixelId!""}
               <input type="hidden" name="contentId" value="${contentId!""}"/>
             </#if>
               </div>
         </div>
        <#-- ==== Spot Name === -->
          <div class="infoRow">
            <div class="infoEntry">
              <div class="infoCaption"><label>${uiLabelMap.PixelScopeCaption}</label></div>
                <#-- ===== Spot Name ==== -->
              <div class="infoValue">
                <select name="pixelScope" id="pixelScope">
                  <option value=""<#if !pixelScope?has_content> selected=selected</#if>>${uiLabelMap.SelectOneLabel}</option>
                  <option value="ALL"<#if parameters.pixelScope?default(pixelScope!"") == "ALL"> selected=selected</#if>>${uiLabelMap.AllLabel}</option>
                  <option value="ALL_EXCEPT_ORDER_CONFIRM"<#if parameters.pixelScope?default(pixelScope!"") == "ALL_EXCEPT_ORDER_CONFIRM"> selected=selected</#if>>${uiLabelMap.AllExceptOrderConfirmLabel}</option>
                  <option value="ORDER_CONFIRM"<#if parameters.pixelScope?default(pixelScope!"") == "ORDER_CONFIRM"> selected=selected</#if>>${uiLabelMap.OrderConfirmLabel}</option>
                </select>
              </div>
            </div>
          </div>
        <#-- ==== Spot Description === -->
        <div class="infoRow">
          <div class="infoEntry long">
            <div class="infoCaption"><label>${uiLabelMap.DescriptionCaption}</label></div>
            <#-- ===== Spot Description ==== -->
            <div class="infoValue">
              <textarea class="smallArea characterLimit" name="description" cols="50" rows="1" maxlength="255">${parameters.description!description!""}</textarea>
              <span class="textCounter"></span>
            </div>
          </div>
        </div>
        <#-- ====== Spot Content ==== -->
        <div class="infoRow">
            <div class="infoEntry long">
                <div class="infoCaption"><label>${uiLabelMap.PixelCodeCaption}</label></div>
                    <div class="infoValue">
                         <textarea class="largeArea" name="textData" cols="50" rows="5">${parameters.textData!eText!""}</textarea>
                    </div>
            </div>
        </div>
        <#-- ====== Created Date ==== -->
        <div class="infoRow">
            <div class="infoEntry">
                <div class="infoCaption"><label>${uiLabelMap.CreatedDateCaption}</label></div>
                <div class="infoValue">${(Static["com.osafe.util.OsafeAdminUtil"].convertDateTimeFormat(createdDate, preferredDateTimeFormat).toLowerCase())!"N/A"}</div>
            </div>
        </div>
        <#-- ===== Status Buttons ====== -->
        <div class="infoRow">
            <div class="infoEntry long">
                <div class="infoCaption">
                    <label>${uiLabelMap.StatusCaption}</label>
                </div>
                <div class="infoValue statusItem">
                    <span id="contentStatus">${statusDesc!""}</span>
                </div>
                <div class="statusButtons">
                    <#if statusId != "CTNT_PUBLISHED">
                        <input id="contentStatusBtn" type="button" class="standardBtn secondary" name="approveBtn" value="${uiLabelMap.ContentMakeActive}" idValue="CTNT_PUBLISHED" onClick="updateStatusBtn('${uiLabelMap.ContentMakeActive}', '${uiLabelMap.ContentSetInactive}', document.${detailFormName!""}, 'contentStatus', 'contentStatusBtn');"/>
                    <#else>
                        <input id="contentStatusBtn" type="button" class="standardBtn secondary" name="approveBtn" value="${uiLabelMap.ContentSetInactive}" onClick="updateStatusBtn('${uiLabelMap.ContentMakeActive}', '${uiLabelMap.ContentSetInactive}', document.${detailFormName!""}, 'contentStatus', 'contentStatusBtn');"/>
                    </#if>
               </div>
            </div>
        </div>

        <#-- ====== Active Date ==== -->
        <div class="infoRow">
            <div class="infoEntry">
                <div class="infoCaption">
                    <label>${uiLabelMap.ActiveDateCaption}</label>
                </div>
                <div class="infoValue">
                    <#if statusId == "CTNT_PUBLISHED" >
                        <#if lastModifiedDate?has_content>
                            ${(Static["com.osafe.util.OsafeAdminUtil"].convertDateTimeFormat(lastModifiedDate, preferredDateTimeFormat).toLowerCase())!"N/A"}
                        </#if>
                    </#if>
                </div>
            </div>
        </div>
    
<#else>
    ${uiLabelMap.NoDataAvailableInfo}
</#if>
