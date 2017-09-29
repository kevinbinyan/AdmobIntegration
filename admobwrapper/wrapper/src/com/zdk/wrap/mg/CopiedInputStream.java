package com.zdk.wrap.mg;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class CopiedInputStream extends StandardInputStreamTemplate {
  private InputStream src;
  private List<OutputStream> copyToStreams=new LinkedList<OutputStream>();

  public CopiedInputStream(InputStream src) {
    this.src=src;    
  }
  
  //public CopiedInputStream(InputStream src, OutputStream copyToStr) { // convenience method
  //  this(src);
  //  addCopier(copyToStr);
  //}
  
  public void addCopier(OutputStream copyToStr) {
    copyToStreams.add(copyToStr);
  }

  public void removeCopier(OutputStream copyToStr) {
    for (Iterator<OutputStream> iter=copyToStreams.iterator();iter.hasNext();) {
      OutputStream elem=iter.next();
      if (elem==copyToStr) {
        iter.remove();
        break;
      }
    }
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    int got=src.read(b,off,len);
    if (got>0) {
      for (OutputStream copyToStr: copyToStreams) {
        copyToStr.write(b,off,len);
      }
    }
    return got;
  }
  
  @Override
  public void close() throws IOException {
    // no: src.close();
    //super.close();
  }
}
