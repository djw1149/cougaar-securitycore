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

import org.cougaar.core.blackboard.Publishable;
import org.cougaar.core.util.UID;

public class OutStandingQuery implements java.io.Serializable {
  private UID uid;
  private boolean outstandingquery=true;
  public OutStandingQuery(UID Uid){
    this.uid=Uid;
  }
   public boolean isPersistable() {
    return true;
  } 
  
  public UID getUID() {
    return this.uid;
  }
  public void setOutStandingQuery(boolean outstanding) {
    outstandingquery=outstanding;
  }
  
  public boolean isQueryOutStanding() {
    return outstandingquery;
  }
  public String toString() {
    StringBuffer buff=new StringBuffer();
    if(uid!=null) {
      buff.append(" uid is :"+ uid.toString() +"\n");
    }
    buff.append (" status :"+ outstandingquery);
    return buff.toString();
  }
}
