/*
 * <copyright>
 *  Copyright 1997-2001 Networks Associates Inc
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


package com.nai.security.policy;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;
import java.util.Iterator;
import java.util.List;
import java.io.*;

import org.w3c.dom.Document;

// Core Cougaar
import org.cougaar.core.plugin.SimplePlugin;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.planning.ldm.policy.Policy;

// KAoS policy management
import kaos.core.policy.PolicyConstants;
import kaos.core.util.*;
import safe.util.*;

// Cougaar security services
import org.cougaar.core.security.policy.XMLPolicyCreator;
import org.cougaar.core.security.policy.TypedPolicy;

import com.nai.security.util.DOMWriter;

/**
 * The PolicyExpanderPlugIn expands policies before
 * they reach the DomainManagerPlugIn for approval.
 * 
 * It subscribes to UnexpandedPolicyUpdates and UnexpandedConditionalPolicyMsgs.
 * 
 * It publishes ConditionalPolicyMsgs and ProposedPolicyUpdates.
 * 
 * The actual policy expansion happens in the expandPolicy function. Please see
 * the comments for that method for details on how to expand policies.
 */
public class PolicyExpanderPlugin
  extends SimplePlugin
{
    private UnaryPredicate _unexCondPolicyPredicate = new UnaryPredicate() {
        public boolean execute(Object o) {
        return (o instanceof UnexpandedConditionalPolicyMsg);
        }
        };
    private UnaryPredicate _unexPolicyUpdatePredicate = new UnaryPredicate() {
        public boolean execute(Object o) {
        return (o instanceof UnexpandedPolicyUpdate);
        }
        };

    public void setupSubscriptions()
    {
        _ucpm = (IncrementalSubscription) subscribe(_unexCondPolicyPredicate);
        _upu = (IncrementalSubscription) subscribe (_unexPolicyUpdatePredicate);

        // should we print debugging info?
        String debug = System.getProperty("SAFE.debug");
        if (debug != null && debug.equalsIgnoreCase("true")) {
            _debug = true;
        }
    }
    
    public void execute()
    {
        if (_debug) System.out.println("PolicyExpanderPlugIn::execute()");
        // check for added UnexpandedConditionalPolicyMsgs
        Enumeration ucpmEnum = _ucpm.getAddedList();
        while (ucpmEnum.hasMoreElements()) {
            UnexpandedConditionalPolicyMsg ucpm = (UnexpandedConditionalPolicyMsg) ucpmEnum.nextElement();
            // extract the ConditionalPolicyMsg
            ConditionalPolicyMsg condPolicyMsg = ucpm.getConditionalPolicyMsg();
            // get the policies
            Vector policies = condPolicyMsg.getPolicies();
            Vector newPolicies = new Vector();
            // expand each policy
            for (int i=0; i<policies.size(); i++) {
                PolicyMsg policyMsg = (PolicyMsg) policies.elementAt(i);
                try {                    
                    expandPolicy (policyMsg);
                }
                catch (Exception xcp) {
                    xcp.printStackTrace();
                }                    
            }
            publishRemove (ucpm);
            if (_debug) System.out.println("publishAdd ConditionalPolicyMsg");
            publishAdd (condPolicyMsg);			
        }
        
        // check for added UnexpandedPolicyUpdates
        Enumeration upuEnum = _upu.getAddedList();
        while (upuEnum.hasMoreElements()) {
            UnexpandedPolicyUpdate upu = (UnexpandedPolicyUpdate) upuEnum.nextElement();
            List policies = upu.getPolicies();
            Iterator policyIt = policies.iterator();
            while (policyIt.hasNext()) {
                PolicyMsg policyMsg = (PolicyMsg) policyIt.next();
                try {
                    expandPolicy (policyMsg);
                }
                catch (Exception xcp) {
                    xcp.printStackTrace();
                }
            }
            publishRemove (upu);
            publishAdd (new ProposedPolicyUpdate(upu.getUpdateType(),
                                                 policies));
        }
    }

  /**
   * This function expands a policy
   * 
   * The original policy should be kept intact, in that no existing fields
   * are removed or changed. You should expand the policy by
   * adding to the original. You may add new attributes, or add new key-value
   * pairs, or add sub-messages to the original policy, whichever way you
   * prefer, as long as the enforcers can parse the additions. The current
   * KAoS infrastructure does not parse these additions so no restrictions
   * are placed on the types of things you add to the original policy.
   * 
   * @param policy	Policy message to expand
   */
  private void expandPolicy(PolicyMsg policyMsg)
    throws Exception
  {
    if (_debug == true) {
      System.out.println("Expanding policy message: " + policyMsg);
    }

    // get the attributes of the policy
    Vector attributes = policyMsg.getAttributes();

    Document xmlContent = null;
    for (int i=0; i<attributes.size(); i++) {
      AttributeMsg attrMsg = (AttributeMsg) attributes.elementAt(i);

      // Find the XML policy attributes and expand them
      if (attrMsg.getName().equals(XML_KEY)) {
	xmlContent = (Document) attrMsg.getValue();

	XMLPolicyCreator policyCreator =
	  new XMLPolicyCreator(xmlContent, getClusterIdentifier().toAddress());
	Policy[] policies = policyCreator.getPolicies();

	if (_debug == true) {
	  System.out.println("\n\nTHERE ARE " + policies.length
			     + " POLICIES");
	  PrintStream out = new PrintStream(System.out);
	  DOMWriter xmlwriter = new DOMWriter(out);
	  xmlwriter.print(xmlContent);
	}

	for (int j = 0 ; j < policies.length ; j++) {
	  if (policies[j] instanceof TypedPolicy){
	    TypedPolicy policyObject = (TypedPolicy) policies[j];
	    // Add policy type.
	    String binderType = policyObject.getType();
	    policyMsg.addSymbol(PolicyConstants.HLP_POLICY_TYPE,
				binderType);

	    policyMsg.addSymbol(org.cougaar.core.security.
				policy.TypedPolicy.POLICY_OBJECT_KEY,
				policyObject);
	    if (_debug == true) {
	      System.out.println("Adding policy object["+ i + "]: " +
				 binderType + " - " + policyObject);
	    }
	  }
	}                   
      }
    }
  }
  private IncrementalSubscription _ucpm;
  private IncrementalSubscription _upu;
  private boolean _debug = false;
  
  public static final String XML_KEY = "XMLContent";

}

