package com.zdk.wrap.mg.test.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zdk.wrap.mg.ServerUtil;
import com.zdk.wrap.mg.ServerUtil.RunExecResult;

public class AccessKeyPaster {
  private static final String stdGDAppPassword="qagood";
  private static final String keyCodeEnter="66"; // KEYCODE_ENTER
  private String adbPath;

  public AccessKeyPaster(String adbPath) {
    this.adbPath=adbPath;
  }

  public AccessKeyPaster() throws IOException {
    RunExecResult result=ServerUtil.runExecutable(new String[] {
      "bash","-l","-c","which adb"
    },true);
    adbPath=result.getStdOut().replaceFirst("[\\r\\n]+$","");
  }
  
  public void enterPasswordForAccess() throws IOException {
    ServerUtil.runExecutable(new String[] {
      adbPath,"shell","input","text",stdGDAppPassword
    },true);
    pressEnter();
  }

  public void enterPasswordForDefinition() throws IOException {
    for (int i=0;i<2;i++) {
      enterPasswordForAccess();
    }
  }

  public void skipInitialScreen() throws IOException {
    // Press "Set up":
    pressEnter();
  }
  
  public void enterUsername(String gdUsername) throws IOException {
    ServerUtil.runExecutable(new String[] {
      adbPath,"shell","input","text",gdUsername
    },true);
  }
  
  public void enterAccessKey(String[] splitGDAccessKey) throws IOException {
    for (int i=0;i<splitGDAccessKey.length;i++) {
      ServerUtil.runExecutable(new String[] {
        adbPath,"shell","input","text",splitGDAccessKey[i]
      },true);
    }
  }
  
  public void pressEnter() throws IOException {
    ServerUtil.runExecutable(new String[] {
      adbPath,"shell","input","keyevent",keyCodeEnter
    },true);
  }
  
  public void enterUsernameAndAccessKey() throws IOException {
    System.out.println("Paste access key email portion here that contains both \"EMAIL ADDRESS\" and \"ACCESS KEY\":");
    String gdUsername=null; // "hassenmacher@gd.qagood.com";
    String[] splitGDAccessKey;
    BufferedReader reader=new BufferedReader(new InputStreamReader(System.in));
    for (;;) {
      String line=reader.readLine();
      Matcher emailMatcher=Pattern.compile("^\\s*\\bEMAIL\\s*ADDRESS\\s*:\\s*([^\\s]+)\\s*$",Pattern.CASE_INSENSITIVE).matcher(line);
      if (emailMatcher.matches()) {
        gdUsername=emailMatcher.group(1);
      }
      else if (gdUsername!=null) {
        Matcher accessKeyMatcher=Pattern.compile("^\\s*\\bACCESS\\s*KEY\\s*:\\s*([a-z0-9]{5})-([a-z0-9]{5})-([a-z0-9]{5})\\s*$",Pattern.CASE_INSENSITIVE).matcher(line);
        if (accessKeyMatcher.matches()) {
          splitGDAccessKey=new String[] {
            accessKeyMatcher.group(1),accessKeyMatcher.group(2),accessKeyMatcher.group(3)
          };
          break;
        }
      }
    }
    
    /*
    try {
      Thread.sleep(2000); // wait for password enter screen
    } catch (InterruptedException e) {
    }
    */
    enterUsername(gdUsername);
    pressEnter();
    enterAccessKey(splitGDAccessKey);
    pressEnter();
  }
  
  public static void main(String[] args) {
    try {
      AccessKeyPaster paster=new AccessKeyPaster();
      paster.enterUsernameAndAccessKey();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
