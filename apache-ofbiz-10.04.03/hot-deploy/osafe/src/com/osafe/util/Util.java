package com.osafe.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.Locale;

import javax.servlet.ServletRequest;

import org.ofbiz.product.product.ProductWorker;
import org.ofbiz.product.store.ProductStoreWorker;
import javolution.util.FastMap;
import javolution.util.FastList;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang.StringUtils;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.StringUtil.StringWrapper;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.ServiceUtil;
import org.w3c.tidy.Tidy;

import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.Currency;
import com.osafe.geo.OsafeGeo;

public class Util {

    public static final String module = Util.class.getName();
    private final static DecimalFormat DFFLOAT=new DecimalFormat("#######.00");
    
    public static String formatTelephone(String areaCode, String contactNumber) 
    {
        return formatTelephone(areaCode, contactNumber,null);
    }
    
    
    //If you update this method also update OsafeAdminUtil.formatTelephone.
    public static String formatTelephone(String areaCode, String contactNumber, String numberFormat) {
    	String sAreaCode="";
    	String sContactNumber="";
    	String sFullPhone="";
    	if (UtilValidate.isNotEmpty(areaCode)) 
    	{
    		sAreaCode=areaCode;
        }
    	if (UtilValidate.isNotEmpty(contactNumber)) 
    	{
            sContactNumber=contactNumber;
        }
        sFullPhone = sAreaCode + sContactNumber;
    	if(UtilValidate.isNotEmpty(numberFormat) && UtilValidate.isNotEmpty(sFullPhone))
    	{
            String sFullPhoneNum = sFullPhone.replaceAll("[^0-9]", "");
            //get count of how many digits in phone number
            int digitsCount =sFullPhoneNum.length();
            //get count of how many pounds in format
            String pounds = numberFormat.replaceAll("[^#]", "");
            int poundsCount = pounds.length();
            
            //if number of digits equal the number of pounds 
            if(digitsCount == poundsCount)
            {
            	for(int i=0; i<digitsCount; i++)
            	{
            		numberFormat=numberFormat.replaceFirst("[#]", "" + sFullPhoneNum.charAt(i));
            	}
            	sFullPhone=numberFormat;
            }
            else if(digitsCount < poundsCount)
            {
            	for(int i=0; i<digitsCount; i++)
            	{
            		numberFormat=numberFormat.replaceFirst("[#]", "" + sFullPhoneNum.charAt(i));
            	}
            	//remove all extra #'s
            	numberFormat=numberFormat.replaceAll("[#]", "");
            	sFullPhone=numberFormat;
            }
            else if(digitsCount > poundsCount)
            {
            	int i = 0;
            	for(i=0; i<poundsCount; i++)
            	{
            		numberFormat=numberFormat.replaceFirst("[#]", "" + sFullPhoneNum.charAt(i));
            	}
            	//add extra numbers to the end
            	numberFormat=numberFormat + sFullPhoneNum.substring(i);
            	sFullPhone=numberFormat;
            }
    	}
        return sFullPhone;
    }

    public static String filterNonAscii(String inString) {
        // from http://www.velocityreviews.com/forums/t140837-convert-utf-8-to-ascii.html
        // Create the encoder and decoder for the character encoding
        Charset charset = Charset.forName("US-ASCII");
        CharsetDecoder decoder = charset.newDecoder();
        CharsetEncoder encoder = charset.newEncoder();
        // This line is the key to removing "unmappable" characters.
        encoder.onUnmappableCharacter(CodingErrorAction.IGNORE);
        String result = inString;

        try {
            // Convert a string to bytes in a ByteBuffer
            ByteBuffer bbuf = encoder.encode(CharBuffer.wrap(inString));

            // Convert bytes in a ByteBuffer to a character ByteBuffer and then to a string.
            CharBuffer cbuf = decoder.decode(bbuf);
            result = cbuf.toString();
        } catch (CharacterCodingException cce) {
            String errorMessage = "Exception during character encoding/decoding: " + cce.getMessage();
            Debug.logError(cce, errorMessage, module);
        }

        return result;
    }

    public static String stripHTML(String content,int wrapLen) {
        
        if (content == null) {
            return "";
        }
        String cleanContent = content;//StringUtil.wrapString(content).toString();
        Tidy tidy = new Tidy();
        InputStream inStream = null;
        try {
            cleanContent = filterNonAscii(cleanContent);
            inStream = new ByteArrayInputStream(cleanContent.getBytes("UTF-8"));

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            if (inStream != null) {

                PrintWriter pw = new PrintWriter(new StringWriter());
                tidy.setWraplen(wrapLen);
                tidy.setErrout(pw);
                tidy.setShowWarnings(false);
                tidy.setMakeClean(true);
                tidy.parse(inStream, outStream);
                if (outStream != null) {
                    cleanContent = outStream.toString("UTF-8");
                    cleanContent = cleanContent.replaceAll("\\<.*?>", "");
                    String[] split = StringUtils.split(cleanContent, "\n\r");
                    cleanContent  = StringUtils.join(split," ");
                }

            }
        } catch (UnsupportedEncodingException e) {
            Debug.logError(e, e.getMessage(), module);
        }
        return cleanContent;
    }
    
    
    public static String stripHTML(String content) {
        return stripHTML(content,800);
    }

    public static String xmlEncode(String conent) {
        String retString = conent;
        // retString = StringUtil.replaceString(retString, "&", "&amp;");
        retString = StringUtil.replaceString(retString, "<", "&lt;");
        retString = StringUtil.replaceString(retString, ">", "&gt;");
        retString = StringUtil.replaceString(retString, "\"", "&quot;");
        retString = StringUtil.replaceString(retString, "'", "&apos;");
        return retString;
    }

    public static boolean isZipCode(String zipCode) {
        boolean isValid = false;

        if (UtilValidate.isNotEmpty(zipCode) && zipCode.length() == 5 && isNumber(zipCode)) {
            isValid = true;//UtilValidate.isZipCode(zipCode);
        }
        return isValid;
    }
    
    public static String checkTelecomNumber(String areaCode, String contactNumber, String required) {
        return checkTelecomNumber(areaCode, contactNumber, null, required);
    }

    public static String checkTelecomNumber(String areaCode, String contactNumber, String extension, String required) {

        if (Boolean.parseBoolean(required) || "Y".equalsIgnoreCase(required)) {
            if (UtilValidate.isEmpty(areaCode) || UtilValidate.isEmpty(contactNumber)) {
                return "missing";
            }
        }

        if (UtilValidate.isNotEmpty(areaCode)) {
            String justNumbers = StringUtil.removeRegex(areaCode, "[\\s-]");
            if (!UtilValidate.isInteger(justNumbers)) {
                return "invalid";
            } else if (justNumbers.length() < 3) {
                return "invalid";
            }

        }
        if (UtilValidate.isNotEmpty(contactNumber)) {
            String justNumbers = StringUtil.removeRegex(contactNumber, "[\\s-]");
            if (!UtilValidate.isInteger(justNumbers)) {
                return "invalid";
            } else if (justNumbers.length() < 7) {
                return "invalid";
            }
        }
        if (UtilValidate.isNotEmpty(extension)) {
            String justNumbers = StringUtil.removeRegex(extension, "[\\s-]");
            if (!UtilValidate.isInteger(justNumbers)) {
                return "invalid";
            }
        }

        return "success";
    }

    @Deprecated
    public static boolean isDateTime(String dateStr) {
        String entryDateFormat = UtilProperties.getPropertyValue("osafeAdmin.properties", "entry-date-format");
        return isDateTime(dateStr, entryDateFormat);
    }

    public static boolean isDateTime(String dateStr, String format) {
        if (UtilValidate.isEmpty(dateStr) || UtilValidate.isEmpty(format) ) {
            return false;
        }
        boolean isValid = false;

        try {
            Object convertedDate = ObjectType.simpleTypeConvert(dateStr, "Timestamp", format, null);
            if (convertedDate != null) {
                isValid = true;
            }
        } catch (GeneralException e) {
            isValid = false;
        }
        return isValid;
    }

    @Deprecated
    public static String checkDateRange(String fromDate, String toDate) {
        String entryDateFormat = UtilProperties.getPropertyValue("osafeAdmin.properties", "entry-date-format");
        return checkDateRange(fromDate, toDate, entryDateFormat);
    }

    public static String checkDateRange(String fromDate, String toDate, String format) {

        if (UtilValidate.isEmpty(format) || !isDateTime(fromDate, format) || !isDateTime(toDate, format)) {
            return "invalidFormat";
        }

        try {
            Timestamp convertedFromDate = (Timestamp) ObjectType.simpleTypeConvert(fromDate, "Timestamp", format, null);
            Timestamp convertedToDate = (Timestamp) ObjectType.simpleTypeConvert(toDate, "Timestamp", format, null);
            if (convertedToDate.before(convertedFromDate)) {
                return "invalidRange";
            }
        } catch (GeneralException e) {
            Debug.logError(e, module);
            return "error";
        }

        return "success";
    }

    @Deprecated
    public static java.sql.Timestamp toTimestamp(String dateStr) {
        String entryDateFormat = UtilProperties.getPropertyValue("osafeAdmin.properties", "entry-date-format");
        return toTimestamp(dateStr, entryDateFormat);
    }

    public static java.sql.Timestamp toTimestamp(String dateStr, String format) {
        if (UtilValidate.isEmpty(dateStr) || UtilValidate.isEmpty(format) ) {
            return null;
        }
        try {
            return (Timestamp) ObjectType.simpleTypeConvert(dateStr, "Timestamp", format, null);
        } catch (GeneralException e) {
            Debug.logError(e, module);
            return null;
        }
    }

    public static Map<String, Object> getCountryGeoInfo(Delegator delegator, String geoId) {
        GenericValue geo = null;
        Map<String, Object> result = FastMap.newInstance();
        try {
            Debug.logInfo("geoId: " + geoId, module);

            geo = EntityUtil.getFirst((delegator.findByAnd("Geo", UtilMisc.toMap("geoId", geoId.toUpperCase(),"geoTypeId","COUNTRY"))));
            Debug.logInfo("Found a geo entity " + geo, module);
            if (UtilValidate.isNotEmpty(geo)) {
                result.put("geoId", (String) geo.get("geoId"));
                result.put("geoName", (String) geo.get("geoName"));
            }
        } catch (Exception e) {
            String errMsg = "Failed to find/setup geo id";
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        return result;
    }

    /**
     *return the parmValue of given parmKey.
     *@param productStoreId 
     *@param pramKey
     *@return String of the pramKey
     */

    public static String getProductStoreParm(String productStoreId, String parmKey) {
        if (UtilValidate.isEmpty(parmKey) || UtilValidate.isEmpty(productStoreId)) {
            return null;
        }
        return getProductStoreParm(DelegatorFactory.getDelegator(null), productStoreId, parmKey);
    }

    /**
     *return the parmValue of given parmKey.
     *
     *@param request
     *@param pramKey
     *@return String of the pramKey
     */
    public static String getProductStoreParm(ServletRequest request, String parmKey) {
        if (UtilValidate.isEmpty(parmKey)) {
            return null;
        }
        return getProductStoreParm((Delegator)request.getAttribute("delegator"), ProductStoreWorker.getProductStoreId(request), parmKey);
    }

    /**
     *return the parmValue of given parmKey.
     *
     *@param Delegator
     *@param productStoreId 
     *@param pramKey
     *@return String of the pramKey
     */

    public static String getProductStoreParm(Delegator delegator, String productStoreId, String parmKey) {
        if (productStoreId == null) 
        {
            return null;
        }
        String parmValue = null;
        GenericValue xProductStoreParam = null;
        try {
            xProductStoreParam = delegator.findOne("XProductStoreParm", UtilMisc.toMap("productStoreId", productStoreId, "parmKey", parmKey), false);
            if (UtilValidate.isNotEmpty(xProductStoreParam)) {
                parmValue = xProductStoreParam.getString("parmValue");
            }
        } catch (Exception e) {
            Debug.logError(e, e.getMessage(), module);
        }
        return parmValue;
    }

    /**
     *return the parmValue of given parmKey.
     *
     *@param pramKey
     *@return String of the pramKey
     */
     @Deprecated
    public static String getProductStoreParm(String parmKey) {
        if (UtilValidate.isEmpty(parmKey)) {
            return null;
        }
        String productStoreId = null;
        Delegator delegator = DelegatorFactory.getDelegator(null);
        try {
            List<GenericValue> productStoreList = delegator.findList("ProductStore",null,null,null,null,true);
            if (UtilValidate.isNotEmpty(productStoreList)) {
                GenericValue productStore = EntityUtil.getFirst(productStoreList);
                productStoreId = productStore.getString("productStoreId");
            }
        } catch (Exception e) {
            Debug.logError(e, "Problem getting Product Store", module);
        }
        if (UtilValidate.isEmpty(productStoreId)) {
            return null;
        }
        return getProductStoreParm(delegator, productStoreId, parmKey);
    }

    public static Map getProductStoreParmMap(Delegator delegator, String webSiteId, String productStoreId) {
        Map mProductStoreParm = FastMap.newInstance();
        if (UtilValidate.isNotEmpty(webSiteId)  || UtilValidate.isNotEmpty(productStoreId)) 
        {
            try 
            {
                String storeId=null;
            
                if (UtilValidate.isEmpty(productStoreId))
                {
                    GenericValue webSite = delegator.findByPrimaryKeyCache("WebSite", UtilMisc.toMap("webSiteId", webSiteId));
                    storeId=webSite.getString("productStoreId");
                }
                else
                {
                    storeId=productStoreId;
                }
                if (UtilValidate.isNotEmpty(storeId))
                {
                    List lProductStoreParam = delegator.findByAnd("XProductStoreParm", UtilMisc.toMap("productStoreId", storeId));
                    if (UtilValidate.isNotEmpty(lProductStoreParam))
                    {
                          Iterator parmIter = lProductStoreParam.iterator();
                           while (parmIter.hasNext()) 
                           {
                                GenericValue prodStoreParm = (GenericValue) parmIter.next();
                                mProductStoreParm.put(prodStoreParm.getString("parmKey"),prodStoreParm.getString("parmValue"));
                           }
                    }
                    
                }
                
            } 
             catch (Exception e) {
                Debug.logError(e, e.getMessage(), module);
            }
        }
        return mProductStoreParm;
    }

    public static Map getProductStoreParmMap(ServletRequest request) {
        return getProductStoreParmMap((Delegator)request.getAttribute("delegator"), null, ProductStoreWorker.getProductStoreId(request));
    }

    /**
     * Return a string formatted as format 
     * if format is wrong or null return dateString for the default locale
     * @param time Stamp
     * @param string date format
     * @return return String formatted for given date with given format
     */
    public static String convertDateTimeFormat(Timestamp timestamp, String format) {
        String dateString ="";
        if (UtilValidate.isEmpty(timestamp)) 
        {
            return "";
        }
        try {
            dateString = UtilDateTime.toDateString(new Date(timestamp.getTime()), format);
        } catch (Exception e) {
            dateString = UtilDateTime.toDateString(new Date(timestamp.getTime()), null);
        }
        return dateString;
    }

    public static boolean isValidDateFormat(String format) {
        if (UtilValidate.isEmpty(format)) {
            return false;
        }
        try {
            UtilDateTime.toDateString(new Date(), format);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean isProductStoreParmTrue(String parmValue) {
        if (UtilValidate.isEmpty(parmValue)) 
         {
             return false;
         }
         if ("TRUE".equals(parmValue.toUpperCase()))
         {
             return true;
         }
         return false;
     }
    
    public static boolean isNumber(String number) {
        if (UtilValidate.isEmpty(number)) {
            return false;
        }
        return UtilValidate.isInteger(number);
    }

    public static String checkPasswordStrength(ServletRequest request, String password) {

        int pwdLength = 6;//Need to confirm this value.
        int minDigit = 0;
        int minUpCase = 0;

        String pwdLenStr = getProductStoreParm((Delegator)request.getAttribute("delegator"), ProductStoreWorker.getProductStoreId(request), "REG_PWD_MIN_CHAR");
        String minDigitStr =  getProductStoreParm((Delegator)request.getAttribute("delegator"), ProductStoreWorker.getProductStoreId(request), "REG_PWD_MIN_NUM");
        String minUpCaseStr = getProductStoreParm((Delegator)request.getAttribute("delegator"), ProductStoreWorker.getProductStoreId(request), "REG_PWD_MIN_UPPER");

        if (isNumber(pwdLenStr)  && (Integer.parseInt(pwdLenStr) > 0)) {
             pwdLength = Integer.parseInt(pwdLenStr);
        }
        if (isNumber(minDigitStr)) {
            minDigit = Integer.parseInt(minDigitStr);
        }
        if (isNumber(minUpCaseStr)) {
            minUpCase = Integer.parseInt(minUpCaseStr);
        }

        return passPattern(password, pwdLength, minDigit, minUpCase);
    }

    public static String passPattern(String password, int passwordLength, int minDigit, int minUpperCase) {
        if (passwordLength > 0) {
            String digitMsgStr = "digits";
            String upperCaseMsgStr = "letters";
        String errormessage = UtilProperties.getMessage("OSafeUiLabels", "PasswordMinLengthError", UtilMisc.toMap("passwordLength", passwordLength), Locale.getDefault());
        if (minDigit > 0) {
            if (minDigit == 1) {
                digitMsgStr = "digit";
                
            }
            errormessage = errormessage+" "+UtilProperties.getMessage("OSafeUiLabels", "PasswordDigitError", UtilMisc.toMap("minDigit", (Integer)minDigit, "digitMsgStr", digitMsgStr), Locale.getDefault());
        }
        
        if (minUpperCase == 1) {
            upperCaseMsgStr = "letter";
        }
        if (minDigit > 0 && minUpperCase > 0) {
            errormessage = errormessage+" and "+UtilProperties.getMessage("OSafeUiLabels", "PasswordUpperCaseError", UtilMisc.toMap("minUpperCase", (Integer) minUpperCase, "upperCaseMsgStr", upperCaseMsgStr), Locale.getDefault());
        } else if (minDigit == 0 && minUpperCase > 0) {
            errormessage = errormessage+" "+UtilProperties.getMessage("OSafeUiLabels", "PasswordWithNoDigitUpperCaseError", UtilMisc.toMap("minUpperCase", (Integer) minUpperCase, "upperCaseMsgStr", upperCaseMsgStr), Locale.getDefault());
        }
        
        if (!(password.length() >= passwordLength)) {
            return errormessage;
        } else {
            char[] passwordChars = password.toCharArray();
            int digitCount = 0;
            int upperCount = 0;
            for (char passwordChar: passwordChars) {
                if (Character.isDigit(passwordChar)) {
                    digitCount = digitCount + 1;
                } else if (Character.isUpperCase(passwordChar)) {
                    upperCount = upperCount + 1;
                }
            }
            if (!(digitCount >= minDigit) || !(upperCount >= minUpperCase)) {
                return errormessage;
            }
        }
        }
        return "success";
    }

    public static double distFrom(OsafeGeo from, OsafeGeo thru, String uom) {
        if (from.isEmpty() || thru.isEmpty() || UtilValidate.isEmpty(uom)) {
            return 0;
        }
        double earthRadius = 0;
        if(uom.equalsIgnoreCase("Kilometers")) {
            earthRadius = 6371;
        } else if (uom.equalsIgnoreCase("Miles")) {
            earthRadius = 3959;
        }
        
        double diffLat = Math.toRadians(Double.valueOf(thru.latitude()) - Double.valueOf(from.latitude()));
        double diffLong = Math.toRadians(Double.valueOf(thru.longitude()) - Double.valueOf(from.longitude()));
        double a = Math.sin(diffLat/2) * Math.sin(diffLat/2) +
                   Math.cos(Math.toRadians(Double.valueOf(from.latitude()))) * Math.cos(Math.toRadians(Double.valueOf(thru.latitude()))) *
                   Math.sin(diffLong/2) * Math.sin(diffLong/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double dist = earthRadius * c;

        return dist;
    }
    
    /** Formats a double into a properly currency symbol string based on isoCode and Locale
     * @param isoCode the currency ISO code
     * @param locale The Locale used to format the number
     * @return A String with the currency symbol
     */
    public static String showCurrency(String isoCode, Locale locale) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(locale);
        if (isoCode != null && isoCode.length() > 1) {
            nf.setCurrency(Currency.getInstance(isoCode));
        }
        return nf.getCurrency().getSymbol(locale);
    } 
    
    /** String with in the given limit
     * @param String that need to refector
     * @param Int length
     * @return A String with in the given limit
     */
    public static String getStringInLength(String str, int length) {
        String strInLength = null;
        if (UtilValidate.isNotEmpty(str))
        { 
            int leng = length - 1;
            if (str.length() > leng)
            {
                if (str.charAt(leng) == ' ')
                {
                    strInLength = str.substring(0, leng);
                }
                else
                {
                    String strWithHalfWord = str.substring(0, leng);
                    try 
                    {
                        strInLength = strWithHalfWord.substring(0, strWithHalfWord.lastIndexOf(" "));
                    }
                    catch (Exception e)
                    {
                        strInLength = strWithHalfWord;
                        Debug.logError(e, e.getMessage(), module);
                    }
                }
            }
            else
            {
                strInLength = str;
            }
        }
        return strInLength;
    }

    public static String stripHTMLInLength(String content) {
        return stripHTMLInLength(content, "800");
    }

    public static String stripHTMLInLength(String content, String length) {
        int maxLength = 800;
        if (isNumber(length)) {
            maxLength = Integer.parseInt(length);
        }
        String stripHTMLStr = stripHTML(content);
        return getStringInLength(stripHTMLStr, maxLength);
    }
    
    /**
     * Method to format text. 
     * @param textContent
     * @return formatted String
     */
    public static StringWrapper getFormattedText(String textContent){
        if (textContent == null) return null;
        String formattedText = textContent.replaceAll("(\r\n|\r|\n|\n\r)", "<br>");
        return StringUtil.wrapString(formattedText);
    }
    
    
    public static double convert(double dOpenAmount,int decimals)  {
        double dValue=0;
        try {

            double dPow=0;
            dPow=Math.pow(10,decimals);
            dValue=Double.parseDouble(""+Double.parseDouble("" + dOpenAmount)*dPow);
            long lValue=(long)dValue;
             double dDiff=dValue-lValue;
             //Added rounding of the diff to correctly calculate
             //numbers that were coming out as .49999.
             //example fValue = 72.615
             //dValue would = 7261.4999999
             if (Math.abs(Math.round(dDiff*100)) >=50)  {
               if (lValue>=0)
                 lValue+=1;
               else
                 lValue-=1;
             }
            dValue=lValue/dPow;
            synchronized(DFFLOAT)
            {
              dValue=DFFLOAT.parse(""+dValue).doubleValue();
            }

}
          catch (Exception e) {

          }
        return dValue;
    }
    
    /** Returns true if single String subString is contained within string s. */
    public static boolean isSubString(String subString, String s) {
        return (s.indexOf(subString) != -1); 
    }

    
}

