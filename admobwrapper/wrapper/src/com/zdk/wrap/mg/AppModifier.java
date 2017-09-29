package com.zdk.wrap.mg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class AppModifier {

  @SuppressWarnings("serial")
  public static class ProcessingException extends Exception {
    public ProcessingException(String message) {
      super(message);
    }
    
    public ProcessingException(Throwable cause) {
      super(cause);
    }
  }
  
  //public interface PackagedPrivateKey extends com.ondeego.appcentral.appguard.ios.AppModifier.PackagedPrivateKey {
  //}
  
  //public interface Organization...
  
  public static enum BinCodeType {
    JAD,
    JAR,
    COD,
    //ZIP
    APK,
    IPA,
    IOSAPPBUNDLE
  }

  public static class AddedFile {
    private String targetPath;
    private InputStream src;
    
    public AddedFile(String targetPath, InputStream src /* does not include closing it */) {
      this.targetPath=targetPath;
      this.src=src;
    }

    public String getTargetPath() {
      return targetPath;
    }
    
    public InputStream getSrc() {
      return src;
    }
    
    public void close() throws IOException {
      src.close();
    }
  }
  
  public static class WrappingParameters {
    private PackagedPrivateKey optSignKey;
    private String optGDApplicationID;
    private String optGDApplicationVersion;
    private String optGDConfigInfo;
    private String optAppTag;
    private List<AddedFile> addedFiles;
    private List<String> optGDLogging;
    // String appJarFileName, String appName, Set targetPlatformNames, byte[] appIconData, String description
    
    public WrappingParameters(PackagedPrivateKey optSignKey, String optGDApplicationID, String optGDApplicationVersion, String optGDConfigInfo, String optAppTag, List<AddedFile> addedFiles, List<String> optGDLogging) {
      this.optSignKey=optSignKey;
      this.optGDApplicationID=optGDApplicationID;
      this.optGDApplicationVersion=optGDApplicationVersion;
      this.optGDConfigInfo=optGDConfigInfo;
      this.optAppTag=optAppTag;
      this.addedFiles=addedFiles;
      this.optGDLogging=optGDLogging;
    }

    public PackagedPrivateKey getOptSignKey() {
      return optSignKey;
    }

    public String getOptGDApplicationID() {
      return optGDApplicationID;
    }

    public String getOptGDApplicationVersion() {
      return optGDApplicationVersion;
    }
    
    public String getOptGDConfigInfo() {
      return optGDConfigInfo;
    }

    public List<AddedFile> getAddedFiles() {
      return addedFiles;
    }

    public String getOptAppTag() {
      return optAppTag;
    }

    public List<String> getOptGDLogging() { // null, empty (= GDFilterNone), or a list of filter names
      return optGDLogging;
    }
    
    public String getAppName() 
    {
    	// optAppTag����./app-debug.apk
		int index= optAppTag.indexOf("/");
		String apkname = optAppTag.substring(index+1);
        return apkname;
      
    }
  }
  
  //public interface Services {
  //  String getResourceFilePath(String relPath);
  //}
  
  //private static ContentService contentService=null;
  //private static com.ondeego.appcentral.appguard.ios.AppModifierServices appModifierServices=null;
  private static String resourceDirPath=null;
  private static boolean verbose=false;

  public static void setVerbose(boolean value) {
    verbose=value;    
  }

  public static void configure(String resourceDirPath) {
    AppModifier.resourceDirPath=resourceDirPath;
    //AppModifier.contentService=contentService;
    //AppModifier.appModifierServices=appModifierServices;
  }
  
  public static boolean isConfigured() {
    return resourceDirPath!=null;
  }

  private boolean debugLoggingEnabled=false;
  
  public AppModifier() throws IOException {
    unpackDistribution();
  }
  
  public static void logVerbose(String message) {
    if (verbose)
      System.out.println("Debug: "+message);
  }
  
  private static boolean unpacked=false;
  
  /* not private to be able to trigger it even before constructor is called for the first time */
	public static void unpackDistribution() throws IOException {
    if (!unpacked) {
      logVerbose("unpackDistribution() starts...");
      boolean available=false;

      ProtectionDomain protectionDomain=AppModifier.class.getProtectionDomain();
      if (protectionDomain!=null) {
        logVerbose("protectionDomain present.");
        CodeSource codeSource=protectionDomain.getCodeSource();
        if (codeSource!=null) {
          logVerbose("codeSource present.");
          URL codeLocation=codeSource.getLocation();
          URI codeLocationURI=null;
          try {
            codeLocationURI=codeLocation.toURI();
          } catch (URISyntaxException e) {
            throw new IOException("Couldn't unpack AppModifier", e);
          }

          if (codeLocationURI.getScheme().equalsIgnoreCase("file")) {
            logVerbose("URL is a file.");
            File jarOrDir=new File(codeLocationURI.getPath());
            if (!jarOrDir.exists()) // e.g. jar file or class folder
              throw new IOException("Cannot find distribution jar file or class directory at "+jarOrDir.getCanonicalPath());
            else if (jarOrDir.isDirectory()) {
              // not within a jar file, i.e. not running a jar-based distribution, i.e. skip unpacking
              logVerbose("Running AppModifier from within an already existing directory. Unpacking skipped.");
              available=true;
            }
            else {
              File distJarFile=jarOrDir;
              logVerbose("Assuming that this is running from within jar "+distJarFile);
              File unpackTargetDir;
              String webInfIndicator=File.separator+"WEB-INF"+File.separator;
              int webInfPos=distJarFile.getCanonicalPath().lastIndexOf(webInfIndicator);
              if (webInfPos>=0) { // runs within web-app context
                unpackTargetDir=new File(distJarFile.getCanonicalPath().substring(0,webInfPos)); // distJarFile.getCanonicalPath()+".unpacked";
                logVerbose("Web application context detected. Unpacking to "+unpackTargetDir);
              }
              else {
                logVerbose("Not within web application context, using jar location to unpack.");
                unpackTargetDir=new File(distJarFile.getParent()+File.separator+distJarFile.getName()+".unpack");
                if (!unpackTargetDir.exists())
                  unpackTargetDir.mkdir();
              }
              String gdwrapperSubdirPath=unpackTargetDir.getCanonicalPath()+webInfIndicator+"gdwrapper";
              File extractStamp=new File(gdwrapperSubdirPath+File.separator+"extract.stamp"); //new File(targetDirPath+File.separator+"extract.stamp");

              boolean unjar=false;
              if (unpackTargetDir.exists() && unpackTargetDir.isDirectory()) {
                if (extractStamp.exists() && distJarFile.lastModified()<extractStamp.lastModified()) {
                  logVerbose("Already unpacked jar still up-to-date.");
                  available=true;
                } else {
                  logVerbose("Already unpacked jar no longer up-to-date.");
                  unjar=true;
                }
              } else {
                throw new IOException("Cannot find unpacking directory "+unpackTargetDir.getCanonicalPath());
              }
              if (unjar) {
                final String unpackTargetDirName=unpackTargetDir.getCanonicalPath();
                logVerbose("Unpacking jar into "+unpackTargetDirName+" ...");
                unpackDistJar(unpackTargetDirName,new FileInputStream(distJarFile));

                extractStamp.delete();
                if (!extractStamp.createNewFile())
                  throw new IOException("Cannot created extraction timestamp file.");

                available=true;
              }

              // self-configuration:
              configure(gdwrapperSubdirPath+File.separator+"resources");
              //addUnpackedJarsToClasspath(new File(libSubdirPath));
            }
          }
        }
      }

      if (!available)
        throw new IOException("Cannot unpack distribution jar file.");

      unpacked=true;
    }
  }

  private static void unpackDistJar(String targetDirPath, InputStream sourceJarFileStream) throws IOException {
    try {
      final String requiredPrefix="unpack/";
      JarInputStream libJar=new JarInputStream(sourceJarFileStream);
      
      for (;;) {
        JarEntry entry=libJar.getNextJarEntry(); // libJar.getNextJarEntry() omits manifest, libJar.getNextEntry() too
        if (entry==null) break;
        
        if (!entry.isDirectory() && entry.getName().startsWith(requiredPrefix)) {
          final String path=targetDirPath+File.separator+entry.getName().substring(requiredPrefix.length()).replace('/',File.separatorChar);
          File entryFile=new File(path);
          ServerUtil.createMissingDirsFor(entryFile);
          OutputStream outStr=new FileOutputStream(path);
          try {
            ServerUtil.copyStream(libJar,outStr);
          } finally {
            outStr.close();
          }
        }
      }
    } finally {
      sourceJarFileStream.close();
    }
  }
  
  public static String getResourceFilePath(String relPath) throws IOException {
    if (resourceDirPath==null)
      throw new IOException("AppModifier.configure() has not been called yet.");
    return resourceDirPath+(relPath.length()>0? File.separator+relPath.replace("/",File.separator):"");
  }
  
  public static Properties loadConfigProperties(boolean isRequired) throws IOException {
    Properties props=new Properties();
    try {
      String perHostConfig=getResourceFilePath("config/"+ServerUtil.getCanonicalHostname()+".properties");
      logVerbose("Reading configuration parameters from "+perHostConfig);
      props.load(new FileInputStream(perHostConfig));
      //resolveIncludes(props);
    } catch (FileNotFoundException e) {
      if (isRequired)
        throw e;
      // premanufactured config not present, that's ok, ignore
    }
    return props;
  }
  
  public BinCodeType getBinCodeFromFileName(final String fileName) throws ProcessingException {
    BinCodeType binCodeType;
    final String lcFilename=fileName.toLowerCase();
    if (lcFilename.endsWith(".jar")) {
      binCodeType=BinCodeType.JAR;
    } else if (lcFilename.endsWith(".cod")) {
      binCodeType=BinCodeType.COD;
    // } else if (lcFilename.endsWith(".zip")) {
    //  binCodeType=BinCodeType.ZIP;
    } else if (lcFilename.endsWith(".apk")) {
      binCodeType=BinCodeType.APK;
    } else if (lcFilename.endsWith(".jad")) {
      binCodeType=BinCodeType.JAD;
    } else if (lcFilename.endsWith(".ipa")) {
      binCodeType=BinCodeType.IPA;
    } else if (lcFilename.endsWith(".app")) { // for in-place app modification
      binCodeType=BinCodeType.IOSAPPBUNDLE;
    } else {
      throw new ProcessingException("Cannot deduce application type from provided filename.");
    }
    return binCodeType;
  }
  
  public String getCapabilityWrapManifest(BinCodeType appBinCodeType) throws ProcessingException {
    switch (appBinCodeType) {
      case APK: {
        com.zdk.wrap.mg.android.AppModifier appModifier=new com.zdk.wrap.mg.android.AppModifier(false);
        return appModifier.getCapabilityWrapManifest();
      }
      default:
        throw new ProcessingException("Capability manifest not yet supported for code type "+appBinCodeType);
    }
  }
  
  public abstract static class ProcessingResult {
    //private InputStream origBinCodeSrc;
    //private byte[] bufferedContent=null;
    private InputStream optWrappedBinCodeStr;
    
    public ProcessingResult(/*InputStream origBinCodeSrc,*/ InputStream optWrappedBinCodeStr) {
      //this.origBinCodeSrc=origBinCodeSrc;
      this.optWrappedBinCodeStr=optWrappedBinCodeStr;
    }


    
    public InputStream getOptWrappedBinCodeStr() {
      return optWrappedBinCodeStr;
    }


  }
  
  /**
   * This overloaded process() calls another overloaded version with null for optGDConfigInfo
   * This is a convenience calls made from the server that don't use optGDConfigInfo or appTag or ...
   * For backward compatibility only. This method will go away at some time!
   * 
   * 
   * @param appBinCodeType
   * @param isAppGuarded
   * @param optSignKey
   * @param appBinCodeStr
   * @param optGDApplicationID
   * @param optGDApplicationVersion
   * @param appTag 
   * @return
   * @throws ProcessingException
   * @throws IOException
   */
  public ProcessingResult process(BinCodeType appBinCodeType, boolean isAppGuarded, 
      PackagedPrivateKey optSignKey, InputStream appBinCodeStr,
      String optGDApplicationID, String optGDApplicationVersion) throws ProcessingException, IOException {
   
    WrappingParameters parameters=new WrappingParameters(optSignKey,optGDApplicationID,optGDApplicationVersion,null,null,new LinkedList<AddedFile>(),null);
    return process(appBinCodeType,isAppGuarded,appBinCodeStr,parameters);
  }
  
  /**
   * @param appBinCodeType Indicates type of app (ios, android, etc)
   * @param isAppGuarded Requests that app should be AppGuarded (obsolete name)
   * @param optSignKey Singing certificate used to resign ap
   * @param appBinCodeStr Stream containing app
   * @param optGDApplicationID The GD Application ID used with GC, will be embedded in Info.plist
   * @param optGDApplicationVersion The GD Version used with GC, will be embedded in Info.plist
   * @param optGDConfigInfo Used to select a different server
   * @param appTag 
   * @return
   * @throws ProcessingException
   * @throws IOException
   */
  public ProcessingResult process(BinCodeType appBinCodeType, boolean isAppGuarded, InputStream appBinCodeStr, WrappingParameters parameters) throws ProcessingException, IOException {
    switch (appBinCodeType) {
      case APK: {
        //if (true || isAppGuarded) {
          com.zdk.wrap.mg.android.AppModifier appModifier=new com.zdk.wrap.mg.android.AppModifier(debugLoggingEnabled);
          return appModifier.process(appBinCodeStr,isAppGuarded,parameters);
        //} else {
        //  return new ProcessingResult(appBinCodeStr,null); // leave unchanged
        //}
      } //break;
      //case JAD: {
      //  com.good.wrap.gd.rim.AppModifier appModifier=new com.good.wrap.gd.rim.AppModifier();
      //  return appModifier.process(appBinCodeStr,isAppGuarded,organization,servletContext,httpRequest);
      //}
      default:
        throw new ProcessingException("Unknown application binary code type.");
    }
  }

  public ProcessingResult processInPlace(BinCodeType appBinCodeType, /*PackagedPrivateKey optSignKey,*/ File appBinDir,
      WrappingParameters parameters) throws ProcessingException, IOException {
    switch (appBinCodeType) {
      default:
        throw new ProcessingException("Inplace wrapping currently only supported for iOS app bundles.");
    }
  }
  
  public void enableDebugLogging() {
    debugLoggingEnabled=true;
  }

  
}
