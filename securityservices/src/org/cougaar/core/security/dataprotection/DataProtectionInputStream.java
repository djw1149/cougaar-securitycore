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

package org.cougaar.core.security.dataprotection;

import java.io.*;
import java.util.*;
import java.security.*;
import javax.crypto.*;
import java.security.cert.*;

// Cougaar core infrastructure
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.LoggingService;

// overlay
import org.cougaar.core.service.*;

// Security Service
import org.cougaar.core.security.services.crypto.*;
import org.cougaar.core.security.crypto.*;
import org.cougaar.core.security.util.*;
import org.cougaar.core.security.monitoring.publisher.EventPublisher;
import org.cougaar.core.security.monitoring.event.FailureEvent;
import org.cougaar.core.security.monitoring.event.DataFailureEvent;
import org.cougaar.core.security.monitoring.plugin.DataProtectionSensor;

public class DataProtectionInputStream extends FilterInputStream {
  private LoggingService log;
  private ServiceBroker serviceBroker;
  private EncryptionService encryptionService;

  private DataProtectionKeyImpl dpKey;
  private String agent;

  private Cipher ci = null;
  private SecureMethodParam policy = null;

  private SecretKey skey = null;

  private SignatureInputStream _sigIn = null;
  private ChunkInputStream _chunkIn = null;

  private boolean debug = false;

  // used to publish data failures
  private EventPublisher eventPublisher;

  public DataProtectionInputStream(InputStream is,
                                   DataProtectionKeyEnvelope pke,
                                   String agent,
                                   ServiceBroker sb)
    throws GeneralSecurityException, IOException {
    super(is);

    serviceBroker = sb;
    // Get encryption service
    encryptionService = (EncryptionService)
      serviceBroker.getService(this,
			       EncryptionService.class,
			       null);
    if (encryptionService == null) {
      throw new RuntimeException("Encryption service not available");
    }

    log = (LoggingService)
      serviceBroker.getService(this,
			       LoggingService.class, null);
    if(log != null) {
      debug = log.isDebugEnabled();
    }

    this.agent = agent;
    DataProtectionKeyCollection keyCollection =
      (DataProtectionKeyCollection)pke.getDataProtectionKey();
    dpKey = (DataProtectionKeyImpl)keyCollection.get(0);
    policy = dpKey.getSecureMethod();

    skey = getSecretKey();
    if (skey == null) {
      publishDataFailure(DataFailureEvent.SECRET_KEY_FAILURE,
			 "Cannot get data protection key");
      throw new GeneralSecurityException("Cannot get data protection key");
    }

    if (policy.secureMethod == SecureMethodParam.ENCRYPT
        || policy.secureMethod == SecureMethodParam.SIGNENCRYPT) {
      try {
        ci=encryptionService.getCipher(policy.symmSpec);
      } catch(GeneralSecurityException gsx) {
        publishDataFailure(DataFailureEvent.INVALID_POLICY, gsx.toString());
        throw gsx;
      }
      ci.init(Cipher.DECRYPT_MODE,skey);
      this.in = new CipherInputStream(this.in, ci);
    }

    _chunkIn = new ChunkInputStream(this.in); 
    this.in = _chunkIn;

    if (policy.secureMethod == SecureMethodParam.SIGN ||
        policy.secureMethod == SecureMethodParam.SIGNENCRYPT) {
      String sigAlg = policy.signSpec;
      PublicKey pkey = dpKey.getCertificateChain()[0].getPublicKey();
      _sigIn = new SignatureInputStream(this.in, sigAlg, pkey);
      this.in = _sigIn;
    }

    if(debug) {
      log.debug("Opening inputStream: " + agent + " : " + new Date());
    }
  }

  public synchronized void close() throws IOException {
    log.debug("Closing...");
    // read until the end of file
    if (this.in == null) {
      log.debug("Closed already");
      return;
    }

    if (_sigIn != null) {
      // read to the end
      while (read() != -1) {
      }
      _chunkIn.resetEnd();
      try {
        _sigIn.verifySignature();
      } catch (SignatureException e) {
        log.debug("Digest does not match");
        publishDataFailure(DataFailureEvent.VERIFY_DIGEST_FAILURE, 
                           e.toString());
        throw new IOException("Digest does not match");
      }
    }

    super.close();
    if (ci != null) {
      encryptionService.returnCipher(policy.symmSpec, ci);
      ci = null;
    }
    this.in = null;
    log.debug("Closed");
  }


  private SecretKey getSecretKey()
    throws GeneralSecurityException
  {
    return (SecretKey)encryptionService.asymmDecrypt(agent,
                                                     policy.asymmSpec, (SealedObject)dpKey.getObject());
  }

  /*
  public void addPublisher(EventPublisher publisher) {
    if(eventPublisher == null) {
      eventPublisher = publisher;
    }
  }
  */

  /**
   * publish a data protection failure idmef alert
   */
  private void publishDataFailure(String reason, String data) {
    FailureEvent event = new DataFailureEvent(agent,
                                              agent,
                                              reason,
                                              data);
    /*
    if(eventPublisher != null) {
      eventPublisher.publishEvent(event);
    }
    else {
      if(debug) {
        log.debug("EventPublisher uninitialized, unable to publish event:\n" + event);
      }
    }
    */
    DataProtectionSensor.publishEvent(event);
  }

  private static class ChunkInputStream extends FilterInputStream {
    boolean _eof = false;
    private boolean _isClosed = false;
    private boolean _isReset = false;

    private byte[] _byteBuf = new byte[40000];
    private ByteArrayInputStream _buf =
      new ByteArrayInputStream(_byteBuf, 0, 0);

    public ChunkInputStream(InputStream is) {
      super(new DataInputStream(is));
    }

    public int read() throws IOException {
      if (_isReset) { return super.read(); }
      if (_eof) {
        return -1;
      }
      refillBuf();
      return _buf.read();
    }

    public int read(byte[] b, int off, int len) throws IOException {
      if (_isReset) {
        return super.read(b, off, len);
      }
      if (_eof) {
        return -1;
      }
      refillBuf();
      return _buf.read(b, off, len);
    }

    public int read(byte[] b) throws IOException {
      return read(b, 0, b.length);
    }

    public synchronized void refillBuf() throws IOException {
      if (_buf.available() == 0 && !_eof) {
        int len = ((DataInputStream) this.in).readInt();
        if (len == 0) {
          _eof = true;
          return;
        }
        if (_byteBuf.length < len) {
          _byteBuf = new byte[len];
        }
	    
        ((DataInputStream)this.in).readFully(_byteBuf, 0, len);
        _buf = new ByteArrayInputStream(_byteBuf, 0, len);
      }
    }

    public synchronized void resetEnd() throws IOException {
      _isReset = true;
    }

    public int available() throws IOException {
      return _buf.available();
    }
    
    public boolean markSupported() {
      return false;
    }

  }
}
