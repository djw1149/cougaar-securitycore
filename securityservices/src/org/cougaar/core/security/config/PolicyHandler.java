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

package org.cougaar.core.security.config;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.Array;

// Cougaar core infrastructure
import org.cougaar.util.ConfigFinder;

// Cougaar security services
import org.cougaar.core.security.policy.*;
import org.cougaar.core.security.util.*;
import org.cougaar.core.security.services.util.*;
import org.cougaar.core.security.provider.SecurityServiceProvider;

public class PolicyHandler
{
  private SecurityPropertiesService secprop = null;
  private ByteArrayInputStream newPolicyInputStream;
  private ByteArrayOutputStream policy;
  private ConfigParserService configParser = null;

  protected static final String POLICIES_TAG = "policies";

  /** Default parser name. */
  protected static final String DEFAULT_PARSER_NAME =
  "org.apache.xerces.parsers.SAXParser";
  
  /** Lexical handler property id (http://xml.org/sax/properties/lexical-handler).
   */
  protected static final String LEXICAL_HANDLER_PROPERTY_ID =
  "http://xml.org/sax/properties/lexical-handler";

  public PolicyHandler(ConfigParserService configParser) {
    secprop = SecurityServiceProvider.getSecurityProperties(null);
    this.configParser = configParser;
  }

  public void addCaPolicy(Hashtable attributeTable) {
    ConfigFinder confFinder = new ConfigFinder();
    File file = null;

    file = confFinder.locateFile("caPolicyTemplate.xml");
    String xmlTemplateFile = file.getPath();

    file = configParser.findPolicyFile("cryptoPolicy.xml");
    String policyFile = file.getPath();

    // First, read the XML template
    ByteArrayOutputStream newPolicyOutputStream =
      parseXmlTemplate(xmlTemplateFile, attributeTable);

    System.out.println("NEW CA POLICY:");
    System.out.println(newPolicyOutputStream.toString());

    ConfigWriter writer = new ConfigWriter();
    writer.replaceAttributes(false);
    writer.replaceJavaProperties(false);

    // Add the new policy
    writer.insertNodeAfterTag(POLICIES_TAG, newPolicyOutputStream);

    ByteArrayOutputStream newPolicy = new ByteArrayOutputStream();
    try {
      writer.setOutput(newPolicy, "US-ASCII");
    }
    catch (UnsupportedEncodingException e) {
      System.out.println("error: Unable to set output.");
      return;
    }
    System.out.println("Parsing policy file");
    parseXmlFile(policyFile, writer);
    System.out.println("Parsing policy file done");

    FileOutputStream newPolicyFile = null;
    file = configParser.findWorkspacePolicyPath("cryptoPolicy.xml");
    try {
      newPolicyFile = new FileOutputStream(file); 
      newPolicyFile.write(newPolicy.toByteArray());
    }
    catch (IOException e) {
      System.out.println("Unable to open policy file for modification");
      return;
    }
  }

  public ByteArrayOutputStream parseXmlTemplate(String xmlTemplateFile,
						Hashtable attributeTable) {

    ConfigWriter writer = new ConfigWriter();
    writer.replaceAttributes(true);
    writer.replaceJavaProperties(true);
    writer.setAttributeTable(attributeTable);
    writer.setXmlHeader(false);
    ByteArrayOutputStream newPolicyOutputStream = new ByteArrayOutputStream(); 
    try {
      writer.setOutput(newPolicyOutputStream, "UTF8");
    }
    catch (UnsupportedEncodingException e) {
      System.err.println("error: Unable to set output.");
    }
    parseXmlFile(xmlTemplateFile, writer);

    return newPolicyOutputStream;
  }

  public void parseXmlFile(String xmlTemplateFile,
			   ConfigWriter writer) {
    XMLReader parser = null;
    try {
      parser = XMLReaderFactory.createXMLReader(DEFAULT_PARSER_NAME);
    }
    catch (Exception e) {
      System.err.println("error: Unable to instantiate parser ("+DEFAULT_PARSER_NAME+")");
    }
    // set parser
    parser.setContentHandler(writer);
    parser.setErrorHandler(writer);
    try {
      parser.setProperty(LEXICAL_HANDLER_PROPERTY_ID, writer);
    }
    catch (SAXException e) {
      // ignore
    }

    try {
      parser.parse(xmlTemplateFile);
    }
    catch (SAXParseException e) {
      // ignore
    }
    catch (Exception e) {
      System.err.println("error: Parse error occurred - "+e.getMessage());
      if (e instanceof SAXException) {
	e = ((SAXException)e).getException();
      }
      e.printStackTrace(System.err);
    }
  }
}

