package com.zdk.wrap.mg.android;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zdk.wrap.mg.GeneratorPhase1ConfigParams;
import com.zdk.wrap.mg.AppModifier.ProcessingException;
import com.zdk.wrap.mg.GeneratorPhase1ConfigParams.GuardedMethodBodyInAlreadyGuardedClass;
import com.zdk.wrap.mg.android.TextManipulator.MatchAccess;

public class GuardedClass implements com.zdk.wrap.mg.WrapManifest.Writer {
  //private String classToBeReplaced;
  //private String replacementClass;
  //private boolean includeMethodSignatures;

  private GeneratorPhase1ConfigParams.GuardedClass cfgItem;
  //private Pattern pattern;
  //private String replacement;
  //private Pattern methodCallPattern;
  //private Pattern classAsParamPattern;
  //private String classAsParamReplacement;
  //private Pattern classFieldPattern;
  //private String classFieldReplacement;
  //private Pattern methodDeclPat;
  private List<GuardedMethodBody> methodBodies;
  private TextManipulator textManipulator;
  
  public GuardedClass(GeneratorPhase1ConfigParams.GuardedClass cfgItem, TextManipulator textManipulator) throws ProcessingException {
    this(cfgItem,textManipulator,cfgItem.getPriority());
  }
  
  public GuardedClass(GeneratorPhase1ConfigParams.GuardedClass cfgItem, TextManipulator textManipulator, int priority) throws ProcessingException {
    //super(cfgItem.getClassToBeReplaced(),cfgItem.getReplacementClass(),cfgItem.getIncludeMethodSignatures());
    this.cfgItem=cfgItem;
    this.textManipulator=textManipulator;
    
    methodBodies=new LinkedList<GuardedMethodBody>();
    for (GuardedMethodBodyInAlreadyGuardedClass methodBody: cfgItem.getMethodBodies()) {
      GuardedMethodBody extendedBody=new GuardedMethodBody(methodBody,priority); // priority for use after processing moves from apply() into TextManipulator
      //extendedBody.setPriority(priority);
      methodBodies.add(extendedBody);
    }
    
    precompile(priority);
  }
  
  private boolean getIncludeFields() {
    return cfgItem.getIncludeMethodSignatures(); // heuristic
  }

  private void precompile(int priority) throws ProcessingException {
    /* cases:
      new-instance v0, Landroid/widget/TextView;
      .super Landroid/widget/TextView;
      invoke-direct {p0, p1}, Landroid/widget/TextView;-><init>(Landroid/content/Context;)V
      invoke-super {p0, p1}, Landroid/widget/TextView;->draw(Landroid/graphics/Canvas;)V
      const-string v3, "android.app.NativeActivity" and then invoke-static {v3}, Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;
    */
    //final Pattern noClassReplacePat=Pattern.compile("^\\s*(check-cast|invoke-virtual)\\s.*");
    
    final String fromClassname=Util.classNameToBytecode(cfgItem.getClassToBeReplaced());
    final String toClassname=Util.classNameToBytecode(cfgItem.getReplacementClass());
    textManipulator.addRegExpr(
      "((\\.super|\\.local|\\.catch|new-instance|(invoke-(super|direct|static|interface|virtual)([/\\-]range)?))\\b([^\\n\\(]*?)"+
      // invoke-direct|invoke-super|..., i.e. not invoke-virtual because it is not necessary and fails, e.g., in kyle_unity_test20.apk:com/unity3d/player/UnityPlayer.smali:.method private c()V: invoke-virtual {v0}, Lcom/ondeego/appcentral/shell/android/guarded/Activity;->isFinishing()Z 
      "\\b){param}",
      new String[] { "\\Q"+fromClassname+"\\E" },
      new TextManipulator.Replacer() {
        public String optReplace(MatchAccess matchAccess) throws ProcessingException {
          if (!(!cfgItem.getIncludeMethodSignatures() && matchAccess.group(2).startsWith("invoke-virtual"))) {
            // e.g. in NonSupportedConvenienceMethodsTest do not replace
            // <     invoke-virtual {v0, v1}, Landroid/app/Activity;->getPreferences(I)Landroid/content/SharedPreferences;
            // >     invoke-virtual {v0, v1}, Lcom/good/wrap/gd/Activity;->getPreferences(I)Landroid/content/SharedPreferences;
            return
              matchAccess.group(1)+
              /*Matcher.quoteReplacement*/ toClassname;
          } else {
            return null;
          }
        }
      },
      priority,this
    );
    
    if (getIncludeFields()) {
      // .field private db:Landroid/database/sqlite/SQLiteDatabase;
      // iput-object v0, p0, Lcom/good/wrap/gd/android/unittest/SQLiteTest;->db:Landroid/database/sqlite/SQLiteDatabase;
      // iget-object v1, p0, Lcom/good/wrap/gd/android/unittest/SQLiteTest;->db:Landroid/database/sqlite/SQLiteDatabase;
      textManipulator.addRegExpr(
        "((\\.field|(i|s)(put|get)-object)\\b[^:\\n]+?:){param}",
        new String[] { "\\Q"+fromClassname+"\\E" },
        new TextManipulator.Replacer() {
          public String optReplace(MatchAccess matchAccess) throws ProcessingException {
            return
              matchAccess.group(1)+
              /*Matcher.quoteReplacement*/ toClassname;
          }
        },
        priority,this
      );
    }
    
    if (cfgItem.getIncludeMethodSignatures()) {
      final Pattern classAsParamPattern=Pattern.compile(/* not \b, an int = I could be its predecessor */ "\\Q"+fromClassname+"\\E");
      final String classAsParamReplacement=Matcher.quoteReplacement(toClassname);
      
      textManipulator.addRegExpr(
        "(\\binvoke-[a-z\\-]+\\b[^\\n\\(]*?\\()([^\\n]*?{param}[^\\n]*?)(\n)",
        new String[] { "\\Q"+fromClassname+"\\E" },
        new TextManipulator.Replacer() {
          public String optReplace(MatchAccess matchAccess) throws ProcessingException {
            String paramTypes=matchAccess.group(2); // including return type
            Matcher paramMatcher=classAsParamPattern.matcher(paramTypes);
            StringBuffer replParam=new StringBuffer();
            while (paramMatcher.find())
              paramMatcher.appendReplacement(replParam,classAsParamReplacement);
            paramMatcher.appendTail(replParam);
            
            return matchAccess.group(1)+/*Matcher.quoteReplacement*/ replParam.toString()+matchAccess.group(3);
          }
        },
        priority,this
      );
      //methodCallReplacement="$1"
      //classAsParamReplacement=Matcher.quoteReplacement(toClassname);
      
      // e.g. .method private doCreate(Landroid/database/sqlite/SQLiteDatabase;)V
      textManipulator.addRegExpr(
        "(\\.method ((private|public|protected|abstract|static|final|constructor|bridge|synthetic|declared-synchronized)\\s+)*([0-9a-zA-Z_\\$]+|<init>))(\\([^\\s]*{param}[^\\s]*)",
        new String[] { "\\Q"+fromClassname+"\\E" },
        new TextManipulator.Replacer() {
          public String optReplace(MatchAccess matchAccess) throws ProcessingException {
            String paramTypes=matchAccess.group(5); // including return type
            Matcher paramMatcher=classAsParamPattern.matcher(paramTypes);
            StringBuffer replParam=new StringBuffer();
            while (paramMatcher.find())
              paramMatcher.appendReplacement(replParam,classAsParamReplacement);
            paramMatcher.appendTail(replParam);
                
            return matchAccess.group(1)+/*Matcher.quoteReplacement*/ replParam.toString();
          }
        },
        priority,this
      );
    }
    
    //precompiledReplaceClassWith.put(Pattern.compile("(const-string\\s[^\\n,]+,\\s*\")\\Q"+fromClassname+"\\E(\")"),
    //    "$1"+toClassname+"$2");
  }
  
  public String apply(String smali, InheritanceHierarchyAnalyzer classAnalyzer) throws ProcessingException {
    // before .super is modified, i.e. before SmaliModifier.injectModifiedClasses()/textManipulator.apply() :
    for (GuardedMethodBody methodBody: methodBodies) {
      smali=methodBody.apply(smali,classAnalyzer);
    }
    
    /*
    now instead: textManipulator.apply in a global place
    
    { Matcher m=pattern.matcher(smali);
      //smali=m.replaceAll(replacement);
      StringBuffer out=new StringBuffer();
      while (m.find()) {
        if (!(!cfgItem.getIncludeMethodSignatures() && m.group(2).startsWith("invoke-virtual"))) {
          // e.g. in NonSupportedConvenienceMethodsTest do not replace
          // <     invoke-virtual {v0, v1}, Landroid/app/Activity;->getPreferences(I)Landroid/content/SharedPreferences;
          // >     invoke-virtual {v0, v1}, Lcom/good/wrap/gd/Activity;->getPreferences(I)Landroid/content/SharedPreferences;
          m.appendReplacement(out,replacement);
        }
      }
      m.appendTail(out);
      smali=out.toString();        
    }
    
    if (getIncludeFields()) {
      smali=classFieldPattern.matcher(smali).replaceAll(classFieldReplacement);
    }
    
    if (cfgItem.getIncludeMethodSignatures()) {
      { Matcher m=methodCallPattern.matcher(smali);
        StringBuffer replStmt=new StringBuffer();
        while (m.find()) {
          String paramTypes=m.group(2); // including return type
          Matcher paramMatcher=classAsParamPattern.matcher(paramTypes);
          StringBuffer replParam=new StringBuffer();
          while (paramMatcher.find()) {
            paramMatcher.appendReplacement(replParam,classAsParamReplacement);
          }
          paramMatcher.appendTail(replParam);
          
          m.appendReplacement(replStmt,"$1"+Matcher.quoteReplacement(replParam.toString())+"$3");
        }
        m.appendTail(replStmt);
        smali=replStmt.toString();
      }
      
      { Matcher m=methodDeclPat.matcher(smali);
        StringBuffer replStmt=new StringBuffer();
        while (m.find()) {
          String paramTypes=m.group(5); // including return type
          Matcher paramMatcher=classAsParamPattern.matcher(paramTypes);
          StringBuffer replParam=new StringBuffer();
          while (paramMatcher.find()) {
            paramMatcher.appendReplacement(replParam,classAsParamReplacement);
          }
          paramMatcher.appendTail(replParam);
          
          m.appendReplacement(replStmt,"$1"+Matcher.quoteReplacement(replParam.toString()));
        }
        m.appendTail(replStmt);
        smali=replStmt.toString();
      }
    }
    */
    
    return smali;
  }
  
  @Override
  public void addManifestEntries(List<String> entries) {
    List<String> usages=new LinkedList<String>();
    usages.add("class");
    usages.add("superclass");
    usages.add("vardeclaration");
    usages.add("catch");
    usages.add("instantiation"); // also cf. GuardedInstantiation
    if (cfgItem.getIncludeMethodSignatures())
      usages.add("methodsignature");
    if (getIncludeFields())
      usages.add("fielddecl");

    for (String usage: usages) {
      entries.add(usage+" "+cfgItem.getClassToBeReplaced()+" -> "+cfgItem.getReplacementClass());
    }
  }
  
  @Override
  public String toString() {
    return getClass().getSimpleName()+" "+cfgItem.getClassToBeReplaced()+" -> "+cfgItem.getReplacementClass();
  }

}
