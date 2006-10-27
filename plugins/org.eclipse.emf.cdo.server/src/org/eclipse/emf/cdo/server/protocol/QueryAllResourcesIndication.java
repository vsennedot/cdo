/***************************************************************************
 * Copyright (c) 2004, 2005, 2006 Eike Stepper, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 **************************************************************************/
package org.eclipse.emf.cdo.server.protocol;


import org.eclipse.net4j.signal.IndicationWithResponse;
import org.eclipse.net4j.util.stream.ExtendedDataInputStream;
import org.eclipse.net4j.util.stream.ExtendedDataOutputStream;

import org.eclipse.emf.cdo.core.CDOResSignals;
import org.eclipse.emf.cdo.server.Mapper;

import java.io.IOException;


/**
 * @author Eike Stepper
 */
public class QueryAllResourcesIndication extends IndicationWithResponse implements CDOResSignals
{
  private Mapper mapper;

  public QueryAllResourcesIndication(Mapper mapper)
  {
    this.mapper = mapper;
  }

  @Override
  protected short getSignalID()
  {
    return QUERY_ALL_RESOURCES;
  }

  @Override
  protected void indicating(ExtendedDataInputStream in) throws IOException
  {
  }

  @Override
  protected void responding(ExtendedDataOutputStream out) throws IOException
  {
    mapper.transmitAllResources(out);
  }
}
