package com.zdk.wrap.mg.android;

import java.util.List;

import com.zdk.wrap.mg.GeneratorPhase1ConfigParams;
import com.zdk.wrap.mg.AppModifier.ProcessingException;
import com.zdk.wrap.mg.android.TextManipulator.MatchAccess;

public class GuardedInstantiation extends GeneratorPhase1ConfigParams.GuardedInstantiation implements com.zdk.wrap.mg.WrapManifest.Writer {
  //private String className;
  //private String replaceByClassName;
  private GuardedMethod constructor;
  
  //private Pattern pattern;
  //private String replacement;

  //private Pattern subclassPattern;
  //private String subclassReplacement;
  
  //private Pattern staticMethodsPattern;
  //private String staticMethodsReplacement;
  
  public GuardedInstantiation(GeneratorPhase1ConfigParams.GuardedInstantiation cfgItem, TextManipulator textManipulator) throws ProcessingException {
    super(cfgItem);
    int priority=cfgItem.getPriority();
    
    constructor=new GuardedMethod(new GeneratorPhase1ConfigParams.GuardedMethod(getClassToBeReplaced(),false,getReplacementClass(),
                                                                                "<init>","<init>","*","V"),textManipulator,priority);
    //constructor.setPriority(priority);
    
    final String fromClassname=Util.classNameToBytecode(getClassToBeReplaced());
    final String toClassname=Util.classNameToBytecode(getReplacementClass());
    
    if (Boolean.TRUE) {
      // or combined into one, cf. below, which is a bit faster:
      textManipulator.addRegExpr(
        "(\\bnew-instance [a-z0-9]+, *){param} *\n",
        new String[] { "\\Q"+fromClassname+"\\E" },
        new TextManipulator.Replacer() {
          public String optReplace(MatchAccess matchAccess) throws ProcessingException {
            return matchAccess.group(1)+/*Matcher.quoteReplacement*/ toClassname+"\n";
          }
        },
        priority,this
      );
      
      textManipulator.addRegExpr(
        "(\\.super\\s+){param} *\n",
        new String[] { "\\Q"+fromClassname+"\\E" },
        new TextManipulator.Replacer() {
          public String optReplace(MatchAccess matchAccess) throws ProcessingException {
            return matchAccess.group(1)+/*Matcher.quoteReplacement*/ toClassname+"\n";
          }
        },
        priority,this
      );
      
      textManipulator.addRegExpr(
        "(invoke-static\\s+\\{[^\\}]+\\}\\s*,\\s+){param}(->)",
        new String[] { "\\Q"+fromClassname+"\\E" },
        new TextManipulator.Replacer() {
          public String optReplace(MatchAccess matchAccess) throws ProcessingException {
            return matchAccess.group(1)+/*Matcher.quoteReplacement*/ toClassname+matchAccess.group(2);
          }
        },
        priority,this
      );
    } else {
      textManipulator.addRegExpr(
       //( *\n|->) 其实是不对的，只不过现实中出现不了，还是看上面那三个比较舒服
        "((\\bnew-instance *[a-z0-9]+, *)|(\\.super\\s+)|(invoke-static\\s+\\{[^\\}]+\\}\\s*,\\s+)){param}( *\n|->)",
        new String[] { "\\Q"+fromClassname+"\\E" },
        new TextManipulator.Replacer() {
          public String optReplace(MatchAccess matchAccess) throws ProcessingException {
            return matchAccess.group(1)+/*Matcher.quoteReplacement*/ toClassname+matchAccess.group(5);
          }
        },
        priority,this
      );
    }
  }
  
  public String apply(String smali, InheritanceHierarchyAnalyzer classAnalyzer) throws ProcessingException {
    //smali=pattern.matcher(smali).replaceAll(replacement);
    //smali=subclassPattern.matcher(smali).replaceAll(subclassReplacement);
    smali=constructor.apply(smali,classAnalyzer);
    //smali=staticMethodsPattern.matcher(smali).replaceAll(staticMethodsReplacement);
    return smali;
  }

  @Override
  public void addManifestEntries(List<String> entries) {
    entries.add("instantiation"+" "+getClassToBeReplaced()+" -> "+getReplacementClass()); // also cf. GuardedClass.addManifestEntries()
  }
  
  @Override
  public String toString() {
    return getClass().getSimpleName()+" "+getClassToBeReplaced()+" -> "+getReplacementClass();
  }
}
