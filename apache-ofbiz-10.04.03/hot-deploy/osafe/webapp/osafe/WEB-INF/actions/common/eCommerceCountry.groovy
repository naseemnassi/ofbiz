import javolution.util.FastList;
import org.ofbiz.base.util.StringUtil;
import com.osafe.util.Util;
import org.ofbiz.common.CommonWorkers;
import org.ofbiz.base.util.*;

defaultCountry = globalContext.get("COUNTRY_DEFAULT");
countryDropDown = globalContext.get("COUNTRY_DROPDOWN");
defaultCountryGeoMap = [:];
List countryList = FastList.newInstance();

if (UtilValidate.isNotEmpty(defaultCountry))
 {
  defaultCountryGeoMap = Util.getCountryGeoInfo(delegator, defaultCountry.trim());
  if (UtilValidate.isNotEmpty(defaultCountryGeoMap))
   {
      context.defaultCountryGeoMap = defaultCountryGeoMap;
   }
 }

if (Util.isProductStoreParmTrue(globalContext.get("COUNTRY_MULTI"))) 
 {
    if(countryDropDown.equals("All")) 
      {
          countryList = CommonWorkers.getCountryList(delegator);
      }
    else
    {
	  countryGeoCodeList = StringUtil.split(countryDropDown,",")
	  for (List geoId: countryGeoCodeList) 
	   {
          countryGeoId = geoId;
          countryGeoMap = Util.getCountryGeoInfo(delegator, countryGeoId.trim());
          if(UtilValidate.isNotEmpty(countryGeoMap))
          {
            countryList.add(countryGeoMap);
          }
	   }
     }
 }

if (UtilValidate.isEmpty(countryList))
 {
    countryList = CommonWorkers.getCountryList(delegator);
 }
if (UtilValidate.isEmpty(defaultCountryGeoMap))
{
   defaultCountryGeoMap.put("geoId","USA");
   context.defaultCountryGeoMap = defaultCountryGeoMap;
}
context.countryList = countryList;