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

package org.cougaar.core.security.crypto;

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.io.Serializable;
import javax.crypto.SealedObject;
import java.security.cert.X509Certificate;

public class PublicKeyEnvelope
  extends ProtectedObject
{
  /** The encrypted symmetric key used to encrypt an object.
   *  This key is encrypted with the public key of the target.
   */
  private SealedObject encryptedSymmetricKey;

  /** The encrypted symmetric key used to encrypt an object.
   *  This key is encrypted with the public key of the sender.
   *  It is the same session key as the one in encryptedSymmetricKey,
   *  however it is encrypted with the sender. This allows the
   *  sender to recover the object if necessary.
   */
  private SealedObject encryptedSymmetricKeySender;

  /** The certificate used to encrypt the symmetric key.
   */
  private X509Certificate receiver;

  /** The certificate corresponding to the private key that was used
      to sign the object.
   */
  private X509Certificate sender;

  public PublicKeyEnvelope(X509Certificate asender,
			   X509Certificate areceiver,
			   SecureMethodParam policy,
			   SealedObject sKey,
			   SealedObject sKeySender,
			   Object encObj) {
    super(policy, encObj);
    encryptedSymmetricKey = sKey;
    encryptedSymmetricKeySender = sKeySender;
    receiver = areceiver;
    sender = asender;
  }

  public SealedObject getEncryptedSymmetricKey() {
    return encryptedSymmetricKey;
  }

  public SealedObject getEncryptedSymmetricKeySender() {
    return encryptedSymmetricKeySender;
  }

  public X509Certificate getReceiver() {
    return receiver;
  }

  public X509Certificate getSender() {
    return sender;
  }
}
