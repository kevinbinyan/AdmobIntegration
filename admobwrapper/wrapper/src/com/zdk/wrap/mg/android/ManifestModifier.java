package com.zdk.wrap.mg.android;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.omg.CORBA.PUBLIC_MEMBER;

import com.zdk.wrap.mg.ServerUtil;
import com.zdk.wrap.mg.AppModifier.ProcessingException;
import com.zdk.wrap.mg.test.CommandLine;

import javafx.scene.layout.ColumnConstraints;
import sun.launcher.resources.launcher;

public class ManifestModifier {
	private static final boolean useGoodDynamics = true;
	private static final boolean injectLaunchActivity = false;
	static AppInfo appInfo;
	static String newManifest;

	public ManifestModifier(AppInfo appInfo, String newManifest) {
		ManifestModifier.appInfo = appInfo;
		ManifestModifier.newManifest = newManifest;
	}

	//通过修改stringbuffer来修改manifest中被匹配的string，
	public static StringBuffer stringbufferModify(StringBuffer in , int start , int end ,String strOld,String strNew)
	{
		String cache = in.subSequence(start, end).toString();
		if(cache.contains(strOld))
		{
			cache = cache.replaceAll(strOld, strNew);
		}
		in.replace(start, end, cache);
		return in;
	}
	
	// 真正开始改manifest
	public static ManifestModifier modifyManifest(String manifest, Map<String, String> optStringMap,
			String optGDApplicationID, String optGDApplicationVersion) throws ProcessingException {
		String optTargetSdkVersion = null;
		@SuppressWarnings("unused")
		int optTargetSdkVersionNum = -1;
		{
			Matcher matcher = Pattern
					.compile("<uses-sdk\\b[^>]*?\\bandroid:targetSdkVersion=\"([^\"]*)\"", Pattern.DOTALL)
					.matcher(manifest);
			if (matcher.find()) {
				optTargetSdkVersion = matcher.group(1);
				try {
					optTargetSdkVersionNum = Integer.parseInt(optTargetSdkVersion);
				} catch (NumberFormatException e) {
					// ignore in this case
				}
			}
		}
		String optMinSdkVersion = null;
		int optMinSdkVersionNum = -1;
		{
			Matcher matcher = Pattern.compile("<uses-sdk\\b[^>]*?\\bandroid:minSdkVersion=\"([^\"]*)\"", Pattern.DOTALL)
					.matcher(manifest);
			if (matcher.find()) {
				optMinSdkVersion = matcher.group(1);
				try {
					optMinSdkVersionNum = Integer.parseInt(optMinSdkVersion);
				} catch (NumberFormatException e) {
					// ignore in this case
				}
			}
		}

		StringBuffer out = new StringBuffer();
		int lastOffset = 0;
		Matcher packageMatcher = Pattern.compile("<manifest\\b[^>]*?\\bpackage=\"([^\"]*)\"", Pattern.DOTALL)
				.matcher(manifest);
		if (!packageMatcher.find())
			throw new ProcessingException("Cannot find element \"package\" in manifest.");
		final String packageName = packageMatcher.group(1);

		Matcher iconMatcher = Pattern.compile("<application\\b[^>]*?\\bandroid:icon=\"([^\"]*)\"", Pattern.DOTALL)
				.matcher(manifest);
		String iconPrefix = "@drawable/icon";
		if (iconMatcher.find()) {
			iconPrefix = iconMatcher.group(1);
		}	
		
		
		String optMainActivityName = null;
		{
			Matcher activityMatcher1 = Pattern
					.compile("<activity\\b[^>]*\\bandroid:name=\"([^\"]+)\"([^>]*)>", Pattern.DOTALL).matcher(manifest);
			Pattern activityPattern2 = Pattern.compile("(.*?)</activity>", Pattern.DOTALL);
			while (activityMatcher1.find()) {
				// final int activityStartOffset=activityMatcher.start();
				final String activityName = activityMatcher1.group(1);

				final int am1End = activityMatcher1.end();
				Matcher activityMatcher2 = activityPattern2.matcher(manifest.substring(am1End));
				if (!activityMatcher1.group(2).endsWith("/") && activityMatcher2.lookingAt()) {
					final String activityBody = activityMatcher2.group(1);
					final int activityBodyStart = am1End + activityMatcher2.start(1);
					final int activityBodyEnd = am1End + activityMatcher2.end(1);
					final int activityEnd = am1End + activityMatcher2.end();

//					if (injectLaunchActivity)
//						out.append(manifest.substring(lastOffset, activityBodyStart));

//有些apk的manifest中<intent-filter>标签中会有其他设置字段，导致<intent-filter>标签无法匹配，例如四川省政府的apk的manifest中包含<intent-filter android:label="@string/launcher_name">
//					Matcher intentFilterMatcher = Pattern
//							.compile("<intent-filter>(.*?)</intent-filter>", Pattern.DOTALL).matcher(activityBody);
					
//修改后的匹配，<intent-filter>中“filter”和“>”之间可以有任意非“>”的字符。
					Matcher intentFilterMatcher = Pattern
							.compile("<intent-filter[^>]*>(.*?)</intent-filter>",Pattern.DOTALL).matcher(activityBody);
					
					int actvityBodyLastOffset = 0;
					while (intentFilterMatcher.find()) {
						final String intentFilterBody = intentFilterMatcher.group(1);
						String newIntentFilterBody = intentFilterBody;

						boolean allMatched = true;
						boolean omitIntent = false;

						{
							Matcher actionMatcher = Pattern
									.compile("<action\\b[^>]*\\bandroid:name=\"android.intent.action.MAIN\"[^>]*/>",
											Pattern.DOTALL)
									.matcher(newIntentFilterBody);
							if (actionMatcher.find()) {
								// you might also opt to keep this action, will
								// work with and without; currently paranoid:
								// remove main to avoid external unit to launch
								// it
								StringBuffer buf = new StringBuffer();
								actionMatcher.appendReplacement(buf, "");
								actionMatcher.appendTail(buf);
								newIntentFilterBody = buf.toString();
							} else {
								allMatched = false;
							}
						}

						{
							Matcher categoryMatcher = Pattern.compile(
									"<category\\b[^>]*\\bandroid:name=\"android.intent.category.LAUNCHER\"[^>]*/>",
									Pattern.DOTALL).matcher(newIntentFilterBody);
							if (categoryMatcher.find()) {
								StringBuffer buf = new StringBuffer();
								categoryMatcher.appendReplacement(buf, "");
								categoryMatcher.appendTail(buf);
								newIntentFilterBody = buf.toString();
							} else {
								allMatched = false;
							}
						}

//						{
//							Matcher actionMatcher = Pattern
//									.compile("<action\\b[^>]*\\bandroid:name=\"android.intent.action.VIEW\"[^>]*/>",
//											Pattern.DOTALL)
//									.matcher(newIntentFilterBody);
//							if (actionMatcher.find()) {
//								// at the moment, prevent VIEW intents from
//								// starting, would lead to GD error:
//								// 'non-task-root Activity'
//								omitIntent = true;
//							}
//						}

//						if (injectLaunchActivity)
//							out.append(activityBody.substring(actvityBodyLastOffset, intentFilterMatcher.start()));
						if (allMatched) {
							newIntentFilterBody = newIntentFilterBody.trim();
							optMainActivityName = activityName;
//							if (injectLaunchActivity) {
//								if (newIntentFilterBody.length() > 0)
//									out.append("<intent-filter>" + newIntentFilterBody + "</intent-filter>");
//							}
							// modified=true;
						} 
//						else {
//							if (injectLaunchActivity) {
//								if (!omitIntent)
//									out.append(intentFilterMatcher.group());
//							}
//						}
						actvityBodyLastOffset = intentFilterMatcher.end();
					}
					if (injectLaunchActivity) {
						out.append(activityBody.substring(actvityBodyLastOffset));
						out.append(manifest.substring(activityBodyEnd, activityEnd));
						lastOffset = activityEnd;
					}

					if (optMainActivityName != null) {
//						if (injectLaunchActivity) {
//							String configChanges = "keyboardHidden|orientation|mcc|mnc|locale|touchscreen|keyboard|navigation|screenLayout|fontScale";
//							if (/* optTargetSdkVersionNum>=13 || */ optMinSdkVersionNum >= 13) {
//								configChanges += "|screenSize|smallestScreenSize"; 
//								// if always added then aapt will complain about "String types not allowed at 'configChanges' ..."
//							}
//
//							out.append("<activity android:name=\"" + (useGoodDynamics ? "com.zdk.wrap.mg.AuthActivity"
//									: "some.other.GuardianActivity") + "\"\n" +
//							/*
//							 * android:configChanges is required to avoid
//							 * calling onDestroy()+onCreate() after orientation
//							 * changes, especially "keyboardHidden|orientation",
//							 * omitted due to API level 7:
//							 * "uiMode|screenSize|smallestScreenSize", however
//							 * even if android:configChanges="..." is specified,
//							 * onDestroy is called when the original activity is
//							 * on top of the Guardian activity when orientation
//							 * changes, i.e., omitted. Update 20121107 by HA:
//							 * reactivated to enable orientation change for
//							 * TDO-5179
//							 */
//									"android:configChanges=\"" + configChanges + "\"\n" +
//									// or
//									// "android:configChanges="keyboardHidden|orientation|screenSize"
//									"android:launchMode=\"standard\"\n" + 
//									// android:launchMode="standard" is ok, "singleTask" is not ok: it would finish the ExampleApp activity when the AGUI is shown
//									">\n" + "  <intent-filter>\n"
//									+ "    <action android:name=\"android.intent.action.MAIN\" />\n"
//									+ "    <category android:name=\"android.intent.category.LAUNCHER\" />\n"
//									+ "  </intent-filter>\n" + "</activity>\n");
//
//						}

						break;
					}
				}
			}

			if (optMainActivityName == null)
				throw new ProcessingException("Cannot find main activity intent.");
		}
		out.append(manifest.substring(lastOffset));

		String optVersionName = null;
		{
			Matcher versionNameMatcher = Pattern
					.compile("<manifest\\b[^>]*?\\bandroid:versionName=\"([^\"]*)\"", Pattern.DOTALL).matcher(manifest);
			if (versionNameMatcher.find())
				optVersionName = versionNameMatcher.group(1);
			// throw new ProcessingException("Cannot find element
			// \"versionName\" in manifest.");
		}
		String optVersionCode = null;
		{
			Matcher versionCodeMatcher = Pattern
					.compile("<manifest\\b[^>]*?\\bandroid:versionCode=\"([^\"]*)\"", Pattern.DOTALL).matcher(manifest);
			if (versionCodeMatcher.find())
				optVersionCode = versionCodeMatcher.group(1);
			// throw new ProcessingException("Cannot find element
			// \"versionCode\" in manifest.");
		}

//		final boolean makeAppDebuggable = ServerUtil.isDebug();
//		if (makeAppDebuggable) {
//			Matcher appMatcher = Pattern.compile("(<application\\b([^>]*?))(>)", Pattern.DOTALL)
//					.matcher(out.toString());
//			StringBuffer mfOut = new StringBuffer();
//			while (appMatcher.find()) {
//				String appAttrs = appMatcher.group(2);
//				if (appAttrs.indexOf("android:debuggable") < 0) {
//					appMatcher.appendReplacement(mfOut, Matcher.quoteReplacement(
//							appMatcher.group(1) + " android:debuggable=\"true\" " + appMatcher.group(3)));
//				}
//			}
//			appMatcher.appendTail(mfOut);
//			out = mfOut; // out=new StringBuffer(mfOut.toString());
//		}
		
		
//		//如果匹配到android：debuggable=就修改成android：debuggable="false"
//		Matcher debuggableMatcher = Pattern.compile("(<application\\b[^>]*?)\\b(android:debuggable=)(\".*?\")\\s([^>]*?)(/?>)", Pattern.DOTALL).matcher(out.toString());
//		StringBuffer debuggerableOut = new StringBuffer();
//		while(debuggableMatcher.find())
//		{ 	
//			debuggableMatcher.appendReplacement(debuggerableOut, Matcher.quoteReplacement(debuggableMatcher.group(1) + debuggableMatcher.group(2) + " \"false\" " + debuggableMatcher.group(4) + debuggableMatcher.group(5)));
//		}
//		debuggableMatcher.appendTail(debuggerableOut);
//		out = debuggerableOut;
//		
//		//如果匹配到Android：allowbackup=就修改成Android：allowbackup="false"
//		Matcher allowbackupMatcher = Pattern.compile("(<application\\b[^>]*?)\\b(android:allowBackup=)(\".*?\")\\s([^>]*?)(/?>)", Pattern.DOTALL).matcher(out.toString());
//		StringBuffer allowbackupOut = new StringBuffer();
//		if(allowbackupMatcher.group(0).contains("allowBackup"))
//		while(allowbackupMatcher.find())
//		{ 	
//			allowbackupMatcher.appendReplacement(allowbackupOut, Matcher.quoteReplacement(allowbackupMatcher.group(1) + allowbackupMatcher.group(2) + " \"false\" " + allowbackupMatcher.group(4) + allowbackupMatcher.group(5)));
//		}
//		allowbackupMatcher.appendTail(allowbackupOut);
//		out = allowbackupOut;
		
		//按照commandline中设置的参数修改allowBackup，有allowBackup就修改true或false，没有就加上allowbackup
//		Matcher allowbackupMatcher = Pattern.compile("(<application\\b([^>]*?))(/?>)", Pattern.DOTALL).matcher(out.toString());
//		StringBuffer allowbackupOut = new StringBuffer();
//		while (allowbackupMatcher.find())
//		{
//			//如果原来没有allowbackup参数
//			if(!(allowbackupMatcher.group(0).contains("android:allowBackup")))
//			{
//				if(CommandLine.ifAllowBackup)
//				{
//					allowbackupMatcher.appendReplacement(allowbackupOut, Matcher.quoteReplacement(allowbackupMatcher.group(1) + " android:allowBackup= \"true\" " + allowbackupMatcher.group(3)));
//				}
//				else
//				{
//					allowbackupMatcher.appendReplacement(allowbackupOut, Matcher.quoteReplacement(allowbackupMatcher.group(1) + " android:allowBackup= \"false\" " + allowbackupMatcher.group(3)));
//				}
//				
//			}
//			//如果原来有allowbackup参数
//			else
//			{
//				allowbackupMatcher = Pattern.compile("(<application\\b[^>]*?)\\b(android:allowBackup=)(\".*?\")\\s([^>]*?)(/?>)", Pattern.DOTALL).matcher(out.toString());
//				allowbackupOut = new StringBuffer();
//				allowbackupMatcher.find();
//				if(CommandLine.ifAllowBackup)
//				{
//					allowbackupMatcher.appendReplacement(allowbackupOut, Matcher.quoteReplacement(allowbackupMatcher.group(1) + allowbackupMatcher.group(2) + " \"true\" " + allowbackupMatcher.group(4) + allowbackupMatcher.group(5)));
//				}
//				else
//				{
//					allowbackupMatcher.appendReplacement(allowbackupOut, Matcher.quoteReplacement(allowbackupMatcher.group(1) + allowbackupMatcher.group(2) + " \"false\" " + allowbackupMatcher.group(4) + allowbackupMatcher.group(5)));
//				}
//			}
//		}
//		allowbackupMatcher.appendTail(allowbackupOut);
//		out = allowbackupOut;
		
		//按照commandline中设置的参数修改debuggable，有debuggable就修改true或者false，没有就加上debuggable
//		Matcher debuggableMatcher = Pattern.compile("(<application\\b([^>]*?))(/?>)", Pattern.DOTALL).matcher(out.toString());
//		StringBuffer debuggableOut = new StringBuffer();
//		while (debuggableMatcher.find())
//		{
//			//如果原来没有debuggable参数
//			if(!(debuggableMatcher.group(0).contains("android:debuggable")))
//			{
//				if(CommandLine.ifDebuggable)
//				{
//					debuggableMatcher.appendReplacement(debuggableOut, Matcher.quoteReplacement(debuggableMatcher.group(1) + " android:debuggable= \"true\" " + debuggableMatcher.group(3)));
//				}
//				else
//				{
//					debuggableMatcher.appendReplacement(debuggableOut, Matcher.quoteReplacement(debuggableMatcher.group(1) + " android:debuggable= \"false\" " + debuggableMatcher.group(3)));
//				}
//				
//			}
//			//如果原来有debuggable参数
//			else
//			{
//				debuggableMatcher = Pattern.compile("(<application\\b[^>]*?)\\b(android:debuggable=)(\".*?\")\\s([^>]*?)(/?>)", Pattern.DOTALL).matcher(out.toString());
//				debuggableOut = new StringBuffer();
//				debuggableMatcher.find();
//				if(CommandLine.ifDebuggable)
//				{
//					debuggableMatcher.appendReplacement(debuggableOut, Matcher.quoteReplacement(debuggableMatcher.group(1) + debuggableMatcher.group(2) + " \"true\" " + debuggableMatcher.group(4) + debuggableMatcher.group(5)));
//				}
//				else
//				{
//					debuggableMatcher.appendReplacement(debuggableOut, Matcher.quoteReplacement(debuggableMatcher.group(1) + debuggableMatcher.group(2) + " \"false\" " + debuggableMatcher.group(4) + debuggableMatcher.group(5)));
//				}
//			}
//		}
//		debuggableMatcher.appendTail(debuggableOut);
//		out = debuggableOut;
		

		// enable permissions if the app has not already done so
		Set<String> requiredPermissions = new HashSet<String>();
		if (useGoodDynamics) {
			// Those should be the same as in
			// /dev/gd/msdk/platform/android/Library/AndroidManifest.xml
			requiredPermissions.add("android.permission.INTERNET");
			requiredPermissions.add("android.permission.ACCESS_NETWORK_STATE");
//			requiredPermissions.add("android.permission.ACCESS_WIFI_STATE");
//			requiredPermissions.add("android.permission.WAKE_LOCK");
//			requiredPermissions.add("android.permission.READ_PHONE_STATE");
		}
		//安全桌面添加需要的权限
//		if(CommandLine.addToSecDeskTop)
//		{
//			requiredPermissions.add("com.zdk.android.agent.permission.READ");
//			requiredPermissions.add("com.zdk.android.agent.permission.WRITE");
//		}
		for (String permission : requiredPermissions) {
			Matcher permissionMatcher = Pattern
					.compile("<uses-permission\\b[^>]*\\bandroid:name=\"\\Q" + permission + "\\E\"", Pattern.DOTALL)
					.matcher(manifest);
			if (!permissionMatcher.find())
				out.insert(out.indexOf("<application "), "<uses-permission android:name=\"" + permission + "\"/>");
		}
		
		out.insert(out.indexOf("<application "), "<meta-data android:name=\"android.support.VERSION\" android:value=\"25.3.1\"/>\n");

		if (useGoodDynamics) {
			{
				injectMGServiceToManifest(out);
			}

//			Matcher appMatcher = Pattern.compile("(<activity\\b([^>]*?))(/?>)", Pattern.DOTALL).matcher(out.toString());
//			StringBuffer mfOut = new StringBuffer();
//			while (appMatcher.find()) {
//				String appAttrs = appMatcher.group(2);
//				if (!appAttrs.matches("(?s).*\\bandroid:exported\\s*=\\s*\"false\".*")
//						&& !appAttrs.matches("(?s).*\\bandroid:alwaysRetainTaskState\\s*=.*") && // i.e. already present and specified
//						!appAttrs.matches("(?s).*\\bandroid:name\\s*=\\s*\"com.zdk.mg.ui.MGInternalActivity\".*") // list of exempt activities here
//				) {
//					appMatcher.appendReplacement(mfOut, Matcher.quoteReplacement(
//							appMatcher.group(1) + " android:alwaysRetainTaskState=\"true\" " + appMatcher.group(3)));
//				}
//			}
//			appMatcher.appendTail(mfOut);
//			out = mfOut; // out=new StringBuffer(mfOut.toString());
		}

	//修改MainAcitivity中的 <category android:name="android.intent.category.LAUNCHER" />为<category android:name="com.zdk.mg.category.DEFAULT" />，
	//如果没有就添加 <category android:name="android.intent.category.INFO" />,
	//将通一个<intent-filter 中的<category android:name=\"android.intent.category.DEFAULT\"/>删掉。
//		if(CommandLine.addToSecDeskTop == true)
//		{
//			Matcher activityMatcher = Pattern.compile("<intent-filter[^>]*>([^~]*?)(</intent-filter>)").matcher(out.toString());
//			while(activityMatcher.find())
//			{
//				if(activityMatcher.group(1).contains("android.intent.action.MAIN")&&activityMatcher.group(1).contains("android.intent.category.LAUNCHER"))
//				{
//					stringbufferModify(out, activityMatcher.start(), activityMatcher.end(), "android.intent.category.LAUNCHER", "com.zdk.mg.category.DEFAULT");
//					stringbufferModify(out, activityMatcher.start(), activityMatcher.end(), "<category android:name=\"android.intent.category.DEFAULT\"/>", "");
//					if(!activityMatcher.group(1).contains("android.intent.category.INFO"))
//					{
//						stringbufferModify(out, activityMatcher.start(), activityMatcher.end(), "</intent-filter>", "<category android:name=\"android.intent.category.INFO\" />\n</intent-filter>");
//					}
//				}
//			}
//		}
		
	//给带有<provider的地方加上android:process= \":fore\"
//		Matcher providerMatcher = Pattern.compile("(<provider\\b([^>]*?))(/?>)", Pattern.DOTALL).matcher(out.toString());
//		StringBuffer providerOut = new StringBuffer();
//		while (providerMatcher.find())
//		{
//			if(!(providerMatcher.group(0).contains("android:process")))
//			{
//				providerMatcher.appendReplacement(providerOut, Matcher.quoteReplacement(providerMatcher.group(1) + " android:process= \":fore\" " + providerMatcher.group(3)));
//			}
//		}
//		providerMatcher.appendTail(providerOut);
//		out = providerOut;
	//singleTask换成singleTop
//		Matcher singletopMatcher = Pattern.compile("(<activity\\b[^>]*?)\\b(android:launchMode=)(\".*?\")\\s([^>]*?)(/?>)", Pattern.DOTALL).matcher(out.toString());
//		StringBuffer singletopout = new StringBuffer();
//		while(singletopMatcher.find())
//		{ 	
//			singletopMatcher.appendReplacement(singletopout, Matcher.quoteReplacement(singletopMatcher.group(1) + singletopMatcher.group(2) + " \"singleTop\" " + singletopMatcher.group(4) + singletopMatcher.group(5)));
//		}
//		singletopMatcher.appendTail(singletopout);
//		out = singletopout;
		
		// otherwise android.permission.ACCESS_WIFI_STATE would require a wifi
		// device, e.g., in Android market
		for (String feature : new String[] { "android.hardware.wifi", "android.hardware.bluetooth",

				// for GeoGuard:
				"android.hardware.location", "android.hardware.location.gps", "android.hardware.location.network" }) {
			Matcher matcher = Pattern
					.compile("<uses-feature\\b[^>]*\\bandroid:name=\"\\Q" + feature + "\\E\"", Pattern.DOTALL)
					.matcher(manifest);
			if (!matcher.find())
				out.insert(out.lastIndexOf("</manifest>"),
						"<uses-feature android:name=\"" + feature + "\" android:required=\"false\" />");
		}

		String caption = null;
		if (optStringMap != null) {
			caption = optStringMap.get("app_name");
			// todo find a better way to return the correct app name
			if (caption == null) {
				caption = optStringMap.get("app_name_full");
				if (caption == null) {
					for (String s : optStringMap.keySet()) {
						if (s.startsWith("app_name")) {
							caption = optStringMap.get(s);
						}
					}
				}
			}
			boolean permitStringReplacement = packageName.equals("com.google.android.apps.unveil")
			// ... more apps here
			;

			if (permitStringReplacement) {
				Matcher m = Pattern.compile("\"@string/([^\"]*)\"").matcher(out.toString());
				out = new StringBuffer();
				while (m.find()) {
					String key = m.group(1);
					String value = optStringMap.get(key);
					if (value != null)
						m.appendReplacement(out, Matcher.quoteReplacement("\"" + value + "\""));
				}
				m.appendTail(out);
			}
		}
		
		
//		修改原APK没有application文件情况下无法进行libcorehook的问题     
//		Matcher applicationMatcher = Pattern.compile("<application\\b[^>]*?\\bandroid:name=\"([^\"]*)\"", Pattern.DOTALL)
//				.matcher(manifest);
//		String applicationName = "";
//		if (applicationMatcher.find()) {
//			applicationName = applicationMatcher.group(1);
//		}
//		
//		if(applicationName.isEmpty())
//		{
//			//修改manifest，增加application信息
//			out.insert(out.lastIndexOf("<application ")+13, "android:name=\"" + "com.zdk.wrap.mg.proxy.Application" + "\""+" ");
//		}
		
		
		
		ServerUtil.debugLog("manifest modify end");
		return new ManifestModifier(new AppInfo(packageName, optVersionName, optVersionCode, optMainActivityName,
				optTargetSdkVersion, optMinSdkVersion, iconPrefix, caption), out.toString());
	}

	private static void injectMGServiceToManifest(StringBuffer out) {
		StringBuffer inApp = new StringBuffer();
		// GD builtin Activity and Service:
		/*
		 *  <activity android:configChanges="keyboard|keyboardHidden|orientation|screenLayout|screenSize|smallestScreenSize|uiMode" android:name="com.google.android.gms.ads.AdActivity" android:theme="@android:style/Theme.Translucent"/>
        <activity android:exported="false" android:name="com.google.android.gms.common.api.GoogleApiActivity" android:theme="@android:style/Theme.Translucent.NoTitleBar"/>
        <meta-data android:name="com.google.android.gms.version" android:value="@integer/google_play_services_version"/>
		 */
		inApp.append("<activity android:configChanges=\"keyboard|keyboardHidden|orientation|screenLayout|screenSize|smallestScreenSize|uiMode\" android:name=\"com.google.android.gms.ads.AdActivity\" android:theme=\"@android:style/Theme.Translucent\"/>\n");
		inApp.append("<activity android:exported=\"false\" android:name=\"com.google.android.gms.common.api.GoogleApiActivity\" android:theme=\"@android:style/Theme.Translucent.NoTitleBar\"/>\n");
		inApp.append("<meta-data android:name=\"com.google.android.gms.version\" android:value=\"@integer/google_play_services_version\"/>\n");
//		if (Boolean.FALSE) { // activate when using app-specific
//								// IccReceivingActivity, then also
//								// uncomment IccReceivingActivity.java
//			// Dedicated activity for receiving ICC intents:
//			// cf. GDIccReceivingActivity entry in
//			// /dev/gd/msdk/platform/android/Library/AndroidManifest.xml:
//			inApp.append("<activity android:name=\"com.zdk.wrap.mg.IccReceivingActivity\"\n"
//					+ "  android:exported=\"true\"\n" + "  android:alwaysRetainTaskState=\"true\">\n"
//					+ "  <intent-filter>\n"
//					+ "    <action android:name=\"com.zdk.mg.intent.action.ACTION_ICC_COMMAND\" />\n"
//					+ "  </intent-filter>\n" + "</activity>\n");
//		} else {
//			// introduced by jwhite on 20130902:
//			inApp.append(
//					"<activity android:name=\"com.zdk.mg.MGIccReceivingActivity\"\n" + "  android:exported=\"true\"\n"
//							+ "  android:alwaysRetainTaskState=\"true\">\n" + "  <intent-filter>\n"
//							+ "      <action android:name=\"com.zdk.mg.intent.action.ACTION_ICC_COMMAND\" />\n"
//							+ "  </intent-filter>\n" + "  </activity>\n");
//		}
//
//		if (Boolean.TRUE) {
//			// cf. IccActivity entry in
//			// /dev/gd/msdk/platform/android/Library/AndroidManifest.xml:
//			inApp.append("<activity android:name=\"com.zdk.zdklib.ndkproxy.icc.IccActivity\"\n"
//					+ "  android:exported=\"true\"\n" + // not certain whether this is really required
//					"  android:alwaysRetainTaskState=\"true\"\n" + // would be added by code below anyway, but be specific
//					"  android:theme=\"@android:style/Theme.NoDisplay\"\n" + "  >\n" + "  <intent-filter>\n"
//					+ "    <action android:name=\"com.zdk.mg.intent.action.ACTION_ICC_COMMAND\" />\n"
//					+ "  </intent-filter>\n" + "</activity>\n"
//					+ "<service android:name=\"com.zdk.zdklib.ndkproxy.icc.IccManagerService\"></service>\n");
//		}
//
//		// cf. GDService entry in
//		// /dev/gd/msdk/platform/android/Library/AndroidManifest.xml:
//		inApp.append(
//				"<service android:name=\"com.zdk.mg.service.MGService\" android:enabled=\"true\" android:exported=\"false\" />\n");
//
//		inApp.append(
//				"  <service android:enabled=\"true\" android:exported=\"true\" android:name=\"com.zdk.mg.service.MGIccService\">\n"
//						+ "<intent-filter>\n"
//						+ "<action android:name=\"com.zdk.mg.intent.action.ACTION_ICC_COMMAND\"/>\n"
//						+ "</intent-filter>\n" + "<meta-data android:name=\"MG_ICC_VERSION\" android:value=\"2.1\"/>\n"
//						+ "</service>\n");
		out.insert(out.indexOf("</application>"), inApp.toString());
	}
}
