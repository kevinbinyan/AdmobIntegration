package com.zdk.wrap.mg.android;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.zdk.wrap.mg.ServerUtil;
import com.zdk.wrap.mg.android.ManifestModifier;
import com.zdk.wrap.mg.test.CommandLine;

public class MGSettingsModifier {
	private static boolean developerDebugMode = true;

	public static void modifyGDSettings(String gdSettingsFilename, String optMGApplicationID,
			String optMGApplicationVersion, List<String> optMGLogging) throws IOException {

		File gdSettings = new File(gdSettingsFilename);
		final String encoding = "UTF-8";
		String jsonText;
		JSONObject root;
		if (gdSettings.exists()) {
			jsonText = ServerUtil.readTextFile(gdSettings, encoding);
			root = new JSONObject(jsonText);
		} else {
			root = new JSONObject();
		}
		// MGLibraryMode
		if (developerDebugMode || optMGLogging != null) {
			root.put("MGLibraryMode", "MGEnterprise");
			JSONArray loggerCfg = new JSONArray();
			// MGApplicationID
			if (ManifestModifier.appInfo.getPackageName() != null)
				root.put("MGApplicationID", ManifestModifier.appInfo.getPackageName());
			else
				root.put("MGApplicationID", optMGApplicationID);
			// MGApplicationVersion
			if (ManifestModifier.appInfo.getOptVersionName() != null)
				root.put("MGApplicationVersion", ManifestModifier.appInfo.getOptVersionName());
			else
				root.put("MGApplicationVersion", optMGApplicationVersion);
			root.put("MGPackageName", ManifestModifier.appInfo.getPackageName());
			root.put("MGActivityName", ManifestModifier.appInfo.getOptMainActivityName());
			// MGConsoleLogger
			root.put("MGConsoleLogger", loggerCfg);
			//如果添加到安全桌面 otaServer设置为空；
			if(CommandLine.addToSecDeskTop == true)
			{
				root.put("otaServer", "");
			}
			else
			{
				root.put("otaServer", PathAndFileConfig.otaServer);
			}
			root.put("otaScheme", PathAndFileConfig.otaScheme);
			// MGFilterNone
			if (developerDebugMode || (optMGLogging != null && optMGLogging.isEmpty()))
				loggerCfg.put("MGFilterNone");
			else if (optMGLogging != null) {
				for (String filterName : optMGLogging)
					loggerCfg.put(filterName);
			}
			root.put("MGIsActivationDelegate", true);
			root.put("MGAllowInternalUIScreenCapture", true);
			root.put("InnerTrustAuthenticatorSupport", true);
			root.put( "easyPro", true);
			root.put("proUserEmail","@tjnetsec.com");
		}
		ServerUtil.createMissingDirsFor(gdSettings); // assets might not yet be
														// there
		{
			FileOutputStream outRaw = new FileOutputStream(gdSettings);
			OutputStreamWriter writer = new OutputStreamWriter(outRaw, encoding);
			root.write(writer);
			writer.close();
			outRaw.close();
		}

	}
}
