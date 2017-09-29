package com.zdk.wrap.mg;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WrapManifest {
  public interface Writer {
    void addManifestEntries(List<String> entries);
  }

  private StringBuffer header;
  private List<String> entries=new ArrayList<String>();
  
  public WrapManifest() {
    header=new StringBuffer();
    header.append("wrapping manifest\n");
    addHeader("manifest-version","2");
    addHeader("created-at",new SimpleDateFormat("yyyyMMdd-HHmmss-SSS Z").format(new Date()));  
  }
  
  public String build() {
    StringBuffer mainLines=new StringBuffer();
    for (String entry: entries)
      mainLines.append(/*platformPrefix+" "+*/ entry+"\n");
    return
      header.toString()+
      "end-of-header\n"+
      mainLines.toString()
      ;
  }
  
  public void addHeader(String key, String value) {
    header.append(key+": "+value+"\n");
  }
  
  public void add(WrapManifest.Writer manifestWriter) {
    manifestWriter.addManifestEntries(entries);
  }
  
}
