/***************************************************************************
 * Copyright (c) 2004 - 2008 Eike Stepper, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 **************************************************************************/
package org.eclipse.net4j.internal.http;

import org.eclipse.net4j.channel.IChannel;
import org.eclipse.net4j.connector.ConnectorException;
import org.eclipse.net4j.connector.ConnectorLocation;
import org.eclipse.net4j.protocol.IProtocol;

import java.text.MessageFormat;

/**
 * @author Eike Stepper
 */
public class HTTPServerConnector extends HTTPConnector
{
  private HTTPAcceptor acceptor;

  private long lastTraffic = System.currentTimeMillis();

  public HTTPServerConnector(HTTPAcceptor acceptor)
  {
    this.acceptor = acceptor;
  }

  public HTTPAcceptor getAcceptor()
  {
    return acceptor;
  }

  public ConnectorLocation getLocation()
  {
    return ConnectorLocation.SERVER;
  }

  public String getURL()
  {
    return "agent://connector:" + getConnectorID();
  }

  public long getLastTraffic()
  {
    return lastTraffic;
  }

  public void multiplexChannel(IChannel channel)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString()
  {
    if (getUserID() == null)
    {
      return MessageFormat.format("HTTPServerConnector[{0}]", getConnectorID()); //$NON-NLS-1$
    }

    return MessageFormat.format("HTTPServerConnector[{1}@{0}]", getConnectorID(), getUserID()); //$NON-NLS-1$
  }

  @Override
  protected void registerChannelWithPeer(int channelID, short channelIndex, IProtocol protocol)
      throws ConnectorException
  {
    throw new UnsupportedOperationException();
  }
}
