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


package org.cougaar.core.security.monitoring.plugin;


import edu.jhuapl.idmef.AdditionalData;
import edu.jhuapl.idmef.Alert;
import edu.jhuapl.idmef.Classification;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.security.crypto.CertificateCache;
import org.cougaar.core.security.crypto.CertificateStatus;
import org.cougaar.core.security.crypto.CertificateUtility;
import org.cougaar.core.security.monitoring.blackboard.Event;
import org.cougaar.core.security.policy.PersistenceManagerPolicy;
import org.cougaar.core.security.services.crypto.KeyRingService;
import org.cougaar.core.security.services.util.PersistenceMgrPolicyService;
import org.cougaar.core.security.util.SharedDataRelay;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.Constraint;
import org.cougaar.planning.ldm.plan.Disposition;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.NewConstraint;
import org.cougaar.planning.ldm.plan.NewPrepositionalPhrase;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.NewWorkflow;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Verb;
import org.cougaar.planning.ldm.plan.Workflow;
import org.cougaar.util.UnaryPredicate;

import java.security.cert.X509Certificate;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;


/**
 * Subscribes to IDMEF Events identifiying a compromised agent, node or host
 *
 * @author ttschampel
 */
public class MnrCompromisePlugin extends ComponentPlugin {
  /** Plugin name */
  private static final String pluginName = "MnrCompromisePlugin";
  private static final String RECOVER_VERB = "RecoverFromFailure";
  private static final String REVOKE_SESSION_KEYS_VERB = CompromiseBlackboard.REVOKE_SESSION_KEY_VERB;
  private static final String REVOKE_AGENT_CERT_VERB = CompromiseBlackboard.REVOKE_AGENT_CERT_VERB;
  /** Subscription to appropriate IDMEF Events */
  private IncrementalSubscription eventSubscription = null;
  /** Subscription to shared data relay to the ca agent */
  private IncrementalSubscription caRelaySubs = null;
  /** Subscription to shared data relay to the persistence manager */
  private IncrementalSubscription pmRelaySubs = null;
  /** Subscription to recover tasks */
  private IncrementalSubscription recoverTasks = null;
  /** Predicate for Recover Tasks */
  private UnaryPredicate recoverPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task t = (Task) o;
        return (t.getVerb() != null) && (t.getVerb().toString().equals(REVOKE_SESSION_KEYS_VERB) || t.getVerb().toString().equals(REVOKE_AGENT_CERT_VERB));
      }

      return false;
    }
  };

  /** Subscription to "Done" revoke session key relays */
  private IncrementalSubscription remoteAgentDoneSubs = null;
  /** Predicate for "done" revoke session key relays */
  private UnaryPredicate removeAgentDonePredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof SharedDataRelay) {
        SharedDataRelay sdr = (SharedDataRelay) o;
        if ((sdr.getResponse() != null) && sdr.getResponse() instanceof Task) {
          Task t = (Task) sdr.getResponse();
          return (t.getVerb() != null) && (t.getVerb().toString().equals(REVOKE_SESSION_KEYS_VERB) || t.getVerb().toString().equals(REVOKE_AGENT_CERT_VERB));
        }
      }

      return false;
    }
  };

  /** KeyRingService */
  private KeyRingService keyRingService = null;
  /** UIDService */
  private UIDService uidService = null;
  /** Domain Service */
  private DomainService domainService = null;
  /** Logging Service */
  private LoggingService logging = null;
  /** Predicate to evenets */
  private UnaryPredicate eventPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Event) {
        Event event = (Event) o;
        if ((event.getEvent() != null) && event.getEvent() instanceof Alert) {
          Alert alert = (Alert) event.getEvent();
          if (alert.getClassifications() != null) {
            Classification[] classifications = alert.getClassifications();
            for (int i = 0; i < classifications.length; i++) {
              if ((classifications[i].getName() != null) && classifications[i].getName().equals(CompromiseBlackboard.CLASSIFICATION)) {
                return true;
              }
            }
          }
        }
      }

      return false;
    }
  };

  private IncrementalSubscription finishedTaskSubs = null;
  private UnaryPredicate finshedPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Disposition) {
        Disposition d = (Disposition) o;
        Task t = d.getTask();
        return (t.getVerb() != null) & (t.getVerb().toString().equals(CompromiseBlackboard.REVOKE_AGENT_CERT_VERB) || t.getVerb().toString().equals(CompromiseBlackboard.REVOKE_SESSION_KEY_VERB));
      }

      return false;
    }
  };

  /** PersistenceMgrPolicyService */
  PersistenceMgrPolicyService pmPolicyService = null;

  /**
   * Set logging service
   *
   * @param service
   */
  public void setLoggingService(LoggingService service) {
    this.logging = service;
  }


  /**
   * Load component
   */
  public void load() {
    super.load();
    //load PersistenceMgrPolicyService
    pmPolicyService = (PersistenceMgrPolicyService) this.getServiceBroker().getService(this, PersistenceMgrPolicyService.class, null);
    if (pmPolicyService == null) {
      if (logging.isErrorEnabled()) {
        logging.error("PersistenceMgrPolicyService is null!");
      }
    }

    keyRingService = (KeyRingService) this.getServiceBroker().getService(this, KeyRingService.class, null);

    this.domainService = (DomainService) this.getServiceBroker().getService(this, DomainService.class, null);
    this.uidService = (UIDService) this.getServiceBroker().getService(this, UIDService.class, null);
  }


  /**
   * Setup subscriptions
   */
  protected void setupSubscriptions() {
    this.eventSubscription = (IncrementalSubscription) getBlackboardService().subscribe(eventPredicate);
    this.recoverTasks = (IncrementalSubscription) getBlackboardService().subscribe(this.recoverPredicate);
    this.remoteAgentDoneSubs = (IncrementalSubscription) getBlackboardService().subscribe(this.removeAgentDonePredicate);
    this.finishedTaskSubs = (IncrementalSubscription) getBlackboardService().subscribe(this.finshedPredicate);
  }


  /**
   * Process Subscriptions
   */
  protected void execute() {
    if (logging.isDebugEnabled()) {
      logging.debug(pluginName + " executing");
    }

    checkForCompromises();
    //check for pm && ca related tasks
    checkForNewTasks();
    checkForCompletedTasks();


    //Check for completed tasks to move to next one 
    checkForFinishedTasks();
    //checkForCompletedProcesses();
  }


  /**
   * Check for fully completed tasks and move to next one in workflow
   */
  private void checkForFinishedTasks() {
    Enumeration enumeration = finishedTaskSubs.getAddedList();
    while (enumeration.hasMoreElements()) {
      Disposition disp = (Disposition) enumeration.nextElement();
      Task t = disp.getTask();
      Workflow wf = t.getWorkflow();
      Constraint c = wf.getNextPendingConstraint();
      if (logging.isDebugEnabled()) {
        logging.debug("Next constraint:" + c);
      }

      if (c != null) {
        Task nexttask = c.getConstrainedTask();
        if (nexttask.getPlanElement() != null) {
          getBlackboardService().publishRemove(nexttask.getWorkflow().getParentTask());
        } else {
          getBlackboardService().publishAdd(nexttask);
        }
      }
    }
  }


  /**
   * Check for revoke session key complete from PersistenceManager or revoke
   * agent cert from CA
   */
  private void checkForCompletedTasks() {
    Enumeration enumeration = remoteAgentDoneSubs.getChangedList();
    while (enumeration.hasMoreElements()) {
      SharedDataRelay sdr = (SharedDataRelay) enumeration.nextElement();
      Task task = (Task) sdr.getContent();

      if (logging.isDebugEnabled()) {
        if (task.getVerb().toString().equals(REVOKE_SESSION_KEYS_VERB)) {
          logging.debug("PersistenceManager finished " + task);
        } else if (task.getVerb().toString().equals(REVOKE_AGENT_CERT_VERB)) {
          logging.debug("CA finished " + task);
        }
      }

      PlanningFactory ldm = (PlanningFactory) domainService.getFactory("planning");
      AspectValue[] values = new AspectValue[1];
      values[0] = AspectValue.newAspectValue(AspectType.END_TIME, (double) System.currentTimeMillis());
      AllocationResult allocResult = ldm.newAllocationResult(1.0, true, values);
      Disposition disp = ldm.createDisposition(task.getPlan(), task, allocResult);
      getBlackboardService().publishAdd(disp);
      getBlackboardService().publishRemove(sdr);
    }
  }


  /**
   * Check for newly added Tasks added to blackboard
   */
  private void checkForNewTasks() {
    Enumeration enumeration = recoverTasks.getAddedList();
    while (enumeration.hasMoreElements()) {
      Task theTask = (Task) enumeration.nextElement();
      PrepositionalPhrase pp = theTask.getPrepositionalPhrase(CompromiseBlackboard.FOR_AGENT_PREP);
      String agent = (String) pp.getIndirectObject();
      PrepositionalPhrase pp2 = theTask.getPrepositionalPhrase(CompromiseBlackboard.COMPROMISE_TIMESTAMP_PREP);
      if (theTask.getVerb().toString().equals(REVOKE_SESSION_KEYS_VERB)) {
        long timestamp = ((Long) pp2.getIndirectObject()).longValue();
        if (logging.isDebugEnabled()) {
          logging.debug("Send message to PersistenceManager to revoke session key for " + agent + " for compromise at " + new Date(timestamp));
        }


        //Get Location of PersitenceManager
        PersistenceManagerPolicy[] pmPolicies = pmPolicyService.getPolicies();
        if (logging.isDebugEnabled()) {
          logging.debug("Policy size:" + pmPolicies.length);
        }

        String pmAgentName = null;
        for (int i = 0; i < pmPolicies.length; i++) {
          PersistenceManagerPolicy pmPolicy = pmPolicies[i];
          if (logging.isDebugEnabled()) {
            logging.debug(pmPolicy.pmDN + ":" + pmPolicy.pmUrl + ":" + pmPolicy.getName());
          }

          //get agent name from url
          String temp = pmPolicy.pmUrl.substring(pmPolicy.pmUrl.indexOf("$") + 1, pmPolicy.pmUrl.length());
          pmAgentName = temp.substring(0, temp.indexOf("/"));
		
		 if(logging.isDebugEnabled()){
		 	logging.debug("Sending revoke session key for agent relay to PersistenceManger at agent:" + pmAgentName);
		 }
          MessageAddress source = this.getAgentIdentifier();
          MessageAddress target = MessageAddress.getMessageAddress(pmAgentName);
          SharedDataRelay relay = new SharedDataRelay(uidService.nextUID(), source, target, theTask, null);
          getBlackboardService().publishAdd(relay);

        }
      } else {
        if (logging.isDebugEnabled()) {
          logging.debug("Send message to CA to revoke agent cert for " + agent);
        }


        //Get CA info
        //for now end to caAgent
        MessageAddress source = this.getAgentIdentifier();
        MessageAddress target = MessageAddress.getMessageAddress("caAgent");
        SharedDataRelay relay = new SharedDataRelay(uidService.nextUID(), source, target, theTask, null);
        getBlackboardService().publishAdd(relay);
      }
    }
  }


  /**
   * Check for compromises, if there are, create a workflow of tasks to
   * complete in order to act accordingly
   */
  private void checkForCompromises() {
    Enumeration enumeration = this.eventSubscription.getAddedList();
    while (enumeration.hasMoreElements()) {
      Event event = (Event) enumeration.nextElement();
      Alert alert = (Alert) event.getEvent();
      AdditionalData[] data = alert.getAdditionalData();
      String scope = null;
      long timestamp = 0;
      String sourceNode = null;
      String sourceAgent = null;
      String sourceHost = null;
      for (int i = 0; i < data.length; i++) {
        AdditionalData adata = data[i];
        String dType = adata.getType();
        String dData = adata.getAdditionalData();

        if ((dType != null) && dType.equals("scope")) {
          scope = dData;
        } else if ((dType != null) && dType.equals("compromise timestamp")) {
          timestamp = Long.parseLong(dData);
        } else if ((dType != null) && dType.equals("sourceAgent")) {
          sourceAgent = dData;
        } else if ((dType != null) && dType.equals("sourceNode")) {
          sourceNode = dData;
        } else if ((dType != null) && dType.equals("sourceHost")) {
          sourceHost = dData;
        }
      }


      if (logging.isDebugEnabled()) {
        logging.debug("Got compromise of scope :" + scope + " and time :" + new Date(timestamp) + " from " + sourceHost + ":" + sourceNode + ":" + sourceAgent);
      }

      if (scope == null) {
        if (logging.isErrorEnabled()) {
          logging.error("Error, received a compromise idmef event without a defined scope!");
        }
      } else {
        ArrayList agentsToBeRevoked = new ArrayList();
        PlanningFactory ldm = (PlanningFactory) domainService.getFactory("planning");
        NewTask rootTask = ldm.newTask();
        rootTask.setVerb(Verb.getVerb(RECOVER_VERB));

        NewWorkflow nwf = ldm.newWorkflow();
        nwf.setParentTask(rootTask);

        AllocationResult estResult = null;
        if (scope.equals(CompromiseBlackboard.AGENT_COMPROMISE_TYPE)) {
          if (logging.isDebugEnabled()) {
            logging.debug("revoking agent only");
          }

          agentsToBeRevoked.add(sourceAgent);
        } else if (scope.equals(CompromiseBlackboard.NODE_COMPROMISE_TYPE)) {
          if (logging.isDebugEnabled()) {
            logging.debug("revoke all agents in node");
          }
        } else if (scope.equals(CompromiseBlackboard.HOST_COMPROMISE_TYPE)) {
          if (logging.isDebugEnabled()) {
            logging.debug("revoke all agents on host");
          }
        }

        ArrayList taskList = new ArrayList();
        for (int i = 0; i < agentsToBeRevoked.size(); i++) {
          String agentName = (String) agentsToBeRevoked.get(i);
          NewPrepositionalPhrase npp = ldm.newPrepositionalPhrase();
          npp.setPreposition(CompromiseBlackboard.FOR_AGENT_PREP);
          npp.setIndirectObject(agentName);
          NewPrepositionalPhrase npp2 = ldm.newPrepositionalPhrase();
          npp2.setPreposition(CompromiseBlackboard.COMPROMISE_TIMESTAMP_PREP);
          npp2.setIndirectObject(new Long(timestamp));
          NewTask pmTask = ldm.newTask();
          pmTask.setWorkflow(nwf);
          pmTask.setParentTask(rootTask);
          pmTask.setVerb(Verb.getVerb(REVOKE_SESSION_KEYS_VERB));
          pmTask.addPrepositionalPhrase(npp);
          pmTask.addPrepositionalPhrase(npp2);
          nwf.addTask(pmTask);
          if (i == 0) {
            getBlackboardService().publishAdd(pmTask);
          }

          NewTask caTask = ldm.newTask();
          caTask.setWorkflow(nwf);
          caTask.setParentTask(rootTask);
          caTask.setVerb(Verb.getVerb(REVOKE_AGENT_CERT_VERB));
          caTask.addPrepositionalPhrase(npp);
          //Get caDN for this agent
          ArrayList caDNs = this.getCaDNs(agentName);
          NewPrepositionalPhrase caPrep = ldm.newPrepositionalPhrase();
          caPrep.setPreposition(CompromiseBlackboard.CA_DN_PREP);
          caPrep.setIndirectObject(caDNs);
          caTask.addPrepositionalPhrase(caPrep);

          nwf.addTask(caTask);
          taskList.add(pmTask);
          taskList.add(caTask);
        }

        Vector constraints = new Vector();
        for (int t = 0; t < taskList.size(); t++) {
          Task t1 = (Task) taskList.get(t);
          if ((t + 1) < taskList.size()) {
            Task t2 = (Task) taskList.get((t + 1));

            NewConstraint constraint = ldm.newConstraint();

            constraint.setConstrainingTask(t1);
            constraint.setConstrainingAspect(AspectType.END_TIME);
            constraint.setConstrainedTask(t2);
            constraint.setConstrainedAspect(AspectType.START_TIME);
            constraint.setConstraintOrder(Constraint.BEFORE);
            constraints.addElement(constraint);
          }
        }

        nwf.setConstraints(constraints.elements());

        Expansion exp = ldm.createExpansion(rootTask.getPlan(), rootTask, nwf, estResult);
        getBlackboardService().publishAdd(exp);
        getBlackboardService().publishAdd(rootTask);
      }
    }
  }


  /**
   * method that takes an action against the culprit
   *
   * @param agentName DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  protected ArrayList getCaDNs(String agentName) {
    ArrayList caDnList = new ArrayList();
    if (logging.isDebugEnabled()) {
      logging.debug("Gettng cadns for agent " + agentName);
    }


    List certList = keyRingService.findCert(agentName);
    if ((certList == null) || (certList.size() == 0)) {
      if (logging.isWarnEnabled()) {
        logging.warn("Could not find cert list for " + agentName);
      }

      return caDnList;
    }

    Iterator certs = certList.iterator();
    String caDN = null;

    // for now there should only be one certificate signed by one CA
    while (certs.hasNext()) {
      CertificateStatus status = (CertificateStatus) certs.next();
      X509Certificate cert = status.getCertificate();
      if (logging.isDebugEnabled()) {
        logging.debug("Found certificate dn = " + cert.getSubjectDN().getName());
      }

      X509Certificate[] certChain = keyRingService.findCertChain(cert);
      if (certChain != null) {
        // get the CA's dn from the certificate chain
        caDN = getCADN(certChain);

        if (caDN != null) {
          if (logging.isDebugEnabled()) {
            logging.debug("CA DN: " + caDN);
          }

          caDnList.add(caDN);

        } else {
          if (logging.isWarnEnabled()) {
            logging.warn("No CA dn(s) where found in certificate chain for: " + agentName);
          }
        }
      } else {
        if (logging.isWarnEnabled()) {
          logging.warn("Can't get certificate chain for cert: " + cert.getSubjectDN().getName());
        }
      }
    }

    return caDnList;
  }


  private String getCADN(X509Certificate[] certChain) {
    int len = certChain.length;
    String title = null;
    String dn = null;

    for (int i = 0; i < len; i++) {
      dn = certChain[i].getIssuerDN().getName();
      title = CertificateUtility.findAttribute(dn, "t");
      if (title.equals(CertificateCache.CERT_TITLE_CA)) {
        return dn;
      }
    }

    return null;
  }
}