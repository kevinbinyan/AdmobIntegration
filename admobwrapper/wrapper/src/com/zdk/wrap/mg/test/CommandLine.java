package com.zdk.wrap.mg.test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import com.zdk.wrap.mg.AppModifier.AddedFile;
import com.zdk.wrap.mg.AppModifier.ProcessingException;
import com.zdk.wrap.mg.AppModifier.WrappingParameters;
import com.zdk.wrap.mg.PackagedPrivateKey;
import com.zdk.wrap.mg.ServerUtil;
import com.zdk.wrap.mg.android.AppInfo;

public class CommandLine {

	public static boolean ifAllowBackup = false;
	public static boolean ifDebuggable = false;
	public static boolean ifNotAaptOriRes = false;
	public static boolean ifTransPngIm = true;
	public static boolean addToSecDeskTop = false;
	
	public static void main(final String[] args) {
		try {
			com.zdk.wrap.mg.AppModifier.setVerbose(true);
			com.zdk.wrap.mg.AppModifier.unpackDistribution(); // before
																// loadConfigProperties()
																// in case that
																// a config file
																// is included
																// already there
			Properties props = null;
			String origAppFilename = null;
			String wrappedAppFilename = null;
			String signKeyPassword = null;
			String signKeyId = null;
			String signKeyFilename = null;
			String capabilityManifestFilename = null;
			String generatedCHeaderFilename = null;
			String optGDApplicationID = null;
			String optGDApplicationVersion = null;
			String optGDConfigInfo = null;
			List<String> optGDLogging = null;
			String optAppTag = null;
			boolean debugLoggingEnabled = false;
			boolean developerDebugMode = false;
			boolean inplaceModification = false;
			String copyBeforeSrc = null;
			String copyBeforeDest = null;
			boolean doInstall = false;
			boolean doRun = false;
			
			List<com.zdk.wrap.mg.AppModifier.AddedFile> addedFiles = new LinkedList<com.zdk.wrap.mg.AppModifier.AddedFile>();
			Set<String> disabledAreas = new HashSet<String>();
			String focusArea = null;
			for (int argc = 0; argc < args.length; argc++) {
				final String arg = args[argc];
				if (argc + 1 < args.length && arg.equals("-config")) {
					if (props == null)
						props = new Properties();
					setProp(props);
					// props.load(new FileInputStream(args[++argc]));
				}
				// else if (arg.equals("-clean"))
				
				//判断是否开启allowbackup，默认不开启
				else if ( arg.equals("allowbackup"))
				{
					ifAllowBackup = true;
					System.out.println("ifAllowBackup = true");
				}
				//判断是否开启debuggable，默认不开启
				else if ( arg.equals("debuggable"))
				{
					ifDebuggable = true;
					System.out.println("ifDebuggable = true");
				}
				//判断是否开启aaptorires，默认不开启
				else if ( arg.equals("aaptorires"))
				{
					ifNotAaptOriRes = true;
					System.out.println("ifNotAaptOriRes = true");
				}
				//判断是否开启png图片格式检测，将后缀为png但是不是png格式的文件转换为整整的png图片
				else if (arg.equals("ifTransPngIm")){
					ifTransPngIm = false;
					System.out.println("ifTransPngIm = false");
				}
				//判断是否要加入安全桌面，如果加入会进行相应修改  http://kb.corp.weigutech.com/pages/viewpage.action?pageId=15139435
				else if (arg.equals("addToSecDeskTop"))
				{
					addToSecDeskTop = true;
					System.out.println("addToSecDeskTop = true");
				}
				else if (argc + 1 < args.length && arg.equals("-in")) 
				{
					origAppFilename = args[++argc];
					if (optAppTag == null)
						optAppTag = origAppFilename;
				} 
				else if (argc + 1 < args.length && arg.equals("-out")) 
				{
					wrappedAppFilename = args[++argc];
					if (inplaceModification)throw new IOException("Cannot use -inplace when using -out.");
				} 
				else if (arg.equals("-inplace")) 
				{
					inplaceModification = true;
					if (wrappedAppFilename != null)throw new IOException("Cannot use -out when using -inplace.");
				} 
				else if (argc + 2 < args.length && arg.equals("-copy-before")) 
				{
					copyBeforeSrc = args[++argc];
					copyBeforeDest = args[++argc];
				} 
				else if (argc + 1 < args.length && arg.equals("-signkey")) 
				{
					signKeyFilename = args[++argc];
				} 
				else if (argc + 1 < args.length && arg.equals("-signkeypassword")) 
				{
					signKeyPassword = args[++argc];
				} 
				else if (argc + 1 < args.length && arg.equals("-signkeyid")) 
				{
					signKeyId = args[++argc];
				} 
				else if (argc + 1 < args.length && arg.equals("-capabilitymanifest")) 
				{ // now
																							// somewhat
																							// obsolete
																							// because
																							// a
																							// full
																							// manifest
																							// is
																							// created
																							// by
																							// runTest()
																							// every
																							// time
					capabilityManifestFilename = args[++argc];
				} 
				else if (argc + 1 < args.length && arg.equals("-disable-area")) 
				{
					disabledAreas.add(args[++argc]);
				} 
				else if (argc + 1 < args.length && arg.equals("-focus-on")) 
				{
					focusArea = args[++argc];
				} 
				else if (argc + 1 < args.length && arg.equals("-generate-c-header")) 
				{
					generatedCHeaderFilename = args[++argc];
				} 
				else if (argc + 1 < args.length && arg.equals("-gdappid")) 
				{
					optGDApplicationID = args[++argc];
				} 
				else if (argc + 1 < args.length && arg.equals("-gdappversion")) 
				{
					optGDApplicationVersion = args[++argc];
				} 
				else if (argc + 1 < args.length && arg.equals("-gdconfiginfo")) 
				{
					optGDConfigInfo = args[++argc];
				} 
				else if (argc + 1 < args.length && arg.equals("-gdlogging")) 
				{
					String filterNames = args[++argc];
					optGDLogging = new LinkedList<String>();
					if (filterNames.equals("none")) 
					{
						// leave empty
					} 
					else 
					{
						for (String filterName : filterNames.split(","))optGDLogging.add(filterName);
					}

				} 
				else if (arg.equals("-debuglog")) 
				{
					debugLoggingEnabled = true;
				} 
				else if (argc + 2 < args.length && arg.equals("-add")) 
				{
					String filename = args[++argc];
					String targetPath = args[++argc];
					InputStream inStr = new FileInputStream(filename);
					addedFiles.add(new com.zdk.wrap.mg.AppModifier.AddedFile(targetPath, inStr));
				} 
				else if (arg.equals("-developerDebugMode")) 
				{
					developerDebugMode = true;
				} 
				else if (arg.equals("-install")) 
				{
					doInstall = true;
				} 
				else if (arg.equals("-run")) 
				{
					doRun = true;
					doInstall = true;
				} 
				else 
				{
					throw new Error("Illegal option " + arg);
				}
			}

			if (copyBeforeSrc != null && copyBeforeDest != null) {
				if (new File(copyBeforeDest).exists())
					ServerUtil.removeRecursively(copyBeforeDest, true, null);
				ServerUtil.copyFilesRecursively(copyBeforeSrc, copyBeforeDest, true, null);
			}

			{
				if (!com.zdk.wrap.mg.AppModifier.isConfigured()) // usually done
																	// within
																	// unpackDistribution(),
																	// this is a
																	// fallback
																	// only
					com.zdk.wrap.mg.AppModifier.configure("resources"); // before
																		// loadConfigProperties()

				if (props == null)
					props = com.zdk.wrap.mg.AppModifier.loadConfigProperties(false);

				/*
				 * use "GDProxySetup.all" instead to avoid the need to keep this
				 * file consistent with GDProxySetup.mm final String[]
				 * gdProxySetupFeatures=new String[] { // cf. GDProxySetup.mm
				 * "NSURLCache", "URLProtocol", "FileURLProtocol", "Alert",
				 * "URL", "FileManager", "NSData", "String", "NSKeyedArchiver",
				 * "NSKeyedUnarchiver", "NSXMLParser", "NSError",
				 * "NSOutputStream", "NSInputStream", "NSFileHandle",
				 * "NSBundle", "UIImage", "CIImage", "UIWindow", "FileWrapper",
				 * "NSPersistentStoreCoordinator", "UIViewController",
				 * "UIWebViewDelegate", "UIDocumentInteractionController",
				 * "PreviewController", };
				 */

				if (focusArea != null) {
					Set<String> allCFuncsAreas = new HashSet<String>();
					final String expectedExt = ".cfuncs";
					for (String fileName : new File(com.zdk.wrap.mg.AppModifier.getResourceFilePath("ios/"))
							.list(new FilenameFilter() {
								@Override
								public boolean accept(File dir, String name) {
									return name.endsWith(expectedExt);
								}
							})) {
						String areaOfFile = fileName.substring(0, fileName.length() - expectedExt.length());
						allCFuncsAreas.add(areaOfFile);
						// cfcntl, cfile, cfsocket, csys_socket, csys_uio,
						// csys_unistd, sqlite
					}

					if (focusArea.equals("startup")) {
						disabledAreas.addAll(allCFuncsAreas);
						disabledAreas.add("GDProxySetup.all");
					} else {
						throw new IOException("Unknown focus area '" + focusArea + "'");
					}
				}

				{
					String cfgProp = props.getProperty("appguard.ios.wrappingDisableList");
					if (cfgProp != null) {
						for (String area : cfgProp.split(","))
							disabledAreas.add(area);
					}
				}

				com.zdk.wrap.mg.android.AppModifier.configure(props.getProperty("appguard.aapt"),
						props.getProperty("appguard.dx"), props.getProperty("appguard.zipalign"));
				if (developerDebugMode) {
					com.zdk.wrap.mg.android.AppModifier.enableDeveloperDebugMode();
				}
			}

			// generate the C header, ignore other options
			// This must be done _after_ configuration.
			if (generatedCHeaderFilename != null) {
				if (!disabledAreas.isEmpty())
					throw new IOException(
							"Generating the header file(s) is only permitted when including all functional areas, e.g. options -disable-area and -focus-on are not permitted.");
				System.out.println("Wrote C header: \'" + generatedCHeaderFilename + "\'.  Ignoring other arguments");
				// don't, might skip cleanup section: return;
			} else if (origAppFilename != null && (wrappedAppFilename != null || inplaceModification)) {
				try {
					com.zdk.wrap.mg.AppModifier appModifier = new com.zdk.wrap.mg.AppModifier();

					final com.zdk.wrap.mg.AppModifier.BinCodeType codeType = appModifier
							.getBinCodeFromFileName(origAppFilename);

					final String signKeyPasswordFinal = signKeyPassword;
					final String signKeyIdFinal = signKeyId;
					final String signKeyFilenameFinal = signKeyFilename;
					PackagedPrivateKey optSignKey = signKeyFilename == null ? null : new PackagedPrivateKey() {
						@Override
						public String getOptPassword() {
							return signKeyPasswordFinal; // possibly null
						}

						@Override
						public String getOptIdentifier() {
							String keyId = signKeyIdFinal;
							if (keyId == null && codeType == com.zdk.wrap.mg.AppModifier.BinCodeType.IPA) {
								// try to deduce the key ID in case of iOS:
								try {
									java.security.cert.X509Certificate cert = pickCertificateFromKeystore(
											getEncodedData(), getOptPassword());
									if (cert != null) {
										keyId = getCertThumbprint(cert);
									}
								} catch (GeneralSecurityException e) {
									e.printStackTrace();
									// keyId stays as null
								} catch (IOException e) {
									e.printStackTrace();
									// keyId stays as null
								}
							}
							return keyId; // possibly null
						}

						@Override
						public byte[] getEncodedData() throws IOException {
							ByteArrayOutputStream signKeyData = new ByteArrayOutputStream();
							FileInputStream signKeyIn = new FileInputStream(signKeyFilenameFinal);
							ServerUtil.copyStream(signKeyIn, signKeyData);
							signKeyIn.close();
							return signKeyData.toByteArray();
						}
					};

					if (debugLoggingEnabled)
						appModifier.enableDebugLogging();

					/*
					 * deduce device platform from file extension:
					 * com.good.wrap.gd.AppModifier.BinCodeType codeType; if
					 * (origAppFilename.toLowerCase().endsWith(".apk"))
					 * codeType=com.good.wrap.gd.AppModifier.BinCodeType.APK;
					 * else if (origAppFilename.toLowerCase().endsWith(".ipa"))
					 * codeType=com.good.wrap.gd.AppModifier.BinCodeType.IPA;
					 * else throw new IOException(
					 * "Unknown application format, expected .apk or .ipa .");
					 */

					if (codeType == com.zdk.wrap.mg.AppModifier.BinCodeType.IPA
							&& !(optGDApplicationID != null && optGDApplicationVersion != null))
						throw new IOException("For an IPA, you now must specify both -gdappid and -gdappversion.");

					if (capabilityManifestFilename != null) {
						OutputStreamWriter out = new OutputStreamWriter(
								new FileOutputStream(capabilityManifestFilename));
						out.write(appModifier.getCapabilityWrapManifest(codeType));
						out.close();
						System.out.println("Wrapping capability manifest written to " + capabilityManifestFilename);
					}

					BufferedInputStream origAppInStr = null;
					try {
						// do the wrapping:
						// WrappingParameters parameters=new
						// WrappingParameters(optSignKey,optGDApplicationID,optGDApplicationVersion,optGDConfigInfo,optAppTag,addedFiles,optGDLogging);

						// example for SampleProjectForWrap.apk
						optGDApplicationID = "com.example.sampleprojectforwrap";
						optGDApplicationVersion = "1.0";
						WrappingParameters parameters = new WrappingParameters(optSignKey, optGDApplicationID,
								optGDApplicationVersion, optGDConfigInfo, optAppTag, addedFiles, optGDLogging);

						com.zdk.wrap.mg.AppModifier.ProcessingResult result;

						if (inplaceModification) {
							File appBinDir = new File(origAppFilename);
							result = appModifier.processInPlace(codeType, /* optSignKey, */ appBinDir, parameters);
						} else {
							origAppInStr = new BufferedInputStream(new FileInputStream(origAppFilename));
							result = appModifier.process(codeType, true, origAppInStr, parameters); 
						}

						if (wrappedAppFilename != null) {
							// store the wrapped app as a file:
							BufferedOutputStream outStr = new BufferedOutputStream(
									new FileOutputStream(wrappedAppFilename));
							InputStream wrappedStr = result.getOptWrappedBinCodeStr();
							ServerUtil.copyStream(wrappedStr, outStr);
							outStr.close();
							wrappedStr.close(); 
							System.out.println("Wrapped application: " + wrappedAppFilename);
						}

						if (result instanceof com.zdk.wrap.mg.android.AppModifier.ProcessingResult) {
							com.zdk.wrap.mg.android.AppModifier.ProcessingResult resultAndroid = (com.zdk.wrap.mg.android.AppModifier.ProcessingResult) result;
							// com.good.wrap.gd.android.AppModifier.AppInfo
							// appInfo=resultAndroid.getAppInfo();

							if (wrappedAppFilename != null) {
								final String wrapManifestFilename = wrappedAppFilename + ".wrapmanifest";
								OutputStreamWriter out = new OutputStreamWriter(
										new FileOutputStream(wrapManifestFilename));
								out.write(resultAndroid.getFullWrapManifest());
								out.close();
								System.out.println("Manifest: " + wrapManifestFilename);
							}

							if (doInstall && wrappedAppFilename != null) {
								String adbPath = null;
								{
									ServerUtil.RunExecResult whichAdbResult1 = ServerUtil
											.runExecutable(new String[] { "/bin/sh", "-c", "which adb" }, false);
									if (whichAdbResult1.getExitStatus() == 0)
										adbPath = whichAdbResult1.getStdOut().trim();
									else {
										ServerUtil.RunExecResult whichAdbResult2 = ServerUtil.runExecutable(
												new String[] { "/bin/bash", "-l", "-c", "which adb" }, false);
										if (whichAdbResult2.getExitStatus() == 0)
											adbPath = whichAdbResult2.getStdOut().trim();
										else {
											for (String tryPath : new String[] { // adding
																					// the
																					// paths
																					// to
																					// ServerUtil.runExecutable()
																					// instead
																					// did
																					// not
																					// work
																					// 20121130
																					// on
																					// a
																					// MacBookPro
													System.getProperty("user.home")
															+ "/java/android-sdk-mac_x86/platform-tools"
													// add more here
											}) {
												String adbTry = tryPath + File.separator + "adb";
												if (new File(adbTry).exists()) {
													adbPath = adbTry;
													break;
												}
											}
										}
									}
									if (adbPath == null)
										throw new ProcessingException("Warning: Cannot find adb executable.");
								}

								// sanity check for connected devices, otherwise
								// "adb install" will block and say "error:
								// device not found, - waiting for device -..."
								{
									ServerUtil.RunExecResult deviceTestResult = ServerUtil
											.runExecutable(new String[] { adbPath, "devices" }, true);
									if (deviceTestResult.getExitStatus() != 0 || deviceTestResult.getStdOut()
											.matches("(?s)^\\s*List of devices attached\\s*$"))
										throw new IOException(
												"No devices detected by adb: " + deviceTestResult.allOutput());
								}

								{
									System.out.println("Installing application on device...");
									ServerUtil.RunExecResult installResult = ServerUtil.runExecutable(
											new String[] { adbPath, "install", "-r", wrappedAppFilename }, true);
									if (installResult.getExitStatus() != 0)
										throw new IOException("adb install failed: " + installResult.allOutput());
								}

								if (doRun) {
									System.out.println("Running application on device...");
									AppInfo appInfo = resultAndroid.getAppInfo();
									ServerUtil.RunExecResult launchResult = ServerUtil
											.runExecutable(new String[] { adbPath, "shell", "am", "start", "-n",
													appInfo.getPackageName() + (appInfo.getOptMainActivityName() != null
															? "/" + appInfo.getOptMainActivityName() : ""),
													"--activity-brought-to-front", "-W" // wait
																						// such
																						// that
																						// sleep()
																						// (see
																						// below)
																						// before
																						// key
																						// entries
																						// gets
																						// more
																						// reliable
									}, true); // for GD apps causes for unknown
												// reason:
												// "java.lang.RuntimeException:
												// onCreate early termination
												// required."
									if (launchResult.getExitStatus() != 0)
										throw new IOException("adb launch failed: " + launchResult.allOutput());
								}
							}
						}
					} finally {
						if (origAppInStr != null)
							origAppInStr.close();
					}
				} catch (ProcessingException e) {
					e.printStackTrace();
					System.exit(1); // required e.g. for ant
				}
			} else {
				System.out.print("Usage:\n" + "  -config [filename] :             configuration properties file\n"
						+ "  -in [filename] :                 original application file\n"
						+ "  -out [filename] :                output file containing wrapped application\n"
						+ "  -focus-on [focus-area-name] :    focus processing on a certain area\n"
						+ "  -signkey [filename] :            signature key file, for iOS: a *.p12\n"
						+ "  -signkeypassword [password] :    signature key password, for iOS: p12 export password\n"
						+ "  -signkeyid [id] :                signature key ID, for iOS: e.g. the SHA-1 fingerprint in hex without blanks, automatically deduced for iOS if not provided\n"
						+ "  -capabilitymanifest [filename] : output a capability manifest in addition to the per-application manifest\n"
						+ "  -generate-c-header [filename] :  write iOS C header file\n"
						+ "  -gdappid [GD Application ID] :   provide the GD Application ID of the app\n"
						+ "  -gdappversion [GD Application Version] :  provide the GD Application Version of the app\n"
						+ "  -gdconfiginfo [config] :         use a specific GD server, i.e. not production, e.g. dev1\n"
						+ "  -gdlogging [\"none\" or comma-separated filters]: set GD's GDConsoleLogger (iOS or Android)\n"
						+ "  -add [filename] [target path]:   for internal non-production use only, add and package a file into the app, always use '/' as separator in target path\n"
						+ "  -debuglog :                      enable logging for debugging issues (potentially revealing private info in the log)\n"
						+ "  -developerDebugMode :            enable internal debug mode for developing and testing\n"
						+ "  -inplace :                       experimental and for internal debugging use only, modify application in-place instead of using -out\n"
						+ "  -disable-area [area-name]:       disable wrapping of functions belonging to the named area\n"
						+ "  -install :                       install application on default device or emulator after wrapping\n"
						+ "  -run :                           run application on default device or emulator after wrapping, implies -install\n");
			}

			for (AddedFile file : addedFiles)
				file.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1); // required e.g. for ant
		} catch (java.lang.OutOfMemoryError t) {
			System.out.println("The Good Dynamics wrapping engine ran out of Java heap space.");
			System.out.println("Increase the heap space by adding  -Xmx1g to the command line, ie:");
			System.out.println("  java -Xmx1g -jar gdwrapper.jar ...");
		}
	}

	private static java.security.cert.X509Certificate pickCertificateFromKeystore(byte[] encodedData,
			String optPassword) throws GeneralSecurityException, IOException {
		// CryptoHelper.addSecurityProviders()
		java.security.cert.X509Certificate pickedCert = null;
		KeyStore keyStore = KeyStore.getInstance("PKCS12", "SunJSSE");
		char[] optKeyPasswd = optPassword != null ? optPassword.toCharArray() : null;
		keyStore.load(new ByteArrayInputStream(encodedData), optKeyPasswd); // might
																			// throw
																			// javax.crypto.BadPaddingException
																			// if
																			// password
																			// is
																			// incorrect
		for (Enumeration<String> aliasIter = keyStore.aliases(); aliasIter.hasMoreElements();) {
			String alias = aliasIter.nextElement();
			// Key key=keyStore.getKey(alias,optKeyPasswd)
			KeyStore.Entry entry = keyStore.getEntry(alias,
					optKeyPasswd != null ? new java.security.KeyStore.PasswordProtection(optKeyPasswd) : null);
			if (entry instanceof KeyStore.PrivateKeyEntry) {
				KeyStore.PrivateKeyEntry privKeyEntry = (KeyStore.PrivateKeyEntry) entry;
				java.security.cert.Certificate lastCertInChain = privKeyEntry.getCertificate();
				if (lastCertInChain instanceof java.security.cert.X509Certificate) {
					java.security.cert.X509Certificate cert = (java.security.cert.X509Certificate) lastCertInChain;
					// cert.checkValidity() // throws exceptions if invalid
					pickedCert = cert;
					break;
				}
			}
			// else if (entry instanceof KeyStore.SecretKeyEntry) {
			// }
			// else if (entry instanceof KeyStore.TrustedCertificateEntry) {
			// }
		}
		return pickedCert;
	}

	private static String getCertThumbprint(java.security.cert.X509Certificate cert) throws GeneralSecurityException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		byte[] der = cert.getEncoded();
		md.update(der);
		String certThumbprint = hexify(md.digest());
		return certThumbprint;
	}

	private static String hexify(byte[] bytes) {
		char[] hexDigits = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
				'f' };
		StringBuffer buf = new StringBuffer(bytes.length * 2);
		for (int i = 0; i < bytes.length; i++) {
			buf.append(hexDigits[(bytes[i] & 0xf0) >> 4]);
			buf.append(hexDigits[bytes[i] & 0x0f]);
		}
		return buf.toString();
	}

	private static Properties setProp(Properties props) {
		try {
			// onlyfortest
			Map<String, String> map = System.getenv();
			Iterator<Entry<String, String>> it = map.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, String> entry = (Entry) it.next();
				if (entry.getKey().equals("ANDROID_HOME")) {
					props.setProperty("appguard.aapt", entry.getValue() + "\\build-tools\\23.0.3\\aapt.exe");
					props.setProperty("appguard.dx", entry.getValue() + "\\build-tools\\23.0.3\\dx.bat");
					props.setProperty("appguard.zipalign", entry.getValue() + "\\build-tools\\23.0.3\\zipalign.exe");
				}
				if (entry.getKey().equals("JAVA_HOME")) {
					props.setProperty("user.home", entry.getValue());
				}
			}

		} catch (Exception e) {

		}
		return props;
	}
}
