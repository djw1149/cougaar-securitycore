/*
 * <copyright>
 *  Copyright 1997-2001 Networks Associates Inc
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


/**
 *
 * @author  rtripath
 * @version 
 */
package com.nai.security.crypto;

import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.cluster.IncrementalSubscription;
import org.cougaar.core.plugin.SimplePlugIn;
import org.cougaar.core.society.UID;
import org.cougaar.core.security.crypto.PublicKeyRing;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;


public class CRLGUIPlugIn extends SimplePlugIn
{

    
  static JFrame frame;


  // Variables declaration - 
  private javax.swing.JPanel jListPanel;
  private javax.swing.JList jCrlList;
  private javax.swing.JButton jCrlButton;
  private javax.swing.JPanel jTimerPanel;
  private javax.swing.JTextField jTimerTextField;
  private javax.swing.JButton jTimerButton;
  private javax.swing.JComboBox jTimerComboBox;
  private javax.swing.JLabel jTimerLabel;
  private String units=new String("Seconds");

  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the FormEditor.
   */
  private void initGuiComponents() 
  {
    frame =new JFrame(getGUITitle());
    frame.setLocation(0,0);


    //  Creates the complete gui
    String [] timedef={"Hours","Minutes","Seconds"};

    jListPanel = new javax.swing.JPanel();
    jCrlList = new javax.swing.JList();
    jCrlButton = new javax.swing.JButton();
    jTimerPanel = new javax.swing.JPanel();
    jTimerTextField = new javax.swing.JTextField();
    jTimerButton = new javax.swing.JButton();
    jTimerComboBox = new javax.swing.JComboBox(timedef);
    jTimerComboBox.setSelectedIndex(jTimerComboBox.getItemCount()-1);
    jTimerLabel = new javax.swing.JLabel();
    frame.getContentPane().setLayout(null);
        
    frame.addWindowListener(new java.awt.event.WindowAdapter(){
	public void windowClosing(java.awt.event.WindowEvent evt) 
	{
	  exitWindow(evt);
	}
      });
        
    jListPanel.setLayout(null);
    jListPanel.setPreferredSize(new Dimension(120,150));
    jListPanel.setMinimumSize(new Dimension(120,150));
    jListPanel.setBounds(0,0,120,150);
    jListPanel.add(jCrlList);
    jCrlList.setBounds(10,5, 110,100);
          
          
    jCrlButton.setName("JgetcrlButton");
    jCrlButton.setText("Get Crl List");
    jCrlButton.addMouseListener(new java.awt.event.MouseAdapter() {
	public void mouseClicked(java.awt.event.MouseEvent evt) 
	{
	  jCrlButtonMouseClicked(evt);
	}
      });
    jListPanel.add(jCrlButton);
    jCrlButton.setBounds(10, 110, 100, 22);
          
    Vector empty=new Vector();
    empty.addElement("Empty"); 
    jCrlList.setListData(empty);
    frame.getContentPane().add(jListPanel);
        
    jTimerPanel.setLayout(null);
    jTimerPanel.setPreferredSize(new Dimension(300,150));
    jTimerPanel.setMinimumSize(new Dimension(300,150));
    jTimerPanel.setBounds(120,0,300,150);
    jTimerTextField.setText("20");
    jTimerPanel.add(jTimerTextField);
    jTimerTextField.setBounds(15, 60, 100, 22);
          
    jTimerButton.setText("Set Interval");
    jTimerButton.addMouseListener(new java.awt.event.MouseAdapter() {
	public void mouseClicked(java.awt.event.MouseEvent evt) 
	{
	  jTimerButtonMouseClicked(evt);
	}
      });
    jTimerPanel.add(jTimerButton);
    jTimerButton.setBounds(60, 100, 140, 25);
    jTimerComboBox.addActionListener(new java.awt.event.ActionListener() {
	public void actionPerformed(java.awt.event.ActionEvent evt) 
	{
	  jTimerComboBoxactionPerformed(evt);
	}
      });
    jTimerPanel.add(jTimerComboBox);
    jTimerComboBox.setBounds(125, 60, 100, 22);
          
    jTimerLabel.setText(" Set Interval Time for CRL");
    jTimerLabel.setFont(new java.awt.Font ("Dialog", 1, 14));
    jTimerPanel.add(jTimerLabel);
    jTimerLabel.setBounds(10,5, 300, 50);
    
          
    frame.getContentPane().add(jTimerPanel);
    frame.pack();
    frame.setBounds(0,0,400,170);
    frame.setVisible(true);
        
  }
  protected final void setupSubscriptions()
  {
    initGuiComponents();
  }
  public final void execute()
  {
		
  }
  protected String getGUITitle()
  {
    return "CRLGUIPlugIn";
  }

  private void jTimerButtonMouseClicked(java.awt.event.MouseEvent evt) 
  {
    long sleep_time=1000L;
    int sleeptime=0;
    String texttimer=jTimerTextField.getText();
    try
      {
	sleeptime=Integer.parseInt(texttimer.trim());
      }
    catch(NumberFormatException nexp)
      {
	sleeptime=1;
	nexp.printStackTrace();
      }
    if(units.equalsIgnoreCase("seconds"))
      {
	sleep_time= sleeptime*1000L;
      }
    else if(units.equalsIgnoreCase("Minutes"))
      {
	sleep_time=sleeptime*1000L*60L;
			
      }
    else if(units.equalsIgnoreCase("Hours"))
      {
			
	sleep_time=sleeptime*1000L*60L*60L;
      }
    PublicKeyRing.setSleeptime(sleep_time);
  }

  private void jTimerComboBoxactionPerformed(java.awt.event.ActionEvent evt)
  {

    units=(String)jTimerComboBox.getSelectedItem();
  }

  private void jCrlButtonMouseClicked(java.awt.event.MouseEvent evt) 
  {
    Vector crllist=PublicKeyRing.getCRL();
    jCrlList.setListData(crllist);
  
  }

 
  /** Exit the Application */
  private void exitWindow(java.awt.event.WindowEvent evt) 
  {
    System.exit (0);
  }
  public static void main(String args[])
  {
    CRLGUIPlugIn crlpgin=new CRLGUIPlugIn();
    crlpgin.initGuiComponents();
    frame.show();
  }
     

}
