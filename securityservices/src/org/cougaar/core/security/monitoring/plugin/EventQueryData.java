/* 
 * <copyright> 
 *  Copyright 1999-2004 Cougaar Software, Inc.
 *  under sponsorship of the Defense Advanced Research Projects 
 *  Agency (DARPA). 
 *  
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).  
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright> 
 */ 

package org.cougaar.core.security.monitoring.plugin;

import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;

import java.io.Serializable;
import java.util.HashSet;

/**
 * This class is used by EventQueryPlugin to store data onto the
 * blackboard for persistence.
 */
public class EventQueryData implements Serializable, UniqueObject {
  public HashSet agents;
  public HashSet queryAdapters;
  public String  unaryPredicateClass;
  public String  classifications[];
  private UID _uid;

  public EventQueryData() {
  }

  public UID getUID() {
    return _uid;
  }

  public void setUID(UID uid) {
    _uid = uid;
  }

  /** Used only for XMLizable */
  public String getUnaryPredicateClass() {
    return unaryPredicateClass;
  }

  /** Used only for XMLizable */
  public String[] getCapabilities() {
    return classifications;
  }

  /** Used only for XMLizable */
  public int getAdapterCount() {
    return queryAdapters.size();
  }

  /** Used only for XMLizable */
  public HashSet getAgents() {
    return agents;
  }
}

