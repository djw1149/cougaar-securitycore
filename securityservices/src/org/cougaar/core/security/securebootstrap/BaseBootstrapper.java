/*
 * <copyright>
 *  Copyright 1997-2001 Networks Associates Technology, Inc.
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).  
 *  
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS 
 *  PROVIDED "AS IS" WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR 
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF 
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT 
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT 
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL 
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS, 
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR 
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.  
 * 
 * </copyright>
 *
 * CHANGE RECORD
 * - 
 */

package org.cougaar.core.security.securebootstrap;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Arrays;
import java.io.File;
import java.io.InputStream;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.Provider;
import java.security.Security;

// Cougaar core services
import org.cougaar.bootstrap.Bootstrapper;

public class BaseBootstrapper
  extends Bootstrapper
{
  private String nodeName;
  private static Logger _logger = Logger.getInstance();

  protected String getNodeName() {
    return nodeName;
  }

  protected ClassLoader prepareVM(String classname, String[] args) {
    nodeName = parseNodename(args);
    ClassLoader cl = null;
    /*
      Setting up  policy & security manager
      if there is a policy and security manager then override the set 
      setPolicy & setSecurityManager methods. 
    */
    try {
      /* Set the Java policy for use by the security manager */
      _logger.debug("Setting policy");
      setPolicy();

      /* Set the Java security manager */
      if (_logger.isDebugEnabled()) {
	_logger.debug("Setting security manager");
      }
      setSecurityManager();

      /* Create a log file to report JAR file verification failures.
	 This is only used when a secure class loader is set. */
      if (_logger.isDebugEnabled()) {
	_logger.debug("Creating Jar verification log");
      }
      createJarVerificationLog();

      /* Create the class loader. Load JAR files securely if
       * a secure class loader is used. */
      if (_logger.isDebugEnabled()) {
	_logger.debug("Creating class loader");
      }
      cl = super.prepareVM(classname, args);
      if (_logger.isDebugEnabled()) {
	_logger.debug("Class Loader:" + cl.getClass().getName());
      }
 
      /* Load cryptographic providers */
      loadCryptoProviders(cl);
    }
    catch (Exception e) {
      _logger.warn("Failed to launch "+classname, e);
    }

    return cl;
  }

  protected void launchMain(ClassLoader cl, String classname, String[] args) {
    if (_logger.isDebugEnabled()) {
      _logger.debug("Starting " + classname + " in "
		    + System.getProperty("user.dir"));
      String s = "Arguments: ";
      for (int i = 0 ; i < args.length ; i++) {
	s = s + args[i] + " ";
      }
      _logger.debug(s);
    }
    super.launchMain(cl, classname, args);
  }

  protected String parseNodename(String[] args) {
    if (args.length <= 1) { // assemble command line from java VM properties
      String nodeClassName = "org.cougaar.core.node.Node";
	
      if (args.length == 1)
	nodeClassName = args[0];

      args = new String[4];
      args[0] = nodeClassName;
      args[1] = "-n";
      args[2] = System.getProperty("org.cougaar.node.name", "unknown-node");
      args[3] = "-c";
    }

    int argc = args.length;
    String check = null;
    String next = null;
    boolean sawname = false;
    for( int x = 0; x < argc;){
      check = args[x++];
      if (! check.startsWith("-") && !sawname) {
	sawname = true;
	if ("admin".equals(check)) 
	  nodeName = "Administrator";
	else
	  nodeName = check;
      }
      else if (check.equals("-n")) {
	nodeName = args[x++];
	sawname = true;
      }
    }
    return nodeName;
  }

  protected void setPolicy() {
  }

  protected void setSecurityManager() {
  }

  protected void createJarVerificationLog() {
  }

  protected ClassLoader createClassLoader(List urlList) {
    if (_logger.isDebugEnabled()) {
      _logger.debug("BaseBootstrapper.createClassLoader");
    }
    removeBootClasses(urlList);

    URL urls[] = (URL[]) urlList.toArray(new URL[urlList.size()]);
    return new BaseClassLoader(urls);
  }

  protected void loadCryptoProviders(ClassLoader cl)
  {
    if (_logger.isDebugEnabled()) {
      _logger.debug("Loading cryptographic providers");
    }
    String config_path = System.getProperty("org.cougaar.config.path");
    /*
    FileFinder fileFinder = FileFinderImpl.getInstance(config_path);
    File file = fileFinder.locateFile("cryptoprovider.conf");
    */

    StringBuffer configfile=new StringBuffer();
    String configproviderpath=
      System.getProperty("org.cougaar.core.security.crypto.cryptoProvidersFile");
    String sep = File.separator;
    if((configproviderpath==null)||(configproviderpath=="")) {
      configproviderpath=System.getProperty("org.cougaar.install.path");    
      if((configproviderpath!=null)||(configproviderpath!="")) {
	configfile.append(configproviderpath);
	configfile.append(sep+"configs"+sep+"security"+sep+"cryptoprovider.conf");
      }
      else {
	System.err.println("Error loading cryptographic providers: org.cougaar.install.path not set");
	return;
      }
    }
    else {
      configfile.append(configproviderpath);
    }
    File file=new File(configfile.toString());

    if(file == null || !file.exists()) {
      _logger.warn("Cannot find Cryptographic Provider Configuration file");
      return;
    }
    try {
      FileReader filereader=new FileReader(file);
      BufferedReader buffreader=new BufferedReader(filereader);
      String linedata=new String();
      int index=0;
      String providerclassname="";
      while((linedata=buffreader.readLine())!=null) {
	linedata.trim();
	if(linedata.startsWith("#")) {
	  continue;
	}
	if(linedata.startsWith("security.provider")) {
	  index=linedata.indexOf('=');
	  if(index!=-1) {
	    providerclassname=linedata.substring(index+1);
	    if (_logger.isDebugEnabled()) {
	      _logger.debug("Loading provider " + providerclassname);
	    }
	    try {
	      if (_logger.isDebugEnabled()) {
		_logger.debug("Loading " + providerclassname
			      + " with " + cl.toString());
	      }
	      Class c = Class.forName(providerclassname, true, cl);
	      Object o = c.newInstance();
	      if (o instanceof java.security.Provider) {
		Security.addProvider((java.security.Provider) o);
	      }
	    } 
	    catch(Exception e) {
	      _logger.warn("Error loading security provider (" + e + ")"); 
	    }
	  }
	}
      }
    }
    catch(FileNotFoundException fnotfoundexp) {
      _logger.warn("cryptographic provider configuration file not found");
    }
    catch(IOException ioexp) {
      _logger.warn("Cannot read cryptographic provider configuration file", ioexp);
    }
    if (_logger.isDebugEnabled()) {
      printProviderProperties();
    }
  }

  public static void printProviderProperties() {
    Provider[] pv = Security.getProviders();
    for (int i = 0 ; i < pv.length ; i++) {
      _logger.debug("Provider[" + i + "]: "
		    + pv[i].getName() + " - Version: " + pv[i].getVersion());
      _logger.debug(pv[i].getInfo());
      // List properties
      String[] properties = new String[1];
      properties = (String[]) pv[i].keySet().toArray(properties);
      Arrays.sort(properties);
      for (int j = 0 ; j < properties.length ; j++) {
	String key, value;
	key = (String) properties[j];
	value = pv[i].getProperty(key);
	_logger.debug("Key: " + key + " - Value: " + value);
      }
    }
  }

  /** Remove bootstrap jar files from the list of URLs.
   * The list of URLs constructed by looking in the $CIP/lib and $CIP/sys
   * directories may contain jar files which are already part of the boot class path.
   * These jar files should be loaded by the system class loader directly.
   * Here, we remove the boot jar files from the list.
   */
  protected void removeBootClasses(List urlList) {
    String bootclassPathProp = System.getProperty("sun.boot.class.path");
    if (_logger.isDebugEnabled()) {
      _logger.debug("Boot Class Path:" + bootclassPathProp);
    }
    StringTokenizer st = new StringTokenizer(bootclassPathProp, ":");
    ArrayList bootclassPath = new ArrayList();
    while(st.hasMoreElements()) {
      String s = st.nextToken();
      try {
	// Need to resolve symbolic names
	URL url = new URL("file", "", (new File(s)).getCanonicalPath());
	bootclassPath.add(url);
      }
      catch (Exception ex) {
	_logger.warn("Unable to parse " + s + " url.");
      }
    }

    // Now, go through the list of URLs and remove the URLs which were
    // already specified in the boot class path.
    Iterator it = urlList.iterator();
    while (it.hasNext()) {
      URL aUrl = (URL) it.next();
      Iterator listIt = bootclassPath.iterator();
      while (listIt.hasNext()) {
	URL bootUrlElement = (URL) listIt.next();
	if (bootUrlElement.equals(aUrl)) {
	  // Don't add the bootclass URLs
	  if (_logger.isDebugEnabled()) {
	    _logger.debug("Removing " + aUrl.toString() + " from URL list");
	  }
	  it.remove();
          break;
	}
      }
    }
  }
}
