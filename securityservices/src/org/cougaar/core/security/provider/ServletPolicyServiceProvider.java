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

// Cougaar core services
import org.apache.catalina.Context;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.security.acl.auth.DualAuthenticator;
import org.cougaar.core.security.acl.auth.ServletPolicyEnforcer;

public class ServletPolicyServiceProvider 
  extends BaseSecurityServiceProvider
{
  static private ServletPolicyEnforcer _servletPolicyService = null;
  static private DualAuthenticator     _dualAuthenticator    = null;
  static private Context               _context              = null;
  static private ServiceBroker         _staticServiceBroker  = null;

  public ServletPolicyServiceProvider(ServiceBroker sb, 
                                      String community) {
    super(sb, community);
    _staticServiceBroker = sb;
  }

  private static synchronized void init() {
    if (_servletPolicyService == null) {
      _servletPolicyService = new ServletPolicyEnforcer(_staticServiceBroker);
      if (_dualAuthenticator != null) {
        _servletPolicyService.setDualAuthenticator(_dualAuthenticator);
      }
      if (_context != null) {
        _servletPolicyService.setContext(_context);
      }
    }
  }

  public static void addAgent(String agent) {
    init();
    _servletPolicyService.addAgent(agent);
  }

  public static synchronized void setContext(Context context) {
    init();
    _context = context;
    if (_servletPolicyService != null) {
      _servletPolicyService.setContext(context);
    }
  }

  public static synchronized void setDualAuthenticator(DualAuthenticator da) {
    init();
    _dualAuthenticator = da;
    da.setServiceBroker(_staticServiceBroker);
    if (_servletPolicyService != null) {
      _servletPolicyService.setDualAuthenticator(da);
    }
  }

  /**
   * Get a service.
   * @param sb a Service Broker
   * @param requestor the requestor of the service
   * @param serviceClass a Class, usually an interface, which extends Service.
   * @return a service
   */
  protected Service getInternalService(ServiceBroker sb, 
				       Object requestor, 
				       Class serviceClass) {
    return _servletPolicyService;
  }

  /** Release a service.
   * @param sb a Service Broker.
   * @param requestor the requestor of the service.
   * @param serviceClass a Class, usually an interface, which extends Service.
   * @param service the service to be released.
   */
  protected void releaseInternalService(ServiceBroker sb,
					Object requestor,
					Class serviceClass,
					Object service) {
  }
}
