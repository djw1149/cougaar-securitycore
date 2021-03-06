package safe.util;

import java.awt.*;
import javax.swing.*;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.Enumeration;
import java.util.Vector;
import java.io.*;
import java.net.MalformedURLException;
import safe.policyManager.SetBlackboardObjectServletComponent;

/**
 * The SetBlackboardObjectDialog allows a user to instantiate or modify
 * an object on a remote blackboard. It accepts three fields from the user:
 * className, fieldName, and value.
 * 
 * If an object of the specified class already exists on the blackboard,
 * it will be modified. Otherwise, a new instance will be instantiated,
 * set, and published.
 * 
 * The restrictions are as follows:
 * The specified class must have a 0-argument constructor
 * The specified field must be publically accessible, and be of a type
 * which can accept a String as the value
 * 
 */
public class SetBlackboardObjectDialog extends javax.swing.JDialog
{
	public SetBlackboardObjectDialog(Frame parent)
	{
		super(parent);
		
		// This code is automatically generated by Visual Cafe when you add
		// components to the visual environment. It instantiates and initializes
		// the components. To modify the code, only use code syntax that matches
		// what Visual Cafe can generate, or Visual Cafe may be unable to back
		// parse your Java file into its visual environment.
		//{{INIT_CONTROLS    
		setModal(true);
	        _thisDialog = this;
		setTitle("Edit Blackboard Object Dialog");
		getContentPane().setLayout(null);
		setSize(334,185);
		setVisible(false);
		classNameLabel.setText("Class Name:");
		getContentPane().add(classNameLabel);
		classNameLabel.setBounds(8,11,80,35);
		getContentPane().add(classNameTextField);
		classNameTextField.setBounds(89,16,221,27);
		fieldLabel.setText("Field Name:");
		getContentPane().add(fieldLabel);
		fieldLabel.setBounds(12,46,67,35);
		getContentPane().add(fieldTextField);
		fieldTextField.setBounds(89,48,221,27);
		valueLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		valueLabel.setText("Value:");
		getContentPane().add(valueLabel);
		valueLabel.setBounds(8,84,68,35);
		getContentPane().add(valueTextField);
		valueTextField.setBounds(89,84,221,27);
		oKButton.setText("Ok");
		oKButton.setActionCommand("Ok");
		getContentPane().add(oKButton);
		oKButton.setBounds(90,130,84,33);
		clearButton.setText("Clear");
		clearButton.setActionCommand("Cancel");
		getContentPane().add(clearButton);
		clearButton.setBounds(196,130,84,33);
		//}}
	
		//{{REGISTER_LISTENERS
		SymAction lSymAction = new SymAction();
		oKButton.addActionListener(lSymAction);
		clearButton.addActionListener(lSymAction);
		SymWindow aSymWindow = new SymWindow();
		this.addWindowListener(aSymWindow);
		//}}
	}

	public SetBlackboardObjectDialog()
	{
		this((Frame)null);
	}

	public SetBlackboardObjectDialog(String sTitle)
	{
		this();
		setTitle(sTitle);
	}

	public void setVisible(boolean b)
	{
		if (b)
			setLocation(50, 50);
		super.setVisible(b);
	}

	public void addNotify()
	{
		// Record the size of the window prior to calling parents addNotify.
		Dimension size = getSize();

		super.addNotify();

		if (frameSizeAdjusted)
			return;
		frameSizeAdjusted = true;

		// Adjust size of frame according to the insets
		Insets insets = getInsets();
		setSize(insets.left + insets.right + size.width, insets.top + insets.bottom + size.height);
	}

	// Used by addNotify
	boolean frameSizeAdjusted = false;

	//{{DECLARE_CONTROLS
	javax.swing.JLabel classNameLabel = new javax.swing.JLabel();
	javax.swing.JTextField classNameTextField = new javax.swing.JTextField();
	javax.swing.JLabel fieldLabel = new javax.swing.JLabel();
	javax.swing.JTextField fieldTextField = new javax.swing.JTextField();
	javax.swing.JLabel valueLabel = new javax.swing.JLabel();
	javax.swing.JTextField valueTextField = new javax.swing.JTextField();
	javax.swing.JButton oKButton = new javax.swing.JButton();
	javax.swing.JButton clearButton = new javax.swing.JButton();
	//}}


	class SymAction implements java.awt.event.ActionListener
	{
		public void actionPerformed(java.awt.event.ActionEvent event)
		{
			Object object = event.getSource();
			if (object == oKButton)
				oKButton_actionPerformed(event);
			else if (object == clearButton)
				clearButton_actionPerformed(event);
		}
	}

	void oKButton_actionPerformed(java.awt.event.ActionEvent event)
	{
        String delim = SetBlackboardObjectServletComponent.DELIMITER;
        String strArgs = classNameTextField.getText().trim()
                         + delim + fieldTextField.getText().trim()
                         + delim + valueTextField.getText().trim();
        String httpString = _url + "/setBlackboardObject" + "?" + strArgs;
        try {
            URL url = new URL (httpString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            InputStream in = conn.getInputStream();
            String message = "";
            int i;
            while ((i = in.read()) > -1) {
                message += (char) i;
            }            
            JOptionPane.showMessageDialog(_thisDialog,
                                          message,
                                          "Result", 
                                          JOptionPane.INFORMATION_MESSAGE, null);
        }
        catch (MalformedURLException ex) {
            JOptionPane.showMessageDialog(this,
                                          "MalformedURLException",
                                          " ", 
                                          JOptionPane.ERROR_MESSAGE, null);
            ex.printStackTrace();
        }
        catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                                          "IOException",
                                          " ", 
                                          JOptionPane.ERROR_MESSAGE, null);
            e.printStackTrace();
        }                
	}

	void clearButton_actionPerformed(java.awt.event.ActionEvent event)
	{
        classNameTextField.setText("");
        fieldTextField.setText("");
        valueTextField.setText("");
	}

	class SymWindow extends java.awt.event.WindowAdapter
	{
		public void windowClosing(java.awt.event.WindowEvent event)
		{
			Object object = event.getSource();
			if (object == SetBlackboardObjectDialog.this)
				SetBlackboardObjectDialog_windowClosing(event);
		}
	}

	void SetBlackboardObjectDialog_windowClosing(java.awt.event.WindowEvent event)
	{
		this.dispose();
	    System.exit(0);
	}
    
    public static void main (String[] args)
	{
        if (args.length < 1) {
            System.out.println ("USAGE: SetBlackboardObjectDialog [PolicyManager URL]");
            System.exit(0);
        }
        _url = args[0];
	    
	    (new SetBlackboardObjectDialog()).setVisible(true);
	}
    
    // Variables
    private JDialog _thisDialog;
    protected static String _url;
}
