package com.zdk.wrap.mg.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.zdk.wrap.mg.ServerUtil;

public class ProguardHelper {

	private static String proguardJarLocation = "";
	private static String proguardRuleLocation = ".\\proguardTemplate.txt";
	private static String proguardRuleTempLocation = "";
//	private static String[] inputJars = { "C:\\workspace\\DemoWrapLayout\\lib\\android-support-v4.jar",
//			"C:\\workspace\\DemoWrapLayout\\lib\\google-play-services.jar",
//			"C:\\workspace\\DemoWrapLayout\\lib\\mg.jar", "C:\\workspace\\DemoWrapLayout\\lib\\wrapper.mg.jar" };
//	private static String outputJar = "result.jar";

//	public static void main(String[] args) throws IOException {
//		startShrink(inputJars, outputJar);
//	}
//	
	public static void startShrink(String[] inputJars, String outputJar) throws IOException {
		proguardJarLocation = getSysJar();
		proguardRuleTempLocation = makeProguardFile(inputJars, outputJar);

		List<String> allArgs = new LinkedList<String>();
		allArgs.add("java");
		allArgs.add("-Xms256m");
		allArgs.add("-Xmx512m");
		allArgs.add("-jar");
		allArgs.add(proguardJarLocation);
		allArgs.add("@" + proguardRuleTempLocation);
		final String jarOutput = ServerUtil.runExecutable(null, ServerUtil.list2Strings(allArgs), null, null, true)
				.allOutput();
		ServerUtil.debugLog(jarOutput);
		if((new File(outputJar)).exists()){
			ServerUtil.debugLog("Debug: shrink success");
		}
		// debugLog(joinArgs(allArgs) + ":\n" + apktoolOutput);
	}

	private static String getSysJar() {
		String location = "";
		try {
			Map<String, String> map = System.getenv();
			Iterator<Entry<String, String>> it = map.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, String> entry = (Entry) it.next();
				if (entry.getKey().equals("ANDROID_HOME")) {
					location = entry.getValue() + "\\tools\\proguard\\lib\\proguard.jar";
				}
			}

		} catch (Exception e) {
			return null;
		}
		return location;
	}

	public static String makeProguardFile(String[] inputJars, String outputJar) throws IOException {
		// create temp folder
		File proguardDir = ServerUtil.createTempDir("proguard");
		//outputJar = proguardDir + File.separator + outputJar;
		// save proguard.txt to temp
		File proguardRecent = new File(proguardDir + File.separator + "proguard.txt");
		proguardRecent.createNewFile();
		writeToFile(inputJars, outputJar, proguardRecent);
		return proguardRecent.getAbsolutePath();
	}

	private static boolean writeToFile(String[] inputJars, String outputJar, File newProguard) {
		try {
			InputStreamReader read = new InputStreamReader(new FileInputStream(new File(proguardRuleLocation)),
					"UTF-8");// 考虑到编码格式
			BufferedReader bufferedReader = new BufferedReader(read);
			String lineTxt = null;
			String resultTxt = "";

			for (String in : inputJars) {
				resultTxt += "-injars " + in + "\n";
			}
			resultTxt += "-outjars " + outputJar + "\n";

			while ((lineTxt = bufferedReader.readLine()) != null) {
				resultTxt += lineTxt + "\n";
			}
			writeTxtFile(resultTxt, newProguard);
			read.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

	}

	public static boolean writeTxtFile(String content, File fileName) throws Exception {
		RandomAccessFile mm = null;
		boolean flag = false;
		FileOutputStream o = null;
		try {
			o = new FileOutputStream(fileName);
			o.write(content.getBytes("UTF-8"));
			o.close();
			flag = true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (mm != null) {
				mm.close();
			}
		}
		return flag;
	}
}
