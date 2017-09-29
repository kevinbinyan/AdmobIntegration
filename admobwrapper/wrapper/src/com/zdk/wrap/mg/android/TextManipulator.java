package com.zdk.wrap.mg.android;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zdk.wrap.mg.GeneratorPhase1ConfigParams;
import com.zdk.wrap.mg.AppModifier.ProcessingException;

public class TextManipulator {
  
  public interface MatchAccess {
    String group(int index) throws ProcessingException;
  }
  
  private static class ParameterizedReplacement {
    /*
    private static class SingleParameter {
      public String literalValue;
      public int sysREIndex;

      public SingleParameter(String literalValue, int sysREIndex) {
        this.literalValue=literalValue;
        this.sysREIndex=sysREIndex;
      }
    }
    */
    
    private static class ParameterSet {
      public String[] valueREs;
      private String[] literalValues;
      public TextManipulator.Replacer replacer;
      private int priority;
      private Object origin;

      public ParameterSet(String[] valueREs, String[] literalValues, TextManipulator.Replacer replacer, int priority, Object origin /* for debugging */) {
        this.valueREs=valueREs;
        this.literalValues=literalValues;
        this.replacer=replacer;
        this.priority=priority;
        this.origin=origin;
      }
      
      public String toString() {
        return "(ParamSet:prio="+priority+" by "+origin+")";
      }
    }
    
    private static Pattern parameterPat=Pattern.compile("\\{param\\}");
    private static Pattern literalParameterPat=Pattern.compile("\\\\Q(.*)\\\\E");
    //private static Pattern openParensPat=Pattern.compile("\\("); //"(^|[^\\\\])\\("); doesn't work for "((abc" because it will only match once not twice
    private String unifiedREAsText;
    private Pattern unifiedREAsPat;
    private List<ParameterSet> parameterSets=new LinkedList<ParameterSet>();
    private Map<Integer,Integer> userToSysREIndex=new HashMap<Integer,Integer>();
    private String parameterizedRegExpr;
    private int numberOfParameters;
    private List<Integer> paramREIndices=new LinkedList<Integer>();
    private Map<String, List<ParameterSet>> firstLiteralValueToParameterSets=new HashMap<String, List<ParameterSet>>();
    private static final int numParensInParamPlaceholder=1; 
    
    public ParameterizedReplacement(String parameterizedRegExpr) throws ProcessingException {
      this.parameterizedRegExpr=parameterizedRegExpr;

      Matcher m=parameterPat.matcher(parameterizedRegExpr);
      int parensCountOffset=0;
      int currUserIndex=1;
      int currSysREIndex=1;
      numberOfParameters=0;
      while (m.find()) {
        numberOfParameters++;
        
        String beforeParam=parameterizedRegExpr.substring(parensCountOffset,m.start());
        for (int off=0;;) {
          int pos=beforeParam.indexOf('(',off);
          if (pos<0)
            break;
          if (pos==0 || beforeParam.charAt(pos-1)!='\\') {
            userToSysREIndex.put(currUserIndex,currSysREIndex);
            currUserIndex++;
            currSysREIndex++;
          }
          off=pos+1;
        }
        parensCountOffset=m.end();
        
        paramREIndices.add(currSysREIndex);
        currSysREIndex+=numParensInParamPlaceholder;
      }
      String pastParam=parameterizedRegExpr.substring(parensCountOffset);
      for (int off=0;;) {
        int pos=pastParam.indexOf('(',off);
        if (pos<0)
          break;
        if (pos==0 || pastParam.charAt(pos-1)!='\\') {
          userToSysREIndex.put(currUserIndex,currSysREIndex);
          currUserIndex++;
          currSysREIndex++;
        }
        off=pos+1;
      }
    }
    
    //public ParameterizedReplacement cloneEmpty(ParameterizedReplacement src) throws ProcessingException {
    //  this(parameterizedRegExpr)
    //}
    
    private void updateUnifiedRE() {
      StringBuffer unifiedREBuf=new StringBuffer();
      Matcher m=parameterPat.matcher(parameterizedRegExpr);
      for (int currParamIndex=0;m.find();currParamIndex++) {
        StringBuffer paramREBuf=new StringBuffer();
        for (ParameterSet parameterSet: parameterSets) {
          if (paramREBuf.length()>0)
            paramREBuf.append('|');
          paramREBuf.append(Matcher.quoteReplacement(parameterSet.valueREs[currParamIndex]));
        }
        m.appendReplacement(unifiedREBuf,"("+paramREBuf.toString()+")");
      }
      m.appendTail(unifiedREBuf);
      unifiedREAsText=unifiedREBuf.toString();
      unifiedREAsPat=Pattern.compile(unifiedREAsText);
    }
    
    private void addParameterSet(String[] valueREs, TextManipulator.Replacer replacer, int priority, Object origin) throws ProcessingException {
      assert valueREs.length==numberOfParameters;

      String[] literalValues=new String[valueREs.length];
      int index=0;
      for (String valueRE: valueREs) {
        Matcher paramM=literalParameterPat.matcher(valueRE);
        if (paramM.matches()) {
          literalValues[index]=paramM.group(1);
        }
        else {
          throw new ProcessingException("Cannot accept text manipulator parameter '"+valueRE+"'");
        }
        //if (param.contains(" "))
        //  throw new ProcessingException("Parameters that contain spaces are currently not supported.");
        index++;
      }
      
      ParameterSet paramSet=new ParameterSet(valueREs,literalValues,replacer,priority,origin);
      addParameterSet(paramSet);
    }
    
    public void addParameterSet(ParameterSet paramSet) throws ProcessingException {
      parameterSets.add(paramSet);
      
      if (paramSet.literalValues.length<=0)
        throw new ProcessingException("Parameter set without any literal parameter value currently not supported.");
      List<ParameterSet> potentialParamSets=firstLiteralValueToParameterSets.get(paramSet.literalValues[0]);
      if (potentialParamSets==null) {
        potentialParamSets=new LinkedList<ParameterSet>();
        firstLiteralValueToParameterSets.put(paramSet.literalValues[0],potentialParamSets);
      }
      potentialParamSets.add(paramSet);
      
      updateUnifiedRE();
      
      /*
      m.appendReplacement(unifiedREBuf,"([^ \r\n]*)"); // as many as possible, not *?
      */
    }
    
    public int mapGroupIndex(int userIndex) throws ProcessingException {
      /*
      int userIndexAt=0;
      int sysREIndexAt=0;
      for (int userParensCount: userParensCounts) {
        int currCorrection=sysREIndexAt-userIndexAt;
        userIndexAt+=userParensCount;
        sysREIndexAt+=userParensCount+numParensInParamPlaceholder;
        
        if (userIndex-1<userIndexAt) {
          return userIndex+currCorrection;
        }
      }
      throw new ProcessingException("User specified RE index out of range.");
      */
      
      Integer sysREIndex=userToSysREIndex.get(userIndex);
      if (sysREIndex==null)
        throw new ProcessingException("User specified RE index out of range.");
      return sysREIndex.intValue();
    }

    /*
    public int numberOfParameters() {
      return numberOfParameters;
    }
    */
    
    public List<ParameterSet> findParameterSetsForMatch(Matcher m) throws ProcessingException {
      List<ParameterSet> potentialParamSets;
      { Iterator<Integer> iter=paramREIndices.iterator();
        if (!iter.hasNext())
          throw new ProcessingException("Empty parameter set not yet supported.");
        int firstREIndex=iter.next();
        String firstActualValue=m.group(firstREIndex);
        potentialParamSets=firstLiteralValueToParameterSets.get(firstActualValue);
        if (potentialParamSets==null)
          throw new ProcessingException("Cannot find matching parameter set for first parameter.");
      }
      
      LinkedList<ParameterSet> matchingParamSets=new LinkedList<ParameterSet>();
      for (ParameterSet potentialParamSet: potentialParamSets) {
        boolean matchesAll=true;
        boolean isFirst=true;
        int literalValueIndex=0; // skip #0
        assert paramREIndices.size()==potentialParamSet.literalValues.length;
        for (int paramREIndex: paramREIndices) {
          if (isFirst)
            isFirst=false;
          else {
            String actualValue=m.group(paramREIndex);
            if (!actualValue.equals(potentialParamSet.literalValues[literalValueIndex])) {
              matchesAll=false;
              break;
            }
          }
          literalValueIndex++;
        }
        if (matchesAll) {
          matchingParamSets.add(potentialParamSet);
        }
      }
//      if (matchingParamSets.isEmpty())
//        throw new ProcessingException("Cannot find any matching parameter set.");
      return matchingParamSets;
    }

    public int getMinPriority() {
      int min=-1;
      for (ParameterSet paramSet: parameterSets) {
        if (paramSet.priority!=GeneratorPhase1ConfigParams.PrioritizableGuardedEntity.PRIO_DONTCARE &&
            (min<0 || paramSet.priority<min))
          min=paramSet.priority;
      }
      //assert min>=0; // i.e. non-empty
      return min;
    }

    public int getMaxPriority() {
      int max=-1;
      for (ParameterSet paramSet: parameterSets) {
        if (paramSet.priority!=GeneratorPhase1ConfigParams.PrioritizableGuardedEntity.PRIO_DONTCARE &&
            (max<0 || paramSet.priority>max))
          max=paramSet.priority;
      }
      //assert max>=0; // i.e. non-empty
      return max;
    }
    
    public String toString() {
      return "(ParameterizedReplacement:"+parameterizedRegExpr+" ["+getMinPriority()+"-"+getMaxPriority()+"] paramSets:"+parameterSets+")";
    }
  }
  
  public interface Replacer {
    String optReplace(MatchAccess matchAccess) throws ProcessingException;
  }
  
  private Map<String,ParameterizedReplacement> replacementsByParamRE=new HashMap<String,ParameterizedReplacement>();
  private List<ParameterizedReplacement> cachedExecutionOrder=null;
  
  public TextManipulator() {
  }
  
  public void addRegExpr(String parameterizedRegExpr, String[] parameterValues, TextManipulator.Replacer replacer, int priority, Object origin) throws ProcessingException {
    ParameterizedReplacement replacement=replacementsByParamRE.get(parameterizedRegExpr);
    if (replacement==null) {
      replacement=new ParameterizedReplacement(parameterizedRegExpr);    
      replacementsByParamRE.put(parameterizedRegExpr,replacement);
    }
    replacement.addParameterSet(parameterValues,replacer,priority,origin);
    
    cachedExecutionOrder=null; // invalidate
  }
  
  private List<ParameterizedReplacement> calculateExecutionSequence() throws ProcessingException {
    if (cachedExecutionOrder==null) {
      List<ParameterizedReplacement> order=new LinkedList<ParameterizedReplacement>();
      Collection<ParameterizedReplacement> replacementsSource=replacementsByParamRE.values();
      //order.addAll(replacementsByParamRE.values());

      // split replacements if it contains too many priorities:
      for (ParameterizedReplacement replacement: replacementsSource) {
        Set<Integer> prios=new HashSet<Integer>();
        for (ParameterizedReplacement.ParameterSet paramSet: replacement.parameterSets)
          prios.add(paramSet.priority);
        //prios.remove(GeneratorPhase1ConfigParams.PrioritizableGuardedEntity.PRIO_DONTCARE); // if present at all
        int numPrios=prios.size();
        //if (prios.contains(GeneratorPhase1ConfigParams.PrioritizableGuardedEntity.PRIO_DONTCARE))
        //  numPrios--;
        if (numPrios==1) {
          order.add(replacement);
        } else {
          //throw new ProcessingException("More than one priority within the same parameter set is currently not supported.");
          // split:
          //prios.remove(prios.iterator().next()); // remove one
          boolean isFirst=true;
          for (int splitPrio: prios) {
            ParameterizedReplacement newReplacement=new ParameterizedReplacement(replacement.parameterizedRegExpr); //replacement.clone();
            for (ParameterizedReplacement.ParameterSet paramSet: replacement.parameterSets) {
              if (paramSet.priority==splitPrio ||
                  (isFirst && paramSet.priority==GeneratorPhase1ConfigParams.PrioritizableGuardedEntity.PRIO_DONTCARE)) {
                newReplacement.addParameterSet(paramSet);
              }
            }
            order.add(newReplacement);
            isFirst=false;
          }
        }
      }
      /*
      if (priority!=GeneratorPhase1ConfigParams.PrioritizableGuardedEntity.PRIO_DONTCARE) {
        boolean allDidntMatter=true;
        for (ParameterSet otherParamSet: parameterSets) {
          if (otherParamSet.priority!=GeneratorPhase1ConfigParams.PrioritizableGuardedEntity.PRIO_DONTCARE) {
            allDidntMatter=false;
            break;
          }
        }
        if (allDidntMatter) {
          for (ParameterSet otherParamSet: parameterSets)
            otherParamSet.priority=priority;
        }
      }
      */

      Collections.sort(order,new Comparator<ParameterizedReplacement>() {
        @Override
        public int compare(ParameterizedReplacement o1, ParameterizedReplacement o2) {
          int p1OrNeg=o1.getMaxPriority(); // or min
          int p2OrNeg=o2.getMaxPriority(); // or min
          if (p1OrNeg==p2OrNeg)
            return 0;
          else if (p1OrNeg<p2OrNeg)
            return 1; // descending order
          else
            return -1;
        }
      });
      
      // assert that all priority ranges are disjunct:
      int prevMin=-1;
      for (ParameterizedReplacement replacement: order) {
        int min=replacement.getMinPriority();
        int max=replacement.getMaxPriority();
        //dbgOut("min="+min+" max="+max);
        if (min>=0 && prevMin>=0) {
          if (prevMin<max) {
            throw new ProcessingException("Execution order priorities not disjunct.");
          }
        }
        if (min>max) // even if <0
          throw new ProcessingException("Execution order priorities inconsistent.");
        
        prevMin=min; // even if <0
      }
      
      cachedExecutionOrder=order;
    }
    return cachedExecutionOrder;
  }
  
  //private void dbgOut(String text) {
  //  System.out.println(text);    
  //}

  public String apply(String text) throws ProcessingException {
    for (final ParameterizedReplacement replacement: calculateExecutionSequence() /*replacementsByParamRE.values()*/) {
      final Matcher globalMatcher=replacement.unifiedREAsPat.matcher(text); 
      final MatchAccess globalMatchAccess=buildMatchAccess(replacement,globalMatcher);
      //Matcher availableMatcherForMatchAccess=globalMatcher;
      //dbgOut("apply unifiedRE: "+replacement.unifiedREAsText);
      
      StringBuffer newText=new StringBuffer();
      while (globalMatcher.find()) {      
        //String fullMatch=globalMatcher.group();
        
        MatchAccess matchAccess=globalMatchAccess;
        String currentReplacementText=null;
        List<ParameterizedReplacement.ParameterSet> matchingParamSets=replacement.findParameterSetsForMatch(globalMatcher);
        for (ParameterizedReplacement.ParameterSet paramSet: matchingParamSets) {
          if (matchAccess==null) {
            assert currentReplacementText!=null;
            Matcher m2=replacement.unifiedREAsPat.matcher(currentReplacementText);
            if (!m2.find())
              throw new ProcessingException("Refreshed matcher should have matched the replaced text because it's a found parameter set for that match.");
            matchAccess=buildMatchAccess(replacement,m2);
          }
          
          //dbgOut("apply paramSet: "+paramSet.toString());
          String localReplacementText=paramSet.replacer.optReplace(matchAccess);
          if (localReplacementText!=null) {
            currentReplacementText=localReplacementText;
            
            // refresh next Matcher and MatchAccess:
            matchAccess=null;
          }
        }
        if (currentReplacementText!=null)
          globalMatcher.appendReplacement(newText,Matcher.quoteReplacement(currentReplacementText));
      }
      globalMatcher.appendTail(newText);
      text=newText.toString();
    }
    return text;
  }

  private MatchAccess buildMatchAccess(final ParameterizedReplacement replacement, final Matcher m) {
    return new MatchAccess() {
      @Override
      public String group(int index) throws ProcessingException {
        if (index==0)
          return m.group();
        else
          return m.group(replacement.mapGroupIndex(index));
      }
    };
  }
}
