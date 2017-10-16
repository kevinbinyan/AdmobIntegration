package com.zdk.wrap.mg;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class GeneratorPhase1ConfigParams {
  private static final boolean injectGDAPI=RuntimePhase3ConfigParams.injectGDAPI;
  private static boolean finishDebugAble = false;
  private static boolean isSQLWrapperNeeded = true;
//  private static boolean isHookNativeNeeded = false;
  //private static final boolean useProxyTier=true;
  
  public static class PrioritizableGuardedEntity {
    public static final int PRIO_DONTCARE=100000;
    public static final int PRIO_LOWEST=PRIO_DONTCARE*2;
    //�������Լ���������entity
    private List<PrioritizableGuardedEntity> entitiesThatComeLater=new LinkedList<PrioritizableGuardedEntity>();
    //�����ȫ�ֵģ��������Ǹ�����һ����
    private static List<PrioritizableGuardedEntity> all=new LinkedList<PrioritizableGuardedEntity>();
    private static boolean processed=false;//���︳ֵtrue�ˣ�
    private int calculatedPriority;
    
    protected PrioritizableGuardedEntity() {
      all.add(this);
      assert !processed;
    }
    
    public void executeBefore(PrioritizableGuardedEntity guardedEntity) {
      entitiesThatComeLater.add(guardedEntity);
      assert !processed;
    }
    
    //计算优先级
    private static void calculatePriorities() {
      if (!processed) {
    	  
    	  //初始化所有优先级为100000
        for (PrioritizableGuardedEntity e: all)
          e.calculatedPriority=PRIO_DONTCARE;
        
        for (;;) 
        {
          boolean hasChanged=false;
          for (PrioritizableGuardedEntity entityInAll: all) {
            int maxPriority=-1;
            for (PrioritizableGuardedEntity later: entityInAll.entitiesThatComeLater) 
            {
            //这优先级，怎么算的？不过instantiation是要在guarded class后才运行。
              if (later.calculatedPriority==PRIO_DONTCARE) // all entities in entitiesThatComeLater now matter regarding priority
                later.calculatedPriority=PRIO_LOWEST;
              
              if (later.calculatedPriority>maxPriority)
                maxPriority=later.calculatedPriority;
            }
            //�������later��������priority���Լ�����Ҫ���������
            if (maxPriority>=0) 
            {
              int newPrio=maxPriority+1000;///1000�Ǹ�ʲô��������PRIO_DONTCARE=100000;
              //e.calculatedPriority=Math.max(e.calculatedPriority,newPrio);
              //要保证entityInAll的优先级大于需要在他后面wrap的entity的优先级
              if (newPrio>entityInAll.calculatedPriority) 
              {
                entityInAll.calculatedPriority=newPrio;
                hasChanged=true;
              }
            }
          }
          if (!hasChanged) // now stable (assuming that it's non-circular)
            break;
        }
        
        //for (PrioritizableGuardedEntity e: all)
        //  System.out.println(e.toString()+" prio="+e.calculatedPriority);
      }
    }
    
    public int getPriority() {
      calculatePriorities();
      return calculatedPriority;
    }
  }
  
  public static class GuardedClass extends PrioritizableGuardedEntity {
    private String classToBeReplaced;
    private String replacementClass;
    private boolean includeMethodSignatures;
    private List<GuardedMethodBodyInAlreadyGuardedClass> methodBodies=new LinkedList<GuardedMethodBodyInAlreadyGuardedClass>();
    
    public GuardedClass(String classToBeReplaced, String replacementClass, boolean includeMethodSignatures) {
      this.classToBeReplaced=classToBeReplaced;
      this.replacementClass=replacementClass;
      this.includeMethodSignatures=includeMethodSignatures;
    }

    public String getClassToBeReplaced() {
      return classToBeReplaced;
    }

    public String getReplacementClass() {
      return replacementClass;
    }

    public boolean getIncludeMethodSignatures() {
      return includeMethodSignatures;
    }
    
    public void addMethodBody(GuardedMethodBodyInAlreadyGuardedClass methodBody) {
      methodBodies.add(methodBody);
    }

    public List<GuardedMethodBodyInAlreadyGuardedClass> getMethodBodies() {
      return methodBodies;
    }
  }
  
  public static class GuardedInstantiation extends PrioritizableGuardedEntity {
    private String classToBeReplaced;
    private String replacementClass;
    private boolean includeAllStaticMethods;
    
    public GuardedInstantiation(String classToBeReplaced, String replacementClass, boolean includeAllStaticMethods) {
      this.classToBeReplaced=classToBeReplaced;
      this.replacementClass=replacementClass;
      this.includeAllStaticMethods=includeAllStaticMethods;
      
    }

    protected GuardedInstantiation(GuardedInstantiation src) {
      this(src.getClassToBeReplaced(),src.getReplacementClass(),src.getIncludeAllStaticMethods());
      
    }

    public String getClassToBeReplaced() {
      return classToBeReplaced;
    }

    public String getReplacementClass() {
      return replacementClass;
    }
    
    public boolean getIncludeAllStaticMethods() {
      return includeAllStaticMethods;
    }
  }
  
  public static class GuardedMethod extends PrioritizableGuardedEntity {
    protected String origClassName;
    protected boolean applyAlsoToSubclasses;
    protected String replaceByClassName;
    protected boolean useStaticReplaceCall;
    protected String oldMethodName;
    protected String newMethodName;
    protected String optOldMethodParams;
    protected String optNewMethodParams;
    protected String oldMethodReturn;
    protected String newMethodReturn;
    //保持调用类，而不是直接使用声明的调用类
    protected boolean needCheckCast;
    
    public GuardedMethod(String origClassName, boolean applyAlsoToSubclasses, String replaceByClassName, 
        boolean useStaticReplaceCall, 
        String oldMethodName, String newMethodName,
        String oldMethodParamsOptOrWildcard, String newMethodParams,
        String oldMethodReturn, String newMethodReturn) {
      this.needCheckCast=needCheckCast;
      this.origClassName=origClassName;
      this.applyAlsoToSubclasses=applyAlsoToSubclasses;
      this.replaceByClassName=replaceByClassName;
      this.useStaticReplaceCall=useStaticReplaceCall;
      this.oldMethodName=oldMethodName;
      this.newMethodName=newMethodName;
      this.optOldMethodParams=oldMethodParamsOptOrWildcard==null? null:getOptOldMethodParams(oldMethodParamsOptOrWildcard);
      this.optNewMethodParams=this.optOldMethodParams!=null? newMethodParams:null;
      this.oldMethodReturn=oldMethodReturn;
      this.newMethodReturn=newMethodReturn;
    }
    
    public GuardedMethod(boolean needCheckCast, String origClassName, boolean applyAlsoToSubclasses, String replaceByClassName, 
            boolean useStaticReplaceCall, 
            String oldMethodName, String newMethodName,
            String oldMethodParamsOptOrWildcard, String newMethodParams,
            String oldMethodReturn, String newMethodReturn) {
          this(origClassName,applyAlsoToSubclasses,replaceByClassName,useStaticReplaceCall,oldMethodName,newMethodName,oldMethodParamsOptOrWildcard,newMethodParams,oldMethodReturn,newMethodReturn);
          this.needCheckCast=needCheckCast;
        }
    
    public GuardedMethod(String className, boolean applyAlsoToSubclasses, String replaceByClassName,
        String oldMethodName, String newMethodName, 
        String methodParams, String methodReturn) {
      this(className,applyAlsoToSubclasses,replaceByClassName,false,oldMethodName,newMethodName,methodParams,methodParams,methodReturn,methodReturn);
    }
    
    protected GuardedMethod(GuardedMethod src) {
      this(src.needCheckCast, src.origClassName,src.applyAlsoToSubclasses,src.replaceByClassName,
           src.useStaticReplaceCall,src.oldMethodName,src.newMethodName,
           src.optOldMethodParams,src.optNewMethodParams,
           src.oldMethodReturn,src.newMethodReturn);
    }
    
    private String getOptOldMethodParams(String oldMethodParams) {
      return oldMethodParams.equals("*")? null:oldMethodParams;
    }
    
    public boolean origMethodEquals(String origClassName, String oldMethodName, String optOldMethodParams) {
      return origClassName.equals(this.origClassName) &&
          oldMethodName.equals(this.oldMethodName) &&
          ((this.optOldMethodParams==null && optOldMethodParams==null) ||
           (this.optOldMethodParams!=null && optOldMethodParams!=null && this.optOldMethodParams.equals(optOldMethodParams)));
    }
  }
  
  public static abstract class GuardedMethodBodyBase extends PrioritizableGuardedEntity {
    public boolean includeSubClasses;
    public String methodName;
    public String methodParams;
    public String methodReturn;
    public String newMethodName;
    public String optNewSmaliBody;
    public String optAccessModifier;//public,private,protected
    
    protected GuardedMethodBodyBase(boolean includeSubClasses, String methodName, String methodParams, String methodReturn,
        String newMethodName, String optNewSmaliBody,String optNewAccessModifier) {
      this.includeSubClasses=includeSubClasses;
      this.methodName=methodName;
      this.methodParams=methodParams;
      this.methodReturn=methodReturn;
      this.newMethodName=newMethodName;
      this.optNewSmaliBody=optNewSmaliBody;
      this.optAccessModifier = optNewAccessModifier;
    }
    
    //protected GuardedMethodBodyBase(GuardedMethodBodyBase src) {
    //  this(src.className,src.newClassName,src.methodName,src.methodParams,src.methodReturn,src.newMethodName,src.optNewSmaliBody);
    //}
    
    public abstract String getClassName();
    public abstract String getNewClassName();
  }
  
  public static class GuardedMethodBodyStandalone extends GuardedMethodBodyBase {
    private String className;
    private String newClassName;

    public GuardedMethodBodyStandalone(String className, boolean includeSubClasses, String newClassName, 
        String methodName, String methodParams, String methodReturn,
        String newMethodName, String optNewSmaliBody) {
    	//todo:以后看看是否也需要newAccessModifier
      super(includeSubClasses,methodName,methodParams,methodReturn,newMethodName,optNewSmaliBody,null);
      this.className=className;
      this.newClassName=newClassName;
    }
    
    @Override
    public String getClassName() {
      return className;
    }
    
    @Override
    public String getNewClassName() {
      return newClassName;
    }
  }
  
  public static class GuardedMethodBodyInAlreadyGuardedClass extends GuardedMethodBodyBase {
    private GuardedClass guardedClass;
    
    public GuardedMethodBodyInAlreadyGuardedClass(GuardedClass guardedClass, 
        boolean includeSubClasses, 
        String methodName, String methodParams, String methodReturn,
        String newMethodName, String optNewSmaliBody,String optNewAccessModifier) {
      super(includeSubClasses,methodName,methodParams,methodReturn,newMethodName,optNewSmaliBody,optNewAccessModifier);
      this.guardedClass=guardedClass;
    }
    
    @Override
    public String getClassName() {
      return guardedClass.classToBeReplaced;
    }
    
    @Override
    public String getNewClassName() {
      return guardedClass.replacementClass;
    }
  }

  // should be in same package to allow "package" method modifier:
  private static final String proxyTierPackagePrefix="";
  private static final String proxyTierClassNamePrefix="Proxied_";
  //public static final String proxyTierPackagePrefix="com.good.wrap.gd.proxy.generated."; 
  //public static final String proxyTierClassNamePrefix="";
  
  public static String getProxiedClassBaseName(String origClassBaseName) {
    return proxyTierClassNamePrefix+origClassBaseName;
  }
  
  public static String getProxiedClassPackage(String origPackage) {
    if (proxyTierPackagePrefix.length()==0 && origPackage.startsWith("android."))
      return "proxied."+origPackage;
    else
      return proxyTierPackagePrefix+origPackage;
  }

  private static String getProxiedClassFullName(String origClassName, boolean useProxyTier) {
    if (useProxyTier) {
      int dotPos=origClassName.lastIndexOf('.');
      return 
        (dotPos>=0?
           getProxiedClassPackage(origClassName.substring(0,dotPos))+
           "."+
           getProxiedClassBaseName(origClassName.substring(dotPos+1)):
           proxyTierPackagePrefix+proxyTierClassNamePrefix+origClassName);      
    } else {
      return origClassName;
    }
  }
  
  public static class GuardedEntities {
    private List<GeneratorPhase1ConfigParams.GuardedClass> guardedClasses;
    private List<GeneratorPhase1ConfigParams.GuardedInstantiation> guardedInstantiations;
    private List<GeneratorPhase1ConfigParams.GuardedMethod> guardedMethods;
    private List<GeneratorPhase1ConfigParams.GuardedMethodBodyStandalone> guardedMethodBodies;
    private boolean useGoodDynamics;
    private boolean useProxyTier;
    
    public GuardedEntities(boolean useGoodDynamics, boolean useProxyTier) {
      this.useGoodDynamics=useGoodDynamics;
      this.useProxyTier=useProxyTier;
      
      guardedClasses=new ArrayList<GeneratorPhase1ConfigParams.GuardedClass>();
      guardedInstantiations=new ArrayList<GeneratorPhase1ConfigParams.GuardedInstantiation>();
      guardedMethods=new ArrayList<GeneratorPhase1ConfigParams.GuardedMethod>();
      guardedMethodBodies=new ArrayList<GeneratorPhase1ConfigParams.GuardedMethodBodyStandalone>();
      
      addGuardedClasses();
      
      // methods have been specified with non-wrapped types, execute them before their parameter types get replaced:
      for (GeneratorPhase1ConfigParams.GuardedMethod high: guardedMethods) 
      {
        for (GeneratorPhase1ConfigParams.GuardedClass low: guardedClasses)
          high.executeBefore(low);
      }
      for (GeneratorPhase1ConfigParams.GuardedMethodBodyStandalone high: guardedMethodBodies) {
        for (GeneratorPhase1ConfigParams.GuardedClass low: guardedClasses)
          high.executeBefore(low);
      }
    }
    
    private GuardedClass addClass(String classToBeReplaced, String replacementClass, boolean includeMethodSignatures) {
      final boolean proxyOnly=classToBeReplaced.equals(replacementClass);
      GuardedClass guardedClass=null;
      
      if (!proxyOnly) {
        guardedClass=new GuardedClass(classToBeReplaced,replacementClass,includeMethodSignatures);
        guardedClasses.add(guardedClass);
      }
      if (useProxyTier || proxyOnly) {
        GuardedInstantiation guardedInstantiation=
          new GuardedInstantiation(replacementClass,getProxiedClassFullName(replacementClass,useProxyTier),false);
        guardedInstantiations.add(guardedInstantiation);
        if (guardedClass!=null)
          guardedClass.executeBefore(guardedInstantiation);
      }
      return guardedClass;
    }

    private GuardedClass addActivityClass(String classToBeReplaced, String replacementClass) {
      GuardedClass guardedClass=addClass(classToBeReplaced,replacementClass,false);
      /*
      guardedClass.addMethodBody(new GuardedMethodBodyInAlreadyGuardedClass(
        guardedClass,true,
        "onCreate","Landroid/os/Bundle;","V","wrappedPreviousOnCreate",
        null
      ));
      guardedClass.addMethodBody(new GuardedMethodBodyInAlreadyGuardedClass(
        guardedClass,true,
        "onResume","","V","wrappedPreviousOnResume",
        null
      ));
      */
      return guardedClass;
    }

    private void addGuardedClasses() {
      if (useGoodDynamics) {
        /*
        guardedClasses.add(new GuardedClass("android.app.Activity","com.good.gd.Activity",false));
        guardedClasses.add(new GuardedClass("android.app.ListActivity","com.good.gd.ListActivity",false));
        guardedClasses.add(new GuardedClass("android.app.ExpandableListActivity","com.good.gd.ExpandableListActivity",false));
        guardedClasses.add(new GuardedClass("android.preference.PreferenceActivity","com.good.gd.PreferenceActivity",false));      
        guardedClasses.add(new GuardedClass("android.support.v4.app.FragmentActivity","com.good.gd.FragmentActivity",false));
        */
         addActivityClass("android.app.Activity","com.admob.sunb.admobauto.AdmobActivity");
        }
        
    }

    public List<GuardedMethod> getGuardedMethods() {
      return guardedMethods;
    }
    
    public List<GuardedMethodBodyStandalone> getGuardedMethodBodies() {
      return guardedMethodBodies;
    }
    
    public List<GuardedClass> getGuardedClasses() {
      return guardedClasses;
    }

    public List<GuardedInstantiation> getGuardedInstantiations() {
      return guardedInstantiations;
    }
    
    public String getStatistics() {
      return
        guardedMethods.size()+" guardedMethods, "+
        guardedMethodBodies.size()+" guardedMethodBodies, "+
        guardedClasses.size()+" guardedClasses, "+
        guardedInstantiations.size()+" guardedInstantiations";
    }
  }
    
}
