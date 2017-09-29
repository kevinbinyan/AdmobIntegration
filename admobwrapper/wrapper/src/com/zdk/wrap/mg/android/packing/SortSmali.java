package com.zdk.wrap.mg.android.packing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.zdk.wrap.mg.ServerUtil;
import com.zdk.wrap.mg.ServerUtil.FileAction;

public class SortSmali
{

	public static String pathKeyName = "smali_classes";

//	public static void main(String[] args) throws IOException
//	{
//		String path = "C:\\Users\\huq\\AppData\\Local\\Temp\\apkextr3112205970036062365.tmp";
//		getList(path);
//		String backUpSmaliPath = "D:\\dele\\apkPath\\backUpSmaliPath";
//		String apkPath = "D:\\dele\\apkPath";
//		String multiSmaliPath = "D:\\dele\\multiPath";
//
//		moveSmali(backUpSmaliPath, apkPath, multiSmaliPath);
//	}

	public static void moveSmali(String fromPath, String toPath, String multidexedPath) throws IOException
	{
		ArrayList<KeyValue> list = getList(multidexedPath);
		Map<String, String> mapToFindFile = new HashMap<String, String>();  
		for (int i = 0; i < list.size(); i++)
		{
			mapToFindFile.put(list.get(i).value,list.get(i).key);
		}

		ServerUtil.forEachFile(fromPath, new FileAction()
		{
			@Override
			public void action(File file) throws IOException
			{
				if(mapToFindFile.containsKey(subStr(file.getPath(), fromPath)))
				{
					moveToLastDir(mapToFindFile.get(subStr(file.getPath(), fromPath)), file, toPath ,fromPath);
				}
//				for (int i = 0; i < list.size(); i++)
//				{
//					if(subStr(file.getPath(), fromPath).equals(list.get(i).value))
//					{
//						moveToLastDir(list.get(i).key, file, toPath ,fromPath);
//					}
//				}
			}
		});

	}

	public static class KeyValue
	{

		String key;

		String value;

		public void setKey(String key)
		{
			this.key = key;
		}

		public void setValue(String value)
		{
			this.value = value;
		}

		public String getKey()
		{
			return this.key;
		}

		public String getValue()
		{
			return this.value;
		}

		public KeyValue(String key, String value)
		{
			this.key = key;
			this.value = value;
		}
	}

	public static void moveToLastDir(String key, File file, String DirPath ,String fromPath) throws IOException
	{
		File moveToFile = new File(DirPath + File.separator+ pathKeyName + key + File.separator + getPathVlaue(file, fromPath));
		if(!moveToFile.exists())
		{
			ServerUtil.createMissingDirsFor(moveToFile);
		}
		InputStream in = new FileInputStream(file);
		OutputStream out = new FileOutputStream(moveToFile);

		byte[] buffer = new byte[1024];

		int length;

		while ((length = in.read(buffer)) > 0)
		{
			out.write(buffer, 0, length);
		}
		in.close();
		out.close();
	}

	public static String getPathVlaue(File file , String fromPath)
	{
		return subStr(file.getPath(), fromPath);
	}
	
	public static String subStr(String str1, String str2)
	{
		return str1.replace(str2, "");
	}

	public static String getmultiPathValue(String path, String key)
	{
		if (key.length() != 0)
		{
			return path.substring(path.indexOf(pathKeyName) + pathKeyName.length() + key.length());
		}
		else
		{
			return path.substring(path.indexOf(pathKeyName) + pathKeyName.length() + key.length());
		}

	}

	public static String getPathKey(String path)
	{
		String strOfAfter = path.substring(path.indexOf(pathKeyName) + pathKeyName.length());
		String key = strOfAfter.substring(0, strOfAfter.indexOf("\\"));
		return strOfAfter.substring(0, strOfAfter.indexOf("\\"));
	}

	public static ArrayList<KeyValue> getList(String DirPath) throws IOException
	{

		ArrayList<KeyValue> list = new ArrayList<>();

		File[] files = new File(DirPath).listFiles();
		for (File oneFile : files)
		{
			if (oneFile.getName().contains(pathKeyName) && oneFile.isDirectory())
			{
				ServerUtil.forEachFile(oneFile, new FileAction()
				{

					@Override
					public void action(File file) throws IOException
					{
						String key = getPathKey(file.getPath());
						String value = getmultiPathValue(file.getPath(), key);
						KeyValue kv = new KeyValue(key, value);
						list.add(kv);
						 if(value.contains("gK") )
						 {
							 System.out.println("key = " + key + " :: " + "value =" + value);
						 }
					}
				});
			}
		}
		return list;
	}

}
