package com.zdk.wrap.mg;

import java.io.IOException;

public interface PackagedPrivateKey {
  byte[] getEncodedData() throws IOException;
  String getOptPassword();
  String getOptIdentifier(); // not: Thumbprint, that's not generic but a special case only for iOS keychain, i.e. in that case the SHA-1 digest may be used as an identifier, other platforms might not use digests at all, plus: it's optional
}
