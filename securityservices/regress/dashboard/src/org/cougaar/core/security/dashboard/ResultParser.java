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

package org.cougaar.core.security.dashboard;

import java.io.*;
import java.util.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class ResultParser
{
  private String resultFile;
  private String summaryFile;
  private XMLReader parser;
  private ResultHandler handler;
  private SummaryHandler summaryHandler;

  public ResultParser(String resultFile, String summaryFile) {
    this.resultFile = resultFile;
    this.summaryFile = summaryFile;
    createParser();
  }

  public void createParser() {
    try {
      // Create SAX 2 parser...
      parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
    }
    catch ( Exception e ) {
      e.printStackTrace();
    }
  }

  public ResultHandler getResultHandler() {
    return handler;
  }
  public SummaryHandler getSummaryHandler() {
    return summaryHandler;
  }

  public void parseResults() {
    // Set the ContentHandler...
    handler = new ResultHandler(parser);
    parser.setContentHandler(handler);

    System.out.println("Parsing result file: " + resultFile);
    try {
      FileInputStream fis = new FileInputStream(resultFile);

      // Parse the file...
      parser.parse(new InputSource(fis));
    }
    catch ( Exception e ) {
      System.out.println("Error: " + e);
      e.printStackTrace();
    }
  }

  public void parseSummary() {
    // Set the ContentHandler...
    summaryHandler = new SummaryHandler(parser);
    parser.setContentHandler(summaryHandler);

    System.out.println("Parsing summary file: " + summaryFile);
    try {
      FileInputStream fis = new FileInputStream(summaryFile);

      // Parse the file...
      parser.parse(new InputSource(fis));
    }
    catch ( Exception e ) {
      System.out.println("Error: " + e);
      e.printStackTrace();
    }
  }

}
