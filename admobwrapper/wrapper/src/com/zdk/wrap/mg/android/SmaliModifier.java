package com.zdk.wrap.mg.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.zdk.wrap.mg.GeneratorPhase1ConfigParams;
import com.zdk.wrap.mg.ServerUtil;
import com.zdk.wrap.mg.WrapManifest;
import com.zdk.wrap.mg.AppModifier.ProcessingException;

public class SmaliModifier {
  private List<GuardedClass> guardedClasses;
  private List<GuardedMethod> guardedMethods;
  private List<GuardedMethodBody> guardedMethodBodies;
  private List<GuardedInstantiation> guardedInstantiations;
  private boolean useGoodDynamics;
  private TextManipulator textManipulator;

	public SmaliModifier(boolean useGoodDynamics, boolean useProxyTier) throws ProcessingException {
    this.useGoodDynamics=useGoodDynamics;
    
    textManipulator=new TextManipulator();
    
    guardedClasses=new ArrayList<GuardedClass>();
    guardedMethods=new ArrayList<GuardedMethod>();
    guardedMethodBodies=new ArrayList<GuardedMethodBody>();
    guardedInstantiations=new ArrayList<GuardedInstantiation>();
    
    registerModifiers(useProxyTier);
  }
  
  private void registerModifiers(boolean useProxyTier) throws ProcessingException {
    //registerInjectedClasses(guardedClasses,useProxyTier);
    //registerGuardedInstantiations(guardedInstantiations,useProxyTier);
    GeneratorPhase1ConfigParams.GuardedEntities guardedEntities=
      new GeneratorPhase1ConfigParams.GuardedEntities(useGoodDynamics,useProxyTier);
    
    com.zdk.wrap.mg.AppModifier.logVerbose("registerModifiers: "+guardedEntities.getStatistics());
    
    for (GeneratorPhase1ConfigParams.GuardedClass cfgGuardedClass: guardedEntities.getGuardedClasses())
      guardedClasses.add(new GuardedClass(cfgGuardedClass,textManipulator));
    for (GeneratorPhase1ConfigParams.GuardedInstantiation cfgGuardedInstantiation: guardedEntities.getGuardedInstantiations())
      guardedInstantiations.add(new GuardedInstantiation(cfgGuardedInstantiation,textManipulator));
    for (GeneratorPhase1ConfigParams.GuardedMethod cfgGuardedMethod: guardedEntities.getGuardedMethods())
      guardedMethods.add(new GuardedMethod(cfgGuardedMethod,textManipulator));
    for (GeneratorPhase1ConfigParams.GuardedMethodBodyStandalone cfgGuardedMethodBody: guardedEntities.getGuardedMethodBodies())
      guardedMethodBodies.add(new GuardedMethodBody(cfgGuardedMethodBody));
  }
  
  //private static final String platformPrefix="Android";
  
  public String getWrapManifest(AppInfo optAppInfo, SmaliClassAnalyzer optClassAnalyzer) {
    WrapManifest manifest=new WrapManifest();
    manifest.addHeader("platform","Android");
    if (optAppInfo!=null) {
      manifest.addHeader("package-name",optAppInfo.getPackageName());
      if (optAppInfo.getOptMainActivityName()!=null)
        manifest.addHeader("main-activity-name",optAppInfo.getOptMainActivityName());
      if (optAppInfo.getOptVersionName()!=null)
        manifest.addHeader("version-name",optAppInfo.getOptVersionName());
      if (optAppInfo.getOptVersionCode()!=null)
        manifest.addHeader("version-code",optAppInfo.getOptVersionCode());
      if (optAppInfo.getOptMinSdkVersion()!=null)
        manifest.addHeader("min-sdk-version",optAppInfo.getOptMinSdkVersion());
      if (optAppInfo.getOptTargetSdkVersion()!=null)
        manifest.addHeader("target-sdk-version",optAppInfo.getOptTargetSdkVersion());
    }

    for (WrapManifest.Writer manifestWriter: guardedClasses)
      manifest.add(manifestWriter);
    for (WrapManifest.Writer manifestWriter: guardedInstantiations)
      manifest.add(manifestWriter);
    for (WrapManifest.Writer manifestWriter: guardedMethods)
      manifest.add(manifestWriter);
    for (WrapManifest.Writer manifestWriter: guardedMethodBodies)
      manifest.add(manifestWriter);
    if (optClassAnalyzer!=null)
      manifest.add(optClassAnalyzer);
    return manifest.build();
  }
  
  @SuppressWarnings("unused")
  public void injectModifiedClasses(File smaliDumpDir, final SmaliClassAnalyzer classAnalyzer, final boolean developerDebugMode, String optAppTag) throws IOException, com.zdk.wrap.mg.AppModifier.ProcessingException {
    final long startTime=System.currentTimeMillis();
    
    final boolean enableLimitToFiles=false; // no longer required due to significant speed improvements
    final List<String> limitToFiles=new LinkedList<String>();
    File modifiedFilesFile=null;
    if (enableLimitToFiles && developerDebugMode) 
    {
      if (optAppTag==null)
        throw new com.zdk.wrap.mg.AppModifier.ProcessingException("optAppTag cannot be null when using developerDebugMode.");
      modifiedFilesFile=new File("/tmp/wrap.modifiedFiles."+optAppTag.replaceAll("[^a-zA-Z0-9_\\-]","_"));
      if (modifiedFilesFile.exists()) {
        com.zdk.wrap.mg.AppModifier.logVerbose("Debugging shortcut: Not processing all files but only those found in "+modifiedFilesFile.getPath());
        BufferedReader reader=new BufferedReader(new FileReader(modifiedFilesFile));
        for (;;) {
          String line=reader.readLine();
          if (line==null)
            break;
          limitToFiles.add(line);
        }
        reader.close();
      }
    }
    //��ʽ��ʼ

    final List<File> modifiedFiles=new LinkedList<File>();
    // would require extended classpath: Map<Class,Class> replaceClassWith=new java.util.HashMap<Class,Class>(); replaceClassWith.put(Activity.class,GuardedActivity.class);
    final String smaliDumpDirPath=smaliDumpDir.getCanonicalPath()+File.separator;
    ServerUtil.forEachFile(smaliDumpDirPath,new ServerUtil.FileAction() {
      public void action(File file) throws IOException 
      {
        String absFilePath=file.toString();
        assert absFilePath.startsWith(smaliDumpDirPath);
        String relFilePath=absFilePath.substring(smaliDumpDirPath.length());
        
        if (relFilePath.endsWith(".smali")) 
        {
          if (limitToFiles.isEmpty() || limitToFiles.contains(relFilePath)) 
          {
            //System.out.println("injectModifiedClasses: "+relFilePath+" ...");
            final String oldSmaliText=ServerUtil.readTextFile(file,"UTF-8");
            String newSmaliText=oldSmaliText;
            
            try {
              // first: single methods
              for (GuardedMethod method: guardedMethods) {
                //if (file.getName().equals("MainActivity.smali") && method.newMethodName.equals("_gd_openOrCreateDatabase"))
                //  System.out.println();
            	  //
                newSmaliText=method.apply(newSmaliText,classAnalyzer); 
              }

              for (GuardedMethodBody methodBody: guardedMethodBodies) {
                newSmaliText=methodBody.apply(newSmaliText,classAnalyzer);
              }
 
              for (GuardedInstantiation instantiation: guardedInstantiations) {
                newSmaliText=instantiation.apply(newSmaliText,classAnalyzer);
              }
             
              // second: classes, i.e. the methods above can still refer to the unmodified parameter types
              for (GuardedClass guardedClass: guardedClasses) {

                newSmaliText=guardedClass.apply(newSmaliText,classAnalyzer);
              }
              
              // after GuardedClass.apply():
              newSmaliText=textManipulator.apply(newSmaliText);              
            } catch (ProcessingException e) {
            	e.printStackTrace();
              throw new IOException(e);
            }
            
            //if (file.getName().equals("SQLiteTest.smali"))
            //  System.out.println();
            //if (file.getName().equals("FileIOTest.smali"))
            //  System.out.println();
            //ServerUtil.writeTextFile(oldSmaliText,new File("/tmp/old.txt"),"UTF-8");
            //ServerUtil.writeTextFile(newSmaliText,new File("/tmp/new.txt"),"UTF-8");
            
            if (!oldSmaliText.equals(newSmaliText)) {
              if (developerDebugMode)
                file.renameTo(new File(file.getAbsolutePath()+".prev"));
              FileWriter out=new FileWriter(file);
              out.write(newSmaliText);
              out.close();
              modifiedFiles.add(file);
            }
          }
        }
      }
    });
    
    if (enableLimitToFiles && developerDebugMode && modifiedFilesFile!=null) {
      FileWriter out=new FileWriter(modifiedFilesFile);
      for (File file: modifiedFiles) {
        String filePath=file.getCanonicalPath();
        //String dirPath=smaliDumpDir.getCanonicalPath()+File.separator;
        assert filePath.startsWith(smaliDumpDirPath);
        String relPath=filePath.substring(smaliDumpDirPath.length());
        out.append(relPath+"\n");
      }
      out.close();
    }
    
    final long endTime=System.currentTimeMillis();
    com.zdk.wrap.mg.AppModifier.logVerbose("injectModifiedClasses: "+(endTime-startTime+500)/1000+" s");
  }

}
