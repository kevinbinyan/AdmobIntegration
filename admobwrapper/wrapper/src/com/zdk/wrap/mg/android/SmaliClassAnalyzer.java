package com.zdk.wrap.mg.android;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zdk.wrap.mg.GeneratorPhase1ConfigParams;
import com.zdk.wrap.mg.ServerUtil;
import com.zdk.wrap.mg.GeneratorPhase1ConfigParams.GuardedEntities;

public class SmaliClassAnalyzer implements InheritanceHierarchyAnalyzer,com.zdk.wrap.mg.WrapManifest.Writer {
  private static class MethodInvocation {
    String invocationType;
    String classNameVM;
    String methodName;
    String methodParams;
    String returnType;
    
    public MethodInvocation(String invocationType, String classNameVM, String methodName, String methodParams, String returnType) {
      this.invocationType=invocationType;
      this.classNameVM=classNameVM;
      this.methodName=methodName;
      this.methodParams=methodParams;
      this.returnType=returnType;
    }

    @Override
    public int hashCode() {
      final int prime=31;
      int result=1;
      result=prime*result+classNameVM.hashCode();
      result=prime*result+invocationType.hashCode();
      result=prime*result+methodName.hashCode();
      result=prime*result+methodParams.hashCode();
      result=prime*result+returnType.hashCode();
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this==obj)
        return true;
      if (obj==null)
        return false;
      if (getClass()!=obj.getClass())
        return false;
      MethodInvocation other=(MethodInvocation) obj;
      return
        classNameVM.equals(other.classNameVM) &&
        invocationType.equals(other.invocationType) &&
        methodName.equals(other.methodName) &&
        methodParams.equals(other.methodParams) &&
        returnType.equals(other.returnType);
    }
  }
  
  private Set<String> classDeclarations;
  private Map<String,String> extendsClassInApp;
  private Map<String,String> extendsClassOS;
  private Set<MethodInvocation> methodInvocations;
  private boolean useGoodDynamics;
  
  public SmaliClassAnalyzer(File smaliDumpDir, boolean useGoodDynamics) throws IOException {
    classDeclarations=new HashSet<String>();
    extendsClassInApp=new HashMap<String, String>();
    extendsClassOS=new HashMap<String, String>();
    methodInvocations=new HashSet<MethodInvocation>();
    this.useGoodDynamics=useGoodDynamics;
    buildClassHierarchy(smaliDumpDir);
  }
  
  private void buildClassHierarchy(File smaliDumpDir) throws IOException {
    addAndroidAPIHierarchy(extendsClassOS);
    
    final Pattern classDeclPat=Pattern.compile("^\\s*\\.class\\s+(public\\s+|protected\\s+|private\\s+|)(final\\s+|)(abstract\\s+|)([^\\s]+)$",Pattern.MULTILINE);
    final Pattern superDeclPat=Pattern.compile("^\\s*\\.super\\s+([^\\s]+)$",Pattern.MULTILINE);
    
    final Pattern methodCallPattern=Pattern.compile( // also cf. GuardedMethod.precompile()
      "(invoke-[a-z\\-/]+) *\\{[^\\}]+\\}, *"+
      "("+"L[^\\s;]+;"+")"+
      "->([_a-zA-Z0-9]+)\\(([^\\)]*)\\)"+
      "([0-9a-zA-Z;\\./\\$]+)"
    );
    
    ServerUtil.forEachFile(smaliDumpDir.getCanonicalPath(),new ServerUtil.FileAction() {
      public void action(File file) throws IOException {
        if (file.toString().endsWith(".smali")) {
          final String smaliText=ServerUtil.readTextFile(file,"UTF-8");
          
          { Matcher methodCallMatcher=methodCallPattern.matcher(smaliText);
            while (methodCallMatcher.find()) {
              //String invocationType=methodCallMatcher.group(1);
              //String className=methodCallMatcher.group(2);
              //String methodName=methodCallMatcher.group(3);
              //String methodParams=methodCallMatcher.group(4);
              //String returnType=methodCallMatcher.group(5);
              methodInvocations.add(
                new MethodInvocation(methodCallMatcher.group(1),methodCallMatcher.group(2),methodCallMatcher.group(3),methodCallMatcher.group(4),methodCallMatcher.group(5))
              );
            }
          }
          
          { Matcher classDeclMatcher=classDeclPat.matcher(smaliText);
            if (classDeclMatcher.find()) 
            {
              String className=classDeclMatcher.group(4);
              
              classDeclarations.add(className);
              
              Matcher superDeclMatcher=superDeclPat.matcher(smaliText);
              if (superDeclMatcher.find()) 
              {
                String superName=superDeclMatcher.group(1);
                //superName形如：Landroid/support/v7/internal/view/menu/MenuBuilder;需要转换为android/support/v7/internal/view/menu/MenuBuilder
               String classNameTmp = className;
                if(classNameTmp != null  || !classNameTmp.isEmpty())
                {
                	classNameTmp = classNameTmp.substring(1, classNameTmp.length()-1);
                }
                
              }
            }
          }
        }
      }
    });
  }

  @Override
  public void addManifestEntries(List<String> entries) {
    GeneratorPhase1ConfigParams.GuardedEntities guardedEntities;
    { final boolean useProxyTier=false;
      guardedEntities=
        new GeneratorPhase1ConfigParams.GuardedEntities(useGoodDynamics,useProxyTier);
    }
    
    addMethodInvocations(guardedEntities,entries);
    addClasses(guardedEntities,entries);
  }

  private Set<String> getClassWhitelist() {
    Set<String> whitelist=new HashSet<String>();
    whitelist.add("java.lang.Object");
    // ...
    return whitelist;
  }
    
  private Set<String> getMethodWhitelist() {
    Set<String> whitelist=new HashSet<String>();
    whitelist.add("java.lang.Class.getPackage");
    // ...
    return whitelist;
  }
  
  private void addMethodInvocations(GuardedEntities guardedEntities, List<String> entries) {
    Set<String> appClassNames=new HashSet<String>();
    for (String className: extendsClassInApp.keySet()) {
      if (!className.startsWith("Landroid/support/")) { // e.g. android-support-v4.jar
        appClassNames.add(className);
      }
    }
    
    Set<String> whitelist=getMethodWhitelist();
    
    //Set<MethodInvocation> apiMethodInvocations=eliminateIntraAppCalls(appClassNames);
    for (MethodInvocation methodInvocation: methodInvocations /*apiMethodInvocations*/) {
      String classNameJava=Util.classNameVMToJava(methodInvocation.classNameVM);
      boolean isGuarded=isGuardedClass(guardedEntities,classNameJava);
      if (!isGuarded) {
        for (GeneratorPhase1ConfigParams.GuardedMethod guardedMethod: guardedEntities.getGuardedMethods()) {
          if (guardedMethod.origMethodEquals(classNameJava,methodInvocation.methodName,methodInvocation.methodParams)) {
            isGuarded=true;
            break;
          }
        }
      }
      
      boolean isWhitelistedAsIrrelevant=whitelist.contains(classNameJava+"."+methodInvocation.methodName);
          
      String subTag;
      if (isIntraAppCall(appClassNames,methodInvocation))
        subTag="intraapp";
      else if (isGuarded)
        subTag="guarded";
      else if (isWhitelistedAsIrrelevant)
        subTag="whitelist";
      else
        subTag="extern";
      
      // also cf. GuardedMethod.addManifestEntries()
      entries.add(
        "methodinvocation "+subTag+" "+
        classNameJava+"."+methodInvocation.methodName+"("+
        methodInvocation.methodParams+
        "):"+methodInvocation.returnType
      );
    }
  }
  
  private boolean isGuardedClass(GuardedEntities guardedEntities, String classNameJava) {
    //String classNameJava=Util.classNameVMToJava(classNameVM);
    
    boolean isGuarded=false;
    for (GeneratorPhase1ConfigParams.GuardedClass guardedClass: guardedEntities.getGuardedClasses()) {
      //if (guardedClass.getClassToBeReplaced().equals("Landroid/app/Activity;"))
      if (guardedClass.getClassToBeReplaced().equals(classNameJava)) {
        isGuarded=true;
        break;
      }
    }
    if (!isGuarded) {
      for (GeneratorPhase1ConfigParams.GuardedInstantiation guardedInstantiation: guardedEntities.getGuardedInstantiations()) {
        if (guardedInstantiation.getClassToBeReplaced().equals(classNameJava)) {
          isGuarded=true;
          break;
        }
      }
    }
    return isGuarded;
  }
  
  private boolean isIntraAppCall(Set<String> appClassNames, MethodInvocation methodInvocation) {
    return appClassNames.contains(methodInvocation.classNameVM);
  }
  
  /*
  private Set<MethodInvocation> eliminateIntraAppCalls(Set<String> appClassNames) {
    Set<MethodInvocation> apiMethodInvocations=new HashSet<MethodInvocation>();
    for (MethodInvocation methodInvocation: methodInvocations) {
      if (!appClassNames.contains(methodInvocation.className))
        apiMethodInvocations.add(methodInvocation);
    }
    return apiMethodInvocations;
  }
  */
  
  private void addClasses(GuardedEntities guardedEntities, List<String> entries) {
    Set<String> inAppBaseClasses=new HashSet<String>();
    
    for (String baseClassName: extendsClassInApp.values()) {
      if (extendsClassInApp.containsKey(baseClassName)) {
        inAppBaseClasses.add(baseClassName);
      }
    }
    
    for (String classNameVM: classDeclarations) {
      String baseClassVM=extendsClassInApp.get(classNameVM);
      entries.add("classdecl "+Util.classNameVMToJava(classNameVM)+(baseClassVM!=null? " extends "+Util.classNameVMToJava(baseClassVM):""));
    }
    
    Set<String> whitelist=getClassWhitelist();
    
    HashSet<String> uniqueBaseClassNames=new HashSet<String>();
    uniqueBaseClassNames.addAll(extendsClassInApp.values());
    for (String baseClassNameVM: uniqueBaseClassNames) {
      String baseClassNameJava=Util.classNameVMToJava(baseClassNameVM);
      boolean isGuarded=isGuardedClass(guardedEntities,baseClassNameJava);
      
      String subTag;
      if (inAppBaseClasses.contains(baseClassNameVM))
        subTag="intraapp";
      else if (isGuarded)
        subTag="guarded";
      else if (whitelist.contains(baseClassNameJava))
        subTag="whitelist";        
      else
        subTag="extern";
      entries.add("baseclassdecl "+subTag+" "+baseClassNameJava);
    }
  }

  private static void addAndroidAPIHierarchy(Map<String, String> extendsClass) {
    apiExtendsClass(extendsClass,"android.app.ListActivity","android.app.Activity");
    apiExtendsClass(extendsClass,"android.app.ExpandableListActivity","android.app.Activity");
    apiExtendsClass(extendsClass,"android.preference.PreferenceActivity","android.app.Activity");
    apiExtendsClass(extendsClass,"android.app.TabActivity","android.app.Activity");
  }

  private static void apiExtendsClass(Map<String, String> extendsClass, String className, String superClassName) {
    extendsClass.put(Util.classNameToBytecode(className),Util.classNameToBytecode(superClassName));
  }

  public boolean isSubclassOfByBytecodeName(String className, String superClassName) {
    if (className.equals(superClassName))
      return true;
    
    String nextSuperName=extendsClassInApp.get(className);
    if (nextSuperName==null)
      nextSuperName=extendsClassOS.get(className);
    if (nextSuperName==null)
      return false;
    
    return isSubclassOfByBytecodeName(nextSuperName,superClassName);
  }

  public boolean isSubclassOfByJavaName(String className, String superClassName) {
    return isSubclassOfByBytecodeName(Util.classNameToBytecode(className),Util.classNameToBytecode(superClassName));
  }
}
