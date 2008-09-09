/***************************************************************************
 * Copyright (c) 2004 - 2008 Eike Stepper, Germany.
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
package org.eclipse.emf.internal.cdo.protocol;

import org.eclipse.emf.cdo.common.CDODataInput;
import org.eclipse.emf.cdo.common.CDODataOutput;
import org.eclipse.emf.cdo.common.CDOProtocolConstants;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDAndVersion;
import org.eclipse.emf.cdo.common.id.CDOIDUtil;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.spi.common.InternalCDORevision;

import org.eclipse.emf.internal.cdo.CDORevisionManagerImpl;
import org.eclipse.emf.internal.cdo.CDOSessionImpl;
import org.eclipse.emf.internal.cdo.bundle.OM;

import org.eclipse.net4j.channel.IChannel;
import org.eclipse.net4j.util.om.trace.ContextTracer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Simon McDuff
 * @since 2.0
 */
public class SyncRevisionRequest extends CDOClientRequest<Set<CDOIDAndVersion>>
{
  private static final ContextTracer PROTOCOL_TRACER = new ContextTracer(OM.DEBUG_PROTOCOL, SyncRevisionRequest.class);

  private Map<CDOID, CDORevision> collectionRevisions;

  private CDOSessionImpl cdoSession;

  private int referenceChunk;

  public SyncRevisionRequest(IChannel channel, CDOSessionImpl cdoSession, Map<CDOID, CDORevision> cdoRevisions,
      int referenceChunk)
  {
    super(channel);
    collectionRevisions = cdoRevisions;
    this.referenceChunk = referenceChunk;
    this.cdoSession = cdoSession;
  }

  @Override
  protected short getSignalID()
  {
    return CDOProtocolConstants.SIGNAL_SYNC;
  }

  @Override
  protected void requesting(CDODataOutput out) throws IOException
  {
    if (PROTOCOL_TRACER.isEnabled())
    {
      PROTOCOL_TRACER.trace("Synchronization " + collectionRevisions.size() + " objects");
    }

    out.writeInt(referenceChunk);
    out.writeInt(collectionRevisions.size());
    for (CDORevision revision : collectionRevisions.values())
    {
      out.writeCDOID(revision.getID());
      out.writeInt(revision.getVersion());
    }
  }

  @Override
  protected Set<CDOIDAndVersion> confirming(CDODataInput in) throws IOException
  {
    int size = in.readInt();
    Set<CDOIDAndVersion> dirtyObjects = new HashSet<CDOIDAndVersion>();
    CDORevisionManagerImpl revisionManager = getRevisionManager();
    for (int i = 0; i < size; i++)
    {
      CDORevision revision = in.readCDORevision();
      CDORevision oldRevision = collectionRevisions.get(revision.getID());
      if (oldRevision == null)
      {
        throw new IllegalStateException("Didn't expect to receive object with id '" + revision.getID() + "'");
      }

      dirtyObjects.add(CDOIDUtil.createIDAndVersion(oldRevision.getID(), oldRevision.getVersion()));
      revisionManager.addCachedRevision((InternalCDORevision)revision);
    }

    if (PROTOCOL_TRACER.isEnabled())
    {
      PROTOCOL_TRACER.trace("Synchronization received  " + size + " dirty objects");
    }

    cdoSession.handleSync(dirtyObjects);
    return dirtyObjects;
  }
}
