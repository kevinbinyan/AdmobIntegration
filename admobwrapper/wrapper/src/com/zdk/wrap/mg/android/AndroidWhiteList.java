package com.zdk.wrap.mg.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class AndroidWhiteList {

	private static WhiteList whitelist;

	public static void init(String packageName) {
		try {
			String whitelistPath = PathAndFileConfig.getWrapperWhiteList();
			InputStream is = new FileInputStream(new File(whitelistPath));
			Reader reader = new InputStreamReader(is);
			GsonBuilder builder = new GsonBuilder();
			Gson gson = builder.create();
			whitelist = gson.fromJson(reader, WhiteList.class);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static boolean isInWhiteList(String smali) {
		return isInWhiteList(null, smali);
	}

	public static boolean isInWhiteList(String packageName, String smali) {
		if (whitelist == null) {
			init(packageName);
		}
		if (isWhitePkgForSmali(whitelist.getGeneral().getPackages(), smali)) {
			return true;
		}

		APKConfig apk = findApk(packageName);
		if (apk == null) {
			return false;
		}

		for (String clazz : apk.getExclude_classes()) {
			int last = smali.lastIndexOf(".");
			String smaliName = smali.substring(0, last);
			String className = smaliName.replace(File.separator, "/");
			int dollarIndex = className.indexOf('$');
			if (dollarIndex != -1)
				className = className.substring(0, dollarIndex);
			if (className.equals(clazz)) {
				return false;
			}
		}

		for (String keyWord : apk.getKeywords()) {
			if (smali.contains(keyWord)) {
				return true;
			}
		}

		if (isWhitePkgForSmali(apk.getPackages(), smali)) {
			return true;
		}

		for (String clazz : apk.getClasses()) {
			int last = smali.lastIndexOf(".");
			String smaliName = smali.substring(0, last);
			String className = smaliName.replace(File.separator, "/");
			int dollarIndex = className.indexOf('$');
			if (dollarIndex != -1)
				className = className.substring(0, dollarIndex);
			if (className.equals(clazz)) {
				return true;
			}
		}

		return false;
	}

	private static APKConfig findApk(String packageName) {
		for (APKConfig apk : whitelist.getApks()) {
			if (apk.equals(packageName != null ? packageName : ManifestModifier.appInfo.getPackageName())) {
				return apk;
			}
		}
		return null;
	}

	private static boolean isWhitePkgForSmali(List<String> pkgs, String smali) {
		for (String pkg : pkgs) {
			int last = smali.lastIndexOf(".");
			String smaliName = smali.substring(0, last);
			String classNameTmp = smaliName.replace(File.separator, "/");
			if (classNameTmp.replace("/", ".").startsWith(pkg)) {
				return true;
			}
		}
		return false;
	}

	// className 为完整路径如android/support/v7/internal/view/menu/MenuBuilder
	public static boolean isClassInWhiteList(String packageName, String className) {

		if (whitelist == null) {
			init(packageName);
		}

		if (isWhitePkgForClass(whitelist.getGeneral().getPackages(), className)) {
			return true;
		}

		APKConfig apk = findApk(packageName);
		if (apk == null) {
			return false;
		}

		for (String clazz : apk.getExclude_classes()) {
			String classNameTmp = className.replace(File.separator, "/");
			int dollarIndex = classNameTmp.indexOf('$');
			if (dollarIndex != -1)
				classNameTmp = classNameTmp.substring(0, dollarIndex);
			if (classNameTmp.equals(clazz)) {
				return false;
			}
		}

		for (String keyWord : apk.getKeywords()) {
			if (className.contains(keyWord)) {
				return true;
			}
		}

		if (isWhitePkgForClass(apk.getPackages(), className)) {
			return true;
		}

		for (String clazz : apk.getClasses()) {

			String classNameTmp = className.replace(File.separator, "/");
			int dollarIndex = className.indexOf('$');
			if (dollarIndex != -1)
				classNameTmp = classNameTmp.substring(0, dollarIndex);
			if (classNameTmp.equals(clazz)) {
				return true;
			}
		}

		return false;
	}

	private static boolean isWhitePkgForClass(List<String> packages, String className) {
		for (String pkg : packages) {
			String classNameTmp = className.replace(File.separator, "/");
			if (classNameTmp.replace("/", ".").startsWith(pkg)) {
				return true;
			}
		}
		return false;
	}

	static class WhiteList {

		private GeneralConfig general;
		private APKConfig[] apks;

		public GeneralConfig getGeneral() {
			return general;
		}

		public void setGeneral(GeneralConfig general) {
			this.general = general;
		}

		public APKConfig[] getApks() {
			return apks;
		}

		public void setApks(APKConfig[] apks) {
			this.apks = apks;
		}

	}

	static class GeneralConfig {
		// 包路径过滤
		private List<String> packages;

		public List<String> getPackages() {
			return packages;
		}

		public void setPackages(List<String> packages) {
			this.packages = packages;
		}

	}

	static class APKConfig {
		// 应用名称
		private String app_name;
		// 应用包名
		private String package_name;
		// apk包的md5校验（区分版本）
		private String apk_MD5;
		// 模糊关键字过滤
		private List<String> keywords;
		// 包路径过滤
		private List<String> packages;
		// 指令类过滤
		private List<String> classes;
		// 排除类
		private List<String> exclude_classes;

		public String getApp_name() {
			return app_name;
		}

		public void setApp_name(String app_name) {
			this.app_name = app_name;
		}

		public String getPackage_name() {
			return package_name;
		}

		public void setPackage_name(String package_name) {
			this.package_name = package_name;
		}

		public String getApk_MD5() {
			return apk_MD5;
		}

		public void setApk_MD5(String apk_MD5) {
			this.apk_MD5 = apk_MD5;
		}

		public List<String> getKeywords() {
			return keywords;
		}

		public void setKeywords(List<String> keywords) {
			this.keywords = keywords;
		}

		public List<String> getPackages() {
			return packages;
		}

		public void setPackages(List<String> packages) {
			this.packages = packages;
		}

		public List<String> getClasses() {
			return classes;
		}

		public void setClasses(List<String> classes) {
			this.classes = classes;
		}

		@Override
		public boolean equals(Object obj) {
			return package_name != null ? package_name.equals((String) obj) : false;
		}

		public List<String> getExclude_classes() {
			return exclude_classes;
		}

		public void setExclude_classes(List<String> exclude_classes) {
			this.exclude_classes = exclude_classes;
		}

	}
}
