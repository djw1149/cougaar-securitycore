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

package com.nai.security.test.crypto;

import java.util.*;
import java.awt.event.*;
import java.awt.*;
import javax.swing.*;

// Cougaar core services
import org.cougaar.core.node.*;
import org.cougaar.core.agent.*;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.MessageTransportService;
import org.cougaar.core.mts.*;

// Cougaar security services
import org.cougaar.core.security.services.identity.*;

public class AgMobPlugin extends org.cougaar.core.plugin.SimplePlugin 
{
  private AgentIdentityService aiService;
  private NodeIdentifier thisNodeID;
  private NodeIdentifier toNodeID;
  private ClusterIdentifier thisAgentID;
  private String targetNode;

  private MessageTransportService mts;
  private MessageTransportClient mtc;

  /** The name of the window */
  private String frame_label;
  private ActionListener button_listener;
  private ActionListener text_listener;
  private String button_label;

  // Swing components
  private JTextField textField;
  private JButton button;

  public void setupSubscriptions()
  {

    // get our agent's ID
    thisAgentID = getBindingSite().getAgentIdentifier();

    // get the nodeID service          
    NodeIdentificationService nodeService = (NodeIdentificationService) 
      getBindingSite().getServiceBroker().getService(
          this, 
          NodeIdentificationService.class,
          null);

    thisNodeID = ((nodeService != null) ? 
		  nodeService.getNodeIdentifier() :
		  null);

    String paramOrigNode = thisNodeID.toAddress();

    // Get agent mobility service
    aiService = (AgentIdentityService)
      getBindingSite().getServiceBroker().getService(
	this, 
	AgentIdentityService.class,
	null);

    button_listener = new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  if (targetNode != null) {
	    initiateTransfer();
	  }
	}
      };

    text_listener = new ActionListener() {
	public void actionPerformed(ActionEvent evt) {
	  targetNode = textField.getText();
	  // parse the destination node ID
	  toNodeID = new NodeIdentifier(targetNode);
	}
      };

    initTransport();
    frame_label = "Agent mobility test";
    button_label = "Bundle keys";
    createGUI();
  }

  private void initiateTransfer() {
    String agent = thisAgentID.toAddress();
    String sourceAgent = thisNodeID.toAddress();
    String targetAgent = toNodeID.toAddress();
    TransferableIdentity ti =
      aiService.initiateTransfer(agent, sourceAgent, targetAgent);

    MoveCryptoMessage moveMsg = 
      new MoveCryptoMessage(
	thisNodeID,  // tell my node
	toNodeID,
	ti);
    System.out.println("Node "+thisNodeID+" sending "+moveMsg);
    mts.sendMessage(moveMsg);
  }

  private void completeTransfer(Message msg) {
    System.out.println("Received Message: " + msg);
    if (!(msg instanceof MoveCryptoMessage)) {
      System.out.println("ERROR: not a MoveCryptoMessage");
      return;
    }
    MoveCryptoMessage cmsg = (MoveCryptoMessage) msg;
    TransferableIdentity ti = cmsg.getTransferableIdentity();

    String sourceAgent = cmsg.getOriginator().toAddress();
    String targetAgent = thisNodeID.toAddress();
    aiService.completeTransfer(ti, sourceAgent, targetAgent);
  }

  public void execute()
  {
    //    System.out.println("ReportCreatorPlugin.execute...");
  }


  /**
   * Create a simple free-floating GUI button with a label
   */
  private void createGUI()
  {
    JFrame frame = new JFrame(frame_label);
    frame.getContentPane().setLayout(new FlowLayout());
    JPanel panel = new JPanel();
    // Create the button
    button = new JButton(button_label);
    textField = new JTextField(30);

    // Register a listener for the button
    button.addActionListener(button_listener);
    textField.addActionListener(text_listener);

    panel.add(button);
    panel.add(textField);

    frame.getContentPane().add("Center", panel);
    frame.pack();
    frame.setVisible(true);
  }

  private void initTransport() {
    // create a dummy message transport client
    mtc = new MessageTransportClient() {
        public void receiveMessage(Message message) {
	  completeTransfer(message);
        }
        public MessageAddress getMessageAddress() {
          return thisAgentID;
        }
      };

    // get the message transport
    mts = (MessageTransportService) 
      getBindingSite().getServiceBroker().getService(
	mtc,   // simulated client 
	MessageTransportService.class,
	null);
    if (mts == null) {
      System.out.println(
	"Unable to get message transport service");
    }
  }
}
