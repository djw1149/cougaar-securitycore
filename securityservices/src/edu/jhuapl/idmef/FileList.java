/*
 * <copyright>
 *  Copyright 1997-2002 Network Associates
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

package edu.jhuapl.idmef;

import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * <pre>
 * The FileList class describes files and other file-like objects on
 *  targets.  It is primarily used as a "container" class for the File
 *  aggregate class, as shown in Figure 5.21.
 *
 *                 +--------------+
 *                 |   FileList   |
 *                 +--------------+       1..* +------+
 *                 |              |<>----------| File |
 *                 |              |            +------+
 *                 +--------------+
 *
 *                    Figure 5.21 - The FileList Class
 *
 *  The aggregate class contained in FileList is:
 *
 *  File
 *     One or more.  Information about an individual file, as indicated
 *     by its "category" and "fstype" attributes (see Section 5.2.7.5.1).
 *
 *  This is represented in the XML DTD as follows:
 *
 *     &lt!ELEMENT FileList ( File+ )&gt
 *
 * </pre>
 * @since IDMEF Message v1.0
 */
public class FileList implements XMLSerializable {
  public static String ELEMENT_NAME = "FileList";
    
  public FileList( IDMEF_File []files ){
    m_files = files;
  }
    
  public FileList( Node node ){
    NodeList childList = node.getChildNodes();
    ArrayList fileList = new ArrayList();
    int len = childList.getLength();
    	
    for ( int i = 0; i < len; i++ ){
      Node child = childList.item( i );
      if( child.getNodeName().equals( IDMEF_File.ELEMENT_NAME ) ){
	// there should be one impact element
	fileList.add( new IDMEF_File( child ) );
      }
    }
	    
    int size = fileList.size();
    if( size > 0 ){ 
      m_files = new IDMEF_File[ size ];
      for( int i = 0; i < size; i++ ){
	m_files[ i ] = ( IDMEF_File )fileList.get( i );
      }
    }
  }
     
  public IDMEF_File []getFiles(){
    return m_files;
  }
  public void setFiles( IDMEF_File []files ){
    m_files = files;
  }
  
  public boolean contains (IDMEF_File infile) {
    boolean contains=false;
    IDMEF_File[] files=this.getFiles();
    if(files==null) {
      return contains;
    }
    IDMEF_File file;
    for(int i=0;i<files.length;i++) {
      file=files[i];
      if(file.equals(infile)) {
	contains=true;
	return contains;
      }
    }
    return contains;
  }

  
  public boolean equals(Object anObject) {
    boolean equals=false;
    boolean arefileListequal=false;
    FileList fileList;
    if(anObject==null) {
      return equals;
    }
    if( anObject instanceof FileList) {
      fileList=(FileList) anObject;
      IDMEF_File [] myarray;
      IDMEF_File [] inarray;
      myarray=this.getFiles();
      inarray=fileList.getFiles();
      if((myarray!=null)&&(inarray!=null)) {
	if(myarray.length==inarray.length) {
	  IDMEF_File file;
	  for(int i=0;i<inarray.length;i++) {
	    file=inarray[i];
	    if(!contains(file)) {
	      arefileListequal=false;
	      break;
	    }
	  }
	  arefileListequal=true;
	}
      }
      else if((myarray==null) && (inarray==null)) {
	arefileListequal=true;
      }
      if(arefileListequal) {
	equals=true;
      }
    }
    return equals;
  }
    
  public Node convertToXML( Document parent ) {
    Element fileListNode = parent.createElement( ELEMENT_NAME );
    int len = m_files.length;
        
    for( int i = 0; i < len; i++ ){
      fileListNode.appendChild( m_files[ i ].convertToXML( parent ) );
    }           
    return fileListNode;
  }
    
  private IDMEF_File m_files[];
}
