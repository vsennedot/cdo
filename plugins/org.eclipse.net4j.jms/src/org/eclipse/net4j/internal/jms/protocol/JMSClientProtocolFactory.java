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
package org.eclipse.net4j.internal.jms.protocol;

import org.eclipse.net4j.jms.JMSProtocolConstants;
import org.eclipse.net4j.util.container.IManagedContainer;

import org.eclipse.internal.net4j.ClientProtocolFactory;

/**
 * @author Eike Stepper
 */
public final class JMSClientProtocolFactory extends ClientProtocolFactory
{
  public static final String TYPE = JMSProtocolConstants.PROTOCOL_NAME;

  public JMSClientProtocolFactory()
  {
    super(TYPE);
  }

  public JMSClientProtocol create(String description)
  {
    return new JMSClientProtocol();
  }

  public static JMSClientProtocol get(IManagedContainer container, String description)
  {
    return (JMSClientProtocol)container.getElement(PRODUCT_GROUP, TYPE, description);
  }
}
