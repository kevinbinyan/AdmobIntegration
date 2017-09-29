package com.zdk.wrap.mg;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
/**
 * Miscellaneous utilities
 *
 */
public class ServerUtil {
  
  public static String quoteForXML(String text) {
    StringBuffer buf=new StringBuffer();
    int length=text.length();
    for (int i=0;i<length;i++) {
      final char c=text.charAt(i);
      switch (c) {
        case '<':
          buf.append("&lt;");
          break;
        case '>':
          buf.append("&gt;");
          break;
        case '&':
          buf.append("&amp;");
          break;
        default:
          buf.append(c);
          break;          
      }
    }
    return buf.toString();
  }

  public static class RunExecResult {
    private String stdOut;
    private String stdErr;
    private int exitStatus;
    private String[] args;

    private RunExecResult(String[] args, int exitStatus, String stdOut, String stdErr) {
      this.args=args;
      this.exitStatus=exitStatus;
      this.stdOut=stdOut;
      this.stdErr=stdErr;
    }

    public int getExitStatus() {
      return exitStatus;
    }

    public String getStdOut() {
      return stdOut;
    }

    public String getStdErr() {
      return stdErr;
    }
    
    public String allOutput() {
      StringBuffer argBuf=new StringBuffer();
      for (int i=0;i<args.length;i++) {
        argBuf.append(args[i]);
        if (i+1<args.length)
          argBuf.append(' ');
      }
      return argBuf.toString()+"\n"+stdOut+(!stdOut.isEmpty() && !stdErr.isEmpty()? "\n":"")+stdErr;
    }
  }
  public static String getRealFilePath(String relaPath) {
	  File aFile = new File(relaPath);
	  return aFile.getAbsolutePath();
	
}
  public static RunExecResult runExecutable(File optWorkingDirFile, final String[] args, String optAddToPath, Map<String,String> optAddToEnv, boolean failOnNonzeroExitStatus) throws IOException {
    try {
      ProcessBuilder processBuilder=new ProcessBuilder(args);
      processBuilder.directory(optWorkingDirFile); // null = current Java process working dir
      
      if (optAddToPath!=null || optAddToEnv!=null) {
        Map<String, String> env=processBuilder.environment();
        if (optAddToPath!=null) {
          String path=env.get("PATH");
          if (path!=null)
            path+=File.pathSeparator;
          else
            path="";
          path+=optAddToPath;
          env.put("PATH",path);
        }
        if (optAddToEnv!=null) {
          env.putAll(optAddToEnv);
        }
      }

      final Process process=processBuilder.start();

      final ByteArrayOutputStream stdOut=new ByteArrayOutputStream();
      final ByteArrayOutputStream stdErr=new ByteArrayOutputStream();

      //System.out.println(args[0]+" stdout: start");
      final Thread stdOutCopier=new Thread() {
        public void run() {
          try {
            copyStream(process.getInputStream(),stdOut);
            /*InputStream inStr=process.getInputStream();
            byte[] buf=new byte[1024];
            for (;;) {
              final int got=inStr.read(buf);
              if (got<0) break;
              else if (got>0) {
                stdOut.write(buf,0,got);
                System.out.println("Read: "+new String(buf,0,got,"US-ASCII"));
              }
            }*/
          } catch (IOException e) {
          }
          //System.out.println(args[0]+" stdout: end");
        }
      };
      stdOutCopier.start();
      final Thread stdErrCopier=new Thread() {
        public void run() {
          try {
            copyStream(process.getErrorStream(),stdErr);
          } catch (IOException e) {
          }
        }
      };
      stdErrCopier.start();
      final int executableRC=process.waitFor();
      //System.out.println("exitStatus="+executableRC);
      stdOutCopier.join();
      stdErrCopier.join();
      RunExecResult result=new RunExecResult(args,executableRC,stdOut.toString("US-ASCII"),stdErr.toString("US-ASCII"));
      if (failOnNonzeroExitStatus && executableRC!=0) {
        StringBuffer argBuf=new StringBuffer();
        for (String arg: args)
          argBuf.append(" "+arg);
        throw new IOException(removePrivateDataFromText("Executable failed, ("+argBuf.toString()+") yields: "+result.getStdOut()+"\n"+result.getStdErr()));
      }
      return result;
    } catch (UnsupportedEncodingException e) {
      throw new IOException(e);
    } catch (InterruptedException e) {
      throw new IOException("Executable has been interrupted.");
    }
  }

  public static ServerUtil.RunExecResult runExecutable(List<String> args, boolean failOnNonzeroExitStatus) throws IOException {
    return ServerUtil.runExecutable(null,list2Strings(args),null,null,failOnNonzeroExitStatus);
  }
  
  public static ServerUtil.RunExecResult runExecutable(String[] args, boolean failOnNonzeroExitStatus) throws IOException {
    return ServerUtil.runExecutable(null,args,null,null,failOnNonzeroExitStatus);
  }

  public static String[] list2Strings(List<String> args) {
    String[] stringArgs=new String[args.size()];
    int ind=0;
    for (String arg : args) {
      stringArgs[ind++] = arg;
    }
    return stringArgs;
  }
  
  
  public interface FileAction {
    public void action(File file) throws IOException;
  }
  
  public static void forEachFile(String path, FileAction fileAction) throws IOException {
    File pathFile=new File(path);
    forEachFile(pathFile,fileAction);
  }
  
  public static void forEachFile(File pathFile, FileAction fileAction) throws IOException {
    if (pathFile.isFile()) {
      fileAction.action(pathFile);
    }
    else if (pathFile.isDirectory()) {
      final String[] entries=pathFile.list();
      for (int i=0;i<entries.length;i++) {
        String entry=entries[i];
        forEachFile(pathFile.toString()+File.separator+entry,fileAction);
      }
    }
  }
  

  public static void removeRecursively(String path, boolean includePathItself, FilenameFilter optFilter) {
    removeRecursivelyHelper(path,optFilter);
    if (includePathItself) {
      File pathAsFile=new File(path);
      //if (optFilter==null || optFilter.accept(pathAsFile.getParentFile(),pathAsFile.getName()))
      pathAsFile.delete();
    }
  }
  
  private static void removeRecursivelyHelper(String path, FilenameFilter optFilter) {
    final File pathFile=new File(path);
    final String[] dirs=
      pathFile.list(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return new File(dir.getPath()+File.separator+name).isDirectory();
        }
      });
    if (dirs!=null) {
      for (int i=0;i<dirs.length;i++) {
        final String subPath=path+File.separator+dirs[i];
        removeRecursivelyHelper(subPath,optFilter);
      }
    }
    
    final String[] entriesToDelete=
      optFilter!=null? pathFile.list(optFilter):pathFile.list();
    if (entriesToDelete!=null) {
      for (int i=0;i<entriesToDelete.length;i++) {
        final String subPath=path+File.separator+entriesToDelete[i];
        new File(subPath).delete();
      }
    }
  }
  
  public static void copyStream(InputStream inStr, OutputStream outStr, int bufferSize) throws IOException {
    byte[] buf=new byte[bufferSize];
    for (;;) {
      final int got=inStr.read(buf);
      if (got<0) break;
      else if (got>0) 
        outStr.write(buf,0,got);
    }
  }
  
  public static void copyStream(InputStream inStr, OutputStream outStr) throws IOException {
    copyStream(inStr,outStr,1024);
  }
  
  public static void copyFile(String fromPath, String toPath) throws IOException {
	  File newFile = new File(toPath);
		if (!newFile.exists()) {
			newFile.getParentFile().mkdirs();
			newFile.createNewFile();
		}
    OutputStream outStr=new FileOutputStream(toPath);
    try {
      InputStream inStr=new FileInputStream(fromPath);
      try {
        copyStream(inStr,outStr);
      } finally {
        inStr.close();
      }
    } finally {
      outStr.close();
    }
  }
  
  public static void copyFilesRecursively(String fromPath, String toPath, boolean createMissingDirs, FilenameFilter optFilter) throws IOException {
    File fromPathFile=new File(fromPath);
    if (fromPathFile.isFile()) {
      if (optFilter==null || optFilter.accept(fromPathFile.getParentFile(),fromPathFile.getName())) {
        if (createMissingDirs)
          ServerUtil.createMissingDirsFor(new File(toPath));
        copyFile(fromPath,toPath);
      }
    }
    else if (fromPathFile.isDirectory()) {
      /* don't create empty directories, omit:
      File toPathFile=new File(toPath);
      if (!toPathFile.exists()) {
        if (!toPathFile.mkdir())
          throw new IOException("Cannot create directory "+toPath+" for recursive file copy.");
      }
      */
      final String[] entries=fromPathFile.list();
      for (int i=0;i<entries.length;i++) {
        String entry=entries[i];
        copyFilesRecursively(fromPath+File.separator+entry,toPath+File.separator+entry,createMissingDirs,optFilter);
      }
    }
  }
  
  
  public static File createTempDir(String prefix) throws IOException {
    //File tmpDir=(File)servletContext.getAttribute("javax.servlet.context.tempdir"); // on Windows might return just "\tmp" without a drive specifier, alternative: use File.createTempFile() without dir parameter
    //if (tmpDir==null)
    //  throw new IOException("Cannot get tempdir attribute.");
    //File tempDir=File.createTempFile(prefix,".tmp",tmpDir);
    File tempDir=File.createTempFile(prefix,".tmp");
    tempDir.delete();
    mkdir(tempDir);
    return tempDir;
  }
  
  public static void mkdir(File dir) throws IOException {
    if (!dir.mkdir())
      throw new IOException("Cannot create directory "+dir.toString()+" .");
  }
  
  public static void createMissingDirsFor(File file) throws IOException {
    File parent=file.getParentFile();
    if (parent!=null && !parent.exists()) {
      createMissingDirsFor(parent);
      if (!parent.mkdir())
        throw new IOException("Cannot create directory "+parent.getCanonicalPath());
    }
  }

  /*
  public static String buildHttpContextPath(HttpServletRequest httpRequest) {
    String hostname=httpRequest.getServerName();
    if (hostname.equals("localhost")) {
      try {
        hostname=java.net.InetAddress.getLocalHost().getHostName();
      } catch (UnknownHostException e) {
        // leave it at localhost
      }
    }
      
    String contextPath=
      httpRequest.getScheme()+"://"+
      hostname+
      (httpRequest.getServerPort()!=80? ":"+httpRequest.getServerPort():"")+
      httpRequest.getContextPath();
    return contextPath;
  }
  */
  
  
  public static void unpackJar(String targetPath, String sourceJarFileName, List<String> optFileList, FilenameFilter optFilter) throws IOException {
    FileInputStream libJarRaw=new FileInputStream(sourceJarFileName);
    unpackJar(targetPath,libJarRaw,optFileList,optFilter);
  }
  
  public static void unpackJar(String targetPath, InputStream sourceJarFileStream, List<String> optFileList, FilenameFilter optFilter) throws IOException {
    try {
      JarInputStream libJar=new JarInputStream(sourceJarFileStream);
      
      for (;;) {
        JarEntry entry=libJar.getNextJarEntry(); // libJar.getNextJarEntry() omits manifest, libJar.getNextEntry() too
        if (entry==null) break;
        
        final String path=targetPath+File.separator+entry.getName().replace('/',File.separatorChar);
        /* don't create empty directories:
        for (int off=0;;) {
          final int pos=path.indexOf(File.separator,off);
          if (pos<0) break;
          if (pos>off) {
            final String subPath=path.substring(0,pos);
            final File subDir=new File(subPath);
            if (!subDir.exists())
              subDir.mkdir();
          }
          off=pos+File.separator.length();
        }
        */
        
        if (!entry.isDirectory()) {
          File entryFile=new File(path);
          boolean accept;
          if (optFilter==null)
            accept=true;
          else {
            accept=optFilter.accept(entryFile.getParentFile(),entryFile.getName());
          }
          if (accept) {
            if (optFileList!=null)
              optFileList.add(path);
            createMissingDirsFor(entryFile);
            OutputStream outStr=new FileOutputStream(path);
            try {
              copyStream(libJar,outStr);
            } finally {
              outStr.close();
            }
          }
        }
      }
    } finally {
      sourceJarFileStream.close();
    }
  }
  

  public static String readTextFile(File inFile, String encoding /* e.g. "UTF-8" or "US-ASCII" */) throws IOException {
	  String ret = "";
      try {
          InputStreamReader read = new InputStreamReader(
                  new FileInputStream(inFile),encoding);
          BufferedReader bufferedReader = new BufferedReader(read);
          String lineTxt = null;
          while((lineTxt = bufferedReader.readLine()) != null){
              ret+=lineTxt+"\n";
          }
          read.close();
      }
      catch (IOException ex){

      }
      return ret; 
  }
  
  public static String readTextFile(InputStream inStr, String encoding) throws IOException {
    ByteArrayOutputStream rawBuf=new ByteArrayOutputStream();
    copyStream(inStr,rawBuf);
    inStr.close();
    return rawBuf.toString(encoding);
  }
  
  public static byte[] readBinaryStream(InputStream inStr) throws IOException {
    ByteArrayOutputStream rawBuf=new ByteArrayOutputStream();
    copyStream(inStr,rawBuf);
    inStr.close();
    return rawBuf.toByteArray();
  }
  

  public static void writeTextFile(String text, File outFile, String encoding) throws IOException {
    writeTextFile(text,new FileOutputStream(outFile),encoding);
  }
  
  public static void writeTextFile(String text, OutputStream outStr, String encoding) throws IOException {
    outStr.write(text.getBytes(encoding));
    outStr.close();
  }
  
  
  //public static String getResourceFilePath(String relPath) {
  //  return "resources"+(relPath.length()>0? File.separator+relPath.replace("/",File.separator):"");
  //}
  
  /*
  public static InputStream getOptConfiguredFile(String propertyValue, String optDefaultValue, ServletContext servletContext) throws IOException {
    InputStream inStr;
    if (!(propertyValue!=null && !propertyValue.isEmpty()))
      propertyValue=optDefaultValue;
    if (propertyValue!=null && !propertyValue.isEmpty()) {
      final String webInfTag="$WEB-INF/";
      if (propertyValue.startsWith(webInfTag)) {
        String resourcePath=propertyValue.replace(webInfTag,"/WEB-INF/");
        inStr=servletContext.getResourceAsStream(resourcePath); // potentially null
      } else {
        File file=new File(propertyValue);
        if (file.exists() && file.canRead())
          inStr=new FileInputStream(file);
        else
          inStr=null;
      }
    } else {
      inStr=null;
    }
    return inStr;
  }
  
  public static InputStream getConfiguredFile(String propertyValue, String optDefaultValue, ServletContext servletContext) throws IOException {
    InputStream inStr=getOptConfiguredFile(propertyValue,optDefaultValue,servletContext);
    if (inStr==null)
      throw new IOException("Cannot open configured file "+(propertyValue!=null? propertyValue:optDefaultValue)+" .");
    return inStr;
  }
  */
  
  public static String getCanonicalHostname() {
    String hostname;
    try {
      hostname=java.net.InetAddress.getLocalHost().getHostName(); // getCanonicalHostName() returns IP address for local networks, like 10.140.11.11
    } catch (UnknownHostException e) {
      hostname="(unknownhostname)";
    }
    for (String trailer: new String[] { ".local",".xsboxgo",".fritz.box" }) {
      if (hostname.endsWith(trailer)) {
        hostname=hostname.substring(0,hostname.length()-trailer.length());
        break;
      }
    }
    return hostname;
  }
  
  /**
   * Useful when behavior has to be different if wrapping engine is 
   * running on a Windows host.
   * @return true if host platform is Microsoft Windows
   */
  public static boolean isWindows(){
    String sOS = System.getenv("OS");
    return sOS!=null && sOS.toLowerCase().contains("windows");
  }

  private static Boolean cachedIsDebug=null;
  public static boolean isDebug() {
    if (cachedIsDebug==null) {
      cachedIsDebug=new Boolean(ServerUtil.getCanonicalHostname().matches("build[0-9]+\\.appcentral\\.com|runner|air|light|cygnus")); //$NON-NLS-1$
    }
    return cachedIsDebug.booleanValue();
  }
  
  
  private static Map<String,String> debugLogPrivateText=new HashMap<String,String>();
  
  public static void registerPrivateText(String tag, String privateText) {
    synchronized(debugLogPrivateText) {
      debugLogPrivateText.put(tag,privateText);
    }
  }
  
  public static void unregisterPrivateText(String tag) {
    synchronized(debugLogPrivateText) {
      debugLogPrivateText.remove(tag);
    }
  }

  public static String removePrivateDataFromText(String text) {
    synchronized(debugLogPrivateText) {
      // might replace more than just those passwords if the passwords are not unique enough, obfuscate anyway:
      String nonPrivateText=text;
          //.replace(keychainPassword,"[keychainPassword]")
          //.replace(defaultPrivKeyPassword,"[privKeyPassword]");
      for (String key: debugLogPrivateText.keySet())
        nonPrivateText=nonPrivateText.replace(debugLogPrivateText.get(key),"["+key+"]");
      return nonPrivateText;
    }
  }

  public static void debugLog(String message) {
    //ServerUtilInLib.debugLog(getClass(),/*command+": "+*/ result.allOutput());
    // careful, log might expose passwords, only use while debugging
    if (Boolean.TRUE || ServerUtil.isDebug())
      System.out.println("Debug: "+ServerUtil.removePrivateDataFromText(message));
  }
  
  public static void debugLog(/*String command,*/ ServerUtil.RunExecResult result) {
    debugLog(result.allOutput());
  }
  

}
