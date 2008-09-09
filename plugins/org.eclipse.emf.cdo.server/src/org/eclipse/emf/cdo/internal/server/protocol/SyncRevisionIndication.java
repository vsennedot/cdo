/***************************************************************************
 * Copyright (c) 2004-2007 Eike Stepper, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Simon McDuff - initial API and implementation
 *    Simon McDuff - http://bugs.eclipse.org/230832
 *    Eike Stepper - maintenance
 **************************************************************************/
package org.eclipse.emf.cdo.internal.server.protocol;

import org.eclipse.emf.cdo.common.CDODataInput;
import org.eclipse.emf.cdo.common.CDODataOutput;
import org.eclipse.emf.cdo.common.CDOProtocolConstants;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.internal.server.bundle.OM;
import org.eclipse.emf.cdo.server.IStoreReader;
import org.eclipse.emf.cdo.server.StoreThreadLocal;
import org.eclipse.emf.cdo.spi.common.InternalCDORevision;

import org.eclipse.net4j.util.om.trace.ContextTracer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Simon McDuff
 */
public class SyncRevisionIndication extends CDOReadIndication
{
  private static final ContextTracer PROTOCOL_TRACER = new ContextTracer(OM.DEBUG_PROTOCOL,
      SyncRevisionIndication.class);

  private List<InternalCDORevision> dirtyObjects = new ArrayList<InternalCDORevision>();

  private int referenceChunk;

  public SyncRevisionIndication()
  {
  }

  @Override
  protected short getSignalID()
  {
    return CDOProtocolConstants.SIGNAL_SYNC;
  }

  @Override
  protected void indicating(CDODataInput in) throws IOException
  {
    IStoreReader reader = StoreThreadLocal.getStoreReader();
    if (PROTOCOL_TRACER.isEnabled())
    {
      PROTOCOL_TRACER.format("Refreshing reader : " + reader);
    }

    reader.refreshRevisions();
    referenceChunk = in.readInt();
    int size = in.readInt();
    for (int i = 0; i < size; i++)
    {
      CDOID cdoID = in.readCDOID();
      int version = in.readInt();
      if (version > 0)
      {
        InternalCDORevision revision = getRevisionManager().getRevision(cdoID, referenceChunk);
        if (revision.getVersion() != version)
        {
          dirtyObjects.add(revision);
        }
      }
    }
  }

  @Override
  protected void responding(CDODataOutput out) throws IOException
  {
    if (PROTOCOL_TRACER.isEnabled())
    {
      PROTOCOL_TRACER.format("Sync found " + dirtyObjects.size() + " dirty objects");
    }

    out.writeInt(dirtyObjects.size());
    for (InternalCDORevision revision : dirtyObjects)
    {
      out.writeCDORevision(revision, referenceChunk);
    }
  }
}
