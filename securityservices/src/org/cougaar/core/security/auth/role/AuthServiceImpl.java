package org.cougaar.core.security.auth.role;

// cougaar classes
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ServiceAvailableEvent;
import org.cougaar.core.component.ServiceAvailableListener;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.LoggingService;

// security services classes
import org.cougaar.core.security.acl.auth.URIPrincipal;
import org.cougaar.core.security.acl.auth.UserRoles;
import org.cougaar.core.security.auth.BlackboardPermission;
import org.cougaar.core.security.auth.BlackboardObjectPermission;
import org.cougaar.core.security.auth.ExecutionContext;
import org.cougaar.core.security.auth.ExecutionPrincipal;
import org.cougaar.core.security.auth.ObjectContext;
import org.cougaar.core.security.policy.enforcers.util.DAMLBlackboardMapping;
import org.cougaar.core.security.policy.enforcers.util.RoleMapping;
import org.cougaar.core.security.services.auth.AuthorizationService;
import org.cougaar.core.security.services.auth.SecurityContextService;
import org.cougaar.core.security.util.ActionPermission;

// java classes
import java.security.Permission;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.security.ProtectionDomain;

// KAoS classes
import kaos.ontology.management.UnknownConceptException;
import kaos.ontology.repository.ActionInstanceDescription;
import kaos.ontology.repository.TargetInstanceDescription;
import kaos.policy.guard.KAoSGuard;
import kaos.policy.information.KAoSProperty;

// safe  classes
import safe.enforcer.NodeEnforcer;
import safe.guard.EnforcerManagerService;
import safe.guard.NodeGuard;

/**
 * TODO: This is only a stub implementation
 */
public class AuthServiceImpl 
  implements AuthorizationService, NodeEnforcer {
  public static final String DAML_PROPERTY = 
    "org.cougaar.core.security.policy.auth.role.useDaml";
  private static final boolean USE_DAML = Boolean.getBoolean(DAML_PROPERTY);

  private static int _counter = 0;
  private SecurityContextService _scs;
  private ServiceBroker          _sb;
  private NodeGuard              _guard;
  private RoleMapping            _roleMap;
  private LoggingService         _log;

  private static final String _enforcedActionType = 
    org.cougaar.core.security.policy.enforcers.ontology.jena.
    UltralogActionConcepts._BlackBoardAccess_;
  private static final Map _damlActionMapping;
  static {
    _damlActionMapping = new HashMap();
    String accessModes[] = {"Add", "Remove", "Change", "Query",
                            "Read", "Write", "Create"};
    for (int i = 0; i < accessModes.length; i++) {
      _damlActionMapping.put(
              accessModes[i].toLowerCase(),
              org.cougaar.core.security.policy.enforcers.ontology.jena.
              EntityInstancesConcepts.EntityInstancesDamlURL
              + "BlackBoardAccess" + accessModes[i]
      );
    }
  }
  private static final Set _blackboardObjectActions;
  static {
    _blackboardObjectActions = new HashSet();
    _blackboardObjectActions.add("create");
    _blackboardObjectActions.add("read");
    _blackboardObjectActions.add("write");
  }
  private static Set            _nameableBlackboardObjects = null;
  private Set                   _allBlackboardObjectsDAML  = null;
  private DAMLBlackboardMapping _damlObjectMap;
  
  public AuthServiceImpl(ServiceBroker sb) {
    _sb = sb;  

    _roleMap = new RoleMapping(sb);

    _log = (LoggingService) 
      _sb.getService(this, LoggingService.class, null);

    _damlObjectMap = new DAMLBlackboardMapping(_sb);
    _damlObjectMap.initialize();
    _nameableBlackboardObjects = _damlObjectMap.namedObjects();
    _allBlackboardObjectsDAML = _damlObjectMap.allDAMLObjectNames();

    _scs = (SecurityContextService)
      _sb.getService(this, SecurityContextService.class, null);
    if (_scs == null) {
      if (_log.isDebugEnabled()) {
        _log.debug("SecurityContextService not available yet...");
      }
      _sb.addServiceListener(new SecurityContextServiceAvailableListener());
    }
    registerEnforcer();
    if (_log.isDebugEnabled()) {
      _log.debug("AuthServiceImp Constructor completed - UseDaml = " +
                 USE_DAML);
    }
  }

  public void registerEnforcer()
  {
    EnforcerManagerService enfMgr = 
      (EnforcerManagerService)
      _sb.getService(this, EnforcerManagerService.class, null);
    if (enfMgr == null) {
      _sb.releaseService(this, EnforcerManagerService.class, enfMgr);
      _log.fatal("Cannot continue without guard", new Throwable());
      throw new RuntimeException("Cannot continue without guard");
    }
    if (!enfMgr.registerEnforcer(this, _enforcedActionType, new Vector())) {
      _sb.releaseService(this, EnforcerManagerService.class, enfMgr);
        _log.fatal("Could not register with the Enforcer Manager Service");
      throw new SecurityException(
                   "Cannot register with Enforcer Manager Service");
    }
    if (enfMgr instanceof NodeGuard) {
      _guard = (NodeGuard) enfMgr;
    } else { 
      _sb.releaseService(this, EnforcerManagerService.class, enfMgr);
      throw new RuntimeException("Cannot get guard");
    }
  }

  public static Set nameableBlackboardObjects()
  {
    return _nameableBlackboardObjects; 
  }
  
  public ExecutionContext createExecutionContext(MessageAddress agent,
                                                 ComponentDescription component) {
    String componentName = component.getClassname();
    Set s = _roleMap.getRolesForComponent(componentName);
    String[] compRoles = (String[]) s.toArray(new String[s.size()]);
    s = _roleMap.getRolesForAgent(agent.toString());
    String[] agentRoles = (String[]) s.toArray(new String[s.size()]);
    String[] userRoles = UserRoles.getRoles();
    String userName = UserRoles.getUserName();
    
    return (new RoleExecutionContext(agent, componentName, userName,
                                     agentRoles, compRoles, userRoles));
  }

  public ExecutionContext createExecutionContext(MessageAddress agent,
                                                 String uri,
                                                 String userName) {
    Set s = _roleMap.getRolesForUri(uri);
    String[] compRoles = (String[]) s.toArray(new String[s.size()]);
    s = _roleMap.getRolesForAgent(agent.toString());
    String[] agentRoles = (String[]) s.toArray(new String[s.size()]);
    String[] userRoles = UserRoles.getRoles();
    
    return (new RoleExecutionContext(agent, uri, userName,
                                     agentRoles, compRoles, userRoles));
  }

  public ObjectContext createObjectContext(ExecutionContext ec, Object object) {
    // no context necessary
    return new RoleObjectContext(((RoleExecutionContext)ec).getAgent());
  }

  public RoleExecutionContext 
    getExecutionContextFromDomain(ProtectionDomain domain)
  {
    // get the principals from the protection domain
    Principal p[] = domain.getPrincipals();
    ExecutionContext ec = null;
    for(int i = 0; i < p.length; i++) {
      // we want to look for the ExecutionPrincipal
      if(p[i] instanceof ExecutionPrincipal) {
        // the ExecutionPrincipal contains the ExecutionContext used for authorization
        ec = ((ExecutionPrincipal)p[i]).getExecutionContext();
        break;
      } 
    }
    
    if (_scs == null) { 
      if (_log.isWarnEnabled()) {
            _log.warn("Failed to get securitycontext service before mediation"
                      + "this may be an issue");
      }
      return null;
    }
    // ec is null because there isn't an ExecutionPrincipal 
    // in the ProtectionDomain
    if(ec == null && _scs != null) {
      // if no ExecutionPrincipal get the ExecutionContext 
      // from the SecurityContextService
      ec = _scs.getExecutionContext(); 
    }
    if (ec instanceof RoleExecutionContext) {
      return (RoleExecutionContext) ec;
    } else {
      return null;
    }
  }

  public boolean implies(ProtectionDomain domain, Permission perm) {
    if (_log.isDebugEnabled()) {
      _log.debug("Checking if the permission is implied");
    }
    
    RoleExecutionContext ec = getExecutionContextFromDomain(domain);
    if (ec != null) {
      if (_log.isDebugEnabled() && ((++_counter) % 10000 == 0)) {
        _log.debug("\n" + _counter + " blackboard mediations.");
      }
      if (_log.isDebugEnabled()) {
        _log.debug("Have an execution context calling isAuthorizeUL");
      }
      boolean ret = isAuthorizedUL(ec, perm);
      if(!ret) {   
        if(_log.isDebugEnabled()) {
          _log.debug("UNAUTHORIZED BLACKBOARD ACCESS: [" + ec.getComponent() + ", " + perm.getName() + ", " + perm.getActions() + "]");
          _log.debug("Component execution context: \n" + ec); 
        }
      }
      else {
        if(_log.isDebugEnabled()) {
          _log.info("AUTHORIZED BLACKBOARD ACCESS: [" + ec.getComponent() + ", " + perm.getName() + ", " + perm.getActions() + "]");
        }
      }
      return ret;
    } else {
      if (_log.isWarnEnabled()) {
        _log.warn("No execution context available at mediation time" +
                  " - is this ok?", new Throwable());
      }
      return true;
    }
  }
  
  public List getPermissions(ProtectionDomain domain) {
    RoleExecutionContext rec = getExecutionContextFromDomain(domain);
    List permissions = new LinkedList();
    if (!USE_DAML) {
    // add all of the permissions for now:
      permissions.add(new BlackboardPermission("*", 
                                               "add,change,remove,query"));
      permissions.add(new BlackboardObjectPermission("*", 
                                                     "create,read,write"));
      return permissions;
    }
    Set targets = new HashSet();
    targets.add(new TargetInstanceDescription(
                      org.cougaar.core.security.policy.enforcers.ontology.jena.
                      UltralogActionConcepts._blackBoardAccessMode_,
                      org.cougaar.core.security.policy.enforcers.ontology.jena.
                      EntityInstancesConcepts.EntityInstancesDamlURL + 
                      "BlackBoardAccessAdd"));
    ActionInstanceDescription action = 
      new ActionInstanceDescription(_enforcedActionType,
                                    "Dummy",
                                    targets);
    KAoSProperty actorProp = 
      action.getProperty(kaos.ontology.jena.ActionConcepts._performedBy_);
    {
      Vector tmp = new Vector();
      tmp.add(rec);
      actorProp.setMultipleInstances(tmp);
    }
    KAoSProperty accessProp =
      action.getProperty(
                org.cougaar.core.security.policy.enforcers.ontology.jena.
                UltralogActionConcepts._blackBoardAccessMode_);
    for (Iterator accessIt = _damlActionMapping.keySet().iterator();
         accessIt.hasNext();) {
      String accessMode = (String) accessIt.next();
      String accessModeDaml = (String) _damlActionMapping.get(accessMode);
      {
        Vector tmp = new Vector();
        tmp.add(accessModeDaml);
        accessProp.setMultipleInstances(tmp);
      }
      Set objectsInDAML = 
        _guard.getAllowableValuesForActionProperty(
                    org.cougaar.core.security.policy.enforcers.ontology.jena.
                    UltralogActionConcepts._blackBoardAccessObject_,
                    action,
                    _allBlackboardObjectsDAML);
      for (Iterator damlObjectNameIt =  objectsInDAML.iterator();
           damlObjectNameIt.hasNext();) {
        String damlObjectName = (String) damlObjectNameIt.next();
        if (damlObjectName.
            equals(DAMLBlackboardMapping.otherBlackboardObjectDAML)) {
          if (_blackboardObjectActions.contains(accessMode)) {
            permissions.add(new BlackboardObjectPermission("%Other%",
                                                           accessMode));
          } else {
            permissions.add(new BlackboardPermission("%Other%",
                                                     accessMode));
          }
        } else {
          Set objectsInUL = _damlObjectMap.damlToClassNames(damlObjectName);
          for (Iterator objectsInULIt = objectsInUL.iterator();
               objectsInULIt.hasNext();) {
            String ulObjectName = (String) objectsInULIt.next();
            if (_blackboardObjectActions.contains(accessMode)) {
              permissions.add(new BlackboardObjectPermission(ulObjectName,
                                                             accessMode));
            } else {
              permissions.add(new BlackboardPermission(ulObjectName,
                                                       accessMode));
            }
          }
        }
      }
    }
    return permissions;
  }


  private boolean isAuthorizedUL(RoleExecutionContext ec, Permission p) {

    if (!USE_DAML) { return true; }
    // i'm allowing everything that isn't a BlackboardPermission 
    if(!(p instanceof BlackboardPermission) &&
       !(p instanceof BlackboardObjectPermission)) {
      if (_log.isWarnEnabled()) {
          _log.warn("should I be here? object type = " +
                    p.getClass());
      }
      return true;
    }
    ActionPermission ap = (ActionPermission) p;

    int firstPolicyUpdateCounter = _guard.getPolicyUpdateCount();
    if (ec.cachedIsAuthorized(p.getName(),
                              p.getActions(),
                              firstPolicyUpdateCounter)) {
      return true;
    }

    // get the classname of the object
    // e.g. org.cougaar.core.adaptivity.OperatingModeImpl
    String object     = p.getName();
    String damlObject = _damlObjectMap.classToDAMLName(object);

    // get the action the plugin wants to perform on the object
    // e.g. add
    String actions [] = ap.getActionList();

    if (_log.isDebugEnabled()) {
        _log.debug("Actions = " + actions);
    }
    if (_log.isDebugEnabled()) {
      _log.debug("authorize plugin(" + ec + ") for (" + actions + 
                 ", " + object + ")");
    }
    for (int i = 0; i < actions.length; i++) {
      String action = actions[i];
      String damlAction = (String) _damlActionMapping.get(action);
      if  (damlAction == null) {
        throw new RuntimeException
          ("Invalid Action Type used in enforcement routines");
      }
      if (! isAuthorizedDaml(ec, damlAction, damlObject)) {
        return false;
      }
    }
    int secondPolicyUpdateCounter = _guard.getPolicyUpdateCount();
    if (firstPolicyUpdateCounter == secondPolicyUpdateCounter) {
      ec.updateCachedAuthorization(ap.getName(), 
                                   ap.getActions(),
                                   secondPolicyUpdateCounter);
    }
    return true;
  }

  public boolean isAuthorizedDaml(RoleExecutionContext rec,
                                  String action,
                                  String objectName)
  {
    Set targets = new HashSet();
    targets.add(new TargetInstanceDescription(
                      org.cougaar.core.security.policy.enforcers.ontology.jena.
                      UltralogActionConcepts._blackBoardAccessMode_,
                      action));
    targets.add(new TargetInstanceDescription(
                      org.cougaar.core.security.policy.enforcers.ontology.jena.
                      UltralogActionConcepts._blackBoardAccessObject_,
                      objectName));
    ActionInstanceDescription aid = 
      new ActionInstanceDescription(_enforcedActionType,
                                    "Dummy",
                                    targets);
    KAoSProperty actorProp = 
      aid.getProperty(kaos.ontology.jena.ActionConcepts._performedBy_);
    {
      Vector tmp = new Vector();
      tmp.add(rec);
      actorProp.setMultipleInstances(tmp);
    }
    try {
      return _guard.isActionAuthorized(aid);
    } catch (UnknownConceptException e) {
      _log.fatal("Something is seriously wrong with policy - " +
                 "the system is probably shutting down now..." +
                 "(hope runs eternal though)", e);
      return false;
    } catch (InterruptedException e) {
      if (_log.isWarnEnabled()) {
        _log.warn("Mediation interrupted - denying access");
      }
      return false;
    }
  }
  
  private URIPrincipal 
    getServletURIPrincipal(ProtectionDomain domain)
  {
    // get the principals from the protection domain
    Principal p[] = domain.getPrincipals();
    URIPrincipal up = null;
    for(int i = 0; i < p.length; i++) {
      // we want to look for the URIPrincipal since this is servlet context call
      if(p[i] instanceof URIPrincipal) {
        up = (URIPrincipal)p[i];
        break;
      } 
    }
    return up;
  }
    
  private class SecurityContextServiceAvailableListener
    implements ServiceAvailableListener
  {
    public void serviceAvailable(ServiceAvailableEvent ae) {
      Class sc = ae.getService();
      if (org.cougaar.core.security.services.auth.SecurityContextService.class.
          isAssignableFrom(sc)) {
        if (_log.isDebugEnabled()) {
          _log.debug("SecurityContext Service is now available");
        }
        _scs = (SecurityContextService)
          _sb.getService(this, SecurityContextService.class, null);        
      }
    }
  }

  public String getName() { return "BlackboardEnforcer"; }

  public Vector getControlledActionClasses () 
  { 
    Vector ret = new Vector();
    ret.add(_enforcedActionType);
    return ret;
  }

}
