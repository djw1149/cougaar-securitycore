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
 * Created on September 12, 2001, 4:01 PM
 */

package org.cougaar.core.security.services.crypto;

// Cougaar core services
import org.cougaar.core.component.Service;

// Cougaar security services
import org.cougaar.core.security.crypto.SecureMethodParam;
import org.cougaar.core.security.policy.CryptoPolicy;

public interface CryptoPolicyService
  extends Service
{

    public SecureMethodParam getSendPolicy(String source, String target);
    public SecureMethodParam getReceivePolicy(String source, String target);

    public CryptoPolicy getIncomingPolicy(String target);
    public CryptoPolicy getOutgoingPolicy(String source);
    public CryptoPolicy getDataProtectionPolicy(String source);
}

