/*
 * <copyright>
 *  Copyright 1997-2001 Networks Associates Technology, Inc.
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 *
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 * Created on September 12, 2001, 10:55 AM
 */

package org.cougaar.core.security.crypto;

import java.io.Serializable;
import java.io.IOException;
import java.security.*;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.security.cert.CertificateException;
import javax.crypto.*;
import java.security.cert.X509Certificate;

// Cougaar core infrastructure
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.ServiceRevokedEvent;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.component.ServiceBroker;


// Cougaar Security Services
import org.cougaar.core.security.services.crypto.KeyRingService;
import org.cougaar.core.security.services.crypto.EncryptionService;
import org.cougaar.core.security.provider.SecurityServiceProvider;
import org.cougaar.core.security.crypto.PublicKeyEnvelope;
import org.cougaar.core.security.crypto.SecureMethodParam;

public class CryptoManagerServiceImpl
  implements EncryptionService
{
  private KeyRingService keyRing;
  private ServiceBroker serviceBroker;
  private LoggingService log;

  public CryptoManagerServiceImpl(KeyRingService aKeyRing, ServiceBroker sb) {
    keyRing = aKeyRing;
    serviceBroker = sb;
    log = (LoggingService)
      serviceBroker.getService(this,
			       LoggingService.class, null);
  }

  public SignedObject sign(final String name,
			   String spec,
			   Serializable obj)
  throws GeneralSecurityException,
	 IOException {
    List pkList = (List)
      AccessController.doPrivileged(new PrivilegedAction() {
	  public Object run(){
	    return keyRing.findPrivateKey(name);
	  }
	});
    if (pkList == null || pkList.size() == 0) {
      throw new CertificateException("Private key not found.");
    }
    PrivateKey pk = ((PrivateKeyCert)pkList.get(0)).getPrivateKey();
    Signature se;
    // if(spec==null||spec=="")spec=pk.getAlgorithm();
    
    // Richard Liao
    // private key might not be found, if pending is required
    // the certficates are not approved automatically.
    // when agent is started with signAndEncrypt without
    // obtaining a certificate successfully this will generate
    // null pointer exception

    spec = AlgorithmParam.getSigningAlgorithm(pk.getAlgorithm());
    se=Signature.getInstance(spec);
    return new SignedObject(obj, pk, se);
  }

  public Object verify(String name, String spec, SignedObject obj)
    throws CertificateException {
    List certList =
      keyRing.findCert(name,
		       KeyRingService.LOOKUP_LDAP | KeyRingService.LOOKUP_KEYSTORE);
    if (certList == null || certList.size() == 0) {
      throw
	new CertificateException("Verify. Unable to get certificate for "
				 + name);
    }
    Iterator it = certList.iterator();
    while (it.hasNext()) {
      try {
	java.security.cert.Certificate c = 
	  ((CertificateStatus)it.next()).getCertificate();
	PublicKey pk = c.getPublicKey();
	Signature ve;
	//if(spec==null||spec=="")spec=pk.getAlgorithm();
	spec = AlgorithmParam.getSigningAlgorithm(pk.getAlgorithm());
	ve=Signature.getInstance(spec);
	if (obj.verify(pk,ve)) {
	  return obj.getObject();
	} else {
	  continue;
	}
      } catch (Exception e) {
	e.printStackTrace();
	continue;
      }
    }
    return null;
  }

  public SealedObject asymmEncrypt(String name, String spec, Serializable obj)
    throws GeneralSecurityException, IOException {
    /*encrypt the secretekey with receiver's public key*/

    List certList =
      keyRing.findCert(name,
		       KeyRingService.LOOKUP_LDAP | KeyRingService.LOOKUP_KEYSTORE);
    if (certList == null || certList.size() == 0) {
      throw new CertificateException("asymmEncrypt. Unable to get certificate for " + name);
    }
    java.security.cert.Certificate cert =
      ((CertificateStatus)certList.get(0)).getCertificate();
    PublicKey key = cert.getPublicKey();
    if (spec==""||spec==null) spec=key.getAlgorithm();
    if (log.isDebugEnabled()) {
      log.debug("Encrypting for " + name + " using " + spec);
    }
    /*init the cipher*/
    Cipher ci;
    ci=Cipher.getInstance(spec);
    ci.init(Cipher.ENCRYPT_MODE,key);
    return new SealedObject(obj,ci);
  }

  public Object asymmDecrypt(final String name,
			     String spec,
			     SealedObject obj){
    /*get secretKey*/
    List keyList = (List)
      AccessController.doPrivileged(new PrivilegedAction() {
	  public Object run(){
	    return keyRing.findPrivateKey(name);
	  }
	});
    if (keyList == null || keyList.size() == 0) {
      return null;
    }
    Iterator it = keyList.iterator();
    PrivateKey key = null;
    Cipher ci = null;
    while (it.hasNext()) {
      key = ((PrivateKeyCert)it.next()).getPrivateKey();
      if(spec==null||spec=="") 
	spec=key.getAlgorithm();
      try {
	ci=Cipher.getInstance(spec);
	ci.init(Cipher.DECRYPT_MODE, key);
	return obj.getObject(ci);
      }
      catch (Exception e) {
	if (log.isDebugEnabled()) {
	  log.debug("Warning: cannot recover message. " + e);
	  e.printStackTrace();
	}
	continue;
      }
    }
    return null;
  }

  public SealedObject symmEncrypt(SecretKey sk,
				  String spec,
				  Serializable obj)
  throws GeneralSecurityException, IOException {
    /*create the cipher and init it with the secret key*/
    Cipher ci;
    ci=Cipher.getInstance(spec);
    ci.init(Cipher.ENCRYPT_MODE,sk);
    return new SealedObject(obj,ci);
  }

  public Object symmDecrypt(SecretKey sk, SealedObject obj){
    Object o = null;
    if (sk == null) {
      if (log.isDebugEnabled()) {
	log.debug("Secret key not provided!");
      }
      return o;
    }

    try{
      return obj.getObject(sk);
    }
    catch(NullPointerException nullexp){
      boolean loop = true;
      if (log.isDebugEnabled()) {
	log.debug("in symmDecrypt" +nullexp);
      }
      while(loop){
	try{
	  Thread.sleep(200);
	  o = obj.getObject(sk);
	  if (log.isDebugEnabled()) {
	    log.debug("Workaround to Cougaar core bug. Succeeded");
	  }
	  return o;
	}
	catch(NullPointerException null1exp){
	  if (log.isDebugEnabled()) {
	    System.err.println(
			       "Workaround to Cougaar core bug (Context not known). Sleeping 200ms then retrying...");
	  }
	  //null1exp.printStackTrace();
	  continue;
	}
	catch(Exception exp1){
	  exp1.printStackTrace();
	  continue;
	}
      }
      return null;
    }
    catch(Exception e){
      e.printStackTrace();
      return null;
    }
  }

  public ProtectedObject protectObject(Serializable object,
				       MessageAddress source,
				       MessageAddress target,
				       SecureMethodParam policy)
  throws GeneralSecurityException, IOException {
    ProtectedObject po = null;

    if (object == null) {
      throw new IllegalArgumentException("Object to protect is null");
    }
    if (source == null) {
      throw new IllegalArgumentException("Source not specified");
    }
    if (target == null) {
      throw new IllegalArgumentException("Target not specified");
    }
    if (policy == null) {
      throw new IllegalArgumentException("Policy not specified");
    }

    int method = policy.secureMethod;
    if (log.isDebugEnabled()) {
      log.debug("Protect object with policy: "
			 + method);
    }
    switch(method) {
    case SecureMethodParam.PLAIN:
      po = new ProtectedObject(policy, object);
      break;
    case SecureMethodParam.SIGN:
      po = sign(object, source, target, policy);
      break;
    case SecureMethodParam.ENCRYPT:
      po = encrypt(object, source, target, policy);
      break;
    case SecureMethodParam.SIGNENCRYPT:
      po = signAndEncrypt(object, source, target, policy);
      break;
    default:
      throw new GeneralSecurityException("Invalid policy");
    }
    return po;
  }

  public Object unprotectObject(MessageAddress source,
				MessageAddress target,
				ProtectedObject protectedObject,
				SecureMethodParam policy)
  throws GeneralSecurityException {
    Object theObject = null;
    if (protectedObject == null) {
      throw new IllegalArgumentException("Object to protect is null");
    }
    if (source == null) {
      throw new IllegalArgumentException("Source not specified");
    }
    if (target == null) {
      throw new IllegalArgumentException("Target not specified");
    }
    if (policy == null) {
      throw new IllegalArgumentException("Policy not specified");
    }

    // Check the policy.
    if (policy.secureMethod != protectedObject.getSecureMethod().secureMethod) {
      // The object does not comply with the policy
      throw new GeneralSecurityException("Object does not comply with the policy");
    }

    // Unprotect the message.
    int method = policy.secureMethod;
    if (log.isDebugEnabled()) {
      log.debug("Unprotect object with policy: "
			 + method);
    }
    switch(method) {
    case SecureMethodParam.PLAIN:
      theObject = protectedObject.getObject();
      break;
    case SecureMethodParam.SIGN:
      theObject = verify(source, target,
			 (PublicKeyEnvelope)protectedObject,
			 policy);
      break;
    case SecureMethodParam.ENCRYPT:
      theObject = decrypt(source, target,
			  (PublicKeyEnvelope)protectedObject,
			  policy);
      break;
    case SecureMethodParam.SIGNENCRYPT:
      theObject = decryptAndVerify(source, target,
				   (PublicKeyEnvelope)protectedObject,
				   policy);
      break;
    default:
      throw new GeneralSecurityException("Invalid policy");
    }
    return theObject;
  }

  private PublicKeyEnvelope sign(Serializable object,
				 MessageAddress source,
				 MessageAddress target,
				 SecureMethodParam policy)
    throws GeneralSecurityException, IOException {
    if (log.isDebugEnabled()) {
      log.debug("Sign object: " + source.toAddress()
			 + " -> " + target.toAddress());
    }
    // Find source certificate
    List senderList =
      keyRing.findCert(source.toAddress(),
		       KeyRingService.LOOKUP_LDAP | KeyRingService.LOOKUP_KEYSTORE);
    if (senderList.size() == 0) {
      throw new CertificateException("Unable to find sender certificate: " 
				     + source.toAddress());
    }
    X509Certificate sender = ((CertificateStatus)senderList.get(0)).getCertificate();

    SignedObject signedObject = sign(source.toAddress(), policy.signSpec, object);

    PublicKeyEnvelope pke =
      new PublicKeyEnvelope(sender, null, policy, null, null, signedObject);
    return pke;
  }

  private PublicKeyEnvelope encrypt(Serializable object,
				    MessageAddress source,
				    MessageAddress target,
				    SecureMethodParam policy)
    throws GeneralSecurityException, IOException {
    if (log.isDebugEnabled()) {
      log.debug("Encrypt object: " + source.toAddress()
			 + " -> " + target.toAddress());
    }
    PublicKeyEnvelope pke = null;

    /*generate the secret key*/
    int i = policy.symmSpec.indexOf("/");
    String a;
    a =  i > 0 ? policy.symmSpec.substring(0,i) : policy.symmSpec;
    SecureRandom random = new SecureRandom();
    KeyGenerator kg = KeyGenerator.getInstance(a);
    kg.init(random);
    SecretKey sk = kg.generateKey();
    // Encrypt session key
    SealedObject secret = asymmEncrypt(target.toAddress(), policy.asymmSpec, sk);
    SealedObject secretSender = asymmEncrypt(source.toAddress(), policy.asymmSpec, sk);

    SealedObject sealedMsg = symmEncrypt(sk, policy.symmSpec, object);
    // Find target certificate
    List receiverList =
      keyRing.findCert(target.toAddress(),
		       KeyRingService.LOOKUP_LDAP | KeyRingService.LOOKUP_KEYSTORE);
    if (receiverList.size() == 0) {
      throw new CertificateException("Unable to find target certificate: " 
				     + target.toAddress());
    }
    X509Certificate receiver = ((CertificateStatus)receiverList.get(0)).getCertificate();

    pke = new PublicKeyEnvelope(null, receiver, policy, secret, secretSender, sealedMsg);
    return pke;
  }

  private PublicKeyEnvelope signAndEncrypt(Serializable object,
					   MessageAddress source,
					   MessageAddress target,
					   SecureMethodParam policy)
    throws GeneralSecurityException, IOException {
    if (log.isDebugEnabled()) {
      log.debug("Sign&Encrypt object: " + source.toAddress()
			 + " -> " + target.toAddress());
    }

    PublicKeyEnvelope envelope = null;
    /* Generate the secret key */
    int i = policy.symmSpec.indexOf("/");
    String a;
    a =  i > 0 ? policy.symmSpec.substring(0,i) : policy.symmSpec;
    if(log.isDebugEnabled()) {
      log.debug("Secret Key Parameters: " + a);
    }
    SecureRandom random = new SecureRandom();
    KeyGenerator kg=KeyGenerator.getInstance(a);
    kg.init(random);
    SecretKey sk=kg.generateKey();

    SealedObject sessionKey = null;
    SealedObject sessionKeySender = null;
    SealedObject sealedObject = null;
    SignedObject signedObject = null;
      
    if(log.isDebugEnabled()) {
      log.debug("Encrypting session key");
    }
    // Encrypt session key
    sessionKey = asymmEncrypt(target.toAddress(), policy.asymmSpec, sk);
    // Encrypt session key with sender key
    sessionKeySender = asymmEncrypt(source.toAddress(), policy.asymmSpec, sk);

    if(log.isDebugEnabled()) {
      log.debug("Signing object");
    }
    // Sign object
    signedObject = sign(source.toAddress(), policy.signSpec, object);

    if(log.isDebugEnabled()) {
      log.debug("Encrypting object");
    }
    // Encrypt object
    sealedObject = symmEncrypt(sk, policy.symmSpec, signedObject);
      
    if(log.isDebugEnabled()) {
      log.debug("Looking up source certificate");
    }
    // Find source certificate
    List senderList =
      keyRing.findCert(source.toAddress(),
		       KeyRingService.LOOKUP_LDAP | KeyRingService.LOOKUP_KEYSTORE);
    if (senderList.size() == 0) {
      if(log.isErrorEnabled()) {
	log.error("Unable to find sender certificate: " 
		  + source.toAddress());
      }
      throw new CertificateException("Unable to find sender certificate: " 
				 + source.toAddress());
    }
    X509Certificate sender = ((CertificateStatus)senderList.get(0)).getCertificate();

    if(log.isDebugEnabled()) {
      log.debug("Looking up target certificate");
    }
    // Find target certificate
    List receiverList =
      keyRing.findCert(target.toAddress(),
		       KeyRingService.LOOKUP_LDAP | KeyRingService.LOOKUP_KEYSTORE);
    if (receiverList.size() == 0) {
      if(log.isErrorEnabled()) {
	log.error("Unable to find target certificate: " 
		  + target.toAddress());
      }
      throw new CertificateException("Unable to find target certificate: " 
				 + target.toAddress());
    }
    X509Certificate receiver =
      ((CertificateStatus)receiverList.get(0)).getCertificate();

    if(log.isDebugEnabled()) {
      log.debug("Creating secure envelope");
    }

    envelope = 
      new PublicKeyEnvelope(sender, receiver, policy,
			    sessionKey, sessionKeySender, sealedObject);
    return envelope;
  }

  /** Return the secret key of a protected object.
   * The session key should have been encrypted with both the source
   * and the target.
   */
  private SecretKey getSecretKey(MessageAddress source,
				 MessageAddress target,
				 PublicKeyEnvelope envelope,
				 SecureMethodParam policy) {
    SecretKey sk = null;
    /* The object was encrypted for a remote agent. However,
     * the remote agent may not be able to process that message, and
     * the source agent wants to get the object back.
     * There could be multiple reasons why the remote agent
     * did not process the object: the infrastructure was
     * not able to send the message, the remote agent did
     * not accept the message, etc.
     */
    sk = (SecretKey)
      asymmDecrypt(target.toAddress(), policy.asymmSpec,
		   envelope.getEncryptedSymmetricKey());
    if (sk == null) {
      // Try with the source address
      sk = (SecretKey)
	asymmDecrypt(source.toAddress(), policy.asymmSpec,
		     envelope.getEncryptedSymmetricKeySender());
    }
    return sk;
  }

  private Object decryptAndVerify(MessageAddress source,
				  MessageAddress target,
				  PublicKeyEnvelope envelope,
				  SecureMethodParam policy)
    throws GeneralSecurityException {
    if (log.isDebugEnabled()) {
      log.debug("Decrypt&verify object: " + source.toAddress()
			 + " -> " + target.toAddress());
    }

    // Retrieve the secret key, which was encrypted using the public key
    // of the target.
    if(log.isDebugEnabled()) {
      log.debug("Retrieving secret key");
    }
    SecretKey sk = getSecretKey(source, target, envelope, policy);
    if (sk == null) {
      if (log.isErrorEnabled()) {
	log.error("Error: unable to retrieve secret key");
      }
      return null;
    }

    if(log.isDebugEnabled()) {
      log.debug("Decrypting object");
    }
    // Decrypt the object
    SignedObject signedObject =
      (SignedObject)symmDecrypt(sk, (SealedObject)envelope.getObject());

    if(log.isDebugEnabled()) {
      log.debug("Verifying signature");
    }
    // Verify the signature
    Object o = null;
    try {
      o = verify(source.toAddress(), policy.signSpec, signedObject);
    }
    catch (CertificateException e) {
      if(log.isErrorEnabled()) {
	log.error("Signature verification failed");
      }
    }
    return o;
  }

  private Object decrypt(MessageAddress source,
			 MessageAddress target,
			 PublicKeyEnvelope envelope,
			 SecureMethodParam policy)
    throws GeneralSecurityException {
    if (log.isDebugEnabled()) {
      log.debug("Decrypt object: " + source.toAddress()
			 + " -> " + target.toAddress());
    }

    // Retrieving the secret key, which was encrypted using the public key
    // of the target.
    SecretKey sk = getSecretKey(source, target, envelope, policy);
    if (sk == null) {
      if (log.isErrorEnabled()) {
	log.error("Error: unable to retrieve secret key");
      }
      return null;
    }
    // Decrypt the object
    Object o =
      symmDecrypt(sk, (SealedObject)envelope.getObject());
    return o;
  }

  private Object verify(MessageAddress source,
			MessageAddress target,
			PublicKeyEnvelope envelope,
			SecureMethodParam policy)
    throws GeneralSecurityException {
    if (log.isDebugEnabled()) {
      log.debug("Verify object: " + source.toAddress()
			 + " -> " + target.toAddress());
    }
    // Verify the signature
    Object o = null;
    o = verify(source.toAddress(), policy.signSpec,
	       (SignedObject)envelope.getObject());
    return o;
  }

}

