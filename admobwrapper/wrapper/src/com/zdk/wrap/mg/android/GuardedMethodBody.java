package com.zdk.wrap.mg.android;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zdk.wrap.mg.GeneratorPhase1ConfigParams;
import com.zdk.wrap.mg.AppModifier.ProcessingException;
import com.zdk.wrap.mg.GeneratorPhase1ConfigParams.GuardedMethodBodyStandalone;

public class GuardedMethodBody implements com.zdk.wrap.mg.WrapManifest.Writer {
  private GeneratorPhase1ConfigParams.GuardedMethodBodyBase cfgItem;
  private Pattern pattern;
  private String replacement;
  private Pattern superClassPattern;
  private String superClassReplacement;
  private Pattern constructorSuperPattern;
  private String constructorSuperReplacement;
  //private Pattern exceptionClassesPat;
  private Pattern thisClassPattern;
  private Pattern _superCallPattern;//super.oncreate-->super.wrappedPreviousOncreate();
  //这样才能保证多层派生时候不是只调用最后一层的wrappedPreviousOncreate
  private String _supperCallReplacement;
  
  public GuardedMethodBody(GeneratorPhase1ConfigParams.GuardedMethodBodyBase cfgItem) 
  {
    this(cfgItem,cfgItem.getPriority());
  }
  
  public GuardedMethodBody(GeneratorPhase1ConfigParams.GuardedMethodBodyBase cfgItem, int priority) {
    this.cfgItem=cfgItem;
    precompile(priority);
  }
  
  //实际上，这里假设太多了，比如，不会有其他地方调用这个被guard的method，所以并没有把其他调用的地方转向到新method
  //对super.guardedMethod调用也没guard。并且proxy class会有老method的正确实现并且在那里调用wrappedPreviousxxx.
  //这基本就是为activity等class的oncreate（）之类的方法定制的，而不是什么通用的guardedMethodBody
  private void precompile(int priority) {
    superClassPattern=Pattern.compile("(\n\\.super +)\\Q"+Util.classNameToBytecode(cfgItem.getClassName())+"\\E( *\\n)");
    //
    if (cfgItem instanceof GuardedMethodBodyStandalone) {
      superClassReplacement=
        "$1"+Util.classNameToBytecode(cfgItem.getNewClassName())+"$2";
    } else {
      superClassReplacement=null;
    }
    
    thisClassPattern=Pattern.compile("(^|[\\r\\n])\\s*\\.class +(public\\s+|protected\\s+|private\\s+|)(final\\s+|)(abstract\\s+|)(.*?) *\\n");
    
    //exceptionClassesPat=Pattern.compile("\\.class public Lcom/good/wrap/gd/TestHarness\\$HelperActivity;");
    //.method public wrappedPreviousOnCreate(Landroid/os/Bundle;)V
    pattern=Pattern.compile(
      "(\n *\\.method +)(public|private|protected) +\\Q"+cfgItem.methodName+"\\E(\\Q("+cfgItem.methodParams+")"+cfgItem.methodReturn+"\\E *?\\n)(.*?)(\n\\.end +method *?\\n)",Pattern.DOTALL
    );
    
    String accessModifier = cfgItem.optAccessModifier==null?"$2"+" ":cfgItem.optAccessModifier+" ";
    replacement=
      "$1"+accessModifier+
      Matcher.quoteReplacement(cfgItem.newMethodName)+
      "$3$4$5";
    // in $4 prevent 'invoke-super {p0}, Landroid/app/Application;->onCreate()V' from calling onCreate() for a second time

    //没有bug吗？难道不是else吗？这样都加了多少了？不能"+=而是="
    if (cfgItem.optNewSmaliBody!=null) {
        //
      replacement+=
        "$1"+accessModifier+Matcher.quoteReplacement(cfgItem.methodName)+"$3"+Matcher.quoteReplacement(cfgItem.optNewSmaliBody)+"$5"
        ;
    }
    //invoke-super {p0, p1}, Lorg/apache/cordova/DroidGap;->onCreate(Landroid/os/Bundle;)V
    _superCallPattern=Pattern.compile(
    	      "(\\n *invoke-super[/range]* +\\{[^\\}]+\\}, +.*?->)(\\Q"+cfgItem.methodName+"\\E)(\\Q("+cfgItem.methodParams+")"+cfgItem.methodReturn+"\\E *?\\n)"
    	    );//这里没写superclass的类名，因为需要运行时决定，得在apply里面做，但是由于有了invoke super，实际上不用写了。
    _supperCallReplacement = "$1"+Matcher.quoteReplacement(cfgItem.newMethodName)+"$3";
    
    
    if (!cfgItem.getClassName().equals(cfgItem.getNewClassName())) {
      constructorSuperPattern=Pattern.compile(
        "(\\n *(invoke-super|invoke-direct) +\\{[a-z0-9, ]+\\}, *)\\Q"+Util.classNameToBytecode(cfgItem.getClassName())+"\\E(->(onCreate\\(\\)|<init>\\(\\)|attachBaseContext\\(Landroid/content/Context;\\))V)"
      );
      constructorSuperReplacement=
        "$1"+Matcher.quoteReplacement(Util.classNameToBytecode(cfgItem.getNewClassName()))+"$3";
    } else {
      constructorSuperPattern=null;
    }
    
    
  }
  
  

  
  public String apply(String smali, InheritanceHierarchyAnalyzer classAnalyzer) throws ProcessingException {
    //if (smali.indexOf(".class public Lcom/infraware/polarisoffice4/OfficeHomeActivity;")>=0)
    //  System.out.println();
    
	//
    boolean isRelevant=false;
    
    { Matcher superClassMatcher=superClassPattern.matcher(smali);
      if (superClassMatcher.find()
          // && !exceptionClassesPat.matcher(smali).find()
          ) {
        isRelevant=true;
        if (superClassReplacement!=null) {          
          smali=superClassMatcher.replaceAll(superClassReplacement);
        }
      }
    }
    //
    if (!isRelevant && cfgItem.includeSubClasses) 
    {
      String thisClassNameVM;
      { 
    	Matcher thisClassMatcher=thisClassPattern.matcher(smali);

        boolean ret = thisClassMatcher.find(0);
        if (!ret)
          throw new ProcessingException("Cannot determine this file's classname.");
        thisClassNameVM=thisClassMatcher.group(5);
      }
      
      
      if (classAnalyzer.isSubclassOfByBytecodeName(thisClassNameVM,Util.classNameToBytecode(cfgItem.getClassName())))
        isRelevant=true;
    }
    
    if (isRelevant) {
      if (constructorSuperPattern!=null)
      {
    	  smali=constructorSuperPattern.matcher(smali).replaceAll(constructorSuperReplacement);
      }
	  smali=pattern.matcher(smali).replaceAll(replacement);
	  smali = _superCallPattern.matcher(smali).replaceAll(_supperCallReplacement);
    }
    
    return smali;
  }
  
  @Override
  public void addManifestEntries(List<String> entries) {
    //entries.add(
    //  "" // TODO
    //);
  }

}
