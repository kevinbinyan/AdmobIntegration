
package com.zdk.wrap.mg.android.packing;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jf.smali.Smali;
import org.jf.smali.SmaliOptions;
import org.jf.smali.smaliParser.implements_spec_return;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import com.sun.corba.se.spi.orbutil.fsm.Action;
import com.zdk.wrap.mg.AppModifier.ProcessingException;
import com.zdk.wrap.mg.ServerUtil;
import com.zdk.wrap.mg.ServerUtil.FileAction;
import com.zdk.wrap.mg.ServerUtil.RunExecResult;
import com.zdk.wrap.mg.android.Util;
import com.zdk.wrap.mg.android.AppModifier;
import com.zdk.wrap.mg.android.PathAndFileConfig;
import com.zdk.wrap.mg.android.ProguardHelper;

public class Multidexer
{
	private static final int Batch_Size = 1000;
	static String  backUpSmaliFilePath, outputPath;
	static List<String> _listSmaliToDex = new ArrayList<String>();
	static final String dirOfBackUpSmali = "backUpSmaliPath";

	public static boolean execute(String multidexerPath) throws IOException
	{
		File outputDir = null;
		try
		{
			outputDir = ServerUtil.createTempDir("multidexIntermediate");
		} 
		catch (IOException e)
		{
			e.printStackTrace();
//			System.exit(0);
			return false;
		}
		outputPath = outputDir.getPath();

		// rename dir , smali to backUpSmaliPath
		File backUpSmaliFile = new File(multidexerPath + "\\smali");
		backUpSmaliFile.renameTo(new File(backUpSmaliFile.getParent() + "/" + dirOfBackUpSmali));
		backUpSmaliFilePath = multidexerPath + "\\" + dirOfBackUpSmali;
		try
		{
			//step 1-9 are the main jobs of this javaclass . it receive a path which have a dir of smali . then after conversion there is one or more smali dir in this path.
			// 1.samli to dex
			smaliToDex(backUpSmaliFilePath, outputPath);
			// 2.dex to jar
			dexToJar(outputPath);
			// 3.delete temp dex file
			deleteFileByFileSuffix(outputPath, ".dex", false);
			//3.5 shrink jars
//			shrinkJar(outputPath);
			// 4.jar to dex
			jarToDex(outputPath, outputPath);
			// 5.delete temp jar file
			deleteFileByFileSuffix(outputPath, ".jar", false);
			// 6.dex to smali
			dexToSmali(outputPath, outputPath);
			//6.5 move smali by multidexed smali
			SortSmali.moveSmali( multidexerPath + File.separator + dirOfBackUpSmali,multidexerPath, outputPath);
			// 7.rename the smali_classes file
			renameSingleFile(multidexerPath, "smali_classes", "smali");
			// 8.delete temp dex file
			deleteFileByFileSuffix(outputPath, ".dex", false);
			// 9.delete multidexerPath backUpSmaliFilePath
			deleteFileByFileSuffix(backUpSmaliFilePath, "", true);
		} 
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}

	static public void shrinkJar(String jarsParh) throws IOException{
		ArrayList<String> jarList = new ArrayList<>();
		//将每个jar加入到jarlist中，再存在jarArray中
		ServerUtil.forEachFile(jarsParh, new FileAction()
		{
			
			@Override
			public void action(File file) throws IOException
			{
				jarList.add(file.getPath());
			}
		});
		String[] jarArray = new String[jarList.size()];
		for(int i = 0 ; i<jarList.size() ; i++){
			jarArray[i] = jarList.get(i);
		}
		//开始shrink
		ProguardHelper.startShrink(jarArray, jarsParh + "\\shrinked.jar" );
		//删除之前的jars
		for(int i = 0 ; i<jarList.size() ; i++){
			new File(jarList.get(i)).delete();
		}
	}
	
	//todo:delete file by file suffix
	static public void deleteFileByFileSuffix(String Path, String Suffix, boolean deleteFileSelf)
	{
		FilenameFilter fileNameFilter = new FilenameFilter()
		{
			@Override
			public boolean accept(File dir, String name)
			{
				return name.endsWith(Suffix);
			}
		};
		ServerUtil.removeRecursively(Path, deleteFileSelf, fileNameFilter);
	}

	//todo:every 100 smali file become to dex file
	static public void smaliToDex(String smaliPath, String outputPath)
	{
		SmaliOptions options = new SmaliOptions();
		try
		{
			ServerUtil.forEachFile(smaliPath, new FileAction()
			{
				@Override
				public void action(File file) throws IOException
				{
					if (file.getName().endsWith(".smali"))
					{
						_listSmaliToDex.add(file.getPath());
						
					}
				}
			});
		} 
		catch (IOException e1)
		{
			e1.printStackTrace();
		}

		for (int i = 0; i <= _listSmaliToDex.size(); i = i + Batch_Size)
		{
			options.outputDexFile = outputPath + "\\" + i + ".dex";
			try
			{
				if (_listSmaliToDex.size() - i <= Batch_Size)
				{
					Smali.assemble(options, _listSmaliToDex.subList(i, _listSmaliToDex.size()));
				} 
				else
				{
					Smali.assemble(options, _listSmaliToDex.subList(i, i + Batch_Size));
				}
			} 
			catch (Exception e)
			{
			}
		}
	}

	// todo: return the path of output file, and also can specify the output
	static public void dexToJar(String pathOfDexFile) throws Exception
	{
		ServerUtil.forEachFile(pathOfDexFile, new FileAction()
		{
			@Override
			public void action(File file) throws IOException
			{
				ArrayList<String> dex2JarArgs = new ArrayList<String>();
				try
				{
					dex2JarArgs.add(PathAndFileConfig.Dex2jarLocation);
					dex2JarArgs.add(file.getPath());
					final RunExecResult dxOutputResult = ServerUtil.runExecutable(dex2JarArgs, true);
					final String dxOutput = dxOutputResult.allOutput();
					if (dxOutputResult.getExitStatus() != 0 || dxOutput.indexOf("trouble processing:") >= 0)
						throw new ProcessingException("jar2Dex failed: " + dxOutput);
				} 
				catch (ProcessingException e)
				{
					e.printStackTrace();
				}

			}
		});
	}
	
	//todo:make jar file to dex file form pathOfJarFile to outputPath by dx.bat
	static public void jarToDex(String pathOfJarFile, String outputPath) throws Exception
	{
		System.out.println(AppModifier.getDxPath() );
		ArrayList<String> jar2DexArgs = new ArrayList<String>();
		jar2DexArgs.add(AppModifier.getDxPath());
		jar2DexArgs.add("-JXmx8192m");//大文件内存会溢出，所以加大
		jar2DexArgs.add("--dex");
		jar2DexArgs.add("--multi-dex");
//		jar2DexArgs.add("--set-max-idx-number=10000");
		jar2DexArgs.add("--output=");
		jar2DexArgs.add(outputPath);
		jar2DexArgs.add(pathOfJarFile);
		final RunExecResult dxOutputResult = ServerUtil.runExecutable(jar2DexArgs, true);
		final String dxOutput = dxOutputResult.allOutput();
		if (dxOutputResult.getExitStatus() != 0 || dxOutput.indexOf("trouble processing:") >= 0)
			throw new ProcessingException("jar2Dex failed: " + dxOutput);
	}

	//todo:every dex file become to smali dir  
	static public void dexToSmali(String dexPath, String outputPath)
	{
		try
		{
			ServerUtil.forEachFile(dexPath, new FileAction()
			{
				@Override
				public void action(File file) throws IOException
				{
					String dexFile = file.getPath();
					String outputPathFile = outputPath + "\\smali_" + file.getName().substring(0, 8);
					Util.dex2Smali(dexFile, outputPathFile);
				}
			});
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	//todo:input file path then rename the file from oldname to newname 
	public static void renameSingleFile(String filePath, String oldName, String newName) {
		File file = new File(filePath+oldName);
		file.renameTo(new File(file.getParent() + "/" + newName));
	}
}
