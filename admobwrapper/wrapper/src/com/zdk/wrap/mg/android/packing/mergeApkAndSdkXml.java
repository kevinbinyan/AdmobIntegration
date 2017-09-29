package com.zdk.wrap.mg.android.packing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class mergeApkAndSdkXml
{

	public static boolean mergeXmlFile(String apkXmlPath, String sdkXmlPath, String mergeString)
	{
		try
		{
			File apkXmlFile = new File(apkXmlPath);
			if (apkXmlFile.exists())
			{
				String apkXmlMergeline[] = readApkXmlLine(apkXmlPath, mergeString);
				removelineFromFile(sdkXmlPath, apkXmlMergeline);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;

	}

	// 返回标签中的name ，用于和sdk比对时，把一样的删掉。
	public static String returnkeyname(String keyString)
	{
		String keyName = null;
		Matcher matcher = Pattern.compile(
				// "<uses-sdk\\b[^>]*?\\bandroid:targetSdkVersion=\"([^\"]*)\""
				// "<dimen\\b[^>]*?\\bname=\"([^\"]*)\""
				"\\bname=\"([^\"]*)\"", Pattern.DOTALL).matcher(keyString);
		if (matcher.find())
		{
			keyName = matcher.group(1);
		}
		return keyName;
	}

	// 文件包含mergeString的每行都存到String【】中
	public static String[] readApkXmlLine(String apkXmlPath, String mergeString)
	{
		String[] lineString = new String[mergeStrLineSum(apkXmlPath, mergeString)];
		try
		{
			FileReader fr = new FileReader(apkXmlPath);
			BufferedReader br = new BufferedReader(fr);
			String currentLine;
			int lineNum = 0;
			while ((currentLine = br.readLine()) != null)
			{
				if (mergeString != null)
				{
					if (currentLine.contains(mergeString))
					{
						// System.out.println(currentLine);
						lineString[lineNum] = returnkeyname(currentLine);
						lineNum++;
					}
				}
				else
				{
					lineString[lineNum] = returnkeyname(currentLine);
					lineNum++;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return lineString;
	}

	// 读一下文件一共有多少行包含要merge的string
	public static int mergeStrLineSum(String filePath, String mergeString)
	{
		int lineNum = 0;
		try
		{
			FileReader fr = new FileReader(filePath);
			BufferedReader br = new BufferedReader(fr);
			String currentLine;
			while ((currentLine = br.readLine()) != null)
			{
				if (currentLine.contains(mergeString))
				{
					// System.out.println(currentLine);
					lineNum++;
				}
			}
		}
		catch (Exception e)
		{

		}
		return lineNum;
	}

	// 删除重复的行
	public static void removelineFromFile(String sdkXmlFilePath, String[] stringToRemoveLine)
	{
		for (int lineNum = 0; lineNum < stringToRemoveLine.length; lineNum++)
		{
			removeSingleLineFromFile(sdkXmlFilePath, stringToRemoveLine[lineNum]);
		}
	}

	// 删除重复的行
	public static void removeSingleLineFromFile(String sdkXmlFilePath, String stringToRemoveLine)
	{
		try
		{
			File inFile = new File(sdkXmlFilePath);
			if (!inFile.isFile())
			{
				System.out.println("Parameter is not an existing file");
				return;
			}

			File tempFile = new File(inFile.getAbsolutePath() + ".tmp");
			BufferedReader br = new BufferedReader(new FileReader(sdkXmlFilePath));
			PrintWriter pw = new PrintWriter(new FileWriter(tempFile));
			String line = null;

			while ((line = br.readLine()) != null)
			{
				if (!line.trim().contains("name=\"" + stringToRemoveLine + "\""))
				{
					pw.println(line);
					pw.flush();
				}
			}
			pw.close();
			br.close();

			// Delete the original file
			if (!inFile.delete())
			{
				System.out.println("Could not delete file");
				return;
			}

			// Rename the new file to the filename the original file had.
			if (!tempFile.renameTo(inFile))
				System.out.println("Could not rename file");

		}
		catch (FileNotFoundException ex)
		{
			ex.printStackTrace();
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
	}
}
