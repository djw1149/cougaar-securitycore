/* 
 * <copyright> 
 *  Copyright 1999-2004 Cougaar Software, Inc.
 *  under sponsorship of the Defense Advanced Research Projects 
 *  Agency (DARPA). 
 *  
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).  
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright> 
 */ 


package org.cougaar.core.security.tools;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.security.provider.SecurityServiceProvider;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class KeyGenerator {

  private int nbCertificates = 0;
  private int nbCertificatesSucceed = 0;
  private String agentName = null;
  private static Logger _log;

  private SecurityServiceProvider secProvider = null;

  static {
    _log = LoggerFactory.getInstance().createLogger("KeyGenerator");
  }

  public KeyGenerator()
  {
    secProvider = new SecurityServiceProvider();
    ServiceBroker sb = secProvider.getServiceBroker();
  }

  public static void main(String args[]) {
    KeyGenerator kg = new KeyGenerator();
    kg.run(args);
  }

  public void run(String args[]) {
    int action = 0;

    if (args.length < 2) {
      _log.debug("Usage: KeyGenerator <xml definition file> options <agent-name>");
      _log.debug("     Where options are one of:");
      _log.debug("          -genkey       : Create key pairs");
      _log.debug("          -gencertsign  : Generate Certificate Signing Requests");
      _log.debug("          -importsigned : Import Certificates signed by CA");
      _log.debug("          -exportpub    : Export public keys from keystore");
      _log.debug("          -importpub    : Import all public keys into each keystore");
      _log.debug("          -removepub    : Remove public keys from keystore");
      _log.debug("                        : Must have called -exportpub before");
      _log.debug("     <agent-name>       : Optional agent name");

      _log.debug("Arguments: " + args.length);
      return;
    }
    if (args.length == 3) {
      agentName = args[2];
      _log.debug("Executing for '" + agentName + "' only");
    }
    if (args[1].equals("-genkey")) {
      action = 1;
    } else if (args[1].equals("-gencertsign")) {
      action = 2;
    } else if (args[1].equals("-importsigned")) {
      action = 3;
    } else if (args[1].equals("-exportpub")) {
      action = 4;
    } else if (args[1].equals("-importpub")) {
      action = 5;
    } else if (args[1].equals("-removepub")) {
      action = 6;
    }
    System.setProperty("org.cougaar.security.crypto.config", args[0]);

    // Dead code. 
    // Element root = configParser.getConfigDocument().getDocumentElement();
    Element root = null;
    iterateKeyStore(root, action);

    _log.debug("Total number of certificates:        " + nbCertificates);
    _log.debug("Certificates successfully processed: " + nbCertificatesSucceed);
  }

  /** This convenience method returns the textual content of the named
      child element, or returns an empty String ("") if the child has no
      textual content. */
  private String getChildText(Element e, String tagName)
  {
    NodeList nodes = e.getElementsByTagName(tagName);
    if (nodes == null || nodes.getLength() == 0) {
      return null;
    }
    // Get first element
    String val = null;
    Node child = nodes.item(0).getFirstChild();
    if (child != null) {
      val = child.getNodeValue();
    }
    return val;
  }

  public void iterateKeyStore(Element element, int action) {
    
    NodeList societyChildren = element.getChildNodes();
    // Iterate through each key store
    for (int i = 0 ; i < societyChildren.getLength() ; i++) {
      Node o = societyChildren.item(i);
      if (o instanceof Element
	  && ((Element)o).getTagName().equals("keystorefile")) {
	Element keyNode = (Element)o;
	String keyStoreName =  getChildText(keyNode, "keystore");
	String keyStorePasswd = getChildText(keyNode, "storepass");
		
	_log.debug("keystore: " + keyStoreName);

	NodeList keyStoreChildren = keyNode.getChildNodes();

	// Iterate through keys
	for (int j = 0 ; j < keyStoreChildren.getLength() ; j++) {
	  Node keyo = keyStoreChildren.item(j);
	  if (keyo instanceof Element) {
	    if (((Element)keyo).getNodeName().equals("key")) {
	      if (agentName != null) {
		String alias = getChildText((Element)keyo, "alias");
		if (alias.equals(agentName) == false) {
		  _log.debug("Skipping " + alias + "...");
		  continue;
		}
	      }
	      switch (action) {
	      case 1: // Generate keys
		createKeyPairWithKeyTool((Element)keyo, keyStoreName, keyStorePasswd);
		break;
	      case 2: // Create Certificate Signing Requests
		createCertificateRequestWithKeyTool((Element)keyo, keyStoreName, keyStorePasswd);
		break;
	      case 3: // Import Signed Certificates
		importSigneCertificateWithKeyTool((Element)keyo, keyStoreName, keyStorePasswd);
		break;
	      case 4: // Export public key Certificates
		exportCertificatesWithKeyTool((Element)keyo, keyStoreName, keyStorePasswd);
		break;
	      case 5: // Import public key Certificates
		importCertificatesWithKeyTool((Element)keyo, keyStoreName, keyStorePasswd);
		break;
	      case 6: // Remove public keys from Certificates
		removeCertificatesWithKeyTool((Element)keyo, keyStoreName, keyStorePasswd);
		break;
	      default:
		break;
	      }
	    }

	    if (((Element)keyo).getNodeName().equals("trustedCA")) {
	      switch (action) {
	      case 3: // Import trusted authority
		importTrustedAuthorityWithKeyTool((Element)keyo, keyStoreName, keyStorePasswd);
		break;
	      default:
		break;
	      }
	    }
	  }
	}
      }
    }
  }

  public KeyStore loadKeyStore(String keyStoreName,
			       String keyStorePasswd) {
    KeyStore keyStore = null;
    InputStream in = null;
    try {
      in = new FileInputStream(keyStoreName);

      keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      keyStore.load(in, keyStorePasswd.toCharArray());
    } catch(java.io.FileNotFoundException e) {
      e.printStackTrace();
    } catch(java.security.KeyStoreException e) {
      e.printStackTrace();
    } catch(java.security.NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch(java.security.cert.CertificateException e) {    
      e.printStackTrace();
    } catch(java.io.IOException e) {
      e.printStackTrace();
    }
    return keyStore;
  }

  public void storeKeyStore(KeyStore keyStore,
			    String keyStoreName,
			    String keyStorePasswd) {
    try {
      OutputStream out = new FileOutputStream(keyStoreName);
      keyStore.store(out, keyStorePasswd.toCharArray());
    } catch(java.io.IOException e) {
      e.printStackTrace();
    } catch(java.security.KeyStoreException e) {
      e.printStackTrace();
    } catch(java.security.NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch(java.security.cert.CertificateException e) {    
      e.printStackTrace();
    }
  }

  public void iterateKey(Element element,
				String keyStoreName,
				String keyStorePasswd) {
    // String alias = getChildText(element, "alias");
    //String keypass = getChildText(element, "keypass");
    //String keyalg = getChildText(element, "keyalg");
    //String sigalg = getChildText(element, "sigalg");
    //String keysize = getChildText(element, "keysize");
    //String dname = getChildText(element, "dname");

  }

  public void createKeyPairWithKeyTool(Element element,
				       String keyStoreName,
				       String keyStorePasswd) {
    String alias = getChildText(element, "alias");
    String keypass = getChildText(element, "keypass");
    String keyalg = getChildText(element, "keyalg");
    String sigalg = getChildText(element, "sigalg");
    String keysize = getChildText(element, "keysize");
    String dname = getChildText(element, "dname");

    List genKeyCom = new ArrayList();

    genKeyCom.add("keytool");
    genKeyCom.add("-genkey");
    if (keyStorePasswd != null) {
      genKeyCom.add("-storepass");
      genKeyCom.add(keyStorePasswd);
    }
    if (keyStoreName != null) {
      genKeyCom.add("-keystore");
      genKeyCom.add("KeyStores/" + keyStoreName);
    }
    if (alias != null) {
      genKeyCom.add("-alias");
      genKeyCom.add(alias);
    }
    if (keypass != null) {
      genKeyCom.add("-keypass");
      genKeyCom.add(keypass);
    }
    if (keyalg != null) {
      genKeyCom.add("-keyalg");
      genKeyCom.add(keyalg);
    }
    if (sigalg != null) {
      genKeyCom.add("-sigalg");
      genKeyCom.add(sigalg);
    }
    if (keysize != null) {
      genKeyCom.add("-keysize");
      genKeyCom.add(keysize);
    }
    if (dname != null) {
      genKeyCom.add("-dname");
      genKeyCom.add(dname);
    }
    _log.debug("Creating key for " + alias);
    executeCommand(genKeyCom);
  }

  public void executeCommand(List commandLine) {
    try {
      String[] command = new String[commandLine.size()];
      for (int i = 0 ; i < commandLine.size() ; i++) {
	command[i] = (String) commandLine.get(i);
      }

      nbCertificates++;

      Runtime rt = Runtime.getRuntime();
      Process process = rt.exec(command);

      BufferedReader in = new BufferedReader(
					     new InputStreamReader(process.getInputStream()));
      BufferedReader err = new BufferedReader(
					      new InputStreamReader(process.getErrorStream()));
   
      boolean isError = false;
      String line = null;

      while ((line = err.readLine()) != null) {
	_log.debug("keytool stderr:" + line);
	isError = true;
      }
      while ((line = in.readLine()) != null) {
	_log.debug("keytool stdout:" + line);
	isError = true;
      }
      process.waitFor();
      if ((process.exitValue() == 0) && (isError == false)) {
	nbCertificatesSucceed++;
      }

    } catch(java.io.IOException e) {
      e.printStackTrace();
    } catch(java.lang.InterruptedException e) {
      e.printStackTrace();
    }
  }

  public void createCertificateRequestWithKeyTool(Element element,
						  String keyStoreName,
						  String keyStorePasswd) {
    String alias = getChildText(element, "alias");
    String keypass = getChildText(element, "keypass");
    String signingAuthority = getChildText(element, "signingAuthority");

    List genKeyCom = new ArrayList();

    genKeyCom.add("keytool");
    genKeyCom.add("-certreq");
    if (keyStorePasswd != null) {
      genKeyCom.add("-storepass");
      genKeyCom.add(keyStorePasswd);
    }
    if (keyStoreName != null) {
      genKeyCom.add("-keystore");
      genKeyCom.add("KeyStores/" + keyStoreName);
    }

    if (alias != null) {
      genKeyCom.add("-alias");
      genKeyCom.add(alias);
    }
    if (keypass != null) {
      genKeyCom.add("-keypass");
      genKeyCom.add(keypass);
    }
    genKeyCom.add("-file");
    genKeyCom.add("CertificateSigningRequests/certSignReq-" + signingAuthority + "-" + alias + ".cer");

    _log.debug("Creating signing certificate request for " + alias);
    executeCommand(genKeyCom);
  }

  public void importSigneCertificateWithKeyTool(Element element,
						String keyStoreName,
						String keyStorePasswd) {
    String alias = getChildText(element, "alias");
    String keypass = getChildText(element, "keypass");
    String signingAuthority = getChildText(element, "signingAuthority");

    List genKeyCom = new ArrayList();

    genKeyCom.add("keytool");
    genKeyCom.add("-import");
    if (keyStorePasswd != null) {
      genKeyCom.add("-storepass");
      genKeyCom.add(keyStorePasswd);
    }
    if (keyStoreName != null) {
      genKeyCom.add("-keystore");
      genKeyCom.add("KeyStores/" + keyStoreName);
    }

    if (alias != null) {
      genKeyCom.add("-alias");
      genKeyCom.add(alias);
    }
    if (keypass != null) {
      genKeyCom.add("-keypass");
      genKeyCom.add(keypass);
    }

    genKeyCom.add("-file");
    genKeyCom.add("CaSignedCertificates/SignedReq-" + signingAuthority + "-" + alias + ".cer");

    _log.debug("Importing Signed Certificate for " + alias);
    executeCommand(genKeyCom);
  }

  public void importTrustedAuthorityWithKeyTool(Element element,
						String keyStoreName,
						String keyStorePasswd) {
    String alias = getChildText(element, "alias");
    String fileName = getChildText(element, "file");

    List genKeyCom = new ArrayList();

    genKeyCom.add("keytool");
    genKeyCom.add("-import");
    genKeyCom.add("-noprompt");
    if (keyStorePasswd != null) {
      genKeyCom.add("-storepass");
      genKeyCom.add(keyStorePasswd);
    }
    if (keyStoreName != null) {
      genKeyCom.add("-keystore");
      genKeyCom.add("KeyStores/" + keyStoreName);
    }
    genKeyCom.add("-trustcacerts");

    if (alias != null) {
      genKeyCom.add("-alias");
      genKeyCom.add(alias);
    }
    else {
      _log.debug("Error: alias missing");
      return;
    }
    if (fileName != null) {
      genKeyCom.add("-file");
      genKeyCom.add("CaCertificates/" + fileName);
    }
    else {
      _log.debug("Error: file name missing");
      return;
    }

    _log.debug("Importing trusted CA: " + alias);
    executeCommand(genKeyCom);
  }

  public void exportCertificatesWithKeyTool(Element element,
					    String keyStoreName,
					    String keyStorePasswd) {
    String alias = getChildText(element, "alias");
    String signingAuthority = getChildText(element, "signingAuthority");

    List genKeyCom = new ArrayList();

    genKeyCom.add("keytool");
    genKeyCom.add("-export");
    genKeyCom.add("-rfc");

    if (keyStorePasswd != null) {
      genKeyCom.add("-storepass");
      genKeyCom.add(keyStorePasswd);
    }
    if (keyStoreName != null) {
      genKeyCom.add("-keystore");
      genKeyCom.add("KeyStores/" + keyStoreName);
    }

    if (alias != null) {
      genKeyCom.add("-alias");
      genKeyCom.add(alias);
    }
    else {
      _log.debug("Error: alias missing");
      return;
    }
    genKeyCom.add("-file");
    genKeyCom.add("PublicKeys/pubKeyCert-" + signingAuthority + "-" + alias + ".cer");

    _log.debug("Exporting public key certificates: " + alias);
    executeCommand(genKeyCom);
  }

  public void importCertificatesWithKeyTool(Element element,
					    String keyStoreName,
					    String keyStorePasswd) {
    String alias = getChildText(element, "alias");
    String signingAuthority = getChildText(element, "signingAuthority");


    // Iterate through all parents (keystores) and store public key in all keystores
    // Except for the current key store, since the key is already there.

    NodeList keyStore =
      element.getParentNode().getParentNode().getChildNodes();

    for (int i = 0 ; i < keyStore.getLength() ; i++) {
      Node o = keyStore.item(i);
      if (o instanceof Element
	  && ((Element)o).getNodeName().equals("keystorefile")) {
	Element keyNode = (Element)o;
	String otherKeyStoreName =  getChildText(keyNode, "keystore");
	String otherKeyStorePasswd = getChildText(keyNode, "storepass");
	if (keyStoreName.equals(otherKeyStoreName)) {
	  // Skipping
	  _log.debug("Skipping " + otherKeyStoreName);
	  continue;
	}

	List genKeyCom = new ArrayList();
		    
	genKeyCom.add("keytool");
	genKeyCom.add("-import");
	genKeyCom.add("-noprompt");
		
	if (keyStorePasswd != null) {
	  genKeyCom.add("-storepass");
	  genKeyCom.add(otherKeyStorePasswd);
	}
	if (keyStoreName != null) {
	  genKeyCom.add("-keystore");
	  genKeyCom.add("KeyStores/" + otherKeyStoreName);
	}
		
	if (alias != null) {
	  genKeyCom.add("-alias");
	  genKeyCom.add(alias);
	}
	else {
	  _log.debug("Error: alias missing");
	  return;
	}
	genKeyCom.add("-file");
	genKeyCom.add("PublicKeys/pubKeyCert-" + signingAuthority + "-" + alias + ".cer");
		
	_log.debug("   Importing public key certificates: " + alias
			   + " into " + otherKeyStoreName);
	executeCommand(genKeyCom);
      }
    }
  }

  public void removeCertificatesWithKeyTool(Element element,
					    String keyStoreName,
					    String keyStorePasswd) {
    String alias = getChildText(element, "alias");
    String signingAuthority = getChildText(element, "signingAuthority");

    List genKeyCom = new ArrayList();

    genKeyCom.add("keytool");
    genKeyCom.add("-delete");

    if (keyStorePasswd != null) {
      genKeyCom.add("-storepass");
      genKeyCom.add(keyStorePasswd);
    }
    if (keyStoreName != null) {
      genKeyCom.add("-keystore");
      genKeyCom.add("KeyStores/" + keyStoreName);
    }

    if (alias != null) {
      genKeyCom.add("-alias");
      genKeyCom.add(alias);
    }
    else {
      _log.debug("Error: alias missing");
      return;
    }
    genKeyCom.add("-file");
    genKeyCom.add("PublicKeys/pubKeyCert-" + signingAuthority + "-" + alias + ".cer");

    _log.debug("Exporting public key certificates: " + alias);
    executeCommand(genKeyCom);
  }
}
