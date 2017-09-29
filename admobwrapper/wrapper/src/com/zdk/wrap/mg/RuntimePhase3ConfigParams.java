package com.zdk.wrap.mg;

public class RuntimePhase3ConfigParams {
  public static final boolean injectGDAPI=true; // even if GD is used it might be done only for the initial UI takeover, this parameter indicates to also inject GD API functions
  public static final String androidCfgFileBasename="mgwrap.cfg"; //$NON-NLS-1$
  public static final boolean permitExternalStorageAccess=true; // SDCard and such
}
