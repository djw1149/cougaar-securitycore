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
package org.cougaar.core.security.acl.user;

import java.util.*;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import javax.naming.*;
import javax.naming.directory.*;

// Cougaar core services
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.community.CommunityService;
import org.cougaar.multicast.AttributeBasedAddress;

import org.cougaar.core.mts.SimpleMessageAddress;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.planning.ldm.policy.RuleParameter;
import org.cougaar.planning.ldm.policy.Policy;
import org.cougaar.planning.ldm.policy.KeyRuleParameterEntry;
import org.cougaar.planning.ldm.policy.KeyRuleParameter;
import org.cougaar.core.blackboard.SubscriptionWatcher;
import org.cougaar.core.service.community.CommunityChangeListener;
import org.cougaar.core.service.community.CommunityChangeEvent;

// Cougaar security services
import org.cougaar.core.security.services.acl.UserService;
import org.cougaar.core.security.services.acl.UserServiceException;
import org.cougaar.core.security.policy.GuardRegistration;
import org.cougaar.core.security.policy.LdapUserServicePolicy;
import org.cougaar.core.security.policy.SecurityPolicy;
import org.cougaar.core.security.acl.user.CasRelay;
import org.cougaar.core.security.acl.user.CasRelay.CasRequest;
import org.cougaar.core.security.acl.user.CasRelay.CasResponse;

// KAoS
import safe.enforcer.NodeEnforcer;

public class AgentUserService implements UserService, BlackboardClient {

  private ServiceBroker _serviceBroker;
  private CommunityService _communityService;
  private BlackboardService _bbs;
  private LoggingService _log;
  private UIDService     _uidService;
  private MessageAddress _source;
  private Set            _myRelays = new HashSet();
  private Object         _lock = new Object();
  private IncrementalSubscription _subscription;
  private HashMap        _targets = new HashMap();
  private String         _defaultDomain;
  public  static final String COMMUNITY_TYPE = "User";
  public  static final String MANAGER_ROLE   = "UserManager";

  private final UnaryPredicate MY_RELAYS = new UnaryPredicate() {
      public boolean execute(Object obj) {
        synchronized (_myRelays) {
          return _myRelays.contains(obj);
        }
      }
    };

  private final SubscriptionWatcher WATCHER = new SubscriptionWatcher() {
      public void signalNotify(int event) {
        super.signalNotify(event);
        synchronized(_lock) {
          _lock.notifyAll();
        }
      }
    };

  public AgentUserService(ServiceBroker sb, MessageAddress agent) {
    try {
    _serviceBroker = sb;
    _log = (LoggingService)
      _serviceBroker.getService(this, LoggingService.class, null);
    _bbs = (BlackboardService) 
      _serviceBroker.getService(this, BlackboardService.class, null);
    _uidService = (UIDService)
      _serviceBroker.getService(this, UIDService.class, null);
    _source = agent;
    setCommunityService();

    _bbs.openTransaction();
    _subscription = (IncrementalSubscription) _bbs.subscribe(MY_RELAYS);
    _bbs.registerInterest(WATCHER);
    _bbs.closeTransaction();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void setCommunityService() {
    _communityService = (CommunityService)
      _serviceBroker.getService(this, CommunityService.class, null);
    if (_communityService != null) {
      Collection communities = 
        _communityService.listParentCommunities(_source.getAddress());
      if (_log.isDebugEnabled()) {
        _log.debug("my communities = " + communities);
      }
      Iterator iter = communities.iterator();
      while (iter.hasNext()) {
        String community = iter.next().toString();
        Attributes attrs = _communityService.getCommunityAttributes(community);
        if (attrs != null) {
          Attribute  attr  = attrs.get("CommunityType");
          if (attr != null) {
            try {
              for (int i = 0; i < attr.size(); i++) {
                if (COMMUNITY_TYPE.equals(attr.get(i).toString())) {
                  // do I have the role in this community?
                  Collection myRoles = 
                    _communityService.getEntityRoles(community,
                                                     _source.getAddress());
                  MessageAddress defaultTarget;
                  if (myRoles.contains(MANAGER_ROLE)) {
                    defaultTarget = _source;
                    if (_log.isDebugEnabled()) {
                      _log.debug("default target = " + defaultTarget);
                    }
                    synchronized (_targets) {
                      _targets.put(community, defaultTarget);
                    }
                  }
                  _defaultDomain = community;
                  return;
                }
              }
            } catch (NamingException e) {
              // error reading value, so it can't be a Security community
            }
          }
        }
      }
    }
  }

  public long currentTimeMillis() {
    return System.currentTimeMillis();
  }

  public String getBlackboardClientName() {
    return "AgentUserService";
  }

  public boolean triggerEvent(Object event) {
    return false;
  }
  
  private String getDomain(String id) throws UserServiceException {
    int index = id.indexOf("\\");
    if (_defaultDomain == null) {
      setCommunityService();
    }
    if (index == -1) {
      return _defaultDomain;
    }
    return id.substring(0, index);
  }

  private MessageAddress getTarget(String id) throws UserServiceException {
    return findTargetAgent(getDomain(id));
  }

  private MessageAddress findTargetAgent(String community) 
    throws UserServiceException {
    synchronized (_targets) {
      MessageAddress target = (MessageAddress) _targets.get(community);
      if (target != null) {
        return target;
      }

      Attributes attrs = _communityService.getCommunityAttributes(community);
      if (attrs != null) {
        Attribute  attr  = attrs.get("CommunityType");
        if (attr != null) {
          try {
            for (int i = 0; i < attr.size(); i++) {
              if (COMMUNITY_TYPE.equals(attr.get(i).toString())) {
                // this is the right type of community.
                // find an agent with that role
                Collection mgr =
                  _communityService.searchByRole(community, MANAGER_ROLE);
                if (mgr.size() != 1) {
                  throw new UserServiceException("Could not find user manager for domain '" + community + "'.");
                }
                target = (MessageAddress) mgr.iterator().next();
                if (_log.isDebugEnabled()) {
                  _log.debug("Found manager for " + community + ": " + target);
                }
                _targets.put(community, target);
                return target;
              }
            }
          } catch (NamingException e) {
            // error reading value, so it can't be a Security community
          }
        }
      }
    }
    throw new UserServiceException("Unknown community: " + community + ". Please try again later.");
  }

  private String getId(String id) {
    int index = id.indexOf("\\");
    if (index != -1) {
      return id.substring(index + 1);
    }
    return id;
  }

  private Object waitResponse(CasRelay relay) throws UserServiceException {
    CasResponse response = null;
    synchronized (_myRelays) {
      _myRelays.add(relay);
    }
    if (_log.isDebugEnabled()) {
      _log.debug("sending: " + relay);
    }
    try {
      _bbs.openTransaction();
      _bbs.publishAdd(relay);
      _bbs.closeTransaction();
    } catch (Exception e) {
      e.printStackTrace();
    }
    synchronized (_lock) {
      while ((response = (CasResponse)relay.getResponse()) == null) {
        try {
          _lock.wait();
        } catch (Exception e) {/* don't care*/}
      }
      if (_log.isDebugEnabled()) {
        _log.debug("Got Response: " + relay);
      }
    }
    synchronized (_myRelays) {
      _myRelays.remove(relay);
    }
    _bbs.openTransaction();
    _bbs.publishRemove(relay);
    _bbs.closeTransaction();
    if (response.isOk()) {
      return response.getResponse();
    }
    throw (UserServiceException) response.getResponse();
  }

  public void disableUser(String uid) throws UserServiceException {
    MessageAddress target = getTarget(uid);
    CasRelay relay = new CasRelay(_uidService.nextUID(), _source, target);
    relay.disableUser(getId(uid));
    waitResponse(relay);
  }

  public void lockCertificate(String uid) throws UserServiceException {
  }

  public void unlockCertificate(String uid) throws UserServiceException {
  }

  public void disableUser(String uid, long milliseconds) 
    throws UserServiceException {
    MessageAddress target = getTarget(uid);
    CasRelay relay = new CasRelay(_uidService.nextUID(), _source, target);
    relay.disableUser(getId(uid), milliseconds);
    waitResponse(relay);
  }

  public void enableUser(String uid) 
    throws UserServiceException {
    MessageAddress target = getTarget(uid);
    CasRelay relay = new CasRelay(_uidService.nextUID(), _source, target);
    relay.enableUser(getId(uid));
    waitResponse(relay);
  }

  public Set getUsers(String domain, String text, 
                      String field, int maxResults) 
    throws UserServiceException {
    if (domain == null) {
      domain = _defaultDomain;
    }
    MessageAddress target = findTargetAgent(domain);
    CasRelay relay = new CasRelay(_uidService.nextUID(), _source, target);
    relay.getUsers(text, field, maxResults);
    return setIdDomains((Set) waitResponse(relay), domain);
  }

  public Map getUser(String uid) throws UserServiceException {
    String domain = getDomain(uid);
    MessageAddress target = findTargetAgent(domain);
    CasRelay relay = new CasRelay(_uidService.nextUID(), _source, target);
    relay.getUser(getId(uid));
    Map user =  changeMapId((Map) waitResponse(relay), 
                            UserEntries.FIELD_UID, domain);
    if (user != null) {
      Set roles = (Set) user.get(getRoleListAttribute());
      if (roles != null) {
        user.put(getRoleListAttribute(), setIdDomains(roles, domain));
      }
    }
    return user;
  }

  public void editUser(String uid, Map added, Map edited, Set deleted)
    throws UserServiceException {
    MessageAddress target = getTarget(uid);
    CasRelay relay = new CasRelay(_uidService.nextUID(), _source, target);
    relay.editUser(getId(uid), added, edited, deleted);
    waitResponse(relay);
  }

  public void addUser(String uid, Map map) throws UserServiceException {
    MessageAddress target = getTarget(uid);
    CasRelay relay = new CasRelay(_uidService.nextUID(), _source, target);
    relay.addUser(getId(uid), map);
    waitResponse(relay);
  }

  public void deleteUser(String uid) throws UserServiceException {
    MessageAddress target = getTarget(uid);
    CasRelay relay = new CasRelay(_uidService.nextUID(), _source, target);
    relay.deleteUser(getId(uid));
    waitResponse(relay);
  }

  private static Set setIdDomains(Set ids, String domain) {
    Iterator iter = ids.iterator();
    Set newSet = new HashSet();
    while (iter.hasNext()) {
      String id = (String) iter.next();
      newSet.add(domain + "\\" + id);
    }
    return newSet;
  }

  public Set getRoles(String uid) throws UserServiceException {
    String domain = getDomain(uid);
    MessageAddress target = findTargetAgent(domain);
    CasRelay relay = new CasRelay(_uidService.nextUID(), _source, target);
    relay.getRoles(getId(uid));
    return setIdDomains( (Set) waitResponse(relay), domain);
  }

  public Set getRoles(String domain, String searchText, 
                      String field, int maxResults) 
    throws UserServiceException {
    if (domain == null) {
      domain = _defaultDomain;
    }
    MessageAddress target = findTargetAgent(domain);
    CasRelay relay = new CasRelay(_uidService.nextUID(), _source, target);
    relay.getRoles(searchText, field, maxResults);
    return setIdDomains((Set) waitResponse(relay), domain);
  }

  public Set getRoles(String domain, int maxResults) 
    throws UserServiceException {
    if (domain == null) {
      domain = _defaultDomain;
    }
    MessageAddress target = findTargetAgent(domain);
    CasRelay relay = new CasRelay(_uidService.nextUID(), _source, target);
    relay.getRoles(maxResults);
    return setIdDomains((Set) waitResponse(relay), domain);
  }

  private Map changeMapId(Map map, String key, String domain) {
    if (map != null) {
      String id = (String) map.get(key);
      if (id != null) {
        map.put(key, domain + "\\" + id);
      }
    }
    return map;
  }

  public Map getRole(String rid) throws UserServiceException {
    String domain = getDomain(rid);
    MessageAddress target = findTargetAgent(domain);
    CasRelay relay = new CasRelay(_uidService.nextUID(), _source, target);
    relay.getRole(getId(rid));
    Map role = changeMapId((Map) waitResponse(relay), UserEntries.FIELD_RID,
                           domain);
    Set roles = (Set) role.get(getRoleListAttribute());
    if (roles != null) {
      role.put(getRoleListAttribute(), setIdDomains(roles, domain));
    }
    return role;
  }

  public void assign(String uid, String rid) 
    throws UserServiceException {
    MessageAddress target = getTarget(rid);
    MessageAddress target2 = getTarget(uid);
    CasRelay relay = new CasRelay(_uidService.nextUID(), _source, target);
    if (!target.equals(target2)) {
      throw new UserServiceException("Domains must be the same for user and role");
    }
    relay.assign(getId(uid), getId(rid));
    waitResponse(relay);
  }

  public void addRoleToRole(String container, String containee) 
    throws UserServiceException {
    MessageAddress target = getTarget(container);
    MessageAddress target2 = getTarget(containee);
    CasRelay relay = new CasRelay(_uidService.nextUID(), _source, target);
    if (!target.equals(target2)) {
      throw new UserServiceException("Role domains must be the same");
    }
    relay.addRoleToRole(getId(container), getId(containee));
    waitResponse(relay);
  }

  public void unassign(String uid, String rid) throws UserServiceException {
    MessageAddress target = getTarget(uid);
    MessageAddress target2 = getTarget(rid);
    CasRelay relay = new CasRelay(_uidService.nextUID(), _source, target);
    if (!target.equals(target2)) {
      throw new UserServiceException("Domains must be the same for user and role");
    }
    relay.unassign(getId(uid), getId(rid));
    waitResponse(relay);
  }

  public void removeRoleFromRole(String container, String containee) 
    throws UserServiceException {
    MessageAddress target = getTarget(container);
    MessageAddress target2 = getTarget(containee);
    CasRelay relay = new CasRelay(_uidService.nextUID(), _source, target);
    if (!target.equals(target2)) {
      throw new UserServiceException("Role domains must be the same");
    }
    relay.removeRoleFromRole(getId(container), getId(containee));
    waitResponse(relay);
  }

  public void addRole(String rid) throws UserServiceException {
    MessageAddress target = getTarget(rid);
    CasRelay relay = new CasRelay(_uidService.nextUID(), _source, target);
    relay.addRole(getId(rid));
    waitResponse(relay);
  }

  public Set expandRoles(String rids[]) throws UserServiceException {
    if (rids == null || rids.length == 0) return null;
    String domain = null;
    String newRids[] = new String[rids.length];
    for (int i = 0; i < rids.length; i++) {
      if (domain == null) {
        domain = getDomain(rids[i]);
      } else if (!domain.equals(getDomain(rids[i]))) {
        throw new UserServiceException("Role domains must be the same"); 
      }
      newRids[i] = getId(rids[i]);
    }
    MessageAddress target = findTargetAgent(domain);
    CasRelay relay = new CasRelay(_uidService.nextUID(), _source, target);
    relay.expandRoles(newRids);
    return setIdDomains((Set) waitResponse(relay), domain);
  }

  public Set getUsersInRole(String rid)  throws UserServiceException {
    String domain = getDomain(rid);
    MessageAddress target = findTargetAgent(domain);
    CasRelay relay = new CasRelay(_uidService.nextUID(), _source, target);
    relay.getUsersInRole(getId(rid));
    return setIdDomains((Set) waitResponse(relay), domain);
  }

  public Set getContainedRoles(String rid) throws UserServiceException {
    String domain = getDomain(rid);
    MessageAddress target = findTargetAgent(domain);
    CasRelay relay = new CasRelay(_uidService.nextUID(), _source, target);
    relay.getContainedRoles(getId(rid));
    return setIdDomains((Set) waitResponse(relay), domain);
  }

  public void addRole(String rid, Map map) throws UserServiceException {
    MessageAddress target = getTarget(rid);
    CasRelay relay = new CasRelay(_uidService.nextUID(), _source, target);
    relay.addRole(getId(rid), map);
    waitResponse(relay);
  }

  public void editRole(String rid, Map added, Map edited, Set deleted) 
    throws UserServiceException {
    MessageAddress target = getTarget(rid);
    CasRelay relay = new CasRelay(_uidService.nextUID(), _source, target);
    relay.editRole(getId(rid), added, edited, deleted);
    waitResponse(relay);
  }

  public void deleteRole(String rid) throws UserServiceException {
    MessageAddress target = getTarget(rid);
    CasRelay relay = new CasRelay(_uidService.nextUID(), _source, target);
    relay.deleteRole(getId(rid));
    waitResponse(relay);
  }

  public String getRoleListAttribute() {
    return UserEntries.FIELD_ROLE_LIST;
  }

  public String  getPasswordAttribute() {
    return UserEntries.FIELD_PASSWORD;
  }

  public String getAuthFieldsAttribute() {
    return UserEntries.FIELD_AUTH;
  }

  public String getEnableTimeAttribute() {
    return UserEntries.FIELD_ENABLE_TIME;
  }

  public String getUserIDAttribute() {
    return UserEntries.FIELD_UID;
  }

  public String getRoleIDAttribute() {
    return UserEntries.FIELD_RID;
  }

  public String getCertOkAttribute() {
    return UserEntries.FIELD_CERT_OK;
  }

}
