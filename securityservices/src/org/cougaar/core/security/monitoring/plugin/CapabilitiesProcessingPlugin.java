/*
 * <copyright>
 *  Copyright 1997-2001 Network Associates
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


package org.cougaar.core.security.monitoring.plugin;

import java.util.Enumeration;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;
import java.util.Iterator;

// Cougaar core services
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.component.ServiceBroker;

import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.service.*;
import org.cougaar.core.security.monitoring.idmef.*;
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.core.security.monitoring.blackboard.*;
import edu.jhuapl.idmef.*;



/**
 * A predicate that matches all "Event object with registration "
 */
class CapabilitiesPredicate implements UnaryPredicate{
  public boolean execute(Object o) {
    boolean ret = false;
    if (o instanceof Event ) {
      Event e=(Event)o;
      IDMEF_Message msg=e.getEvent();
      if(msg instanceof Registration){
	return true;
      }
    }
    return ret;
  }
}
/**
   Subscribes to Relay with RegistrationAlert 
 */
class CapabilitiesRelayPredicate implements UnaryPredicate{
  public boolean execute(Object o) {
    boolean ret = false;
    if (o instanceof CmrRelay ) {
      CmrRelay relay = (CmrRelay)o;
     if(relay.getContent() instanceof Event) {
	Event event = (Event)relay.getContent();
	ret = (event.getEvent() instanceof Registration);
      }
      else {
	return ret;
      }
    }
    return ret;
  }
}

/**
 * A predicate that matches all CapabilitiesObject
 */
class CompleteCapabilitiesPredicate implements UnaryPredicate{
  public boolean execute(Object o) {
    boolean ret = false;
    if (o instanceof CapabilitiesObject ) {
      return true;
    }
    return ret;
  }
}

/**
 * Predicate that matches all ConsolidateCapabilities
 *
 */
class ConsolidatedCapabilitiesPredicate implements UnaryPredicate{
  public boolean execute(Object o) {
    boolean ret = false;
    if (o instanceof Event ) {
      Event e=(Event)o;
      IDMEF_Message msg=e.getEvent();
      if(msg instanceof AgentRegistration){
	return true;
      }
    }
    return ret;
  }
}

/**
 *
 **/

public class CapabilitiesProcessingPlugin
  extends ComponentPlugin
{

  // The domainService acts as a provider of domain factory services
  private DomainService domainService = null;
  private IncrementalSubscription capabilities;
  private IncrementalSubscription completecapabilities;
  private IncrementalSubscription subordinatecapabilities;
  private IncrementalSubscription capabilitiesRelays;
  private int firstobject=0;
  private LoggingService loggingService;
  private MessageAddress myAddress=null;
  private Object param;
  private String mgrrole=null;
  private MessageAddress mgrAddress;

  /**
   * Used by the binding utility through reflection to set my DomainService
   */
  public void setDomainService(DomainService aDomainService) {
    domainService = aDomainService;
  }

  /**
   * Used by the binding utility through reflection to get my DomainService
   */
  public DomainService getDomainService() {
    return domainService;
  }
  
  public void setParameter(Object o){
    this.param=o;
  }

  public java.util.Collection getParameters() {
    return (Collection)param;
  }


        
  /**
   * 
   */
  protected void setupSubscriptions() {
    loggingService = (LoggingService)getBindingSite().getServiceBroker().getService
      (this, LoggingService.class, null);
    myAddress = getBindingSite().getAgentIdentifier();
    loggingService.debug("setupSubscriptions of CapabilitiesProcessingPlugin called :"+ myAddress.toString()); 
    Collection col=getParameters();
    if(col.size()>1) {
      loggingService.debug("setupSubscriptions of CapabilitiesProcessingPlugin called  too many parameters from:"
			   + myAddress.toString()); 
    }
    if(col.size()!=0){
      String params[]=new String[1];
      String parameters[]=(String[])col.toArray(new String[0]);
      mgrrole=parameters[0];
      
    }
    
    //System.out.println(" got Role as  in cpp%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"+role);
    CapabilitiesObject object=new CapabilitiesObject();
    getBlackboardService().publishAdd(object);
    capabilities= (IncrementalSubscription)getBlackboardService().subscribe
      (new CapabilitiesPredicate());
    capabilitiesRelays= (IncrementalSubscription)getBlackboardService().subscribe
      (new CapabilitiesRelayPredicate());
    completecapabilities= (IncrementalSubscription)getBlackboardService().subscribe
      (new CompleteCapabilitiesPredicate() );
    subordinatecapabilities=(IncrementalSubscription)getBlackboardService().subscribe
      (new ConsolidatedCapabilitiesPredicate()); 
    //getBlackboardService().publishAdd(object);
    //published=true

  }


  /**
   * Top level plugin execute loop.  
   */
  protected void execute () {
    // process unallocated tasks
    loggingService.debug("  execute of Capabilities processing plugin called @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@"+ myAddress.toString());
    updateRelayedCapabilities();
    Event event=null;
    RegistrationAlert registration=null;
    CapabilitiesObject capabilitiesobject=null;
    Collection  capabilities_col=completecapabilities.getCollection();
    ArrayList list=new ArrayList(capabilities_col);
    boolean modified=false;

    if((list==null)||(list.size()==0)){
      if(loggingService.isDebugEnabled())
	loggingService.debug("No capabilities object present in capabilities processing plugin : RETURNING !!!!!!!!!!!"+ myAddress.toString());
      return;
    }

    if(list.size()>1) {
      if(loggingService.isDebugEnabled()) {
	loggingService.debug(" Error Multiple capabilities  object on blackboard  CapabilitiesprocessingPlugin in agent::"
			     + myAddress.toString());
	loggingService.debug("CONFUSION ......  CONFUSION!!!!!!!!!!!!! Exiting !!!!!!!!:");
      }
      return;
   
    }
		
    capabilitiesobject=(CapabilitiesObject)list.get(firstobject);
    Enumeration capabilities_enum = capabilities.getAddedList();
    Collection subcol= subordinatecapabilities.getAddedCollection();
      loggingService.debug(" got collection size for sub is :"+subcol.size());
    Iterator  subordinatecapabilities_enum=subcol.iterator();
    //Enumeration subordinatecapabilities_enum = subordinatecapabilities.getAddedList();
    Analyzer analyzer=null;
    String analyzer_id=null;
    /*
      Process capabilities received from within the agent
    */
    while(capabilities_enum.hasMoreElements()){
      event=( Event)  capabilities_enum.nextElement();
      if(loggingService.isDebugEnabled())
	loggingService.debug("Event received is  :"+ event.toString() + "\n in agent "+myAddress.toString());
      registration=(RegistrationAlert)event.getEvent();
      /*
      if(loggingService.isDebugEnabled()) {
	if(registration!=null) {
	  loggingService.debug("registration was not null is :"+ registration.toString());
	}
	else {
	  loggingService.debug("registration was  null is :");
	}
	//loggingService.debug(" registration is :"+ registration.toString());
      }
      */
      analyzer=registration.getAnalyzer();
      analyzer_id=analyzer.getAnalyzerid();
      if(loggingService.isDebugEnabled())
	loggingService.debug(" Got analyzer id #####################"+ analyzer_id );

      if(capabilitiesobject.containsKey(analyzer_id)) {
	if(loggingService.isDebugEnabled())
	  loggingService.debug("Analyzer is registered. registering Analyzer again in agent :"+myAddress.toString() 
			       +" analyzer id" + analyzer_id );
	RegistrationAlert existingregistartion=(RegistrationAlert)capabilitiesobject.get(analyzer_id);
	if(registration.getOperation_type()==IdmefMessageFactory.addtoregistration)  {
	  if(loggingService.isDebugEnabled()) {
	    loggingService.debug(" registration type is add");
	    printConsolidation(existingregistartion.getClassifications(),"!!!!!before  adding add reg object"); 
	    printConsolidation(registration.getClassifications(),"!!!!!New  add reg object");
	  }
	  existingregistartion= addtoRegistartion(existingregistartion,registration);
	  if(loggingService.isDebugEnabled())
	    printConsolidation(existingregistartion.getClassifications(),"After adding add reg object"); 
	  //capabilitiesobject.put(analyzer_id,registration);
	}
	if(registration.getOperation_type()==IdmefMessageFactory.removefromregistration)  {
	  if(loggingService.isDebugEnabled())
	    loggingService.debug(" registration type is remove");
	  // printConsolidation(existingregistartion.getClassifications(),"!!!!!before removing  remove reg object"); 
	  //printConsolidation(registration.getClassifications(),"!!!!!New remove  reg object"); 
	  existingregistartion= removefromRegistartion(existingregistartion,registration); 
	  // printConsolidation(existingregistartion.getClassifications(),"After removing remove reg object");
	  // existingregistartion= removefromRegistartion(existingregistartion,registration); 
	}
	modified=true;
	capabilitiesobject.put(analyzer_id,existingregistartion);
      }
      else {
	if(loggingService.isDebugEnabled())
	  loggingService.debug("Analyzer is not yet registered. registering Analyzer  in agent :"+myAddress.toString() 
			       +" analyzer id" + analyzer_id );
	modified=true;
	//printConsolidation(registration.getClassifications(),"!!!!!Classification before reg first time @@@@"); 
	capabilitiesobject.put(analyzer_id,registration);
      }

    }
    /**
       Process capabilities received from subordinate agent 
    */
    ConsolidatedCapabilities consolidatedcapabilities;
    while(subordinatecapabilities_enum.hasNext())  {
      event=( Event)  subordinatecapabilities_enum.next();
      if(event.getSource().equals(myAddress)) {
	loggingService.debug(" $$$$$ recived event from my source address :"+myAddress.toString()
			     +" event is :"+event.toString());
	continue;
      }
      consolidatedcapabilities=(ConsolidatedCapabilities)event.getEvent(); 
      if(loggingService.isDebugEnabled())
	loggingService.debug(" got consolidatedcapabilities in agent :>"+myAddress.toString() +
			     "consolidated capabilities "+ consolidatedcapabilities.toString()+
			     "Analyzer id is "+ consolidatedcapabilities.getAnalyzer().getAnalyzerid());
      /*
	Not sure what to get .. Should we do ClusterIdentifier.toString() or ClusterIdentifier.toAddress()
      */
      String agent_id= event.getSource().toAddress();
      if(loggingService.isDebugEnabled())      
        loggingService.debug(" got agent id as :===============================>"+ agent_id);
      /*
	Consolidating capabilities from subordinate agent 
      */
      analyzer_id=consolidatedcapabilities.getAnalyzer().getAnalyzerid();
      if(capabilitiesobject.containsKey(analyzer_id)) {
	if(loggingService.isDebugEnabled())
	  loggingService.debug(" Agent is already registered :");
	//System.out.println("Agent is already registered :"); 
	capabilitiesobject.put(analyzer_id,getRegistrationAlert(consolidatedcapabilities));
	if(loggingService.isDebugEnabled())
	  loggingService.debug(" replacing !!!!!!!!!!!!!!! RegistrationAlert foranalyzer id :"+ analyzer_id );
	//System.out.println(" replacing !!!!!!!!!!!!!!! RegistrationAlert foranalyzer id :"+ analyzer_id );
	modified=true;
      }
      else {
	//System.out.println(" Agent is not  registered :" + analyzer_id);
	capabilitiesobject.put(analyzer_id,getRegistrationAlert(consolidatedcapabilities));
	loggingService.debug(" Agent is not  registered :" + analyzer_id);
	modified=true;
      }
    }
    if(modified) {
      loggingService.debug(" CAPABILITIES object is modified publishing change from agent :"+ myAddress.toString());
      // System.out.println(" CAPABILITIES object is modified publishing change from agent :"+ myAddress.toString())
      getBlackboardService().publishChange(capabilitiesobject);
    }
	
  }


  public void printConsolidation(Classification[] classifications, String msg) {
    loggingService.debug(msg);
    Classification classification=null;
    for(int i=0;i<classifications.length;i++){
      classification= classifications[i];
      converttoString( classification);
    }
  }

    
  public void converttoString(Classification classification) {
    loggingService.debug(" Classification origin :"+classification.getOrigin());
    loggingService.debug(" Classification Name :"+classification.getName());
    loggingService.debug(" Classification URL :"+classification.getUrl());
  }


  public RegistrationAlert addtoRegistartion(RegistrationAlert existingregObject,RegistrationAlert newregobject) {
	
    Classification [] existingClassifications=existingregObject.getClassifications();
    Source[] existingSources=existingregObject.getSources();
    Target[]existingTargets=existingregObject.getTargets();
    AdditionalData[] existingData=existingregObject.getAdditionalData();
    Classification[] classifications=newregobject.getClassifications();
    Source[] sources=newregobject.getSources();
    Target[] targets=newregobject.getTargets();
    AdditionalData[]data=newregobject.getAdditionalData();
    int existinglength=existingClassifications.length;
    int newlength=classifications.length;
    if(classifications!=null) {
      Classification[] updatedclassification=new Classification[existinglength +newlength];
      System.arraycopy(existingClassifications,0,updatedclassification,0,existinglength);
      System.arraycopy(classifications,0,updatedclassification,existinglength,newlength);
      existingregObject.setClassifications(updatedclassification);
	   
    }

    if(sources!=null) {
      existinglength=existingSources.length;
      newlength=sources.length;
      Source[] updatedsources=new Source[existinglength +newlength];
      System.arraycopy(existingSources,0,updatedsources,0,existinglength);
      System.arraycopy(sources,0,updatedsources,existinglength,newlength);
      existingregObject.setSources(updatedsources);
	    
    }

    if(targets!=null) {
      existinglength=existingTargets.length;
      newlength=targets.length;
      Target[] updatedtargets=new Target[existinglength +newlength];
      System.arraycopy(existingTargets,0,updatedtargets,0,existinglength);
      System.arraycopy(targets,0,updatedtargets,existinglength,newlength);
      existingregObject.setTargets(updatedtargets); 
	    
    }

    if(data!=null) {
	    
    }

    return existingregObject;
	
  }

 
  public RegistrationAlert removefromRegistartion(RegistrationAlert existingregObject,RegistrationAlert newregobject) {
    Classification [] existingClassifications=existingregObject.getClassifications();
    Source[] existingSources=existingregObject.getSources();
    Target[]existingTargets=existingregObject.getTargets();
    AdditionalData[] existingData=existingregObject.getAdditionalData();
    Classification[] classifications=newregobject.getClassifications();
    Source[] sources=newregobject.getSources();
    Target[] targets=newregobject.getTargets();
    AdditionalData[]data=newregobject.getAdditionalData();
    int existinglength=existingClassifications.length;
    int newlength=classifications.length;
    if(classifications!=null){
	    
      //loggingService.debug("classifications is not null !!!!!!!!!!");
      Classification newclassification=null;
      Classification existingclassification=null;
      Vector modifiedclassification=new Vector();
      boolean found= false;
      int foundindex=-1;
      for(int i=0;i<newlength;i++) {
	newclassification=classifications[i];
	found=false;
	foundindex=-1;
	for(int j=0;j<existinglength;j++) {
	  existingclassification=existingClassifications[j];
	  if(!existingclassification.equals(newclassification)){
	    continue;
	  }
	  loggingService.debug("Found classification to remove:!!!!!!!!!!!!!!!!!");
	  found=true;
	  foundindex=j;
	  break;
	}
		 
	if((found)&&(foundindex!=-1)) {
	  loggingService.debug(" Found classification to remove at :"+foundindex);
	  Classification modifiedClassifications[]=new Classification[existinglength-1];
	  System.arraycopy(existingClassifications,0,modifiedClassifications,0,foundindex);
	  /* doing an array copy till the index where classification is found and skiping 
	     the index where classification is found in existing classification. If the index 
	     is the last one there is no need to copy the last classification */
	  if((foundindex+1)!=existinglength)
	    System.arraycopy(existingClassifications,foundindex+1,modifiedClassifications,foundindex,existinglength-1);
	  // printConsolidation(modifiedClassifications,"After removing :###########");
	  existinglength=modifiedClassifications.length;
	  existingClassifications=modifiedClassifications;
		    
		     
	}
      }
      existingregObject.setClassifications(existingClassifications);
	     
    }
    if(sources!=null) {
      existinglength=existingSources.length;
      newlength=sources.length;
	    
    }
    if(targets!=null) {
      existinglength=existingTargets.length;
      newlength=targets.length;
    }
    if(data!=null) {
	    
    }
	
    return existingregObject;

  }

  /*
    Reads RegistrationAlert relay and if it is not local then  it publish the content to the 
    blackboard
  */
  private void updateRelayedCapabilities() {
    if (capabilitiesRelays.hasChanged()) {
      if (loggingService.isDebugEnabled())
	loggingService.debug("capabilitiesRelays has changed in  "+ myAddress.toString());
      CmrRelay relay;
      // New relays
      Iterator iter = capabilitiesRelays.getAddedCollection().iterator();
      while (iter.hasNext()) {
	relay = (CmrRelay)iter.next();
	if (!relay.getSource().equals(myAddress)) { // make sure it's remote, not local
	  if (loggingService.isDebugEnabled())
	    loggingService.debug(" printing received relay which is not LOCAL :===================>"+
				 relay.getContent().toString());
	  getBlackboardService().publishAdd(relay.getContent());
	}
      }
           
      // Changed relays
      iter = capabilitiesRelays.getChangedCollection().iterator();
      while (iter.hasNext()) {
	relay = (CmrRelay)iter.next();
	if (!relay.getSource().equals(myAddress)) {
	  Event oldCapabilities = findEventFrom(relay.getSource());
	  if (oldCapabilities != null)
	    getBlackboardService().publishRemove(oldCapabilities);
	  getBlackboardService().publishAdd(relay.getContent());
	}
      }
      // Removed relays
      iter = capabilitiesRelays.getRemovedCollection().iterator();
      while (iter.hasNext()) {
	relay = (CmrRelay)iter.next();
	if (!relay.getSource().equals(myAddress)) {
	  Event oldCapabilities = findEventFrom(relay.getSource());
	  if (oldCapabilities != null)
	    getBlackboardService().publishRemove(oldCapabilities);
	}
      }
    }
  }
  
  /**
   * Find the previous AgentRegistration Event from this source (if any)
   */
  private Event findEventFrom(MessageAddress source) {
    Iterator iter = this.capabilities.iterator();
    while (iter.hasNext()) {
      Event event = (Event)iter.next();
      if (event.getSource().equals(source))
	return event;
    }
    return null;
  }

  public RegistrationAlert getRegistrationAlert(ConsolidatedCapabilities cons) {
    DomainService service=getDomainService();
    if(service==null) {
      loggingService.debug(" Got service as null in  getRegistrationAlert of Capabilities ProcessingPlugin  :"+ myAddress.toString());
      return null;
    }
    CmrFactory factory=(CmrFactory)getDomainService().getFactory("cmr");
    IdmefMessageFactory imessage=factory.getIdmefMessageFactory();
      
    RegistrationAlert ralert= null;
    if(mgrrole!=null) {
      ralert=imessage.createRegistrationAlert(cons.getAnalyzer(),
					      cons.getSources(),
					      cons.getTargets(),
					      cons.getClassifications(),
					      cons.getAdditionalData(),
					      cons.getIdent(),
					      IdmefMessageFactory.newregistration,
					      IdmefMessageFactory.EnclaveMgrType,
					      cons.getAgentName());
    }
    else {
      ralert=imessage.createRegistrationAlert(cons.getAnalyzer(),
					      cons.getSources(),
					      cons.getTargets(),
					      cons.getClassifications(),
					      cons.getAdditionalData(),
					      cons.getIdent(),
					      IdmefMessageFactory.newregistration,
					      IdmefMessageFactory.SocietyMgrType,
					      cons.getAgentName());
    }
    return ralert;
  }

}
