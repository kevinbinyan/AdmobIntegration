package com.zdk.wrap.mg.android;

public interface InheritanceHierarchyAnalyzer {
  public boolean isSubclassOfByBytecodeName(String className, String superClassName);
  public boolean isSubclassOfByJavaName(String className, String superClassName);
}
