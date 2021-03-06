/* 
 * <copyright> 
 *  Copyright 1999-2004 Cougaar Software, Inc.
 *  under sponsorship of the Defense Advanced Research Projects 
 *  Agency (DARPA). 
 *  
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).  
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright> 
 */ 

package org.cougaar.core.security.test.dataprotection;

import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.UIDService;
import org.cougaar.util.UnaryPredicate;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.cougaar.core.util.UID;
import java.util.Random;
import java.util.Date;
import java.util.List;
import java.util.Collection;

public class TestPersistencePlugin extends ComponentPlugin
{
  private LoggingService  _log;
  private AlarmService  _alarmService;
  private BlackboardService  _bbs;
  private long _blackboardSize;
  private Alarm _alarm = new MyAlarm();
  private UIDService _myUIDService;

  public void setParameter(Object o) {
    //    Thread.dumpStack();
    if (!(o instanceof List)) {
      throw new IllegalArgumentException("Expecting a List parameter " +
                                         "instead of " + 
                                         ( (o == null)
                                           ? "null" 
                                           : o.getClass().getName() ));
    }

    List l = (List) o;
    Object[] arr = l.toArray();
    //System.out.println("argument array = " + arr + " with length " + arr.length + "KB");

    if (arr.length != 0) {
      _blackboardSize = Long.parseLong(arr[0].toString()) * 1024;
    }
  }

  protected void setupSubscriptions() {
    _log = (LoggingService)
	getServiceBroker().getService(this, LoggingService.class, null);

    _alarmService = (AlarmService)
	getServiceBroker().getService(this, AlarmService.class, null);

    _bbs = (BlackboardService)
	getServiceBroker().getService(this, BlackboardService.class, null);

    _log.debug("_blackboardSize = " + (_blackboardSize / 1024) + "KB");

    _alarmService.addAlarm(_alarm);
  }
  
  protected void execute() {
    if (_bbs.didRehydrate()) {
      // Verify objects at rehydration
      // Get rehydrated objects
      Collection col = getBlackboardService().query(new MyObjectPredicate());
      _log.debug("Rehydrated " + col.size() + " objects");
    }
    else {
      _log.debug("Did not rehydrate");
    }
  }

  public void setUIDService(UIDService service) {
    _myUIDService = service;
  }

  private class MyAlarm
    implements Alarm
  {
    // Allow the agent to fully initialize itself before testing
    private final int EXPIRATION_TIME = 20000;
    private boolean _hasExpired = false;

    private final int MAX_STRING_SIZE = 1000;
    private ByteArrayOutputStream _bos;
    private ObjectOutputStream _oos;
    private Random _random = new Random();

    public MyAlarm() {
      _bos = new ByteArrayOutputStream();
      try {
	_oos = new ObjectOutputStream(_bos);
      }
      catch (java.io.IOException ex) {
	_log.warn("Unable to create object output stream", ex);
      }
    }

    public boolean cancel() {
      boolean ret = _hasExpired;
      _hasExpired = true;
      return ret;
    }

    public void expire() {
      _log.debug("Alarm has expired");
      if (_hasExpired) {
	return;
      }

      Date d1 = new Date();
      // Publish a bunch of blackboard objects.
      publishObjects();
      Date d2 = new Date();
      _log.debug("Time to publish objects: " + ((d2.getTime() - d1.getTime()) / 1000) + "s" );

      // Force persistence
      Date d3 = new Date();
      try {
	_bbs.persistNow();
      }
      catch (org.cougaar.core.persist.PersistenceNotEnabledException ex) {
	_log.warn("Unable to persist blackboard", ex);
      }
      Date d4 = new Date();
      _log.debug("Time to persist: " + ((d4.getTime() - d3.getTime()) / 1000) + "s");

      _hasExpired = true;
    }
    public long getExpirationTime() {
      return EXPIRATION_TIME;
    }
    public boolean hasExpired() {
      return _hasExpired;
    }

    private void publishObjects() {
      long cumulatedSize = 0;
      long numberOfObjects = 0;
      while (cumulatedSize < _blackboardSize) {
	cumulatedSize += publishObject();
	numberOfObjects++;
	if ((cumulatedSize % 10000) == 0) {
	  _log.debug("Publishing objects: size=" + (cumulatedSize / 1024)
		     + "KB - objects:" + numberOfObjects 
		     + " - average size=" + cumulatedSize / numberOfObjects);
	}
      }
      _log.debug("Done Publishing objects: size=" + (cumulatedSize / 1024)
		 + "KB - objects:" + numberOfObjects 
		 + " - average size=" + cumulatedSize / numberOfObjects);
    }

    private long publishObject() {
      int stringSize = _random.nextInt(MAX_STRING_SIZE);
      StringBuffer sb = new StringBuffer(stringSize);
      for (int i = 0 ; i < stringSize ; i++) {
	sb.append((char)_random.nextInt());
      }
      MyBlackboardObject mo = new MyBlackboardObject(10, sb.toString(), _myUIDService);
      long size = getObjectSize(mo);
      //_log.debug("stringSize=" + stringSize + " - sb.size=" + sb.toString().length()
      // + " - o.size=" + size);
      _bbs.openTransaction();
      _bbs.publishAdd(mo);
      _bbs.closeTransaction();
      return size;
    }

    private long getObjectSize(Object o) {
      _bos.reset();
      try {
	_oos.writeObject(o);
      }
      catch (java.io.IOException ex) {
	_log.warn("Unable to write object to stream", ex);
      }
      return _bos.size();
    }
  }

  public static class MyBlackboardObject
    implements Serializable
  {
    // The content is not very relevant. It's used to store some random
    // blackboard objects.

    public MyBlackboardObject (int a, String s, UIDService uidService) {
      setUID(uidService.nextUID());
      _a = a;
      _s = s;
    }

    public UID getUID() { return _uid; }
    public void setUID(UID uid) { _uid = uid; }

    private int _a;
    private String _s;
    protected UID _uid;
  }

  public static class MyObjectPredicate
    implements UnaryPredicate
  {
    public boolean execute(Object o) {
      boolean ret = false;
      if (o instanceof MyBlackboardObject) {
	return true;
      }
      return ret;
    }
  }

}
