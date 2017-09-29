package com.zdk.wrap.mg;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

public class CombinedInputStream extends StandardInputStreamTemplate {
  private List<InputStream> streams;
  
  public CombinedInputStream() {
    streams=new LinkedList<InputStream>();
  }
  
  public CombinedInputStream(InputStream str1, InputStream str2) { // convenience
    this();
    add(str1);
    add(str2);
  }
  
  public void add(InputStream str) {
    streams.add(str);
  }
  
  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    int got;
    if (streams.isEmpty()) {
      got=-1;
    } else {
      InputStream str=streams.get(0);
      got=str.read(b,off,len);
      if (got<=0) {
        str.close();
        streams.remove(0);
        got=read(b,off,len);
      }
    }
    return got;
  }
  
  @Override
  public void close() throws IOException {
    //super.close();
    for (InputStream str: streams)
      str.close();
  }
}
