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


package org.cougaar.core.security.provider;

import java.util.LinkedHashMap;

import org.cougaar.core.security.services.util.SecurityPropertiesService;
import org.cougaar.core.service.LoggingService;

/**
 * A Map<Service, ServiceProvider>
 * <p>
 * The class extends from LinkedHashMap so we can iterate over the elements
 * in the order in which they were inserted.
 * 
 * @author srosset
 */
public class SecurityServiceTable
  extends LinkedHashMap
{
  private LoggingService log;

  public SecurityServiceTable(LoggingService aLog) {
    log = aLog;
  }

  public Object put(Object key, Object value) {
    throw new UnsupportedOperationException("Use addService() instead");
  }
  
  public synchronized Object addService(Class serviceClass, ServiceEntry serviceEntry) {
    if (log.isDebugEnabled()) {
     log.debug("Adding service " + serviceClass.getName());
    }
    serviceEntry.getServiceBroker().
      addService(serviceClass, serviceEntry.getServiceProvider());
    return super.put(serviceClass, serviceEntry);
  }
}
