/*
 * <copyright>
 *  Copyright 1997-2003 Cougaar Software, Inc.
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

import org.cougaar.core.service.ThreadService;

// overlay classes
import org.cougaar.core.security.constants.IdmefClassifications;

// securityservices classes
import org.cougaar.core.security.auth.ExecutionContext;
import org.cougaar.core.security.monitoring.publisher.EventPublisher;
import org.cougaar.core.security.monitoring.publisher.IdmefEventPublisher;
import org.cougaar.core.security.monitoring.event.FailureEvent;
import org.cougaar.core.security.services.auth.SecurityContextService;

/**
 * This class must be placed in the Node ini file to allow
 * the DataProtectionService to report data protection failures.
 * This class reports the sensor capabilities to the enclave security
 * manager.
 *
 * Add the following line to your Node ini file's Plugins section:
 * <pre>
 * plugin = org.cougaar.core.security.monitoring.plugin.DataProtectionSensor
 * </pre>
 * The plugin also takes an optional parameter indicating the role
 * of the security manager to report to. The default is "Manager".
 * The communities that the capabilities are sent to are all the ones that
 * this sensor belongs to.
 */
public class DataProtectionSensor extends  SensorPlugin
{
  private static final String[] CLASSIFICATIONS = {
    IdmefClassifications.DATA_FAILURE
  };

  protected SensorInfo getSensorInfo() {
    if(_sensorInfo == null) {
      _sensorInfo = new DPSensorInfo();
    }
    return _sensorInfo;
  }

  protected String []getClassifications() {
    return CLASSIFICATIONS;
  }

  protected boolean agentIsTarget() {
    return false;
  }

   protected boolean agentIsSource() {
    return false;
  }

  public void setThreadService(ThreadService ts) {
    _threadService = ts;
  }

  /**
   * Register this sensor's capabilities, and initialize the services that need to
   * to publish message failure events to this plugin's blackboard.
   *
   */
  protected void setupSubscriptions() {
    super.setupSubscriptions();
    //initialize the EventPublisher in the following services
    // need to get the execution context for this sensor for publishing idmef event
    EventPublisher publisher =
      new IdmefEventPublisher(_blackboard, _scs, _cmrFactory, _log, getSensorInfo(), _threadService);
    setPublisher(publisher);
    publishIDMEFEvent();
  }

  public static void publishEvent(FailureEvent event) {
    publishEvent(DataProtectionSensor.class, event);
  }

  private class DPSensorInfo implements SensorInfo {
    public String getName(){
      return "DataProtectionSensor";
    }
    public String getManufacturer(){
      return "CSI";
    }
    public String getModel(){
      return "Cougaar Data Protection Failure Sensor";
    }
    public String getVersion(){
      return "1.0";
    }
    public String getAnalyzerClass(){
      return "Cougaar Security";
    }
  }

  private SensorInfo _sensorInfo;
  private ThreadService _threadService;
}

