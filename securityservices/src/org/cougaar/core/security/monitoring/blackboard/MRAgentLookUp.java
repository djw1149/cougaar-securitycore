/*
 * <copyright>
 *  Copyright 1997-2001 Network Associates
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */


package org.cougaar.core.security.monitoring.blackboard;


import edu.jhuapl.idmef.*;
import java.io.Serializable;


public class MRAgentLookUp implements java.io.Serializable {

  public String community;
  public String role;
  public Source source;
  public Target target;
  public Classification classification;
  public String source_agent; //Agent name of Source of attack.
  public String target_agent;//Agent name of Target of attack. 
  public boolean updates;
  public MRAgentLookUp (String findcommunity,
			String findrole,
			Source findsource,
			Target findtarget,
			Classification findclassification,String sourceagent,String targetagent,boolean updates) {
    this.community=findcommunity;
    this.role=findrole;
    this.source=findsource;
    this.target=findtarget;
    this.classification=findclassification;
    this.source_agent=sourceagent;
    this.target_agent=targetagent;
    this.updates=updates;
  }
  
  public MRAgentLookUp (String findcommunity,
			String findrole,
			Source findsource,
			Target findtarget,
			Classification findclassification,String sourceagent,String targetagent) {
    this.community=findcommunity;
    this.role=findrole;
    this.source=findsource;
    this.target=findtarget;
    this.classification=findclassification;
    this.source_agent=sourceagent;
    this.target_agent=targetagent;
    this.updates=false;
  }
  public String toString() {
    StringBuffer buff=new StringBuffer(" MRAgent Look up Object :\n");
    if(community!=null) {
      buff.append(" Destination Community : "+community +"\n");
    }
    if(role!=null) {
      buff.append(" Destination Role : "+role+"\n" );
    }
    if(source!=null) {
      buff.append(" Destination Source : "+ source+"\n" );
    }
    if(target!=null) {
      buff.append(" Destination Target: "+target +"\n");
    }
     if(classification!=null) {
      buff.append(" Destination Classification : "+ classification.getName() +"\n" );
    }
     return buff.toString();
  }


}
