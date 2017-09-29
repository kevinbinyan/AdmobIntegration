package com.zdk.wrap.mg.android;

import java.io.File;
import java.io.IOException;

import jdk.internal.org.objectweb.asm.commons.StaticInitMerger;

//
public class PathAndFileConfig {
	//todo:以后从aar解包来自动打包
	
	public static final String ShrinkedLibJar =  "shrinkedMGLib.jar";
	public static final String MGAndroidSupportV4Location =  "android/android-support-v4.jar";
	public static final String MGGooglePlayServiceLocation =  "android/google-play-services.jar";
	
	public static final String MGLibAarLocation =  "android/"+"mg.aar";
	public static final String MGLibJarLocation =  "android/"+"mg.jar";
	public static final String ProxyJarLocation = 				"android/" +"wrapper.mg.jar";
	public static final String CacheDir = 						"cache";
	public static final String WhiteList =                 "android/" +"whitelist.json";

	public static final String WapperTimestampJarFile = 		"wrapper.jar.stamp";
	public static final String CachedFrameworkRelPath = 		"android.framework\\brut\\androlib\\android-framework.jar";	// apktool-2.2.1\brut\androlib
	public static final String MiscDir = 						"misc";									// C:\\Users\\user\AppData\Local\Temp\misc*******.tmp
	//apk展开到哪
	public static final String ApkExtractDir = 					"apkextr";								// C:\Users\\user\AppData\Local\Temp\apkextr*******.tmp
	
	public static final String AdmobExtractDir = 				"admobextr";								// C:\Users\\user\AppData\Local\Temp\admobextr*******.tmp
	//smali展开到哪
	public static final String SmaliDumpDir = 					"smalidump";							// C:\Users\\user\AppData\Local\Temp\smalidump*******.tmp\smali
	//resource 展开到哪
	public static final String compressedRsrcPkgExtractDir = 	"comprextr";							// C:\Users\\user\AppData\Local\Temp\comprextr*******.tmp
	
	public static final String CachedSmaliDir = 				"android.wrapper.smali";
	public static final String ResourceDir = 					"android.wrapper.res-and-assets";
	public static final String AndroidFrameworkDir = 			"android.framework";
	public static final String WrapperDexDir = 					"android.wrapper.dex";
	public static final String apktoolExtractDir = 				"apktoolextr";
	public static final String cfgFileBaseName =                com.zdk.wrap.mg.RuntimePhase3ConfigParams.androidCfgFileBasename;
	public static final String cfgFilePathName =                "assets/" + cfgFileBaseName;
	
	public static final String MGLibraryMode = "";
	public static final String MGApplicationID = "";
	public static final String MGApplicationVersion = "";
	public static final String MGConsoleLogger = "[\"MGFilterNone\"]";
	public static final String otaServer = "10.100.120.44:9443";
	public static final String otaScheme = "https";
	public static final Boolean MGIsActivationDelegate = true;
	public static final Boolean MGAllowInternalUIScreenCapture = true;
	public static final String baksmaliPath = ".\\lib\\baksmali-2.2b4.jar";
	public static final String Dex2jarLocation = ".\\resources\\android\\dex2jar2\\dex2jar.bat";
	public static final String DynamicLlinkLibraryLocation = ".\\resources\\android\\dynamicLlinkLibrary";
	public static final String AndroidAssets = ".\\resources\\android\\assets";
	
	public static final String XmlInApktoolExtractDirPath = File.separator + "res" + File.separator + "values" + File.separator;
	public static final String[] allMergeString = {"<dimen","<string"};
	public static final String[] allApkXmlInApktoolExtractDirPath = {XmlInApktoolExtractDirPath + "dimens.xml" , XmlInApktoolExtractDirPath +"strings.xml"};
	public static final String sdkXmlInApktoolExtractDirPath =	File.separator + "res" + File.separator + "values" + File.separator + "values.xml";
	//添加安全桌面时使用的keystore
	public static final String secDesktopKeyStroePath = ".\\resources\\android\\keystore_SCS.jks";
			
	public static String getWrapperWhiteList() throws IOException {
		return com.zdk.wrap.mg.AppModifier.getResourceFilePath(PathAndFileConfig.WhiteList);
	}
	 
	public static String getWrapperLibLocation() throws IOException{
		return com.zdk.wrap.mg.AppModifier.getResourceFilePath(PathAndFileConfig.ProxyJarLocation);
	}

	public static String getAndroidSupportV4Location() throws IOException{
		return com.zdk.wrap.mg.AppModifier.getResourceFilePath(PathAndFileConfig.MGAndroidSupportV4Location);
	}
	
	public static String getGooglePlayLibLocation() throws IOException{
		return com.zdk.wrap.mg.AppModifier.getResourceFilePath(PathAndFileConfig.MGGooglePlayServiceLocation);
	}
	
	public static String getWrapperTimestampFileLocation(File rootDir) {
		return rootDir + File.separator + PathAndFileConfig.WapperTimestampJarFile;
	}

	public static String getCachedSmaliDir(File rootDir) {
		return rootDir.getAbsolutePath() + File.separator + CachedSmaliDir;
	}
	
	public static String getResourceDir(File rootDir) {
		return rootDir.getAbsolutePath() + File.separator+ ResourceDir;
	}
	
	public static String getCachedFrameworkDir(File rootDir) {
		return rootDir.getAbsolutePath() + File.separator+AndroidFrameworkDir;
	}
	
	public static String getCachedFrameworkAbsPath(File rootDir) {
		return rootDir.getAbsolutePath() + File.separator + CachedFrameworkRelPath;
	}
	
	public static String getShrinkedWrapperLib(File rootDir) {
		return rootDir.getAbsolutePath() + File.separator + ShrinkedLibJar;
	}
	public static String getWrapperDex(File rootDir) {
		return rootDir.getAbsolutePath() + File.separator + WrapperDexDir;
	}
	public static String getConfig(File rootDir){
		return rootDir.getAbsolutePath()+File.separator + "assets"+ File.separator + "config.info";
	}
	public static String getSettingsFileName(File rootDir){
		return rootDir.getAbsolutePath()+File.separator + "assets"+ File.separator +"settings.json";
	}
	public static String getNewApkFileName(File rootDir){
		return rootDir.getAbsoluteFile() + File.separator + "new.apk";
	}
	public static String getAlignedApkFileName(File rootDir){
		return rootDir.getAbsoluteFile() + File.separator + "aligned.apk";
	}
	public static String getWrappedManifestFileName(File rootDir){
		return rootDir.getAbsoluteFile() + File.separator + "AndroidManifest.xml";
	}
	public static String getWrappedApktoolymlFileName(File rootDir){
		return rootDir.getAbsoluteFile() + File.separator + "apktool.yml";
	}
	public static String getClassesDex(File rootDir){
		return rootDir.getAbsoluteFile() + File.separator + "classes.dex";
	}
	public static String getOrigApkFileName(File rootDir){
		return rootDir.getAbsoluteFile() + File.separator + "orig.apk";
	}
	public static String getCompressedRsrcPkgFilename(File rootDir){
		return rootDir.getAbsoluteFile() + File.separator + "compress.apk";
	}
	public static String getCfgFileBaseName(File rootDir){
		return rootDir.getAbsoluteFile() + File.separator + cfgFileBaseName;
	}
	public static String getCfgFilePathName(File rootDir){
		return rootDir.getAbsoluteFile() + File.separator + cfgFilePathName;
	}
}
