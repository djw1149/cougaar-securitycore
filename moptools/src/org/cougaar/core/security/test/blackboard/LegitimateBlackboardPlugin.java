/*
 * <copyright>
 *  Copyright 2000-2003 Cougaar Software, Inc.
 *  All Rights Reserved
 * </copyright>
 */


/*
 * Created on Jun 4, 2003
 *
 *
 */
package org.cougaar.core.security.test.blackboard;


import edu.jhuapl.idmef.Analyzer;

import java.util.Collection;
import java.util.Iterator;


/**
 * DOCUMENT ME!
 *
 * @author ttschampel
 */
public class LegitimateBlackboardPlugin extends AbstractBlackboardPlugin {
  /**
   * DOCUMENT ME!
   */
  public void load() {
    super.load();
    setPluginName("LegitimateBlackboardPlugin");
  }


  /**
   * Query for org activities and produce idmef event when  org activities
   * should be present, but can't get any through querying the blacboard
   */
  protected void queryBlackboard() {
    Collection orgActivities = getBlackboardService().query(this.orgActivityPredicate);
    Iterator iter = orgActivities.iterator();
    this.totalRuns++;
    if (iter.hasNext()) {
      //success	
      this.successes++;
    } else {
      //failure
      this.failures++;
      this.createIDMEFEvent(new LegitAnalyzer());
    }
  }

  public class LegitAnalyzer extends Analyzer {
    public LegitAnalyzer() {
      this.setAnalyzerid(pluginName);

    }
  }
}