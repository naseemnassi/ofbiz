package common;

import javolution.util.FastMap;
import javolution.util.FastList;
import com.osafe.util.Util;
import com.osafe.services.OsafeManageXml;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;

XmlFilePath = FlexibleStringExpander.expandString(UtilProperties.getPropertyValue("osafe.properties", "ecommerce-UiSequence-xml-file"), context);
searchRestrictionMap = FastMap.newInstance();
searchRestrictionMap.put("screen", "Y");
uiSequenceSearchList =  OsafeManageXml.getSearchListFromXmlFile(XmlFilePath, searchRestrictionMap, uiSequenceScreen,true, false, true);

for(Map uiSequenceScreenMap : uiSequenceSearchList) {
     if ((uiSequenceScreenMap.value instanceof String) && (UtilValidate.isInteger(uiSequenceScreenMap.value))) {
         if (UtilValidate.isNotEmpty(uiSequenceScreenMap.value)) {
             uiSequenceScreenMap.value = Integer.parseInt(uiSequenceScreenMap.value);
         } else {
             uiSequenceScreenMap.value = 0;
         }
     }
 }
uiSequenceSearchList = UtilMisc.sortMaps(uiSequenceSearchList, UtilMisc.toList("value"));
context.divSequenceList = uiSequenceSearchList;