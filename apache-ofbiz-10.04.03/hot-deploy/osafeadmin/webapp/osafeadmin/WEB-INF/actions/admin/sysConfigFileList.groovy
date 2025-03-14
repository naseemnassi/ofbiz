package admin;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.*
import org.ofbiz.base.util.string.*;
import java.util.List;
import java.util.Map;

Map<String, Object> svcCtx = FastMap.newInstance();
userLogin = session.getAttribute("userLogin");
svcCtx.put("userLogin", userLogin);
svcRes = dispatcher.runSync("getSysConfigFiles", svcCtx);
configFileList = svcRes.sysConfigFileList;
fileList = FastList.newInstance();
if(UtilValidate.isNotEmpty(configFileList)) {
      for (configFile in configFileList) {
          sysConfigFile = FastMap.newInstance();
          sysConfigFile.put("fileName", configFile.getName());
          sysConfigFile.put("filePath", configFile);
          fileList.add(sysConfigFile);
      }
      context.configFileResultList = UtilMisc.sortMaps(fileList, UtilMisc.toList("fileName"));
}
