package com.zdk.wrap.mg;

import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class CryptoHelper {
  private static boolean securityProvidersAdded=false;

  public static synchronized void addSecurityProviders() {
    if (!securityProvidersAdded) {
      Security.addProvider(new BouncyCastleProvider());
      securityProvidersAdded=true;
    }
  }
}
