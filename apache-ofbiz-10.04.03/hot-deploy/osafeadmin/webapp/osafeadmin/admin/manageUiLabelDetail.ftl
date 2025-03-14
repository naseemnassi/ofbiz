<#if mode?has_content>
  <#if uiLabelEntry?has_content>
    <#assign key = uiLabelEntry.key?if_exists />
    <#assign description = uiLabelEntry.description!"" />
    <#assign category = uiLabelEntry.category!"" />
    <#assign value = uiLabelEntry.value!"" />
  </#if>
    
        <div class="infoRow">
        <#-- ==== Spot Name === -->
            <div class="infoEntry">
                <div class="infoCaption"><label>${uiLabelMap.LabelCaption}</label></div>
                <#-- ===== Spot Name ==== -->
                <div class="infoValue">
                  <#if mode?has_content && mode == "add">
                    <input type="text" name="key" value="${parameters.key!key!""}" />
                  <#elseif mode?has_content && mode == "edit">
                    <input type="hidden" name="key" value="${key!""}" />${key!""}
                  </#if>
               </div>
            </div>
        </div>
        <div class="infoRow">
        <#-- ==== Category === -->
            <div class="infoEntry">
                <div class="infoCaption"><label>${uiLabelMap.CategoryCaption}</label></div>
                <div class="infoValue">
                  <#if mode?has_content && mode == "add">
                    <input type="text" name="category" value="${parameters.category!category!""}" />
                  <#elseif mode?has_content && mode == "edit">
                    <input type="hidden" name="category" value="${category!""}" />${category!""}
                  </#if>
               </div>
            </div>
        </div>
        <div class="infoRow">
        <#-- ==== Description === -->
            <div class="infoEntry">
                <div class="infoCaption"><label>${uiLabelMap.DescriptionCaption}</label></div>
                <#-- ===== Spot Name ==== -->
               <div class="infoValue">
                 <textarea class="smallArea" name="description" cols="50" rows="1">${parameters.description!description!""}</textarea>
               </div>
            </div>
        </div>
        <#-- ====== Value ==== -->
        <div class="infoRow">
            <div class="infoEntry long">
                <div class="infoCaption"><label>${uiLabelMap.CaptionCaption}</label></div>
                <div class="infoValue">
                    <textarea class="smallArea" name="value" cols="50" rows="5">${parameters.value!value!""}</textarea>
                </div>
            </div>
        </div>
<#else>
    ${uiLabelMap.NoDataAvailableInfo}
</#if>
