package com.zdk.wrap.mg.android;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zdk.wrap.mg.AppModifier.ProcessingException;
import com.zdk.wrap.mg.ServerUtil;

public class Walkaround 
{
	public static String wrappedApktoolymlFilename = null;
	
	public static String getOrgApktoolyml(String apktoolymlPath)
	{
		FileInputStream inStrApktoolyml;
		try 
		{
			inStrApktoolyml = new FileInputStream(apktoolymlPath);
			ByteArrayOutputStream outStrApktoolyml = new ByteArrayOutputStream();
			ServerUtil.copyStream(inStrApktoolyml, outStrApktoolyml);
			inStrApktoolyml.close();
			return outStrApktoolyml.toString("UTF-8");
		}
		 catch (FileNotFoundException e)
		{
			 e.printStackTrace();
		}
		catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		return null;
		
	}
	
	public static String modifyApktoolyml(String apktoolymlStringBuf) throws ProcessingException 
	{
		StringBuffer out = new StringBuffer();
		String optTargetSdkVersion = null;
		@SuppressWarnings("unused")
		int optTargetSdkVersionNum = -1;
		{
			//targetSdkVersion: '22'
			Matcher matcher = Pattern
					.compile("targetSdkVersion: '([0-9][0-9]*)'", Pattern.DOTALL)
					.matcher(apktoolymlStringBuf);
			if (matcher.find())
			{
				optTargetSdkVersion = matcher.group(1);
				try 
				{
					optTargetSdkVersionNum = Integer.parseInt(optTargetSdkVersion);
					if(optTargetSdkVersionNum>22)
					{
						apktoolymlStringBuf = apktoolymlStringBuf.replaceAll("targetSdkVersion: '([0-9][0-9]*)'",
								"targetSdkVersion: '22'");
					}
				} catch (NumberFormatException e) 
				{
					// ignore in this case
				}
			}
		}
		out.append(apktoolymlStringBuf);
		
		return out.toString();
	}
	
	
	public static String write2File(File apktoolymlPath,String newApktoolyml) 
	{
		wrappedApktoolymlFilename = PathAndFileConfig.getWrappedApktoolymlFileName(apktoolymlPath);
		{
			FileOutputStream out;
			try 
			{
				out = new FileOutputStream(wrappedApktoolymlFilename);
				out.write(newApktoolyml.getBytes("UTF-8"));
				out.close();
			}
			catch (FileNotFoundException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (IOException e1) 
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
		}
		
		return wrappedApktoolymlFilename;
	}
	public static void copyWrappedApktoolyml2apktoolExtractDirPath(String apktoolExtractDirPath) 
	{
		try 
		{
			ServerUtil.copyFile(wrappedApktoolymlFilename, apktoolExtractDirPath + File.separator + "apktool.yml");
		} 
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
		

}
