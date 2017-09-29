package com.zdk.wrap.mg;

import java.io.IOException;
import java.io.OutputStream;

public class PositionedOutputStream extends OutputStream {
  private int pos=0;
  private OutputStream out;
  
  public PositionedOutputStream(OutputStream out) {
    this.out=out;
  }
  
  @Override
  public void write(int b) throws IOException {
    out.write(b);
    pos++;
  }

  @Override
  public void write(byte[] buf) throws IOException {
    write(buf,0,buf.length);
  }
  
  @Override
  public void write(byte[] buf, int off, int len) throws IOException {
    out.write(buf,off,len);
    pos+=len;
  }
  
  public int getCurrentPosition() {
    return pos;
  }
}
