/*
 * <copyright>
 *  Copyright 1997-2003 Cougaar Software, Inc.
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


package org.cougaar.core.security.test.blackboard;


import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.glm.ldm.oplan.OrgActivity;
import org.cougaar.util.UnaryPredicate;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;


/**
 * Tries to modify a org activity. Must have query permission for the org
 * activity and not have the modify permission
 *
 * @author ttschampel
 */
public class MaliciousBlackboardModifyPlugin extends AbstractBlackboardPlugin {
  private static final String ACTIVITY_NAME = "MaliciousBlackboardModifyPlugin";
  private IncrementalSubscription orgSubs = null;
  private UnaryPredicate changedPredicate = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof OrgActivity) {
          OrgActivity orgA = (OrgActivity) o;
          return orgA.getActivityName().equals(ACTIVITY_NAME);
        }

        return false;
      }
    };

  /**
   * DOCUMENT ME!
   */
  public void load() {
    super.load();
    this.setPluginName("MaliciousBlackboardModifyPlugin");
  }


  /**
   * DOCUMENT ME!
   */
  public void setupSubscriptions() {
    super.setupSubscriptions();
    orgSubs = (IncrementalSubscription) getBlackboardService().subscribe(changedPredicate);
  }


  /**
   * DOCUMENT ME!
   */
  public void execute() {
    super.execute();
    checkModified();
  }


  private void checkModified() {
    Enumeration enumeration = orgSubs.getChangedList();
    while (enumeration.hasMoreElements()) {
      this.successes--;
      this.failures++;
      this.createIDMEFEvent(pluginName,
        "Able to modify OrgActivity on the Blackboard!");
    }
  }


  /**
   * Try to modify a org activity
   */
  protected void queryBlackboard() {
    Collection collection = this.getBlackboardService().query(this.orgActivityPredicate);
    Iterator iterator = collection.iterator();
    if (iterator.hasNext()) {
      OrgActivity orgActivity = (OrgActivity) iterator.next();
      orgActivity.setActivityName(ACTIVITY_NAME);
      getBlackboardService().publishChange(orgActivity);
      this.totalRuns++;
      this.successes++;
    }
  }
}
