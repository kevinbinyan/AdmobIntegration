package com.zdk.wrap.mg;

import java.io.IOException;
import java.io.InputStream;

public abstract class StandardInputStreamTemplate extends InputStream {
  
  @Override
  public final int read() throws IOException {
    byte[] buf=new byte[1];
    final int got=read(buf);
    if (got>0) {
      int b=buf[0];
      if (b<0) b+=256;
      return b;
    } else {
      return got;
    }
  }

  @Override
  public final int read(byte[] b) throws IOException {
    return read(b,0,b.length);
  }
  
  @Override
  public abstract int read(byte[] b, int off, int len) throws IOException;
  
  @Override
  public /*final*/ long skip(long numBytes) throws IOException {
    byte[] buf=new byte[(int)Math.min(20000,numBytes)];
    for (long remaining=numBytes;remaining>0;) {
      int willRead=(int)Math.min(buf.length,remaining);
      int got=read(buf,0,willRead);
      if (got<=0)
        throw new IOException("Premature end of input source stream.");
      //copyToStr.write(buf,0,willRead);
      remaining-=willRead;
    }
    long actuallySkipped=numBytes;
    return actuallySkipped;
  }
  
  @Override
  public abstract void close() throws IOException;
}
