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


package org.cougaar.core.security.dataprotection;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PrivilegedAction;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.security.crypto.NoValidKeyException;
import org.cougaar.core.security.crypto.SecureMethodParam;
import org.cougaar.core.security.monitoring.event.DataFailureEvent;
import org.cougaar.core.security.monitoring.event.FailureEvent;
import org.cougaar.core.security.monitoring.publisher.SecurityEventPublisher;
import org.cougaar.core.security.services.crypto.EncryptionService;
import org.cougaar.core.security.services.crypto.KeyRingService;
import org.cougaar.core.security.crypto.SignatureOutputStream;
import org.cougaar.core.service.DataProtectionKeyEnvelope;
import org.cougaar.core.service.LoggingService;

import sun.security.x509.X500Name;

public class DataProtectionOutputStream extends FilterOutputStream {
  private static LoggingService log;
  private static KeyRingService _keyRing;
  private static EncryptionService encryptionService;
  private static boolean debug = false;

  private String agent;
  private SecureMethodParam policy;
  private DataProtectionKeyImpl dpKey;
  private SignatureOutputStream _sigOut;
  private Cipher ci;
  private SecretKey skey;
  private DataProtectionKeyEnvelope _pke;

  /**
   * buffer size, when reached will flush to output stream
   */
  private static int buffersize = 30000;
//  private ByteArrayOutputStream bos = new ByteArrayOutputStream((int)(buffersize * 1.2));
  private ByteArrayOutputStream bos = getStream();
  private static ArrayList streams = new ArrayList();

  public DataProtectionOutputStream(OutputStream os,
                                    DataProtectionKeyEnvelope pke,
                                    String agent, ServiceBroker sb)
    throws GeneralSecurityException, IOException {
    super(os);

    init(sb);
    this.agent = agent;
    _pke = pke;

    DataProtectionKeyCollection keyCollection =
      (DataProtectionKeyCollection)pke.getDataProtectionKey();
    dpKey = (DataProtectionKeyImpl)keyCollection.get(0);
    policy = dpKey.getSecureMethod();

    if (policy.secureMethod == SecureMethodParam.ENCRYPT
        || policy.secureMethod == SecureMethodParam.SIGNENCRYPT) {
      // unprotect key
      String failureIfOccurred = DataFailureEvent.UNKNOWN_FAILURE;
      skey = null;
      try {
        failureIfOccurred = DataFailureEvent.SECRET_KEY_FAILURE;
        skey = getSecretKey();
	if (log.isDebugEnabled()) {
	  log.debug("skey class: " + skey.getClass().getName()
		    + " - Algorithm: " + skey.getAlgorithm() 
                    + " key length: " + skey.getEncoded().length
                    + " format: " + skey.getFormat());
	}
        //Cipher ci=Cipher.getInstance(policy.symmSpec);
        failureIfOccurred = DataFailureEvent.INVALID_POLICY;
        ci = encryptionService.getCipher(policy.symmSpec);
      }
      catch(GeneralSecurityException gsx) {
        publishDataFailure(failureIfOccurred, gsx.toString());
        throw gsx;
      }
      ci.init(Cipher.ENCRYPT_MODE,skey);
      this.out = new CipherOutputStream(this.out, ci);
    }

    if (policy.secureMethod == SecureMethodParam.SIGN ||
        policy.secureMethod == SecureMethodParam.SIGNENCRYPT) {
      // sign the results
      PrivateKey pkey = getPrivateKey(dpKey.getCertificateChain()[0]);
      String sigAlg = policy.signSpec;
      _sigOut = new SignatureOutputStream(this.out, sigAlg, pkey);
      this.out = _sigOut;
    }

    if(debug) {
      log.debug("Opening output stream " + agent + 
		" Policy: " + policy.secureMethod + " : " + new Date());
    }
    this.out = new DataOutputStream(this.out);
  }

  private void init(final ServiceBroker sb) {
    if (encryptionService == null) {
      // Get encryption service
      encryptionService = (EncryptionService)
        AccessController.doPrivileged(new PrivilegedAction() {      
            public Object run() {
              return sb.getService(this, EncryptionService.class, null);
            }
          });
      if (encryptionService == null) {
        throw new RuntimeException("Encryption service not available");
      }
    }

    if (log == null) {
      log = (LoggingService)
        sb.getService(this,  LoggingService.class, null);
      if(log != null) {
        debug = log.isDebugEnabled();
      }
    }
    if (_keyRing == null) {
      _keyRing = (KeyRingService)
        AccessController.doPrivileged(new PrivilegedAction() {      
            public Object run() {
              return sb.getService(this, KeyRingService.class, null);
            }
          });
      if (_keyRing == null) {
        throw new RuntimeException("KeyRingService is not available");
      }
    }
  }

  private SecretKey getSecretKey()
    throws GeneralSecurityException
  {
    return encryptionService.decryptSecretKey(
      policy.asymmSpec, (byte[])dpKey.getObject(),
      policy.symmSpec, dpKey.getCertificateChain()[0]);
  }

  private PrivateKey getPrivateKey(final X509Certificate cert)
    throws GeneralSecurityException {
    PrivateKey pk = (PrivateKey)
      AccessController.doPrivileged(new PrivilegedAction() {
          public Object run(){
            return _keyRing.findPrivateKey(cert);
          }
        });
    if (pk == null) {
      String message = "Unable to get private key of " +
        cert + " -- does not exist.";
      throw new NoValidKeyException(message);
    }
    return pk;
  }

  public void write(int b) throws IOException {
    bos.write(b);
    if (bos.size() > buffersize) {
      writeChunk();
    }
  }

  public void write(byte b[]) throws IOException {
    bos.write(b);
    if (bos.size() > buffersize) {
      writeChunk();
    }
  }

  public void write(byte b[], int offset, int len) throws IOException {
    bos.write(b, offset, len);
    if (bos.size() > buffersize) {
      writeChunk();
    }
  }

  public synchronized void writeChunk() throws IOException {
    ((DataOutputStream) this.out).writeInt(bos.size());
    if (debug) {
      log.debug("Writing " + bos.size() + " to stream");
    }

    bos.writeTo(this.out);
    bos.reset();
  }

  public synchronized void close() throws IOException {
    if (this.out == null) {
      return;
    }
    if (bos.size() > 0) {
      writeChunk();
    }
    ((DataOutputStream) this.out).writeInt(0);
    if (_sigOut != null) {
      byte[] sig = _sigOut.writeSignature();

      DataProtectionKeyCollection keyCollection =
        (DataProtectionKeyCollection)_pke.getDataProtectionKey();
      long timestamp = System.currentTimeMillis();
      keyCollection.setTimestamp(timestamp);
      keyCollection.setSignature(sig);      
      _pke.setDataProtectionKey(keyCollection);

      String sendSignature = System.getProperty("org.cougaar.core.security.dataprotection.sendSignature", "true");
      if (sendSignature.equals("true")) {
        if (log.isDebugEnabled()) {
          log.debug("trying to send session key using relay " + agent + " at time " + timestamp);
        }
//        KeyRecoveryRequestHandler.printBytes(sig, 0, 10, log);

        for (int i = 1; i < keyCollection.size(); i++) {
          DataProtectionKeyImpl pmKey = (DataProtectionKeyImpl)
            keyCollection.get(i);
          X509Certificate pmCert = pmKey.getCertificateChain()[0];
          String pmName = null;
          try {
            pmName = new X500Name(pmCert.getSubjectDN().getName()).getCommonName();
          }
          catch (IOException iox) {
            if (log.isWarnEnabled()) {
              log.warn("Failed to get common name for PM cert: " + pmCert);
              continue;
            }
          }
//          RelaySessionKey.getInstance().relaySessionKey(keyCollection,pmName,agent);
          SessionKeySenderPlugin.sendSessionKey(agent, keyCollection, pmName);
        }
      }
    }
    super.close();
    if (ci != null) {
      encryptionService.returnCipher(policy.symmSpec, ci);
      ci = null;
    }
    returnStream(bos);

    this.out = null;

    DataProtectionStatus.addOutputStatus(
      agent, DataProtectionStatus.OUTPUT_COMPLETE);
  }


  public static int getBufferSize() {
    return buffersize;
  }

  public static void setBufferSize(int size) {
    buffersize = size;
  }

  /**
   * publish a data protection failure idmef alert
   */
  private void publishDataFailure(String reason, String data) {
    FailureEvent event = new DataFailureEvent(agent,
                                              agent,
                                              reason,
                                              data);
    SecurityEventPublisher.publishEvent(event);
  }

  private static ByteArrayOutputStream getStream() {
    synchronized (streams) {
      if (!streams.isEmpty()) {
        return (ByteArrayOutputStream) streams.remove(streams.size() - 1);
      }
    }

    return new ByteArrayOutputStream((int)(buffersize * 1.2));
  }

  private static void returnStream(ByteArrayOutputStream stream) {
    stream.reset();
    synchronized (streams) {
      streams.add(stream);
    }
  }
}
