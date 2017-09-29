package com.zdk.wrap.mg.test;

import java.io.File;

import com.zdk.wrap.mg.ServerUtil;
import com.zdk.wrap.mg.android.PathAndFileConfig;
import com.zdk.wrap.mg.android.SmaliClassAnalyzer;
import com.zdk.wrap.mg.android.SmaliModifier;

import jdk.internal.org.objectweb.asm.commons.StaticInitMerger;

public class SimpleSmaliWrap
{

	static String smaliDir = "d:\\SimpleSmali"; 
	public static void main (String[] args)
	{
		try
		{
			boolean useGoodDynamics = true;
			boolean useProxyTier = false;
			boolean developerDebugMode = false;
			
//			File smaliDumpDir = ServerUtil.createTempDir(PathAndFileConfig.SmaliDumpDir);
			File smaliDumpDir = new File(smaliDir);
			SmaliClassAnalyzer classAnalyzer = new SmaliClassAnalyzer(smaliDumpDir, useGoodDynamics);
			SmaliModifier smaliModifier = new SmaliModifier(useGoodDynamics, useProxyTier);
			smaliModifier.injectModifiedClasses(smaliDumpDir, classAnalyzer, developerDebugMode,null);
		} 
		catch (Exception e)
		{
			// TODO: handle exception
		}
		
	}

}
