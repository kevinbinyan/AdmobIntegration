package com.zdk.wrap.mg.android;

public class AppInfo {
	private String packageName;
	private String optVersionName;
	private String optVersionCode;
	private String optMainActivityName;
	private String optTargetSdkVersion;
	private String optMinSdkVersion;
	private String iconPrefix;
	private String caption;
	// later maybe: application android:label (for app name)

	public AppInfo(String packageName, String optVersionName, String optVersionCode, String optMainActivityName,
			String optTargetSdkVersion, String optMinSdkVersion, String iconPrefix, String caption) {
		this.packageName = packageName;
		this.optVersionName = optVersionName;
		this.optVersionCode = optVersionCode;
		this.optMainActivityName = optMainActivityName;
		this.optTargetSdkVersion = optTargetSdkVersion;
		this.optMinSdkVersion = optMinSdkVersion;
		this.iconPrefix = iconPrefix;
		this.caption = caption;
	}

	public String getPackageName() {
		return packageName;
	}

	public String getOptVersionName() {
		return optVersionName;
	}

	public String getOptVersionCode() {
		return optVersionCode;
	}

	public String getOptMainActivityName() {
		return optMainActivityName;
	}

	public String getOptTargetSdkVersion() {
		return optTargetSdkVersion;
	}

	public String getOptMinSdkVersion() {
		return optMinSdkVersion;
	}

	public String getIconPrefix() {
		return iconPrefix;
	}

	public String getCaption() {
		return caption;
	}
}
