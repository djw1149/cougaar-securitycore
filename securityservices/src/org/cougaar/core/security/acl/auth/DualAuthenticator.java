/*
 * <copyright>
 *  Copyright 1997-2001 Networks Associates Technology, Inc.
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
 *
 * Created on September 12, 2001, 10:55 AM
 */

package org.cougaar.core.security.acl.auth;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.security.Principal;

import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.ServletOutputStream;

import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Context;
import org.apache.catalina.Container;
import org.apache.catalina.ValveContext;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.Realm;
import org.apache.catalina.valves.ValveBase;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.deploy.SecurityCollection;
import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.authenticator.SSLAuthenticator;
import org.apache.catalina.authenticator.BasicAuthenticator;
import org.apache.catalina.connector.HttpResponseWrapper;

import org.cougaar.core.security.crypto.ldap.CougaarPrincipal;
import org.cougaar.core.security.crypto.ldap.KeyRingJNDIRealm;
import org.cougaar.core.security.provider.ServletPolicyServiceProvider;

public class DualAuthenticator extends ValveBase {
  static final int CONST_NONE     = 0x00;
  static final int CONST_PASSWORD = 0x01;
  static final int CONST_CERT     = 0x02;
  static final int CONST_BOTH     = 0x03;

  private static ResourceBundle _authenticators = null;
  AuthenticatorBase _primaryAuth;
  AuthenticatorBase _secondaryAuth;
  LoginConfig       _loginConfig = new LoginConfig();
  Context           _context     = null;
  HashMap           _constraints = new HashMap();
  long              _failSleep   = 1000;
  HashSet           _agentList   = new HashSet();

  public DualAuthenticator() {
    this(new SSLAuthenticator(), new BasicAuthenticator());
  }

  public DualAuthenticator(AuthenticatorBase secondaryAuth) {
    this(new SSLAuthenticator(), secondaryAuth);
  }

  public DualAuthenticator(AuthenticatorBase primaryAuth,
                           AuthenticatorBase secondaryAuth) {
    setPrimaryAuthenticator(primaryAuth);
    setSecondaryAuthenticator(secondaryAuth);
    ServletPolicyServiceProvider.setDualAuthenticator(this);
  }

  /**
   * Valve callback. First check the primary authentication method
   * and if not authenticated, call the secondary authentication method.
   */
  public void invoke(Request request, Response response,
                     ValveContext context) throws IOException, ServletException {
    // ensure that authentication containers are set
    setContainer(); 

    // create dummies so that authentication doesn't do anything
    // we really do want it to do
    DummyValveContext  dummyValveContext = new DummyValveContext();
    HttpServletResponse no_err =
      new NoErrorResponse((HttpServletResponse) response.getResponse());
    ResponseDummy tmpRes = 
      new ResponseDummy((HttpResponse) response, no_err);

    HttpServletRequest hreq = (HttpServletRequest) request.getRequest();
    HttpServletResponse hres = (HttpServletResponse) request.getResponse();
    boolean certInvoked;
    boolean passInvoked;

    // expand the "*" agent identifier -- can't think of another place
    // to get all agent names and this works.
    expandStarConstraint(hreq.getRequestURI());

    int userConstraint = CONST_NONE;
    int pathConstraint = getConstraint(hreq.getRequestURI());

    _primaryAuth.invoke(request,tmpRes, dummyValveContext);

    Principal certPrincipal = hreq.getUserPrincipal();
    Principal passPrincipal = null;

    if (certPrincipal instanceof CougaarPrincipal) {
      userConstraint = convertConstraint( ((CougaarPrincipal)certPrincipal).
                                          getLoginRequirements() );
      if ( (userConstraint & CONST_PASSWORD) != 0 ) {
        ((HttpRequest)request).setUserPrincipal(null);
      }
    }

    certInvoked = (dummyValveContext.getInvokeCount() > 0);
    dummyValveContext.resetInvokeCount();
    int totalConstraint = pathConstraint | userConstraint;

    if (totalConstraint == CONST_NONE || certPrincipal == null || 
        (totalConstraint & CONST_PASSWORD) != 0) {
      _secondaryAuth.invoke(request, response, dummyValveContext);
      passPrincipal = hreq.getUserPrincipal();
      if (certPrincipal == null && 
          passPrincipal instanceof CougaarPrincipal) {
        userConstraint = convertConstraint(( (CougaarPrincipal)passPrincipal).
                                           getLoginRequirements() );
        totalConstraint = pathConstraint | userConstraint;
      }
    }

    passInvoked = (dummyValveContext.getInvokeCount() > 0);
    Realm realm = _context.getRealm();

    if (authOk(certPrincipal, passPrincipal, totalConstraint, 
               certInvoked, passInvoked, hres, realm)) {
      context.invokeNext(request,response);
    } else if (certPrincipal == null) {
      try {
        Thread.sleep(_failSleep);
      } catch (InterruptedException e) {
        // no sweat
      }
    }
  }

  private void expandStarConstraint(String uri) {
    if (!uri.startsWith("/$")) {
      return; // not an agent... screw it
    }

    int slashIndex = uri.indexOf("/",2);
    if (slashIndex != -1) {
      uri = uri.substring(2, slashIndex);
    } else {
      uri = uri.substring(2);
    }
    synchronized (_agentList) {
      if (_agentList.contains(uri)) {
        return; // already there
      }
      _agentList.add(uri);
      SecurityConstraint scs[] = _context.findConstraints();
      if (scs == null) {
        return; // no constraints! 
      }

      for (int i = 0; i < scs.length; i++) {
        SecurityCollection scn = scs[i].findCollection("*");
        if (scn != null) {
          SecurityConstraint scNew = new SecurityConstraint();
          String roles[] = scs[i].findAuthRoles();
          for (int j = 0; j < roles.length; j++) {
            scNew.addAuthRole(roles[j]);
          }
          
          SecurityCollection scnNew = new SecurityCollection("*");
          String patterns[] = scn.findPatterns();
          for (int j = 0; j < patterns.length; j++) {
            if (!patterns[j].startsWith("/$")) {
              if (patterns[j].startsWith("/")) {
                scnNew.addPattern("/$" + uri + patterns[j]);
              } else {
                scnNew.addPattern("/$" + uri + "/" + patterns[j]);
              }
            }
            scnNew.addPattern(patterns[j]);
          }
          _context.removeConstraint(scs[i]);
          scNew.addCollection(scnNew);
          _context.addConstraint(scNew);
        }
      }
    }
  }

  private static boolean authOk(Principal certPrincipal,
                                Principal passPrincipal,
                                int totalConstraint,
                                boolean certInvoked,
                                boolean passInvoked,
                                HttpServletResponse hres,
                                Realm realm) 
    throws ServletException, IOException {
//     System.out.println("certPrincipal:   " + certPrincipal);
//     System.out.println("passPrincipal:   " + passPrincipal);
//     System.out.println("totalConstraint: " + totalConstraint);
//     System.out.println("certInvoked:     " + certInvoked);
//     System.out.println("passInvoked:     " + passInvoked);

    if (certPrincipal != null && passPrincipal != null &&
        !certPrincipal.getName().equals(passPrincipal.getName())) {
      // the certificate and password authorization credentials
      // should be the same!
      hres.sendError(hres.SC_UNAUTHORIZED,
                     "You have entered a different user name than " +
                     "in your certificate.");
      if (realm instanceof KeyRingJNDIRealm) {
        KeyRingJNDIRealm krjr = (KeyRingJNDIRealm) realm;
        krjr.alertLoginFailure( krjr.LF_USER_MISMATCH, 
                                certPrincipal.getName(),
                                passPrincipal.getName() );
      }
      return false;
    } else if ( ( certInvoked && passInvoked ) ||
                ( passInvoked && (totalConstraint & CONST_CERT) == 0 ) ||
                ( certInvoked && (totalConstraint & CONST_PASSWORD) == 0 ) ) {
      // ok, there is no role requirement so no authentication is
      // necessary.
//       System.out.println("no requirement");
      return true;
    } else if ( (totalConstraint & CONST_PASSWORD) != 0 &&
                passPrincipal == null) {
      // needed password authentication. We must have already
      // sent the bad response
      return false;
    } else if ((totalConstraint & CONST_CERT) != 0) {
      if (certPrincipal == null) {
        // needed certificate authentication. We need to send a response
        // indicating that:
        hres.sendError(hres.SC_UNAUTHORIZED,
                       "You must provide a client certificate in order " +
                       "to access this URL");
        if (realm instanceof KeyRingJNDIRealm) {
          KeyRingJNDIRealm krjr = (KeyRingJNDIRealm) realm;
          krjr.alertLoginFailure( krjr.LF_REQUIRES_CERT, 
                                  passPrincipal.getName() );
        }
        return false;
      } else if (!certInvoked && !passInvoked) {
        hres.sendError(hres.SC_UNAUTHORIZED,
                       "You do not have the required role to access this URL");
        return false;
      } else {
//         System.out.println("user is granted");
        return true; // user is granted access
      }
    } else if (!certInvoked && !passInvoked) {
      // nobody authenticated this user and therefore we must deny them
      // the password authentication has already given a response.
      return false;
    } else {
      // authentication is accepted
//       System.out.println("user auth is accepted");
      return true;
    }
  }
        
  /**
   * Sets an authentication constraints. It allows
   * the specification of whether the path should support authentication
   * using certificates, password, both, or either. When checking
   * paths, a combination of the most restrictive constraints is used.
   * Therefore, if "/*" is given "EITHER" and "/$foo/*" is given
   * "PASSWORD", then PASSWORD is used as the constraint.<p>
   *
   * All constraints are replaced by the argument.
   *
   * @param constraints A map with the pattern as the key and the
   *                    constraint as the value. The constraint can be
   *                    "CERT" for certificate authentication, "PASSWORD" for
   *                    BASIC auth or DIGEST authentication, "BOTH"
   *                    to require both a certificate and proper password, and
   *                    "EITHER" to not have any password requirements beyond
   *                    what is required by the role-based constraints.
   */
  public synchronized void setAuthConstraints(Map constraints) {
    _agentList.clear();
    _constraints = new HashMap(constraints);
  }

  /**
   * Sets the time to sleep when a user has a login failure.
   */
  public synchronized void setLoginFailureSleepTime(long sleepTime) {
    _failSleep = sleepTime;
  }
  
  private static int convertConstraint(String constraint) {
    if ("BOTH".equals(constraint)) {
      return CONST_BOTH;
    } else if ("PASSWORD".equals(constraint)) {
      return CONST_PASSWORD;
    } else if ("CERT".equals(constraint)) {
      return CONST_CERT;
    }
    return CONST_NONE;
  }

  private synchronized int getConstraint(String path) {
    int constraint = 0;
    Iterator iter = _constraints.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry entry = (Map.Entry) iter.next();
      String wildPath = (String) entry.getKey();
      boolean match;

      if (wildPath.startsWith("*")) {
        match = path.endsWith(wildPath.substring(1));
      } else if (wildPath.endsWith("*")) {
        match = path.startsWith(wildPath.substring(0,wildPath.length()-1));
      } else {
        match = path.equals(wildPath);
      }
      if (match) {
        String type = (String) entry.getValue();
        constraint |= convertConstraint(type);
        if ((constraint & CONST_BOTH) == CONST_BOTH) {
          return constraint;
        }
      }
    }
    return constraint;
  }

  private static AuthenticatorBase getAuthenticator(Class authClass) {
    try { 
      Object auth = authClass.newInstance();
      if (auth instanceof AuthenticatorBase) {
        return (AuthenticatorBase) auth;
      }
      throw new IllegalArgumentException("The Class " + 
                                         authClass.getName() + 
                                         " is not an Authenticator");
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException("You don't have access to Class " + 
                                         authClass.getName());
    } catch (InstantiationException e) {
      throw new IllegalArgumentException("Class " + 
                                         authClass.getName() + 
                                         " does not have a default constructor");
    }
  }

  private static AuthenticatorBase getAuthenticator(String authType) {
    // Load our mapping properties if necessary
    getResourceBundle();

    String authName = null;
    try {
      authName = _authenticators.getString(authType);
      return getAuthenticator(Class.forName(authName));
    } catch (MissingResourceException e) {
      throw new IllegalArgumentException("I don't know about the authenticator type: " +
                                         authType);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("I can't find authenticator type (" + 
                                         authType + ") " + e);
    }
  }

  public void setPrimaryAuthenticator(AuthenticatorBase primaryAuth) {
    _primaryAuth = primaryAuth;
//     _primaryAuth.setDebug(100);
  }

  public void setSecondaryAuthenticator(AuthenticatorBase secondaryAuth) {
    _secondaryAuth = secondaryAuth;
//     _secondaryAuth.setDebug(100);
  }

  public void setPrimaryAuthenticatorName(String primaryAuth) {
    setPrimaryAuthenticator(getAuthenticator(primaryAuth));
  }

  public void setSecondaryAuthenticatorName(String secondaryAuth) {
    setSecondaryAuthenticator(getAuthenticator(secondaryAuth));
  }

  public void setPrimaryAuthenticatorClass(Class primaryAuth) {
    setPrimaryAuthenticator(getAuthenticator(primaryAuth));
  }

  public void setSecondaryAuthenticatorClass(Class secondaryAuthClass) {
    setSecondaryAuthenticator(getAuthenticator(secondaryAuthClass));
  }

  public String getErrorPage() { return _loginConfig.getErrorPage(); }
  public void setErrorPage(String errorPage) {
    _loginConfig.setErrorPage(errorPage);
  }

  public String getLoginPage() { return _loginConfig.getLoginPage(); }
  public void setLoginPage(String loginPage) {
    _loginConfig.setLoginPage(loginPage);
  }

  public String getRealmName() { return _loginConfig.getRealmName(); }
  public void setRealmName(String realmName) {
    _loginConfig.setRealmName(realmName);
    KeyRingJNDIRealm.setRealmName(realmName);
  }

  public String getAuthMethod() { return _loginConfig.getAuthMethod(); }
  public void setAuthMethod(String authMethod) {
    _loginConfig.setAuthMethod(authMethod);
    setSecondaryAuthenticatorName(authMethod);
  }

  private synchronized void setContainer() {
    if (_context == null) {
      _context = (Context) getContainer();
      _primaryAuth.setContainer(_context);
      _secondaryAuth.setContainer(_context);
      _context.setLoginConfig(_loginConfig);
    }
  }

  private static synchronized void getResourceBundle() {
    if (_authenticators == null) {
      try {
        _authenticators = ResourceBundle.getBundle
          ("org.apache.catalina.startup.Authenticators");
      } catch (MissingResourceException e) {
        throw new IllegalStateException("Could not open Authenticators setup resource: " + e.getMessage());
      }
    }
  }

  private class ResponseDummy extends HttpResponseWrapper {
    HttpServletResponse _hres;
    HttpResponse        _resp;

    public ResponseDummy(HttpResponse resp, HttpServletResponse hres) {
      super(resp);
      _hres = hres;
      _resp = resp;
      
    }

    public ServletResponse getResponse() {
      return _hres;
    }

    public boolean isAppCommitted() {
      return _resp.isAppCommitted();
    }

    public boolean isError() {
      return _resp.isError();
    }

    public boolean isSuspended() {
      return _resp.isSuspended();
    }

    public void setAppCommitted(boolean appCommitted) {
      _resp.setAppCommitted(appCommitted);
    }

    public void setError() {
      _resp.setError();
    }

    public void setSuspended(boolean suspended) {
      _resp.setSuspended(suspended);
    }

    public javax.servlet.http.Cookie[] getCookies() {
      return _resp.getCookies();
    }

  }

  private class NoErrorResponse extends HttpServletResponseWrapper {
    boolean             _error = false;
    public NoErrorResponse(HttpServletResponse resp) {
      super(resp);
    }
    
    public void sendError(int sc) {
    }
    
    public void sendError(int sc, String msg) {
    }
  }

  private class DummyValveContext implements ValveContext {
    int _invoked = 0;

    public String getInfo() { 
      return "Dummy Valve Context"; 
    }
    public void invokeNext(Request request, Response response) {
      _invoked++;
    }
    public void resetInvokeCount() {
      _invoked = 0;
    }

    public int getInvokeCount() {
      return _invoked;
    }
  }
}
