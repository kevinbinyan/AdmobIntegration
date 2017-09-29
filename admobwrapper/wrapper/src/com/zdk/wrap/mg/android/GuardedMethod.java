package com.zdk.wrap.mg.android;

import java.util.List;

import com.zdk.wrap.mg.GeneratorPhase1ConfigParams;
import com.zdk.wrap.mg.AppModifier.ProcessingException;
import com.zdk.wrap.mg.android.TextManipulator.MatchAccess;

public class GuardedMethod extends GeneratorPhase1ConfigParams.GuardedMethod implements com.zdk.wrap.mg.WrapManifest.Writer {
  //private Pattern pattern;
  //private String replacement;
  private String origClassNameByteCode;
  private TextManipulator textManipulator;
  private InheritanceHierarchyAnalyzer cachedClassAnalyzer=null;
  private int priority; //=GeneratorPhase1ConfigParams.PrioritizableGuardedEntity.PRIO_DEFAULT;
  
  /*
  public GuardedMethod(String className, boolean applyAlsoToSubclasses, String replaceByClassName,
      String oldMethodName, String newMethodName, 
      String methodParams, String methodReturn) {
    this(className,applyAlsoToSubclasses,replaceByClassName,false,oldMethodName,newMethodName,methodParams,methodParams,methodReturn,methodReturn);
  }
  */
  
  /*
  public GuardedMethod(String origClassName, boolean applyAlsoToSubclasses, String replaceByClassName, 
      boolean useStaticReplaceCall, 
      String oldMethodName, String newMethodName,
      String oldMethodParams, String newMethodParams,
      String oldMethodReturn, String newMethodReturn) {
    super(origClassName,applyAlsoToSubclasses,replaceByClassName,
        useStaticReplaceCall,oldMethodName,newMethodName,oldMethodParams,newMethodParams,
        oldMethodReturn,newMethodReturn);
    precompile();
  }
  */
  
  public GuardedMethod(GeneratorPhase1ConfigParams.GuardedMethod cfgItem, TextManipulator textManipulator) {
    this(cfgItem,textManipulator,cfgItem.getPriority());
  }
  
  public GuardedMethod(GeneratorPhase1ConfigParams.GuardedMethod cfgItem, TextManipulator textManipulator, int priority) {
    /*super(cfgItem.origClassName,cfgItem.applyAlsoToSubclasses,replaceByClassName,
        cfgItem.useStaticReplaceCall,cfgItem.oldMethodName,cfgItem.newMethodName,
        cfgItem.oldMethodParams,cfgItem.newMethodParams,
        cfgItem.oldMethodReturn,cfgItem.newMethodReturn);*/
    super(cfgItem);
    this.textManipulator=textManipulator;
    this.priority=priority;
    //precompile();
  }
  
  private void precompile(final InheritanceHierarchyAnalyzer classAnalyzer) throws ProcessingException {
    /*
    "(invoke-[a-z\\-/]+) *\\{([^\\}]+)\\}, *"+ // '/' to include 'invoke-direct/range {v22 .. v23}, Ljava/io/File;-><init>(Ljava/lang/String;)V'
    "("+(applyAlsoToSubclasses? "L[^\\s;]+;":"\\Q"+origClassNameByteCode+"\\E")+")"+
    "\\Q->"+oldMethodName+"(\\E("+(optOldMethodParams!=null? "\\Q"+optOldMethodParams+"\\E":"[^)]*")+")"+
    "\\Q)"+oldMethodReturn+"\\E",
    */
    // typically invoke-virtual|invoke-direct|invoke-super|invoke-static and ODEX invoke-*
    // invoke-virtual {v0, v1}, Landroid/text/ClipboardManager;->setText(Ljava/lang/CharSequence;)V
    // invoke-static {v0, v1}, Lcom/ondeego/appcentral/shell/android/example/common/ClipboardManager;->setText(Landroid/text/ClipboardManager;Ljava/lang/CharSequence;)V
    
    origClassNameByteCode=Util.classNameToBytecode(origClassName);
    textManipulator.addRegExpr(
      "(invoke-[a-z\\-/]+) *\\{([^\\}]+)\\}, *"+ // '/' to include 'invoke-direct/range {v22 .. v23}, Ljava/io/File;-><init>(Ljava/lang/String;)V'
      "("+"L[^\\s;]+;"+")"+
      "->{param}\\("+
      "("+
      (optOldMethodParams!=null? "{param}":"[^)]*")+ // "[^)]*"+ // (optOldMethodParams!=null? "\\Q"+optOldMethodParams+"\\E":"[^)]*")+
      ")"+
      "\\){param}",
      optOldMethodParams!=null?
        new String[] { 
          "\\Q"+oldMethodName+"\\E",
          "\\Q"+optOldMethodParams+"\\E",
          "\\Q"+oldMethodReturn+"\\E"
        }:
        new String[] { 
          "\\Q"+oldMethodName+"\\E",
          "\\Q"+oldMethodReturn+"\\E"
        },
      new TextManipulator.Replacer() {
        public String optReplace(MatchAccess matchAccess) throws ProcessingException {
          //String fullMatch=matchAccess.group(0);
          //原始调用是否是静态的
          boolean isOriginStaticCall = matchAccess.group(1).startsWith("invoke-static");
          boolean useStaticCall = !isOriginStaticCall & useStaticReplaceCall;
          
          final String methodInvocationClassName=matchAccess.group(3);
          //if (methodInvocationClassName.indexOf("android/content/Context")>=0)
          //  System.out.println();
          
          boolean doReplace=true;
          
          //if (optOldMethodParams==null ||
          //    matchAccess.group(4).equals(optOldMethodParams))
          //  doReplace=true;
          if (doReplace) {
            if (applyAlsoToSubclasses) {
              doReplace=classAnalyzer.isSubclassOfByBytecodeName(methodInvocationClassName,origClassNameByteCode);
            } else {
              doReplace=origClassNameByteCode.equals(methodInvocationClassName);
            }
          }
          
          String replacement=null;
          if (doReplace) {
        	  replacement="";
        	  //获取第一个参数(可能有range ... 的case没处理)
        	  String firstParam = matchAccess.group(2).split(",").length>0 ? matchAccess.group(2).split(",")[0] : null;
        	  if(firstParam!=null && needCheckCast){
        		  if(useStaticCall){
        			  replacement = "check-cast " + firstParam + ", " + origClassNameByteCode;
        		  }else if(!isOriginStaticCall && !useStaticCall){
        			  replacement = "check-cast " + firstParam + ", " + Util.classNameToBytecode(replaceByClassName);
        		  }
        	  }
        	  String instructions = "";
        	  if(matchAccess.group(1).contains("rang"))
    		  {    		  
        		  instructions = "invoke-static/range";  		  
    		  }
        	  else
        	  {
        		  instructions = "invoke-static";    
        	  }
            replacement+=
              (useStaticCall&&!isOriginStaticCall? instructions:matchAccess.group(1))+
              " {"+matchAccess.group(2)+"}, "+
              /*Matcher.quoteReplacement*/ Util.classNameToBytecode(replaceByClassName)+
              "->"+newMethodName+"("+
              (useStaticCall ? /*Matcher.quoteReplacement*/ origClassNameByteCode:"")+
              (optNewMethodParams!=null? /*Matcher.quoteReplacement*/ optNewMethodParams:matchAccess.group(4))+")"+
              /*Matcher.quoteReplacement*/ newMethodReturn;
          }
          
          return replacement;
        }
      },
      priority,this
    );
  }
  
  public String apply(String smali, InheritanceHierarchyAnalyzer classAnalyzer) throws ProcessingException {
    if (cachedClassAnalyzer!=classAnalyzer) {
      cachedClassAnalyzer=classAnalyzer;
      precompile(classAnalyzer);
    }
    return smali;
    
    //return pattern.matcher(smali).replaceAll(replacement);
    /*
    Matcher m=pattern.matcher(smali);
    StringBuffer out=new StringBuffer();
    while (m.find()) {
      boolean doReplace;
      if (applyAlsoToSubclasses) {
        String methodInvocationClassName=m.group(3);
        doReplace=classAnalyzer.isSubclassOfByBytecodeName(methodInvocationClassName,origClassNameByteCode);
      } else {
        doReplace=true;
      }
      if (doReplace) {
        String replacement=
          (useStaticReplaceCall? "invoke-static":"$1")+" {$2}, "+Matcher.quoteReplacement(Util.classNameToBytecode(replaceByClassName))+
          "->"+newMethodName+"("+
          (useStaticReplaceCall && !m.group(1).equals("invoke-static")? Matcher.quoteReplacement(origClassNameByteCode):"")+
          (optNewMethodParams!=null? Matcher.quoteReplacement(optNewMethodParams):"$4")+")"+Matcher.quoteReplacement(newMethodReturn);
        
        m.appendReplacement(out,replacement);
      } else {
        m.appendReplacement(out,Matcher.quoteReplacement(m.group()));
      }
    }
    m.appendTail(out);
    return out.toString();
    */
  }

  @Override
  public void addManifestEntries(List<String> entries) {
    entries.add(
      "classmethod "+(applyAlsoToSubclasses? "inclsubclasses":"thisclassonly")+" "+
      origClassName+"."+oldMethodName+"("+
      (optOldMethodParams!=null? optOldMethodParams:"*")+
      "):"+oldMethodReturn+
      " -> "+
      (useStaticReplaceCall? "static":"this")+" "+
      replaceByClassName+"."+newMethodName+"("+
      (optNewMethodParams!=null? optNewMethodParams:"*")+
      "):"+newMethodReturn
    );
  }

  //public void setPriority(int priority) {
  //  this.priority=priority;
  //}

  @Override
  public String toString() {
    return getClass().getSimpleName()+" "+origClassName+"."+oldMethodName+"() -> "+replaceByClassName+"."+newMethodName+"()";
  }

}
