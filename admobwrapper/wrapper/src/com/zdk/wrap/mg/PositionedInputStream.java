package com.zdk.wrap.mg;

import java.io.IOException;
import java.io.InputStream;

public class PositionedInputStream extends StandardInputStreamTemplate {
  private InputStream src;
  private int pos;
  private Object userData=null;
  private int readAhead=-1;
  private boolean detached=false;
  private boolean readAheadWasInUse=false;

  public PositionedInputStream(InputStream src) {
    this.src=src;
    pos=0;
  }
  
  @Override
  public int read(byte[] buf, int off, int len) throws IOException {
    int got=0;
    boolean isEOF=false;
    if (readAhead>=0) {
      buf[off]=(byte)readAhead;
      readAhead=-1;
      got++;
    }
    if (len>got) {
      int result=src.read(buf,off+got,len-got);
      if (result>0)
        got+=result;
      else if (result<0)
        isEOF=true;
    }
    pos+=got; // i.e. include readAhead
    return isEOF? -1:got;
  }
  
  /*
  @Override
  public int read() throws IOException {
    int b;
    if (readAhead>=0) {
      b=(byte)readAhead;
      readAhead=-1;
    } else {
      b=super.read();
    }
    if (b>0)
      pos++;
    return b;
  }
  */
  
  @Override
  public long skip(long numBytes) throws IOException {
    if (numBytes>0) {
      /* no correction for readAhead because readAhead doesn't affect the position:
      if (readAhead>=0) {
        readAhead=-1;
        numBytes--;
      }
      */
      return super.skip(numBytes);
    }
    else if (numBytes<0)
      throw new IOException("Trying to use a negative skip count.");
    else {
      return 0;
    }
  }
  
  public int getCurrentPosition() {
    //if (readAhead>=0)
    //  pos--;
    return pos;
  }

  public byte mustRead() throws IOException { // convenience
    int c=read();
    if (c<0)
      throw new IOException("Premature end of file.");
    return (byte)c;
  }
  
  public int readAhead() throws IOException {
    readAheadWasInUse=true;
    if (readAhead>=0) {
      //return readAhead; // should not happen
      throw new IOException("Cannot readAhead() multiple times in a row.");
    } else {
      int b=super.read();
      if (b>=0) {
        pos--; // compensate the change in pos by read()
        readAhead=b;
      }
      return b;
    }
  }

  @Override
  public void close() throws IOException {
    src.close();
    //super.close();
    checkOnClose();
  }
  
  private void checkOnClose() throws IOException {
    if (readAheadWasInUse && !detached)
      throw new IOException("Using readAhead() during the lifetime of the PositionedInputStream but never detaching it from its underlying InputStream.");
  }

  public void detach() throws IOException {
    if (readAhead>=0)
      throw new IOException("Cannot detach PositionedInputStream from underlying stream if a read-ahead is still pending.");
    detached=true;
  }
  
  protected void finalize() throws Throwable {
    checkOnClose();
  }
  
  public void setUserData(Object userData) {
    this.userData=userData;
  }
  
  public Object getUserData() throws IOException {
    if (userData==null)
      throw new IOException("Requesting user data that has not yet been set.");
    return userData;
  }
}
