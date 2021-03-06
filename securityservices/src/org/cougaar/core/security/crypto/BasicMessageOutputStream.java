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


package org.cougaar.core.security.crypto;

import java.io.IOException;
import java.io.OutputStream;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.ProtectedOutputStream;
import org.cougaar.core.service.LoggingService;

public class BasicMessageOutputStream
  extends ProtectedOutputStream
{
  /** ProtectedOutputStream can read old attributes (check to see if it should
   *  write the signature. Note at this point the attributes were already sent)
   */
  private OutputStream outputStream;
  private ServiceBroker serviceBroker;
  private LoggingService log;
  
  public BasicMessageOutputStream(OutputStream stream,
				  MessageAddress source,
				  MessageAddress target,
				  ServiceBroker sb) {
    super(stream);
    serviceBroker = sb;
    log = (LoggingService)
      serviceBroker.getService(this,
			       LoggingService.class, null);

    outputStream = stream;
  }
  
  /* ***********************************************************************
   * FilterOutputStream implementation
   */

  public void write(byte[] b)
    throws IOException {
    outputStream.write(b);
  }

  public void write(byte[] b, int off, int len)
    throws IOException {
    outputStream.write(b, off, len);
  }

  public void write(int b)
    throws IOException {
    outputStream.write(b);
  }

  public void flush()
    throws IOException {
    outputStream.flush();
  }

  public void close()
    throws IOException {
    outputStream.close();
  }

  /* **********************************************************************
   * ProtectedOutputStream implementation
   */

  public void finishOutput(MessageAttributes attributes)
    throws java.io.IOException {
  }

  /* ***********************************************************************
   * Private methods
   */
}
