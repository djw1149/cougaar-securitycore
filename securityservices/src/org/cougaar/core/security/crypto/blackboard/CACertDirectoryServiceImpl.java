/*
 * <copyright>
 *  Copyright 1997-2003 Cougaar Software, Inc.
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

package org.cougaar.core.security.crypto.blackboard;

import java.util.*;
import java.io.*;
import java.security.cert.X509Certificate;
import java.security.*;

// Cougaar core services
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceAvailableListener;
import org.cougaar.core.component.ServiceAvailableEvent;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.blackboard.BlackboardClient;

// Cougaar security services
import org.cougaar.core.security.services.util.CACertDirectoryService;
import org.cougaar.core.security.crypto.CertificateRevocationStatus;
import org.cougaar.core.security.crypto.CertificateType;
import org.cougaar.core.security.crypto.CertificateUtility;
import org.cougaar.core.security.naming.CertificateEntry;

public class CACertDirectoryServiceImpl
  implements CACertDirectoryService, BlackboardClient
{
  private ServiceBroker _serviceBroker;
  private BlackboardService _blackboardService;
  private LoggingService _log;
  //private CertificateBlackboardStore _certStore = new CertificateBlackboardStore();
  private Hashtable _certStore = new Hashtable();

  public CACertDirectoryServiceImpl(ServiceBroker sb) {
    _serviceBroker = sb;
    _log = (LoggingService)_serviceBroker.getService
      (this, LoggingService.class, null);

    BlackboardService bbs = (BlackboardService)
      _serviceBroker.getService(this,
			       BlackboardService.class,
			       null);
    if(bbs==null) {
      if (_log.isDebugEnabled()) {
	_log.debug("Adding service listner for blackboard service :");
      }
      _serviceBroker.addServiceListener(new BlackboardServiceAvailableListener());
    }
    else {
      if (_log.isDebugEnabled()) {
	_log.debug("acquired blackboard service :");
      }
      setBlackboardService();
    }
  }

  /**
   * Publish a certificate (managed by a CA) in the blackboard.
   * The certificate is assumed to be valid.
   */
  public void publishCertificate(X509Certificate cert, int type, PrivateKey privatekey) {
    if (_log.isDebugEnabled()) {
      _log.debug("Publish certificate: " + cert.toString());
    }
    // Assume the certificate is valid if we publish it.
    CertificateRevocationStatus certStatus = CertificateRevocationStatus.VALID;
    CertificateType certType = null;
    switch (type) {
    case CertificateUtility.CACert:
      certType = CertificateType.CERT_TYPE_CA;
      break;
    default:
      certType = CertificateType.CERT_TYPE_END_ENTITY;
    }
    CertificateEntry certEntry = new CertificateEntry(cert,
						      certStatus, certType);
    publishCertificate(certEntry);
  }

  /**
   * Publish a certificate (managed by a CA) in the blackboard
   */
  public void publishCertificate(CertificateEntry certEntry) {
    String dnname = certEntry.getCertificate().getSubjectDN().getName();
    if (_log.isDebugEnabled()) {
      _log.debug("Publish certificate: " + dnname);
    }

    synchronized(_certStore) {
      List certList = (List)_certStore.get(dnname);
      if (certList == null) {
	certList = new ArrayList();
      }
      // indexOf only compares by reference, but publishCertificate(Certificate...)
      // creates a new one every time it is called
      //int index = certList.indexOf(certEntry);
      int index = -1;
      PublicKey pubKey = certEntry.getCertificate().getPublicKey();
      for (int i = 0; i < certList.size(); i++) {
        CertificateEntry entry = (CertificateEntry)certList.get(i);
        if (entry.getCertificate().getPublicKey().equals(pubKey)) {
          // Entry is being modified
          certList.set(index, certEntry);
          index = i;
          break;
        }
      }
      if (index == -1) {
	// New entry
	certList.add(certEntry);
      }
      /*
      else {
	// Entry is being modified
	certList.set(index, certEntry);
      }
      */
      // Add entry for DN and Unique ID so that we can search using both strings
      _certStore.put(dnname, certList);
      _certStore.put(certEntry.getUniqueIdentifier(), certEntry);
    }
    updateBlackBoard();
  }

  private synchronized void updateBlackBoard() {
    if (_blackboardService == null) {
      if (_log.isDebugEnabled()) {
        _log.debug("Blackboard service not yet available");
      }
      return;
    }

    try {
      _blackboardService.openTransaction();
      _blackboardService.publishChange(_certStore);
    }
    catch (Exception e) {
      _log.error("Failed to add to blackboard: ", e);
    }
    finally {
      _blackboardService.closeTransaction();
    }
    try {
      // Persist the blackboard so that keys generated by the CA are not accidentaly lost
      // by an agent crash.
      _blackboardService.persistNow();
    }
    catch (Exception e) {
      _log.info("Persistence is not enabled. Certificates will not be persisted");
    }
  }

  /**
   * Return a list of all the certificates managed by the CA, including the CA itself.
   */
  public List getAllCertificates() {
    if (_log.isDebugEnabled()) {
      _log.debug("Get all certificates, cache size " + _certStore.size());
    }
    Enumeration enum = _certStore.elements();
    List completeList = new ArrayList();
    while (enum.hasMoreElements()) {
      Object o = enum.nextElement();
      if (o instanceof List) {
	completeList.addAll((List)o);
      }
    }
    return completeList;
  }

  /**
   * Find a list of certificates matching a distinguished name.
   * @param identifier - The distinguished name of the certificate to look for.
   */
  public List findCertByDistinguishedName(String distinguishedName) {
    if (_log.isDebugEnabled()) {
      _log.debug("Get certificates for " + distinguishedName);
    }
    List certList = (List)_certStore.get(distinguishedName);
    return certList;
  }

  /**
   * Find a certificate given its unique identifier.
   * @param identifier - The unique identifier of the certificate to look for.
   */
  public CertificateEntry findCertByIdentifier(String uniqueIdentifier) {
    if (_log.isDebugEnabled()) {
      _log.debug("Get certificates for " + uniqueIdentifier);
    }
    CertificateEntry certEntry = (CertificateEntry)_certStore.get(uniqueIdentifier);
    return certEntry;
  }

  /** Set the blackboard service and initialize the Certificate
   *  Blackboard Store.
   */
  private final synchronized void setBlackboardService() {
    _blackboardService = (BlackboardService)
      _serviceBroker.getService(this,BlackboardService.class, null);

    Collection collection = null;
    //CertificateBlackboardStore bbstore = null;
    Hashtable bbstore = null;
    if(_blackboardService.didRehydrate()) {
      // Retrieve persisted instance of the Certificate Blackboard Store.
      // There should be only one instance of the the Certificate Store.
      collection =
	_blackboardService.query(new CertificateBlackboardStorePredicate());
      if (collection.size() > 1) {
	throw new RuntimeException
	  ("Can support at most one CertificateBlackboardStore. Current items:" + collection.size());
      }
      if (collection.isEmpty()) {
	collection = null;
      }
      else {
	Iterator it = collection.iterator();
	//bbstore = (CertificateBlackboardStore) it.next();
	bbstore = (Hashtable) it.next();
      }
    }
    if (!_blackboardService.didRehydrate() || collection == null) {
      if (_log.isDebugEnabled()) {
        _log.debug("adding cert store to BB");
      }
      try {
        _blackboardService.openTransaction();
        _blackboardService.publishAdd(_certStore);
      }
      catch (Exception e) {
        _log.error("Failed to add to blackboard: ", e);
      }
      finally {
        _blackboardService.closeTransaction();
      }
      try {
        // Persist the blackboard so that keys generated by the CA are not accidentaly lost
        // by an agent crash.
        _blackboardService.persistNow();
      }
      catch (Exception e) {
        _log.info("Persistence is not enabled. Certificates will not be persisted");
      }
    }
    else {
      if (bbstore != null) {
        // need to handle collisions
        _certStore.putAll(bbstore);
        if (_certStore.size() != 0) {
          updateBlackBoard();
          if (_log.isDebugEnabled()) {
            _log.debug("updating cert store to BB");
          }

        }
      }
      else {
        _log.warn("Found predicate in blackboard but no cert store object.");
      }
    }

  }

  /** Service listener for the Blackboard Service.
   *  Set the blackboard service when it becomes available.
   */
  private class BlackboardServiceAvailableListener implements ServiceAvailableListener {
    public void serviceAvailable(ServiceAvailableEvent ae) {
      Class sc = ae.getService();
      if(org.cougaar.core.service.BlackboardService.class.isAssignableFrom(sc)) {
	  _log.debug("BB Service is now available");
	if(_blackboardService==null){
	  setBlackboardService();
	}
      }
    }
  }

  private class CertificateBlackboardStorePredicate implements UnaryPredicate {
    public boolean execute(Object o) {
      boolean ret = false;
      //if (o instanceof CertificateBlackboardStore) {
      if (o instanceof Hashtable) {
	return true;
      }
      return ret;
    }
  }

  /*
  private class CertificateBlackboardStore extends Hashtable implements Serializable {
  }
  */


  /** ********************************************************************
   *  BlackboardClient implementation
   */

  // odd BlackboardClient method:
  public String getBlackboardClientName() {
    return "CACertDirectoryService";
  }

  // odd BlackboardClient method:
  public long currentTimeMillis() {
    throw new UnsupportedOperationException(
        this+" asked for the current time???");
  }

  // unused BlackboardClient method:
  public boolean triggerEvent(Object event) {
    // if we had Subscriptions we'd need to implement this.
    //
    // see "ComponentPlugin" for details.
    throw new UnsupportedOperationException(
        this+" only supports Blackboard queries, but received "+
        "a \"trigger\" event: "+event);
  }

}
