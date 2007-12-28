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
package org.eclipse.net4j.jms.internal.server.protocol.admin;

import org.eclipse.net4j.jms.JMSAdminProtocolConstants;
import org.eclipse.net4j.util.container.IManagedContainer;

import org.eclipse.internal.net4j.ServerProtocolFactory;

/**
 * @author Eike Stepper
 */
public final class JMSAdminServerProtocolFactory extends ServerProtocolFactory
{
  public static final String TYPE = JMSAdminProtocolConstants.PROTOCOL_NAME;

  public JMSAdminServerProtocolFactory()
  {
    super(TYPE);
  }

  public JMSAdminServerProtocol create(String description)
  {
    return new JMSAdminServerProtocol();
  }

  public static JMSAdminServerProtocol get(IManagedContainer container, String description)
  {
    return (JMSAdminServerProtocol)container.getElement(PRODUCT_GROUP, TYPE, description);
  }
}
