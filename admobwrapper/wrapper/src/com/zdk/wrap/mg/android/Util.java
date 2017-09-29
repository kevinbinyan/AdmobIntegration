package com.zdk.wrap.mg.android;

import java.io.IOException;

import com.zdk.wrap.mg.ServerUtil;

public class Util {
  
  public static String classNameToBytecode(String className) {
    return"L"+className.replace('.','/')+";";
  }

  public static String classNameVMToJava(String classNameVM) {
    assert classNameVM.startsWith("L") && classNameVM.endsWith(";");
    return classNameVM.substring(1,classNameVM.length()-1).replace('/','.');
  }
  public static void dex2Smali(String dexPath,String outSmaliPath)
  {
		try {
//			debugLog("baksmali.jar execute");
			ServerUtil.runExecutable(new String[]{"java","-jar",PathAndFileConfig.baksmaliPath,"disassemble",dexPath,"-o",outSmaliPath}, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
  }
}
