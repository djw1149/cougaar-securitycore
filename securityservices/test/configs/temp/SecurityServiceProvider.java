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

package org.cougaar.core.security.provider;

// Cougaar core services
import org.cougaar.core.component.ServiceAvailableEvent;
import org.cougaar.core.component.ServiceAvailableListener;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceBrokerSupport;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.logging.LoggingControlService;
import org.cougaar.core.logging.LoggingServiceProvider;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.security.policy.dynamic.DynamicPolicy;
import org.cougaar.core.security.services.acl.AccessControlPolicyService;
import org.cougaar.core.security.services.acl.UserService;
import org.cougaar.core.security.services.auth.AuthorizationService;
import org.cougaar.core.security.services.auth.SecurityContextService;
import org.cougaar.core.security.services.crypto.CRLCacheService;
import org.cougaar.core.security.services.crypto.CertValidityService;
import org.cougaar.core.security.services.crypto.CertificateCacheService;
import org.cougaar.core.security.services.crypto.CertificateManagementService;
import org.cougaar.core.security.services.crypto.CrlManagementService;
import org.cougaar.core.security.services.crypto.CryptoPolicyService;
import org.cougaar.core.security.services.crypto.EncryptionService;
import org.cougaar.core.security.services.crypto.KeyRingService;
import org.cougaar.core.security.services.crypto.SSLService;
import org.cougaar.core.security.services.crypto.ServletPolicyService;
import org.cougaar.core.security.services.crypto.UserSSLService;
import org.cougaar.core.security.services.identity.WebserverIdentityService;
import org.cougaar.core.security.services.ldap.CertDirectoryServiceCA;
import org.cougaar.core.security.services.ldap.CertDirectoryServiceClient;
import org.cougaar.core.security.services.util.CACertDirectoryService;
import org.cougaar.core.security.services.util.CertificateSearchService;
import org.cougaar.core.security.services.util.ConfigParserService;
import org.cougaar.core.security.services.util.PersistenceMgrPolicyService;
import org.cougaar.core.security.services.util.PolicyBootstrapperService;
import org.cougaar.core.security.services.util.SecurityPropertiesService;
import org.cougaar.core.security.ssl.JaasSSLFactory;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.DataProtectionService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.MessageProtectionService;
import org.cougaar.core.service.identity.AgentIdentityService;
import org.cougaar.planning.ldm.LDMServesPlugin;
import org.cougaar.planning.service.LDMService;

public class SecurityServiceProvider
{
  /** The name of the community of type SecurityCommunity. */
  private String mySecurityCommunity;
  private SecurityServiceTable services;
  private ServiceBroker serviceBroker;
  private ServiceBroker rootServiceBroker;
  private NodeControlService nodeControlService;
  private LoggingService log;
  /** True if the application is run with a Cougaar node */
  private boolean isExecutedWithinNode = true;
  private boolean initDone = false;

  private ServiceBroker _serviceBrokerProxy;
  private ServiceBroker _rootServiceBrokerProxy;

  public SecurityServiceProvider() {
    ServiceBroker sb = new ServiceBrokerSupport();
    init(sb, null);
  }

  public SecurityServiceProvider(ServiceBroker sb, String community) {
    init(sb, community);
  }

  public ServiceBroker getServiceBroker() {
    return serviceBroker;
  }

  /** **********************************************************************
   * Private methods
   */

  private void init(ServiceBroker sb, String community) {
    serviceBroker = sb;
    mySecurityCommunity = community;
    registerServices();
  }

  private void registerServices() {

    // Get root service broker
    nodeControlService = (NodeControlService)
      serviceBroker.getService(this, NodeControlService.class, null);
    if (nodeControlService != null) {
      rootServiceBroker = nodeControlService.getRootServiceBroker();
      if (rootServiceBroker == null) {
	throw new RuntimeException("Unable to get root service broker");
      }
    }
    else {
      // We are running outside a Cougaar node.
      // No Cougaar services are available.
      isExecutedWithinNode = false;
      rootServiceBroker = serviceBroker;

      /* ********************************
       * Logging service
       */
      // NodeAgent has not started the logging service at this point,
      // but we need it.
      // Removed because the Logging service is started early in 9.4

      LoggingServiceProvider loggingServiceProvider =
	      new LoggingServiceProvider();
      rootServiceBroker.addService(LoggingService.class,
				   loggingServiceProvider);
      rootServiceBroker.addService(LoggingControlService.class,
				   loggingServiceProvider);
    }

    System.setProperty("org.cougaar.core.security.isExecutedWithinNode",
		       String.valueOf(isExecutedWithinNode));
    this.log = (LoggingService)
      rootServiceBroker.getService(this,
				   LoggingService.class, null);

    services = SecurityServiceTable.getInstance(log);

    if (log.isDebugEnabled()) {
      log.debug("Registering security services");
    }
    if (log.isDebugEnabled()) {
      if(mySecurityCommunity!=null) {
        log.debug("Registering security services for security community "+mySecurityCommunity );
      }
      else {
        log.error("Un able to get Security community as mySecurityCommunity is Null:");
      }
    }

    if (log.isInfoEnabled() && isExecutedWithinNode == false) {
      log.info("Running outside a Cougaar node");
    }

    /* ********************************
     * Property service
     */
    ServiceProvider newSP = null;

    newSP = new SecurityPropertiesServiceProvider(rootServiceBroker, mySecurityCommunity);
    services.put(SecurityPropertiesService.class, newSP);
    rootServiceBroker.addService(SecurityPropertiesService.class, newSP);
    SecurityPropertiesService secprop = (SecurityPropertiesService)
      rootServiceBroker.getService(this, SecurityPropertiesService.class, null);
      /*
    boolean standalone = false;
    try {
      standalone = new Boolean(secprop.getProperty(
        "org.cougaar.core.security.standalone", "false")).booleanValue();

      if (!standalone)
        new NodeInfo().setNodeName(serviceBroker);
    } catch (Exception ex) {
      log.warn("Unable to get value of standalone mode");
    }
    */

    /* ********************************
     * Configuration services
     */
    newSP = new ConfigParserServiceProvider(serviceBroker, mySecurityCommunity);
    services.put(ConfigParserService.class, newSP);
    rootServiceBroker.addService(ConfigParserService.class, newSP);

      // configured to use SSL?
      if (secprop.getProperty(secprop.WEBSERVER_HTTPS_PORT, null) != null) {
	newSP = new WebserverSSLServiceProvider(serviceBroker, mySecurityCommunity);
        services.put(WebserverIdentityService.class, newSP);
        rootServiceBroker.addService(WebserverIdentityService.class, newSP);
        rootServiceBroker.getService(this, WebserverIdentityService.class, null);
      }

    /* ********************************
     * Encryption services
     */
    /* Certificate Directory lookup services */
    newSP = new CertDirectoryServiceProvider(serviceBroker, mySecurityCommunity);
    services.put(CertDirectoryServiceClient.class, newSP);
    rootServiceBroker.addService(CertDirectoryServiceClient.class, newSP);

    newSP = new CertDirectoryServiceProvider(serviceBroker, mySecurityCommunity);
    services.put(CertDirectoryServiceCA.class, newSP);
    rootServiceBroker.addService(CertDirectoryServiceCA.class, newSP);

    newSP = new CertDirectoryServiceProvider(serviceBroker, mySecurityCommunity);
    services.put(CACertDirectoryService.class, newSP);
    rootServiceBroker.addService(CACertDirectoryService.class, newSP);

    newSP = new CertificateSearchServiceProvider(serviceBroker, mySecurityCommunity);
    services.put(CertificateSearchService.class, newSP);
    rootServiceBroker.addService(CertificateSearchService.class, newSP);

    newSP = new CertificateSearchServiceProvider(serviceBroker, mySecurityCommunity);
    services.put(CACertDirectoryService.class, newSP);
    rootServiceBroker.addService(CACertDirectoryService.class, newSP);

    /* Certificate Management service */
    newSP = new CertificateManagementServiceProvider(serviceBroker, mySecurityCommunity);
    services.put(CertificateManagementService.class, newSP);
    rootServiceBroker.addService(CertificateManagementService.class, newSP);


    /* Starting Certificate Cache  service */

    newSP = new CertificateCacheServiceProvider(serviceBroker, mySecurityCommunity);
    services.put(CertificateCacheService.class, newSP);
    rootServiceBroker.addService(CertificateCacheService.class, newSP);



    /* Key lookup service */
    newSP = new KeyRingServiceProvider(serviceBroker, mySecurityCommunity);
    services.put(KeyRingService.class, newSP);
    rootServiceBroker.addService(KeyRingService.class, newSP);


/* Starting CRL Cache  service */
    log.debug("Service broker passed to CRLCacheServiceProvider is :"+serviceBroker.toString());
    newSP = new CRLCacheServiceProvider(serviceBroker, mySecurityCommunity);
    services.put(CRLCacheService.class, newSP);
    rootServiceBroker.addService(CRLCacheService.class, newSP);
    /*CRLCacheService crlCacheService=(CRLCacheService)serviceBroker.getService(this,
                                                      CRLCacheService.class,
                                                      null);
    */


    /* Certificate validity service */
    newSP = new CertValidityServiceProvider(serviceBroker, mySecurityCommunity);
    services.put(CertValidityService.class, newSP);
    rootServiceBroker.addService(CertValidityService.class, newSP);

    if (isExecutedWithinNode) {
      
      /* Persistence Manager Service */
      newSP = new PersistenceMgrPolicyServiceProvider(serviceBroker, mySecurityCommunity);
      services.put(PersistenceMgrPolicyService.class, newSP);
      rootServiceBroker.addService(PersistenceMgrPolicyService.class, newSP);
      
      /* Encryption Service */
      newSP = new EncryptionServiceProvider(serviceBroker, mySecurityCommunity);
      services.put(EncryptionService.class, newSP);
      rootServiceBroker.addService(EncryptionService.class, newSP);

      /* Data protection service */
      boolean dataOn =
	Boolean.valueOf(System.getProperty("org.cougaar.core.security.dataprotection", "true")).booleanValue();
      if (dataOn) {
	newSP = new DataProtectionServiceProvider(serviceBroker, mySecurityCommunity);
	services.put(DataProtectionService.class, newSP);
	rootServiceBroker.addService(DataProtectionService.class, newSP);
      }
      else {
	log.warn("Data protection service disabled");
      }

      /* Message protection service */
      newSP = new MessageProtectionServiceProvider(serviceBroker, mySecurityCommunity);
      services.put(MessageProtectionService.class, newSP);
      rootServiceBroker.addService(MessageProtectionService.class, newSP);

      /* ********************************
       * Identity services
       */
      /* Agent identity service */
      newSP = new AgentIdentityServiceProvider(serviceBroker, mySecurityCommunity);
      services.put(AgentIdentityService.class, newSP);
      rootServiceBroker.addService(AgentIdentityService.class, newSP);

      /* ********************************
       * Access Control services
       */

      /* ********************************
       * Policy services
       */
      newSP = new PolicyBootstrapperServiceProvider(serviceBroker, mySecurityCommunity);
      services.put(PolicyBootstrapperService.class, newSP);
      rootServiceBroker.addService(PolicyBootstrapperService.class, newSP);

      if (!org.cougaar.core.security.access.AccessAgentProxy.USE_DAML) {
	newSP = new AccessControlPolicyServiceProvider(serviceBroker, mySecurityCommunity);
	services.put(AccessControlPolicyService.class, newSP);
	rootServiceBroker.addService(AccessControlPolicyService.class, newSP);
      }

      newSP = new CryptoPolicyServiceProvider(serviceBroker, mySecurityCommunity);
      services.put(CryptoPolicyService.class, newSP);
      rootServiceBroker.addService(CryptoPolicyService.class, newSP);

      // Request the CryptoPolicyService now. This will create the
      // singleton instance of the CryptoPolicyService, which means
      // other requests will not have to instantiate a new
      // CryptoPolicyService.
      // The CryptoPolicyService registers itself with the guard,
      // which in turns sends a Relay message to the Policy Domain
      // manager.
      // If we didn't do this, we could have deadlock issues. For example,
      // a component could try to persist the blackboard, which would
      // lock the blackboard. The distributor would then invoke
      // the data protection service to protect the blackboard,
      // and the data protection service would request the
      // CryptoPolicyService, which would cause the guard to publish a Relay,
      // but the blackboard is locked.
      rootServiceBroker.getService(this, CryptoPolicyService.class, null);

      newSP = new ServletPolicyServiceProvider(serviceBroker, mySecurityCommunity);
      services.put(ServletPolicyService.class, newSP);
      rootServiceBroker.addService(ServletPolicyService.class, newSP);

    /* ********************************
     * SSL services
     */
/*
      newSP = new SSLServiceProvider(serviceBroker, mySecurityCommunity);
      services.put(SSLService.class, newSP);
      rootServiceBroker.addService(SSLService.class, newSP);

      // SSLService and WebserverIdentityService are self started
      // they offer static functions to get socket factory
      // in the functions the permission will be checked.
      rootServiceBroker.getService(this, SSLService.class, null);
*/

      KeyRingService krs =
        (KeyRingService) rootServiceBroker.getService(this,
                                                      KeyRingService.class,
                                                      null);

      javax.net.ssl.HttpsURLConnection.
        setDefaultSSLSocketFactory(new JaasSSLFactory(krs, rootServiceBroker));

      krs.finishInitialization();

      /* ********************************
       * LDAP user administration
       */
      newSP = new UserServiceProvider(rootServiceBroker);
      serviceBroker.addService(UserService.class, newSP);

      org.cougaar.core.security.crypto.ldap.KeyRingJNDIRealm.
        setNodeServiceBroker(serviceBroker);

      /**********************************
       * Security context service
       * NOTE: This service should only be accessible by the security services codebase
       */
      newSP = new SecurityContextServiceProvider(serviceBroker, mySecurityCommunity);
      services.put(SecurityContextService.class, newSP);
      rootServiceBroker.addService(SecurityContextService.class, newSP);
      
      /**
       * Authorization service
       */
      newSP = new AuthorizationServiceProvider(serviceBroker, 
                                               mySecurityCommunity);
      services.put(AuthorizationService.class, newSP);
      rootServiceBroker.addService(AuthorizationService.class, newSP);

      /* ********************************
       * Change the policy to support
       * DAML
       */
      AuthorizationService authServ = (AuthorizationService) rootServiceBroker.
        getService(this, AuthorizationService.class, null);
      DynamicPolicy.install(authServ);
    }
    else {
      KeyRingService krs =
        (KeyRingService) rootServiceBroker.getService(this,
                                                      KeyRingService.class,
                                                      null);
      log.info("Running in standalone mode");
      if (krs != null) {
	newSP = new UserSSLServiceProvider(serviceBroker, mySecurityCommunity);
	services.put(UserSSLService.class, newSP);
	rootServiceBroker.addService(UserSSLService.class, newSP);
	krs.finishInitialization();
      }
    }
    LDMService ldms =null;
    if(serviceBroker.hasService
       (org.cougaar.planning.service.LDMService.class)){
      ldms = (LDMService)	rootServiceBroker.getService(this, LDMService.class, null);
      log.info("LDM Service is available initially in Security Service Provider ");
      if(ldms!=null){
	LDMServesPlugin ldm=ldms.getLDM();
	newSP = new CrlManagementServiceProvider(ldm,serviceBroker, mySecurityCommunity);
	services.put(CrlManagementService.class, newSP);
	rootServiceBroker.addService(CrlManagementService.class, newSP);
      }
    }
    else {
      log.debug("Registering  LDMServiceAvailableListener ");
      serviceBroker.addServiceListener(new LDMServiceAvailableListener ());
    }
    /*
    if(serviceBroker.hasService(org.cougaar.core.service.BlackboardService.class)){
       log.debug("Black Board Service is available initially in Security Service Provider ");
    }
    else {
      log.debug("Registering Black Board Service Listener ");
      serviceBroker.addServiceListener(new BBServiceAvailableListener());
    }
    */
   log.debug("Root service broker is :"+rootServiceBroker.toString());
   log.debug("Service broker is :"+ serviceBroker.toString());
  }

  private class LDMServiceAvailableListener implements ServiceAvailableListener
  {
    public void serviceAvailable(ServiceAvailableEvent ae) {
      LDMService ldms=null;
       ServiceProvider newSP = null;
      Class sc = ae.getService();
      if( org.cougaar.planning.service.LDMService.class.isAssignableFrom(sc)) {
	ldms = (LDMService) serviceBroker.getService(this, LDMService.class, null);
	log.info("LDM Service is available now in Security Service provider ");
	if(ldms!=null){
	  LDMServesPlugin ldm=ldms.getLDM();
	  newSP = new CrlManagementServiceProvider(ldm,serviceBroker, mySecurityCommunity);
	  services.put(CrlManagementService.class, newSP);
	  rootServiceBroker.addService(CrlManagementService.class, newSP);
	  log.info("Added  CrlManagementService service  ");
	}
	else {
	  log.info("LDM Service is null in LDMServiceAvailableListener  ");
	}
      }

    }
  }
  private class BBServiceAvailableListener implements ServiceAvailableListener {
    public void serviceAvailable(ServiceAvailableEvent ae) {
      BlackboardService bbs=null;
      //ServiceProvider newSP = null;
      Class sc = ae.getService();
      if( org.cougaar.core.service.BlackboardService.class.isAssignableFrom(sc)) {
	//bbs = (BlackboardService) serviceBroker.getService(this, BlackboardService.class, null);
	log.debug("Black Board  Service is available now in Security Service provider "+ sc.getName());

      }

    }
   }
}
