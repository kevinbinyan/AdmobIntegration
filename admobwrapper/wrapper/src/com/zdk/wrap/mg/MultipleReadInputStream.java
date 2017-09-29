package com.zdk.wrap.mg;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MultipleReadInputStream extends StandardInputStreamTemplate {
  private File fileSrc=null;
  private File fileTmp=null;
  private byte[] buffer=null;
  private InputStream currInStr=null;
  
  public MultipleReadInputStream(InputStream readOnceSource) throws IOException {
    try {
      fileTmp=File.createTempFile("multiread",".tmp");
      boolean success=false;
      try {
        FileOutputStream out=new FileOutputStream(fileTmp);
        try {
          ServerUtil.copyStream(readOnceSource,out,10240);
          success=true;
        } finally {
          out.close();
        }
      } finally {
        if (!success)
          fileTmp.delete();
      }
    } finally {
      readOnceSource.close();
    }
  }

  public MultipleReadInputStream(File file) {
    fileSrc=file;
  }

  public MultipleReadInputStream(byte[] buffer) throws IOException {
    this.buffer=buffer;
  }
  
  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    if (currInStr==null) {
      if (fileSrc!=null)
        currInStr=new FileInputStream(fileSrc);
      else if (fileTmp!=null)
        currInStr=new FileInputStream(fileTmp);
      else if (buffer!=null)
        currInStr=new ByteArrayInputStream(buffer);
      else
        throw new IOException("No input source available.");
    }
    return currInStr.read(b,off,len);
  }

  private void closeCurrent() throws IOException {
    if (currInStr!=null) {
      currInStr.close();
      currInStr=null;
    }
  }
  
  @Override
  public void close() throws IOException {
    closeCurrent();
    if (fileTmp!=null) {
      fileTmp.delete();
      fileTmp=null;
    }
    if (buffer!=null) // for clarity
      buffer=null; // release memory
    if (fileSrc!=null) // for clarity
      fileSrc=null; // provoke error if reused
  }

  public void resetToStart /* not reset */ () throws IOException {
    closeCurrent();
  }
}
