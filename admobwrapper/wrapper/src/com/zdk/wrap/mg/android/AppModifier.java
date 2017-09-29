
package com.zdk.wrap.mg.android;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.zdk.wrap.mg.AppModifier.ProcessingException;
import com.zdk.wrap.mg.AppModifier.WrappingParameters;
import com.sun.corba.se.spi.activation.Server;
import com.zdk.wrap.mg.PackagedPrivateKey;
import com.zdk.wrap.mg.ServerUtil;
import com.zdk.wrap.mg.ServerUtil.FileAction;
import com.zdk.wrap.mg.ServerUtil.RunExecResult;
import com.zdk.wrap.mg.android.packing.ImapgeToPng;
import com.zdk.wrap.mg.android.packing.Multidexer;
import com.zdk.wrap.mg.android.packing.mergeApkAndSdkXml;
import com.zdk.wrap.mg.test.CommandLine;

//import org.jf.baksmali.*;
public class AppModifier
{

	private static final boolean useGoodDynamics; // at a later point in time
													// one might use wrapping to
													// perform other functions
													// than inserting GD
	static
	{
		// String hostname=ServerUtil.getCanonicalHostname();
		// String currDir=new File(".").getAbsolutePath();
		useGoodDynamics = true;
	}
	// private static final boolean useProxyTier=true;

	private static File cacheDir = null;

	private static String aaptPath = null;
	private static String dxPath = null;
	private static String zipalignPath = null;
	private static int androidJarVersion = 25;
	// private static int defaultMinSDKVersion=-1;
	private static boolean developerDebugMode = false;

	public static String getDxPath()
	{

		return dxPath;

	}

	public static String getAndroidSdkPath()
	{

		String sdkPath = null;
		Map<String, String> map = System.getenv();
		Iterator<Entry<String, String>> it = map.entrySet().iterator();
		while (it.hasNext())
		{
			Entry<String, String> entry = (Entry) it.next();
			if (entry.getKey().equals("ANDROID_HOME"))
			{
				sdkPath = entry.getValue();
			}
		}
		return sdkPath;
	}

	public static String extractMgJar() throws IOException
	{

		String mgjarTempDirName = "mgjar";
		File mgjarTempDir = ServerUtil.createTempDir(mgjarTempDirName);
		
		String resourcesAndroidPath = com.zdk.wrap.mg.AppModifier.getResourceFilePath("android/");
		String mgaarPath = resourcesAndroidPath + "\\admob.aar";
		ServerUtil.unpackJar(mgjarTempDir.getPath(), mgaarPath, null, null);
		Multidexer.renameSingleFile(mgjarTempDir.getPath(), "\\classes.jar", "mg.jar");
		String mgjarPath = mgjarTempDir.getPath() + "\\mg.jar";
		
		File tempMgjar = new File(mgjarPath);
		File resourcesAndroid = new File(resourcesAndroidPath);
		tempMgjar.renameTo(new File(resourcesAndroid,tempMgjar.getName()));
		
		ServerUtil.removeRecursively(mgjarTempDir.getPath(),true ,null);
		
		File mgjar = new File(resourcesAndroidPath + "\\mg.jar");
		if (!mgjar.exists())
		{
			throw new IOException("Can not find mg.jar");
		}
		return mgjar.getPath();
	}

	public static String getAndroidJarPath(int version) throws IOException
	{

		String sdkPath = getAndroidSdkPath();
		String androidJarPathInSdkPath = "\\platforms" + "\\android-" + String.valueOf(version);
		String androidJarPath = sdkPath + androidJarPathInSdkPath + "\\android.jar";
		String androidJar = null;
		File androidJarFile = new File(androidJarPath);
		if (!androidJarFile.exists())
		{
			androidJar = null;
			throw new IOException("can not find android.jar of version " + version);
		} else
		{
			androidJar = androidJarPath;
		}
		return androidJar;
	}

	public static class ProcessingResult extends com.zdk.wrap.mg.AppModifier.ProcessingResult
	{

		private AppInfo appInfo;
		private String fullWrapManifest;

		private ProcessingResult(/* InputStream origBinCodeSrc, */ InputStream optWrappedBinCodeStr, AppInfo appInfo,
				String fullWrapManifest)
		{
			super(/* origBinCodeSrc, */ optWrappedBinCodeStr);
			this.appInfo = appInfo;
			this.fullWrapManifest = fullWrapManifest;
		}

		public AppInfo getAppInfo()
		{

			return appInfo;
		}

		// public int getVersionOfInjectedLib() {
		// }

		public String getFullWrapManifest()
		{

			return fullWrapManifest;
		}
	}

	private static class ResourceReconstruction
	{

		public String origManifest;
		public String origApktoolyml;
		private File apktoolExtractDir;
		private File admobtoolExtractDir;
		public Map<String, String> optStringMap;

		public void cleanup()
		{

			deleteDir(apktoolExtractDir);
		}

		public String getResourceDirPath()
		{

			return apktoolExtractDir.getAbsolutePath() + File.separator + "res";
		}

		public File getRootDir()
		{

			return apktoolExtractDir;
		}
	}

	public static void configure(String aaptPath, String dxPath,
			String zipalignPath /* , int defaultMinSDKVersion */)
	{

		AppModifier.aaptPath = aaptPath;
		AppModifier.dxPath = dxPath;
		AppModifier.zipalignPath = zipalignPath;
		// AppModifier.defaultMinSDKVersion=defaultMinSDKVersion;
	}

	public static void enableDeveloperDebugMode()
	{

		developerDebugMode = true;
	}

	private boolean debugLoggingEnabled;
	private boolean useProxyTier;
	private String cachedFrameworkAbsPath = null;

	public AppModifier(boolean debugLoggingEnabled)
	{
		this.debugLoggingEnabled = debugLoggingEnabled;
		// 因为我们没proxylayer可用了
		useProxyTier = false;// debugLoggingEnabled;
	}

	public String getCapabilityWrapManifest() throws ProcessingException
	{

		SmaliModifier smaliModifier = new SmaliModifier(useGoodDynamics, useProxyTier);
		return smaliModifier.getWrapManifest(null, null);
	}

	public ProcessingResult process(InputStream origApkData, boolean isAppGuarded, WrappingParameters parameters
	/*
	 * PackagedPrivateKey optSignKey, String optGDApplicationID, String
	 * optGDApplicationVersion, String optGDConfigInfo, String optAppTag
	 */) throws ProcessingException, IOException
	{

		if (isAppGuarded)
			return applyWrapping(origApkData, parameters);

		else
		{
			ProcessingResult wrappingResult;
			File miscDir = ServerUtil.createTempDir("misc");
			try
			{
				final String origApkFileName = PathAndFileConfig.getOrigApkFileName(miscDir);
				writeToFile(origApkData, origApkFileName);
				ResourceReconstruction resourceReconstruction = reconstructManifestAndResources(origApkFileName, true,parameters.getAppName());
				try
				{
					ManifestModifier.modifyManifest(resourceReconstruction.origManifest,
							resourceReconstruction.optStringMap, parameters.getOptGDApplicationID(),
							parameters.getOptGDApplicationVersion());
					wrappingResult = new ProcessingResult(
							/* new FileInputStream(origApkFileName), */ null, ManifestModifier.appInfo,
							"" /* empty manifest */); // potentially
														// problematic: later
														// reading of the file
														// stream from within a
														// deleted directory
														// (miscDir)
				} finally
				{
					resourceReconstruction.cleanup();
				}
			} finally
			{
				deleteDir(miscDir);
			}
			return wrappingResult;
		}
	}

	private void writeToFile(InputStream data, String fileName) throws IOException
	{

		FileOutputStream out = new FileOutputStream(fileName);
		// out.write(origApkData);
		ServerUtil.copyStream(data, out);
		out.close();
	}

	private static class SignaturesFilter implements FilenameFilter
	{ // don't use anonymous class below due to
		// Groovy/Java compiler bug that once
		// creates AppModifier$2.class
		// instead of AppModifier$1.class
		// @Override OZ: removed for compiler warning (override not allowed in
		// interfaces)

		public boolean accept(File dir, String name)
		{

			return (name.endsWith(".SF") || name.endsWith(".RSA"));
		}
	}

	private ProcessingResult applyWrapping(InputStream origApkData,
			WrappingParameters parameters /*
											 * PackagedPrivateKey optSignKey,
											 * String optGDApplicationID, String
											 * optGDApplicationVersion, String
											 * optGDConfigInfo, String optAppTag
											 */) throws ProcessingException, IOException
	{

		if (aaptPath == null)
			throw new ProcessingException("Missing configuration parameter 'aapt'.");
		if (dxPath == null)
			throw new ProcessingException("Missing configuration parameter 'dx'.");
		//将Mg.aar中的mg.jar解压缩到resources\android中
//		extractMgJar(); 
		ProcessingResult wrappingResult = null;
		File miscDir = ServerUtil.createTempDir(PathAndFileConfig.MiscDir);
		File apkExtractDir = ServerUtil.createTempDir(PathAndFileConfig.ApkExtractDir);
		
		File admobExtractDir = ServerUtil.createTempDir(PathAndFileConfig.AdmobExtractDir);
		// File apktoolExtractDir=createTempDir(servletContext,"apktoolextr");
		File smaliDumpDir = ServerUtil.createTempDir(PathAndFileConfig.SmaliDumpDir);
		File compressedRsrcPkgExtractDir = ServerUtil.createTempDir(PathAndFileConfig.compressedRsrcPkgExtractDir);

		ResourceReconstruction resourceReconstruction = null;
		try
		{
			final String origApkFileName = PathAndFileConfig.getOrigApkFileName(miscDir);
			// 存到misc/xxxorig.apk
			writeToFile(origApkData, origApkFileName);

			// String pwd=ServerUtil.runExecutable(new String[] { "pwd" });
			resourceReconstruction = reconstructManifestAndResources(origApkFileName, false,parameters.getAppName());

			// apkExtractDir=resourceReconstruction.apktoolExtractDir;
			deleteDir(smaliDumpDir);

			smaliDumpDir = new File(resourceReconstruction.apktoolExtractDir, "smali");
			// 合并smali,smali_classes2等
			gatherSmali(resourceReconstruction.apktoolExtractDir + File.separator + "smali");

//			if (/* !permitUploadOfDebuggingApp && */ hasAlreadyBeenWrapped(apkExtractDir, smaliDumpDir))
//				throw new ProcessingException(
//						"Wrapping or a wrapping SDK has already been applied to this application. Please submit the original application instead.");
			// String
			// wrappedManifest,packageName,activityName,versionName,versionCode;
			ManifestModifier.modifyManifest(resourceReconstruction.origManifest, resourceReconstruction.optStringMap,
					parameters.getOptGDApplicationID(), parameters.getOptGDApplicationVersion());
			
			
//			String newApktoolyml = Walkaround.modifyApktoolyml(resourceReconstruction.origApktoolyml);
			String newApktoolyml = resourceReconstruction.origApktoolyml;
			
			String wrappedApktoolymlFilename = Walkaround.write2File(miscDir, newApktoolyml); 
			
			final String wrappedManifestFilename = PathAndFileConfig.getWrappedManifestFileName(miscDir);
			{
				FileOutputStream out = new FileOutputStream(wrappedManifestFilename);
				out.write(ManifestModifier.newManifest.getBytes("UTF-8"));
				out.close();
			}
			
			SmaliClassAnalyzer classAnalyzer = new SmaliClassAnalyzer(smaliDumpDir, useGoodDynamics);
			List<String> entries = new ArrayList<String>();
			classAnalyzer.addManifestEntries(entries);

			SmaliModifier smaliModifier = new SmaliModifier(useGoodDynamics, useProxyTier);
			smaliModifier.injectModifiedClasses(smaliDumpDir, classAnalyzer, developerDebugMode,
					parameters.getOptAppTag() != null ? parameters.getOptAppTag() : parameters.getOptGDApplicationID());
			// 从这开始有问题，缺少wrapperlib
			copyWrapperLib(smaliDumpDir,
					resourceReconstruction
							.getRootDir(),
					wrappedManifestFilename, resourceReconstruction.getResourceDirPath(),
					ManifestModifier.appInfo.getPackageName());

			// wrapper自己的配置
			final String cfgPropFilename = PathAndFileConfig.getCfgFileBaseName(miscDir);
			createAppcentralConfigFile(cfgPropFilename, parameters.getOptGDApplicationID(),
					parameters.getOptGDApplicationVersion());
			final String gdSettingsFilename = PathAndFileConfig
					.getSettingsFileName(resourceReconstruction.getRootDir());
			MGSettingsModifier.modifyGDSettings(gdSettingsFilename, parameters.getOptGDApplicationID(),
					parameters.getOptGDApplicationVersion(), parameters.getOptGDLogging());

			// gd配置，clear
			String gdConfigInfoFilename = null;
			if (useGoodDynamics && parameters.getOptGDConfigInfo() != null)
			{
				gdConfigInfoFilename = PathAndFileConfig.getConfig(resourceReconstruction.getRootDir());
				PrintWriter writer = new PrintWriter(new File(gdConfigInfoFilename), "UTF-8");
				writer.println(parameters.getOptGDConfigInfo());
				writer.close();
			}

			final String newApkFileName = PathAndFileConfig.getNewApkFileName(miscDir);
			// final String
			// i18nSrcPath=servletContext.getRealPath("/WEB-INF/appguard/i18n");
			// 开始wrap
			installAndroidjarToApktool();
			normalApkUsageWrap(resourceReconstruction, wrappedManifestFilename, cfgPropFilename, newApkFileName);

			final String alignedApkFileName = PathAndFileConfig.getAlignedApkFileName(miscDir);
			// Unnecessary because jarsigner will be re-run at download time
			// again due to changed appcentral.cfg. However, keep it that way to
			// allow for debugging and for potential later signing of only
			// appcentral.cfg.
			// 签名打包
			signAndAlignApk(newApkFileName, alignedApkFileName, parameters.getOptSignKey());
			{ // ByteArrayOutputStream wrappedBuf=new ByteArrayOutputStream();
				FileInputStream in = new FileInputStream(alignedApkFileName);

				final String fullWrapManifest = smaliModifier.getWrapManifest(ManifestModifier.appInfo, classAnalyzer);
				wrappingResult = new ProcessingResult(
						/* new FileInputStream(origApkFileName), */ in /* wrappedApkData */, ManifestModifier.appInfo,
						fullWrapManifest);
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		} finally
		{
			deleteDir(compressedRsrcPkgExtractDir);
			deleteDir(smaliDumpDir);
			if (resourceReconstruction != null)
				resourceReconstruction.cleanup();
			// deleteDir(apktoolExtractDir);
			deleteDir(apkExtractDir);
			deleteDir(miscDir);
		}
		return wrappingResult;
	}

	// androidjar25安装到apktool中
	private void installAndroidjarToApktool() throws IOException, ProcessingException
	{

		List<String> args = new LinkedList<String>();
		androidJarVersion = 25;
		final String androidjarPath = getAndroidJarPath(androidJarVersion);
		args.add("if");
		args.add("-f");
		args.add(androidjarPath);
		execAPKTool(args);
	}

	// 前面关键的inject已经完成此处其实就是链接了。
	private void normalApkUsageWrap(ResourceReconstruction resourceReconstruction, final String wrappedManifestFilename,
			final String cfgPropFilename, final String newApkFileName) throws IOException, ProcessingException
	{

		final String apktoolExtractDirPath = resourceReconstruction.apktoolExtractDir.getAbsolutePath();
		ServerUtil.copyFile(wrappedManifestFilename, apktoolExtractDirPath + File.separator + "AndroidManifest.xml");
		
		Walkaround.copyWrappedApktoolyml2apktoolExtractDirPath(apktoolExtractDirPath);

		File cfgFileDest = new File(apktoolExtractDirPath + File.separator + PathAndFileConfig.cfgFilePathName);
		ServerUtil.createMissingDirsFor(cfgFileDest);

		ServerUtil.copyFile(cfgPropFilename, cfgFileDest.getAbsolutePath());

		// ServerUtil.copyFilesRecursively(i18nSrcPath,apktoolExtractDirPath+File.separator+"assets"+File.separator+"i18n",true,(FilenameFilter)null);
		
		//删除sdk与apk中重复的资源，mergeString和xml如果有需要可以添加。		
		for(int mergeXmlNum = 0 ; mergeXmlNum < PathAndFileConfig.allMergeString.length ; mergeXmlNum++)
		{
			if(mergeApkAndSdkXml.mergeXmlFile(apktoolExtractDirPath + PathAndFileConfig.allApkXmlInApktoolExtractDirPath[mergeXmlNum], 
				apktoolExtractDirPath + PathAndFileConfig.sdkXmlInApktoolExtractDirPath,
				PathAndFileConfig.allMergeString[mergeXmlNum])==false)
			{
				throw new IOException("strings Xml merge failed ");
			}
		}
				
		// java -build工作了，把classes最后打包
		List<String> args = new LinkedList<String>();
		args.add("build");
		args.add("-f");
		args.add(apktoolExtractDirPath);
		args.add("-o");
		args.add(newApkFileName);
		// (*s-option-2*)
		execAPKTool(args);
	}

	// 生成mgwrap.cfg
	private void createAppcentralConfigFile(String cfgPropFilename, String optGDApplicationID,
			String optGDApplicationVersion) throws ProcessingException, IOException
	{

		// new File(miscDir.getAbsoluteFile()+File.separator+"assets").mkdir();
		{
			Properties cfgProps = new Properties();
			cfgProps.setProperty("wrappedActivityPackage", ManifestModifier.appInfo.getPackageName());
			if (ManifestModifier.appInfo.getOptMainActivityName() != null)
				cfgProps.setProperty("wrappedActivityClass", ManifestModifier.appInfo.getOptMainActivityName());

			cfgProps.setProperty("debugLoggingEnabled", Boolean.toString(debugLoggingEnabled));
			cfgProps.setProperty("useProxyTier", Boolean.toString(useProxyTier));

			FileOutputStream out = new FileOutputStream(cfgPropFilename);
			cfgProps.store(out, "Preliminary GD wrapping configuration");
			out.close();
		}
	}

	// appGuard
	// props.getProperty("appguard.aapt"),
	// props.getProperty("appguard.dx"),
	// props.getProperty("appguard.zipalign")
	private void signAndAlignApk(String toBeSignedApkFileName, String alignedApkFileName, PackagedPrivateKey optSignKey)
			throws ProcessingException, IOException
	{

		File keyStoreFile = null;
		if (optSignKey != null)
		{
			// wrapper\wrapper\resources\android
			keyStoreFile = File.createTempFile("android", ".jks");
			FileOutputStream keyOut = new FileOutputStream(keyStoreFile);
			keyOut.write(optSignKey.getEncodedData());
			keyOut.close();
		}
		final String keystoreLocation = keyStoreFile != null ? keyStoreFile.getCanonicalPath() : getKeystorePath();
		
//			final String jarsignerOutput = ServerUtil.runExecutable(new String[]
//					{ 
//				/*			"jarsigner", // "-verbose",
//							"-sigalg", "MD5withRSA", 
//							"-digestalg", "SHA1", // required since JDK 7 because defaults have changed
//							"-keystore", keystoreLocation, 
//							"-storepass",optSignKey != null && optSignKey.getOptPassword() != null ? optSignKey.getOptPassword() : "uuioop",
//							"-keypass",optSignKey != null && optSignKey.getOptPassword() != null ? optSignKey.getOptPassword() : "uuioop",
//							toBeSignedApkFileName, 
//							optSignKey != null && optSignKey.getOptIdentifier() != null ? optSignKey.getOptIdentifier() : "odgappcntandroid" 
//				*/				
//							"jarsigner", // "-verbose",
//							"-sigalg", "MD5withRSA", 
//							"-digestalg", "SHA1",  
//							"-keystore", keystoreLocation, 
//							"-storepass",optSignKey != null && optSignKey.getOptPassword() != null ? optSignKey.getOptPassword() : "123$567qwe",
//							"-keypass",optSignKey != null && optSignKey.getOptPassword() != null ? optSignKey.getOptPassword() : "ijnmmki$778",
//							toBeSignedApkFileName, 
//							optSignKey != null && optSignKey.getOptIdentifier() != null ? optSignKey.getOptIdentifier() : "zdk_key" 
//					},
//					true).allOutput();
//			debugLog("jarsigner:\n" + jarsignerOutput);		
		
		//签名需要的默认参数，默认使用zdk自己的key；
		String keystore = keystoreLocation;
		String storepass = "123$567qwe";
		String keypass = "ijnmmki$778";
		String optsignkey = "zdk_key";
		//如果addToSecDeskTop为true，签名时使用新的keystore。
		if(CommandLine.addToSecDeskTop == true)
		{
			keystore = PathAndFileConfig.secDesktopKeyStroePath;
			storepass = "1qa2ws3ed@SCS";
			keypass = "1qa2ws3ed@SCS";
			optsignkey = "securecenterservice";
		}

		final String jarsignerOutput = ServerUtil.runExecutable(new String[]
				{ 
			/*			"jarsigner", // "-verbose",
						"-sigalg", "MD5withRSA", 
						"-digestalg", "SHA1", // required since JDK 7 because defaults have changed
						"-keystore", keystoreLocation, 
						"-storepass",optSignKey != null && optSignKey.getOptPassword() != null ? optSignKey.getOptPassword() : "uuioop",
						"-keypass",optSignKey != null && optSignKey.getOptPassword() != null ? optSignKey.getOptPassword() : "uuioop",
						toBeSignedApkFileName, 
						optSignKey != null && optSignKey.getOptIdentifier() != null ? optSignKey.getOptIdentifier() : "odgappcntandroid" 
			*/				
						"jarsigner", // "-verbose",
						"-sigalg", "MD5withRSA", 
						"-digestalg", "SHA1",  
						"-keystore", keystore, 
						"-storepass",optSignKey != null && optSignKey.getOptPassword() != null ? optSignKey.getOptPassword() : storepass,
						"-keypass",optSignKey != null && optSignKey.getOptPassword() != null ? optSignKey.getOptPassword() : keypass,
						toBeSignedApkFileName, 
						optSignKey != null && optSignKey.getOptIdentifier() != null ? optSignKey.getOptIdentifier() : optsignkey
				},
				true).allOutput();
		debugLog("jarsigner:\n" + jarsignerOutput);
	
		

		if (keyStoreFile != null)
			keyStoreFile.delete();

		if (zipalignPath == null)
			throw new ProcessingException("Missing configuration parameter 'zipalign'.");
		final String zipalignOutput = ServerUtil.runExecutable(new String[]
		{ zipalignPath, "4", toBeSignedApkFileName, alignedApkFileName }, true).allOutput();
		debugLog("zipalign:\n" + zipalignOutput);
	}

	private String getKeystorePath() throws ProcessingException, IOException
	{

		String keystoreLocation = com.zdk.wrap.mg.AppModifier.getResourceFilePath("android/zdk.jks");
		if (keystoreLocation == null)
			throw new ProcessingException("Cannot locate android keystore.");
		return keystoreLocation;
	}

	private void addToZipfile(String apkFileName, String srcPathName, String zipPathName) throws IOException
	{

		List<String> srcPathNames = new LinkedList<String>();
		List<String> zipPathNames = new LinkedList<String>();
		srcPathNames.add(srcPathName);
		zipPathNames.add(zipPathName);
		addToZipfile(apkFileName, srcPathNames, zipPathNames);
	}

	private void addToZipfile(String apkFileName, List<String> srcPathNames, List<String> zipPathNames)
			throws IOException
	{

		FileInputStream inRaw = new FileInputStream(apkFileName);
		ZipInputStream inZip = new ZipInputStream(inRaw);
		FileOutputStream outRaw = new FileOutputStream(apkFileName + ".new");
		ZipOutputStream outZip = new ZipOutputStream(outRaw);
		for (;;)
		{
			ZipEntry entry = inZip.getNextEntry();
			if (entry == null)
				break;
			// outZip.putNextEntry(entry);
			ZipEntry newEntry = new ZipEntry(entry.getName());
			newEntry.setTime(entry.getTime());
			outZip.putNextEntry(newEntry);
			ServerUtil.copyStream(inZip, outZip);
			inZip.closeEntry();
			outZip.closeEntry();
		}
		inZip.close();
		inRaw.close();

		Iterator<String> zipPathNameIter = zipPathNames.iterator();
		for (String srcPathName : srcPathNames)
		{
			if (!zipPathNameIter.hasNext())
				throw new IOException("File name lists for addToZipfile have different lengths.");
			String zipPathName = zipPathNameIter.next();

			outZip.putNextEntry(new ZipEntry(zipPathName));
			FileInputStream in = new FileInputStream(srcPathName);
			ServerUtil.copyStream(in, outZip);
			in.close();
			outZip.closeEntry();
		}

		outZip.close();
		outRaw.close();

		ServerUtil.copyFile(apkFileName + ".new", apkFileName);
	}

	private static void deleteDir(File dir)
	{

		File[] entries = dir.listFiles();
		if (entries != null)
		{
			for (int i = 0; i < entries.length; i++)
			{
				File entry = entries[i];
				if (entry.isDirectory())
					deleteDir(entry);
				else
					entry.delete();
			}
		}
		dir.delete();
	}

	private void unpackAPK(InputStream origApkData, File apkExtractDir) throws IOException, ProcessingException
	{

		ZipInputStream zipInStr = new ZipInputStream(origApkData);
		for (;;)
		{
			ZipEntry entry = zipInStr.getNextEntry();
			if (entry == null)
				break;
			final String pathName = entry.getName();
			if (entry.isDirectory())
			{
				createDirectoriesForPathname(apkExtractDir, pathName, false);
			} else
			{
				createDirectoriesForPathname(apkExtractDir, pathName, true);
				FileOutputStream outStr = new FileOutputStream(
						apkExtractDir.getAbsolutePath() + File.separator + pathName);
				byte[] buf = new byte[1024];
				for (;;)
				{
					int got = zipInStr.read(buf);
					if (got < 0)
						break;
					else if (got > 0)
					{
						outStr.write(buf, 0, got);
					}
				}
				outStr.close();
			}
			zipInStr.closeEntry();
		}
		zipInStr.close();
		origApkData.close();
	}

	private void createDirectoriesForPathname(File parentDir, String pathName, boolean excludeLastNameComponent)
			throws ProcessingException, IOException
	{

		String[] components = pathName.split("[/\\" + File.separator + "]");
		StringBuffer pathBuf = new StringBuffer();
		pathBuf.append(parentDir.getAbsolutePath());
		pathBuf.append(File.separator);
		for (int i = 0; i < components.length - (excludeLastNameComponent ? 1 : 0); i++)
		{
			pathBuf.append(components[i]);
			File dir = new File(pathBuf.toString());
			if (!(dir.exists() && dir.isDirectory()))
				ServerUtil.mkdir(dir);
			pathBuf.append(File.separator);
		}
	}

	private String getApktoolLocation() throws IOException, ProcessingException
	{

		final String apktoolLocation = com.zdk.wrap.mg.AppModifier.getResourceFilePath("android/apktool-2.2.1.jar");
		if (apktoolLocation == null)
			throw new ProcessingException("Cannot locate apktool.");
		return apktoolLocation;
	}

	private void execAPKTool(List<String> args) throws ProcessingException, IOException
	{

		final String apktoolLocation = getApktoolLocation();

		File aaptExecDir = new File(aaptPath).getParentFile();
		final String aaptExecDirPath = aaptExecDir.getAbsolutePath();

		List<String> allArgs = new LinkedList<String>();
		allArgs.add("java");
		allArgs.add("-jar");
		allArgs.add(apktoolLocation);
		allArgs.addAll(args);
		final String apktoolOutput = ServerUtil
				.runExecutable(null, ServerUtil.list2Strings(allArgs), aaptExecDirPath, null, true).allOutput();
		debugLog(joinArgs(allArgs) + ":\n" + apktoolOutput);
	}
	
	private void execScanner(List<String> args) throws ProcessingException, IOException
	{

		
		List<String> allArgs = new LinkedList<String>();
		allArgs.add("java");
		allArgs.add("-jar");
		allArgs.add("scanner.jar");
		allArgs.addAll(args);
		final String apktoolOutput = ServerUtil
				.runExecutable(null, ServerUtil.list2Strings(allArgs), null, null, true).allOutput();
		debugLog(joinArgs(allArgs) + ":\n" + apktoolOutput);
	}
	

	public void debugLog(String message)
	{

		ServerUtil.debugLog(message);
		// System.out.println(message);
	}

	private String joinArgs(List<String> list)
	{

		StringBuffer out = new StringBuffer();
		for (String item : list)
		{
			if (out.length() > 0)
				out.append(' ');
			out.append(item);
		}
		return out.toString();
	}

	// 这里minimizeApkToolUsage还是有用的，因为有的地方要用true来访问他，所以留着
	private ResourceReconstruction reconstructManifestAndResources(String origApkFileName, boolean minimizeApkToolUsage,String apkNameInParameters)
			throws ProcessingException, IOException
	{

		ResourceReconstruction rr = new ResourceReconstruction();
		rr.apktoolExtractDir = ServerUtil.createTempDir(PathAndFileConfig.ApkExtractDir);
		rr.apktoolExtractDir.delete();
		final String apkExtractDirPath = rr.apktoolExtractDir.getAbsolutePath();
		
		rr.admobtoolExtractDir = ServerUtil.createTempDir(PathAndFileConfig.AdmobExtractDir);
		rr.admobtoolExtractDir.delete();
		final String admobExtractDirPath = rr.admobtoolExtractDir.getAbsolutePath();

		List<String> args = new LinkedList<String>();
		args.add("decode");
		if (minimizeApkToolUsage)
		{
			args.add("--no-src");
			// args.add("--no-res");
		}
		args.add("-f");
		args.add(origApkFileName);
		args.add("-o");
		args.add(apkExtractDirPath);
		// 展开要处理的apk
		execAPKTool(args);
		
		String admobApk = com.zdk.wrap.mg.AppModifier.getResourceFilePath("android/" +"admob.apk");
		args = new LinkedList<String>();
		args.add("decode");
		if (minimizeApkToolUsage)
		{
			args.add("--no-src");
			// args.add("--no-res");
		}
		args.add("-f");
		args.add(admobApk);
		args.add("-o");
		args.add(admobExtractDirPath);
		// 展开要处理的apk
		execAPKTool(args);
		
		//将so文件拷贝到APK对应目录下
		
//		ServerUtil.forEachFile(PathAndFileConfig.DynamicLlinkLibraryLocation, new ServerUtil.FileAction(){
//            
//
//            @Override
//            public void action(File file) throws IOException
//            {
//                if( file.getName().endsWith(".so"))
//                {
//                	if(new File(apktoolExtractDirPath + "\\"+"lib"+"\\"+"armeabi").exists())
//                	{
//                		writeToFile(new FileInputStream(file), apktoolExtractDirPath + "\\"+"lib"+"\\"+"armeabi"+"\\"+ file.getName());
//                	}
//                	else
//                	{
//                		new File(apktoolExtractDirPath + "\\"+"lib"+"\\"+"armeabi").mkdirs();
//                		writeToFile(new FileInputStream(file), apktoolExtractDirPath + "\\"+"lib"+"\\"+"armeabi"+"\\"+ file.getName());
//                	}
//                	if(new File(apktoolExtractDirPath + "\\"+"lib"+"\\"+"armeabi-v7a").exists())
//                	{
//                		writeToFile(new FileInputStream(file), apktoolExtractDirPath + "\\"+"lib"+"\\"+"armeabi-v7a"+"\\"+ file.getName());
//                	}
//                	else
//                	{
//                		new File(apktoolExtractDirPath + "\\"+"lib"+"\\"+"armeabi-v7a").mkdirs();
//                		writeToFile(new FileInputStream(file), apktoolExtractDirPath + "\\"+"lib"+"\\"+"armeabi-v7a"+"\\"+ file.getName());
//                	}
//                }
//            }
//        });
		
		//将assets文件拷贝到APK对应目录下
//		ServerUtil.forEachFile(PathAndFileConfig.AndroidAssets, new ServerUtil.FileAction(){
//            
//
//            @Override
//            public void action(File file) throws IOException
//            {
//            	if(new File(apktoolExtractDirPath + "\\"+"assets").exists())
//                {
//                	writeToFile(new FileInputStream(file), apktoolExtractDirPath + "\\"+"assets"+"\\"+ file.getName());
//                }
//            	else
//            	{
//            		new File(apktoolExtractDirPath + "\\"+"assets").mkdir();
//            		writeToFile(new FileInputStream(file), apktoolExtractDirPath + "\\"+"assets"+"\\"+ file.getName());
//            	}
//            }
//        });
				
		
		//扫描只存在于manifest中而在代码中不存在的Activity
//		List<String> args4Scanner = new LinkedList<String>();
//		args4Scanner.add(apkNameInParameters);
//		execScanner(args4Scanner);

//		{
			FileInputStream inStr = new FileInputStream(
					rr.apktoolExtractDir.getAbsolutePath() + File.separator + "AndroidManifest.xml");
			ByteArrayOutputStream outStr = new ByteArrayOutputStream();
			ServerUtil.copyStream(inStr, outStr);
			inStr.close();
//			// 去掉？我们没有polarisoffice
//			//去掉只存在于manifest中而在代码中不存在的Activity
			rr.origManifest = outStr.toString("UTF-8");
//			
			rr.origApktoolyml = Walkaround.getOrgApktoolyml(rr.apktoolExtractDir.getAbsolutePath() + File.separator + "apktool.yml");
//			
//					
//		}

		// 开始
		final String stringsXml = apkExtractDirPath + File.separator + "res" + File.separator + "values"
				+ File.separator + "strings.xml";
		final File stringsXmlFile = new File(stringsXml);
		rr.optStringMap = null;
		// name value对形成。
		if (stringsXmlFile.exists())
		{
			String stringsXmlText = ServerUtil.readTextFile(stringsXmlFile, "UTF-8");
			Map<String, String> strings = new java.util.HashMap<String, String>();
			Matcher m = Pattern.compile("<\\s*string\\s+name\\s*=\\s*\"([^\"]*)\"\\s*>([^<]*)<\\s*/string\\s*>")
					.matcher(stringsXmlText);
			while (m.find())
			{
				String value = m.group(2);
				value = value.replaceAll("\\\\'", "'").replaceAll("&lt;", "<").replaceAll("&gt;", ">");
				strings.put(m.group(1), value);
			}
			rr.optStringMap = strings;
		}

		// 开始修改layout.xml
//		LayoutModifier layoutModifier = new LayoutModifier(apktoolExtractDirPath);
//		layoutModifier.exeModifier();

		return rr;
	}

	// 这个函数有用吗？polarisoffice我们没有啊
	private String fixAPKToolManifestBugs(String manifest,String apkNameInParameters)
	{
		
		ArrayList<String> activityList = new ArrayList<String>();
		activityList.addAll(getElemFromFile(apkNameInParameters+".txt"));
		
		for(String activityToDelete:activityList)
		{
//			String tmp = "(<activity(.*?)android:name=\""+activityToDelete+"\"(.*?)/>)";
			
					manifest = manifest.replaceAll("(<activity(.*?)android:name=\""+activityToDelete+"\"(.*?)/>)",
							"");
		}
		for(String activityToDelete:activityList)
		{
			
			manifest = manifest.replaceAll("(<activity ([\\s\\S]*)android:name=\""+activityToDelete+"\"([\\s\\S]*?)>)([\\s\\S]*?)(</activity>)",
					"");
		}

		if (Pattern.compile("<manifest [^>]+ package=\"com\\.infraware\\.polarisoffice\\.entbiz\"", Pattern.DOTALL)
				.matcher(manifest).find())
		{
			manifest = manifest.replaceAll("(<activity [^>]+ android:name=\")(\\.print\\.NotificationConfirm\")",
					"$1com.infraware.polarisprint$2");
		}
		
//		if (Pattern.compile("android:process=\".*?\"", Pattern.DOTALL)
//				.matcher(manifest).find())
//		{
//			manifest = manifest.replaceAll("android:process=\".*?\""," ");
//		}
		return manifest;
	}
	
	private ArrayList<String> getElemFromFile(String fileName)
	{
		ArrayList<String> activityList = new ArrayList<String>();
		String filePath = System.getProperty("user.dir")+File.separator+fileName;
		try 
		{

            String encoding="GBK";

            File file=new File(filePath);

            if(file.isFile() && file.exists())
            { //判断文件是否存在

                InputStreamReader read = new InputStreamReader(new FileInputStream(file),encoding);//考虑到编码格式

                BufferedReader bufferedReader = new BufferedReader(read);

                String lineTxt = null;

                while((lineTxt = bufferedReader.readLine()) != null)
                {

                	activityList.add(lineTxt);

                }

                read.close();

            } 
            else
            {
            	
            	System.out.println("文件不存在");
            }
		}
        catch (Exception e) 
        {

	        System.out.println("读取文件内容出错");
	
	        e.printStackTrace();
        }
		
		return activityList;

	}

	private boolean hasAlreadyBeenWrapped(File apkExtractDir, File dexDumpDir)
	{

		final String cfgPropFilename = PathAndFileConfig.getCfgFilePathName(apkExtractDir);
		String thisPackage = this.getClass().getPackage().getName();
		final String serverPackageSpecificSuffix = ".android";
		if (thisPackage.endsWith(serverPackageSpecificSuffix))
			thisPackage = thisPackage.substring(0, thisPackage.length() - serverPackageSpecificSuffix.length());
		final String someWrapperClassDir = thisPackage.replace(".", File.separator); // "com/ondeego/appcentral";
		final boolean wrapperClassesExist = new File(
				dexDumpDir.getAbsolutePath() + File.separator + someWrapperClassDir).exists();
		final boolean gdSDKClassesExist = new File(
				dexDumpDir.getAbsolutePath() + "/com/zdk/mg".replace("/", File.separator)).exists();
		final boolean isUnitTest = new File(dexDumpDir.getAbsolutePath() + File.separator
				+ "com.zdk.wrap.mg.android.unittest.WrappedActivity".replace(".", File.separator) + ".smali").exists();
		final boolean isPermittedEvenWhenWrapperClassesExist = isUnitTest; // ||

		// 有wrapper
		return (((wrapperClassesExist || gdSDKClassesExist) && !isPermittedEvenWhenWrapperClassesExist)
				|| new File(cfgPropFilename).exists());
	}

	private static class NonClassFilter implements FilenameFilter
	{ // don't use anonymous class below due to Groovy/Java compiler bug that
		// once
		// creates AppModifier$2.class instead of AppModifier$1.class

		public boolean accept(File dir, String name)
		{

			return !(name.endsWith(".class") || name.endsWith(".smali"));
		}
	}

	private static class SmaliFilter implements FilenameFilter
	{ // don't use anonymous class below due to Groovy/Java compiler bug that
		// once creates AppModifier$2.class instead of AppModifier$1.class

		public boolean accept(File dir, String name)
		{

			return name.endsWith(".smali");
		}
	}

	private void copyWrapperLib(File targetDirectory,
			File resourcesTarget, /* int optMinSDKVersion, */
			String wrappedManifestFilename, String reconstructedResourceDirPath, String packageName)
			throws ProcessingException, IOException
	{

		synchronized (getClass())
		{
			if (cacheDir == null)
				cacheDir = ServerUtil.createTempDir(PathAndFileConfig.CacheDir);

			String wrapperlibLocation = PathAndFileConfig.getWrapperLibLocation();
			String googlePlayLibLocation = PathAndFileConfig.getGooglePlayLibLocation();
			String androidSupportV4Location = PathAndFileConfig.getAndroidSupportV4Location();

			if (!(wrapperlibLocation != null && new File(wrapperlibLocation).exists()))
				throw new ProcessingException("Cannot locate android wrapper jar.");

			final String wrapperTimestamp = PathAndFileConfig.getWrapperTimestampFileLocation(cacheDir);

			final String cachedSmaliDir = PathAndFileConfig.getCachedSmaliDir(cacheDir);

			final String cachedResourcesDir = PathAndFileConfig.getResourceDir(cacheDir);

			final String cachedFrameworkDir = PathAndFileConfig.getCachedFrameworkDir(cacheDir);

			final String cachedFrameworkRelPath = PathAndFileConfig.CachedFrameworkRelPath;

			cachedFrameworkAbsPath = PathAndFileConfig.getCachedFrameworkAbsPath(cacheDir);

			String shrinkedLibJar = PathAndFileConfig.getShrinkedWrapperLib(cacheDir);
			
			String mgJar = com.zdk.wrap.mg.AppModifier.getResourceFilePath("android/mg.jar");
			
			
			String v4PackagePath = targetDirectory+File.separator+"android"+File.separator+"support"+File.separator+"v4";
			
//			File v4Packagefile = new File(v4PackagePath);
			
//将shrink的位置放到后面，为了不将apk可能用到的函数shrink没了。
//			if(v4Packagefile.exists())
//			{
//				String[] inputJars =
//					{ ServerUtil.getRealFilePath(wrapperlibLocation), ServerUtil.getRealFilePath(googlePlayLibLocation),
//							ServerUtil.getRealFilePath(mgJar) };
//				ProguardHelper.startShrink(inputJars, shrinkedLibJar);
//			}
//			else
//			{
//				String[] inputJars =
//					{ ServerUtil.getRealFilePath(wrapperlibLocation), ServerUtil.getRealFilePath(googlePlayLibLocation),
//							ServerUtil.getRealFilePath(androidSupportV4Location), ServerUtil.getRealFilePath(mgJar) };
//				ProguardHelper.startShrink(inputJars, shrinkedLibJar);
//			}
			

			if (!new File(wrapperTimestamp).exists() || isNewer(wrapperlibLocation, wrapperTimestamp))
			{
				new FileOutputStream(wrapperTimestamp).close();
				final String wrapperDex = PathAndFileConfig.getWrapperDex(cacheDir);

				if (new File(wrapperDex).exists())
					ServerUtil.removeRecursively(wrapperDex, true, null);

				if (new File(cachedResourcesDir).exists())
					ServerUtil.removeRecursively(cachedResourcesDir, true, null);
				ServerUtil.mkdir(new File(cachedResourcesDir));

				List<String> dxArgs = new LinkedList<String>();
				dxArgs.add(dxPath);
				dxArgs.add("--dex");
				dxArgs.add("--output=" + wrapperDex);
//				dxArgs.add(shrinkedLibJar);
				
				dxArgs.add(wrapperlibLocation);
				dxArgs.add(mgJar);
				dxArgs.add(androidSupportV4Location);
				
				// dxArgs.add(wrapperlibLocation);
				// todo:以后改成自动的，现在都是手动的
				// dxArgs.add(androidSupportV4Location);
				// dxArgs.add(googlePlayLibLocation);

				String mgAar = com.zdk.wrap.mg.AppModifier.getResourceFilePath("android/mg.aar");
				// String gdAddJar =
				// com.zdk.wrap.mg.AppModifier.getResourceFilePath("android/gdadd.jar");
//				if (!(mgJar != null && new File(mgJar).exists()))
//					throw new ProcessingException("Cannot locate mg.aar .");
				// if (!(gdAddJar != null && new File(gdAddJar).exists()))
				// throw new ProcessingException("Cannot locate gdadd.jar .");
				// dxArgs.add(mgJar);
				// dxArgs.add(gdAddJar);

				ServerUtil.unpackJar(cachedResourcesDir, mgAar, null, new NonClassFilter());
				
				// ServerUtil.unpackJar(cachedResourcesDir, gdAddJar, null, new
				// NonClassFilter());

				final RunExecResult dxOutputResult = ServerUtil.runExecutable(dxArgs, true);
				final String dxOutput = dxOutputResult.allOutput();
				if (dxOutputResult.getExitStatus() != 0 || dxOutput.indexOf("trouble processing:") >= 0)
					throw new ProcessingException("dx failed: " + dxOutput);
				debugLog("dx wrapper.jar:\n" + dxOutput);
				if (new File(cachedSmaliDir).exists())
					ServerUtil.removeRecursively(cachedSmaliDir, true, null);
				ServerUtil.removeRecursively(shrinkedLibJar, true, null);
				// org.jf.baksmali.Main.main(new String[] {"disassemble",
				// "-o",cachedSmaliDir, wrapperDex });
				try
				{
					ServerUtil.debugLog("baksmali.jar execute");
					ServerUtil.runExecutable(new String[]
					{ "java", "-jar", PathAndFileConfig.baksmaliPath, "disassemble", wrapperDex, "-o", cachedSmaliDir },
							true);
				} catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

//				ServerUtil.unpackJar(cachedSmaliDir, wrapperlibLocation, null, new SmaliFilter());
				redirectSupportForSDK(cachedSmaliDir);

				ServerUtil.unpackJar(cachedResourcesDir, wrapperlibLocation, null, new NonClassFilter()); // e.g.
				// libgdWrapAndroid.so

				{
					String apktoolLocation = getApktoolLocation();
					if (new File(cachedFrameworkDir).exists())
						ServerUtil.removeRecursively(cachedFrameworkDir, true, null);
					ServerUtil.mkdir(new File(cachedFrameworkDir));
					ServerUtil.unpackJar(cachedFrameworkDir, apktoolLocation, null, new FilenameFilter()
					{

						@Override
						public boolean accept(File dir, String name)
						{

							return (dir.getPath() + File.separator + name)
									.endsWith(File.separator + cachedFrameworkRelPath);
						}
					});
				}
			}

			if (useGoodDynamics)
			{
				File rFileDir = ServerUtil.createTempDir("rfile");
				try
				{
					File srcDir = new File(rFileDir.getAbsolutePath() + File.separator + "src");
					ServerUtil.mkdir(srcDir);
					File classesDir = new File(rFileDir.getAbsolutePath() + File.separator + "classes");
					ServerUtil.mkdir(classesDir);
					// 用sdk的plantforms中的Android-25中的android。jar替换掉了cachedFrameworkAbsPath
					// ， cachedFrameworkAbsPath值为frameworkLocation
					androidJarVersion = 25;
					final String androidjarPath = getAndroidJarPath(androidJarVersion);
					final String aaptPackageOutput = ServerUtil.runExecutable
							(new String[]
									{ 
											aaptPath, "package",
											"--extra-packages", 
											":com.zdk.mg",
							// "--generate-dependencies",
											"--auto-add-overlay", 
											"-f", "-m", 
											"-I", androidjarPath, // -I后参数原为cachedFrameworkAbsPath
											"-S", reconstructedResourceDirPath,
											"-S", cachedResourcesDir + File.separator + "res", // (*s-option-1*)
							// "-S",gdResDir.getAbsolutePath()+File.separator+"res",
											"-M", wrappedManifestFilename,
											"-J", srcDir.getAbsolutePath() 
									}, true).allOutput();
					debugLog("aapt package:\n" + aaptPackageOutput);
					// RunExecResult ree=ServerUtil.runExecutable(new String[] {
					// "which","javac" },false);
					
					
					ServerUtil.runExecutable(srcDir, new String[]
					{ "javac", "-source", "1.6", "-target", "1.6", // prevent a
																	// Java 7
																	// compiler
																	// from
																	// generating
																	// version
																	// 51.0
																	// class
																	// files
																	// that dx
																	// cannot
																	// process
							"-d", classesDir.getAbsolutePath(),
							packageName.replace('.', File.separatorChar) + File.separator + "R.java",
							"com/zdk/mg/R.java" }, null, null, true);
					
					
					String rFileDex = rFileDir.getAbsolutePath() + File.separator + "rfile.dex";
					ServerUtil.runExecutable(new String[]
					{ dxPath, "--dex", "--output=" + rFileDex, classesDir.getAbsolutePath()}, true);
					// org.jf.baksmali.Main.main(new String[] {
					// "disassemble","-o", targetDirectory.getAbsolutePath(),
					// rFileDex });
					try
					{
						//创建临时目录，将apk的R文件和sdk的R文件存在里面，之后取走sdk的R文件，因为aapt编译出来的apk的R文件可能会少东西，所以不进行替换
						File backSmaliPath  = ServerUtil.createTempDir(PathAndFileConfig.baksmaliPath);
						
						ServerUtil.debugLog("baksmali.jar execute");
						ServerUtil.runExecutable(new String[]
								{ 
										"java", "-jar", PathAndFileConfig.baksmaliPath, 
										"disassemble", rFileDex, 
//										"-o",targetDirectory.getAbsolutePath() 
										"-o",backSmaliPath.getPath()
								}, true);
						
//						ManifestModifier.appInfo.getPackageName(); 获得原apk的包名
						if(!CommandLine.ifNotAaptOriRes)
						{
							//原apk以com开头，删除除了zdk以外其他的文件
							File[] filesOfCom = new File(backSmaliPath.getPath()+File.separator+"com").listFiles();
							for(int j =0 ; j<filesOfCom.length ; j++)
							{
								if(!filesOfCom[j].getName().equals("zdk"))
								{
									ServerUtil.removeRecursively(filesOfCom[j].getPath(), true, null);
								}
							}
							//如果原apk包名不是com开头，删除不是com的文件夹
							File[] filesOfBackSmaliPath = new File(backSmaliPath.getPath()).listFiles();
							for(int j =0 ; j<filesOfBackSmaliPath.length ; j++)
							{
								if(!filesOfBackSmaliPath[j].getName().equals("com"))
								{
									ServerUtil.removeRecursively(filesOfBackSmaliPath[j].getPath(), true, null);
								}
							}
						}
						ServerUtil.copyFilesRecursively(backSmaliPath.getPath(), targetDirectory.getAbsolutePath(),false, null);
						
						//检测res中名称以png结尾的文件是否为png图片，如果不是转换为png图片
						if(CommandLine.ifTransPngIm == true)
						{
							File pngPath = new File(targetDirectory.getParent()+File.separator+"res");
							ImapgeToPng.imToPng(pngPath.getPath());
						}
					} 
					catch (IOException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				} finally
				{
					deleteDir(rFileDir);
				}
			}

			mergeResourceFiles("res" + File.separator + "values" + File.separator + "strings.xml", cachedResourcesDir,
					resourcesTarget);
			mergeResourceFiles("res" + File.separator + "values" + File.separator + "styles.xml", cachedResourcesDir,
					resourcesTarget);
			// TODO: also include GD's values-v12 and values-v8 directories
			// 合并smali

			ServerUtil.copyFilesRecursively(cachedSmaliDir, targetDirectory.getAbsolutePath(), true,
					(FilenameFilter) null);

			if(Multidexer.execute(new File(targetDirectory.getAbsolutePath()).getParent())==false)
			{
				throw new IOException("multidexer.execute raise an exception ");
			}

			ServerUtil.copyFilesRecursively(cachedResourcesDir + "\\jni", resourcesTarget.getAbsolutePath() + "\\lib",
					true, (FilenameFilter) null);
			// ServerUtil.copyFilesRecursively(cachedResourcesDir+"\\libs",
			// resourcesTarget.getAbsolutePath()+"\\lib",true, (FilenameFilter)
			// null);
			// ServerUtil.removeRecursively(cachedResourcesDir+"\\libs", true,
			// null);

			ServerUtil.copyFilesRecursively(cachedResourcesDir, resourcesTarget.getAbsolutePath(), true,
					new FilenameFilter()
					{

						public boolean accept(File dir, String name)
						{

							return !(dir.getParentFile().getName().equals("res") && dir.getName().equals("values")
									&& (name.equals("strings.xml") || name.equals("styles.xml")));
						}
					});
		}
	}

	private void redirectSupportForSDK(String cachedSmaliDir) throws IOException {
		ServerUtil.forEachFile(cachedSmaliDir, new ServerUtil.FileAction() {
			public void action(File file) throws IOException {
				String absFilePath = file.toString();
				assert absFilePath.startsWith(cachedSmaliDir);
				String relFilePath = absFilePath.substring(cachedSmaliDir.length());
				String wrapperPath = File.separator + "com" + File.separator + "zdk" + File.separator + "wrap" + File.separator + "mg";
				if (!relFilePath.startsWith(wrapperPath) && relFilePath.endsWith(".smali")) {
					final String oldSmaliText = ServerUtil.readTextFile(file, "UTF-8");
					String newSmaliText = oldSmaliText;

					Pattern pattern = Pattern.compile("Landroid/support");
					Matcher thisClassMatcher = pattern.matcher(newSmaliText);
					newSmaliText = pattern.matcher(newSmaliText).replaceAll("Lweigu/android/support");
					if (!oldSmaliText.equals(newSmaliText)) {
						FileWriter out = new FileWriter(file);
						out.write(newSmaliText);
						out.close();
					}
				}
			}
		});
		String originPath = cachedSmaliDir + File.separator + "android" + File.separator + "support";
		String targetPath = cachedSmaliDir + File.separator + "weigu" + File.separator + "android" + File.separator + "support";
		ServerUtil.copyFilesRecursively(originPath, targetPath , true, null);
		ServerUtil.removeRecursively(originPath, true, null);
	}

	private void mergeResourceFiles(String resRelName, String cachedResourcesDir, File resourcesTarget)
			throws IOException, ProcessingException
	{

		final File wrapperResFile = new File(cachedResourcesDir + File.separator + resRelName);
		if (wrapperResFile.exists())
		{
			final File appResFile = new File(resourcesTarget.getAbsolutePath() + File.separator + resRelName);
			if (appResFile.exists())
			{
				StringBuffer appResContents = new StringBuffer(ServerUtil.readTextFile(appResFile, "UTF-8"));
				String wrapperResContents = ServerUtil.readTextFile(wrapperResFile, "UTF-8");
				int pos = appResContents.indexOf("</resources>");
				if (pos < 0)
					throw new ProcessingException("Cannot find insertion point in " + resRelName);

				Matcher m = Pattern.compile("<resources[^>]*>(.*)</resources>", Pattern.DOTALL)
						.matcher(wrapperResContents);
				if (!m.find())
					throw new ProcessingException("Cannot parse wrapper's " + resRelName);
				appResContents.insert(pos, m.group(1));

				ServerUtil.writeTextFile(appResContents.toString(), appResFile, "UTF-8");
			} else
			{
				ServerUtil.copyFile(wrapperResFile.getAbsolutePath(), appResFile.getAbsolutePath());
			}
		}
	}

	private boolean isNewer(String file1, String file2)
	{

		return new File(file1).lastModified() > new File(file2).lastModified();
	}

	static ArrayList<File> filelist;

	/**
	 * 合并smali,smali_classes2...
	 * 
	 * @param smaliPath
	 * @throws IOException
	 */
	private void gatherSmali(String smaliPath) throws IOException
	{

		filelist = new ArrayList<>();
		// 当apk本身存在多个dex时，将除第一个外的smali全部移动到第一个文件夹中
		for (int smalinum = 2; smalinum < 99; smalinum++)
		{
			String childSmaliPath = smaliPath + "_classes" + smalinum;
			File childSmali = new File(childSmaliPath);
			if (childSmali.exists())
			{
				ArrayList<File> fileOfMultiClass = new ArrayList<>();
				fileOfMultiClass = getFileList(childSmaliPath);
				for (File file : fileOfMultiClass)
				{
					String targetPath = smaliPath + file.getAbsolutePath().substring(childSmaliPath.length(),
							file.getAbsolutePath().indexOf(file.getName())) + file.getName();
					debugLog("targetPath= " + targetPath);
					ServerUtil.copyFile(file.getAbsolutePath(), targetPath);
					file.delete();
				}
			} else
			{
				break;
			}
			// 内容转移后删除smali_classes*
			deleteDir(childSmali);
		}
	}

	public static ArrayList<File> getFileList(String filepath)
	{

		filelist = new ArrayList<>();
		try
		{
			ServerUtil.forEachFile(filepath, new FileAction()
			{

				@Override
				public void action(File file) throws IOException
				{

					if (file.getName().endsWith(".smali"))
					{
						filelist.add(file);
					}
				}
			});
		} catch (IOException e1)
		{
			e1.printStackTrace();
		}
		return filelist;
	}
}
