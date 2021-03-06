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


package org.cougaar.core.security.policy.enforcers.util;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.security.policy.ontology.EntityInstancesConcepts;
import org.cougaar.core.service.LoggingService;


/**
 * The purpose of this class is to facilitate the mapping between DAML
 * concepts and the UltraLog concepts.  For now I am using
 * configuration files but some of this will change later...
 */
public class OwlBlackboardMapping  {

  public static final String otherBlackboardObjectDAML = 
    EntityInstancesConcepts.EntityInstancesOwlURL() + "otherBlackboardObjects";
  private boolean            _initialized = false;
  private List                _objectMap;
  private ServiceBroker       _sb;
  private LoggingService      _log; 

  public OwlBlackboardMapping(ServiceBroker sb)
  {
    _sb = sb;
    _log = (LoggingService) sb.getService(this, LoggingService.class, null);
    if (_log.isDebugEnabled()) {
      _log.debug("Initializing DAML Blackboard Mapper");
    }
  }


  public void initialize()
  {
    try {
      if (_log.isDebugEnabled()) {
        _log.debug("loading daml blackboard object mappings...");
      }
      _objectMap = new StringPairMapping(_sb, "OwlMapBlackboardObjects")
                                                       .buildPairList();
    } catch (IOException e) {
      _log.error("IO Exception reading DAML <-> " + 
                 "blackboard configuration file", e);
    }
  }

  public String classToDAMLName(String classname)
  {
    try {
      if (_log.isDebugEnabled()) {
        _log.debug("Converting " + classname + " to KAoS name");
      }
      for (Iterator objectIt = _objectMap.iterator();
           objectIt.hasNext();) {
        StringPair pair = (StringPair) objectIt.next();
        if (match(pair._first, classname)) {
          return pair._second;
        }
      }
    } catch (Exception e) {
      _log.warn("This is probably not good", e);
      return null;
    }
    return otherBlackboardObjectDAML;
  }


  public Set damlToClassNames(String damlname)
  {
    HashSet objectNamesUL = new HashSet();
    for (Iterator objectIt = _objectMap.iterator();
         objectIt.hasNext();) {
      StringPair pair = (StringPair) objectIt.next();
      if (pair._second.equals(damlname)) {
        objectNamesUL.add(pair._first);
      }
    }
    return objectNamesUL;
  }

  public Set namedObjects()
  {
    HashSet namedClasses = new HashSet();
    for (Iterator objectIt = _objectMap.iterator();
         objectIt.hasNext();) {
      StringPair pair = (StringPair) objectIt.next();
      namedClasses.add(pair._first);
    }
    return namedClasses;
  }

  public Set allDAMLObjectNames()
  {
    HashSet namedClasses = new HashSet();
    for (Iterator objectIt = _objectMap.iterator();
         objectIt.hasNext();) {
      StringPair pair = (StringPair) objectIt.next();
      namedClasses.add(pair._second);
    }
    namedClasses.add(otherBlackboardObjectDAML);
    return namedClasses;
  }

  protected static boolean match(String model, String classname)
  {
    if (model.endsWith("*")) {
      return classname.startsWith(model.substring(0,model.length() - 1));
    } else {
      return model.equals(classname);
    }
  }
}
