package org.cougaar.core.security.policy.enforcers;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import kaos.ontology.jena.ActionConcepts;

// Cougaar core services
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.LoggingService;
import org.cougaar.planning.ldm.policy.Policy;
import org.cougaar.planning.ldm.policy.RuleParameter;

// KAoS policy management
import kaos.ontology.management.UnknownConceptException;
import kaos.ontology.repository.ActionInstanceDescription;
import kaos.ontology.repository.TargetInstanceDescription;
import kaos.policy.guard.PolicyDistributor;

import safe.enforcer.AgentEnforcer;
import safe.enforcer.NodeEnforcer;
import safe.guard.EnforcerManagerService;
import safe.guard.NodeGuard;

/*
 * This is the enforcer associated with an agent.  It is not yet clear
 * that we need these to do enforcement but the advantage of writing one is that
 * registering an agent enforcer ensures that the agent is registered
 * to the KAoS directory service.
 */

public class SampleAgentEnforcer
    implements AgentEnforcer
{
    private ServiceBroker _sb;
    protected LoggingService _log;
    private final String _agentName;
    // This action is a fake for now - though it seems likely that
    // there will be actions that are mediated by an enforcer that is
    // specific to an agent.
    private String _action = ActionConcepts.actionDamlURL + "MobilityAction";
    private Vector _agents;
    private NodeGuard _guard;


    /*
     * This interface is only necessary if this is an angent
     * enforcer.  In the March demos, we will probably only use
     * NodeEnforcers and this function will not need to be written. 
     *
     * I am not really sure what this should return.  In the IHMC
     * version of this code it returns the empty string so I don't
     * think it is critical.
     */
    public String getAgentName()
    {
	return _agentName;
    }

    /*
     * This interface is only necessary if this is an angent
     * enforcer.  In the March demos, we will probably only use
     * NodeEnforcers and this function will not need to be written. 
     *
     * This function returns an identifier for the agent.  I think
     * that this function is important because it becomes the name
     * that JTP and the policy information object use to represent the
     * agent.
     */
    public String getAgentId()
    {
	return _agentName;
    }

    /*
     * Right now I return an empty vector of actions.
     */
    public Vector getControlledActionClasses()
    {
	return new Vector();
    }

    /*
     * ?
     */
    public String getName() { return "SampleAgentEnforcer"; }


    /*
     * This function initializes the SampleAgentEnforcer by providing it
     * with a service broker and the name of the agent it is covering.
     */
    public SampleAgentEnforcer(ServiceBroker sb, String agentName) {
	_agentName = agentName;
	_sb = sb;
	_agents=new Vector();
	_agents.add(_agentName);
	_log = (LoggingService) 
	    _sb.getService(this, LoggingService.class, null);
    }

    /*
     * This method registers the enforcer to the guard and sets up
     * needed variables (such as the service broker and the logging
     * service). 
     *
     * This code used to be in the class 
     *   org.cougaar.core.security.policy.GuardRegistration
     * from securityservices.  I have modified the registerEnforcer
     * call a little - I need to check but I don't think that the
     * previous version works (compiles?) any longer?
     */
    public void registerEnforcer()
    {
	if (!_sb.hasService(EnforcerManagerService.class)) {
	    _log.fatal("Guard service is not registered");
	    throw new SecurityException("Guard service is not registered");
	}

	EnforcerManagerService _enfMgr = 
	    (EnforcerManagerService)
	    _sb.getService(this, EnforcerManagerService.class, null);
	if (_enfMgr == null) {
	    _log.fatal("Cannot continue without guard", new Throwable());
	    throw new SecurityException("Cannot continue without guard");
	}
	if (!_enfMgr.registerEnforcer(this, _action, _agents)) {
	    _log.fatal("Could not register with the Enforcer Manager Service");
	    throw new SecurityException(
			      "Cannot register with Enforcer Manager Service");
	}
	if (_enfMgr instanceof NodeGuard) {
	    _guard = (NodeGuard) _enfMgr;
	} else { 
	    throw new SecurityException("Cannot get guard");
	}
    }

}
