/*
 * <copyright>
 *  Copyright 2003 Cougaar Software, Inc.
 *  under sponsorship of the Defense Advanced Research Projects Agency *  (DARPA).
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

package org.cougaar.core.security.policy.builder;

import antlr.Token;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class ParsedPolicyFile 
{
  private Map    _declarations;
  private List   _policies;
  private String _prefix;

  public ParsedPolicyFile()
  {
    _prefix       = "";
    _declarations = new HashMap();
    _policies     = new Vector();
  }

  public void setPrefix(String prefix)
  {
    _prefix = prefix;
  }

  public void addPolicy(ParsedPolicy pp)
  {
    pp.setPolicyPrefix(_prefix);
    _policies.add(pp);
  }

  public void declareInstance(String instanceName,
                              String className)
  {
    _declarations.put(instanceName, className);
  }

  public Map  declarations() { return _declarations; }
  public List policies()     { return _policies;     }


  /*-------------------------------------------------------------------
   * Here are some support functions for the policy grammar.  They are
   * a convenieint way of converting tokens from the parsed file
   * into  various formats.  Is there some wat of putting these as private 
   * routines in policyGrammar.g?
   */

  /**
   * This is a utility routine that gives me a shorthand which replaces the 
   * "$" with the string "http://ontology.coginst.uwf.edu/".  I am thinking 
   * of removing this function.
   */
  public static String identifierToURI(Token u)
    throws PolicyCompilerException
  {
    String str = u.getText();
    try {
      if (str.startsWith("$")) {
        str =  str.substring(1, str.length());
        return "http://ontology.coginst.uwf.edu/" + str;
      } else {
        return str.substring(1, str.length());
      }
    } catch (IndexOutOfBoundsException e) {
      PolicyCompilerException pe 
        = new PolicyCompilerException("Malformed URI: " + str + " on line " +
                                      u.getLine());
      throw pe;
    }
  }

  public static int identifierToInt(Token u)
    throws PolicyCompilerException
  {
    String str = u.getText();
    try {
      return Integer.parseInt(str);
    } catch (NumberFormatException e) {
      PolicyCompilerException pe 
        = new PolicyCompilerException("Coding error: Parsing token " + 
                                      str + " on line: " + str);
      throw pe;
    }
  }


  public static String tokenToText(Token u)
  {
    String text = u.getText();
    if (text.startsWith("\"")) {
      return text.substring(1, text.length() - 1);
    } else {
      return text;
    }
  }
}
