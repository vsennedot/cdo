/***************************************************************************
 * Copyright (c) 2004-2007 Eike Stepper, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 **************************************************************************/
package org.eclipse.emf.cdo.internal.server.protocol;

import org.eclipse.emf.cdo.internal.protocol.bundle.CDOProtocol;
import org.eclipse.emf.cdo.internal.server.Repository;
import org.eclipse.emf.cdo.internal.server.RepositoryManager;
import org.eclipse.emf.cdo.internal.server.Session;
import org.eclipse.emf.cdo.internal.server.SessionManager;
import org.eclipse.emf.cdo.protocol.CDOProtocolConstants;
import org.eclipse.emf.cdo.server.IRepository;
import org.eclipse.emf.cdo.server.ISession;

import org.eclipse.net4j.signal.IndicationWithResponse;
import org.eclipse.net4j.util.om.trace.ContextTracer;
import org.eclipse.net4j.util.stream.ExtendedDataInputStream;
import org.eclipse.net4j.util.stream.ExtendedDataOutputStream;

import java.io.IOException;

/**
 * @author Eike Stepper
 */
public class OpenSessionIndication extends IndicationWithResponse
{
  private static final ContextTracer PROTOCOL = new ContextTracer(CDOProtocol.DEBUG_PROTOCOL,
      OpenSessionIndication.class);

  private String repositoryName;

  public OpenSessionIndication()
  {
  }

  @Override
  protected short getSignalID()
  {
    return CDOProtocolConstants.OPEN_SESSION_SIGNAL;
  }

  @Override
  protected void indicating(ExtendedDataInputStream in) throws IOException
  {
    repositoryName = in.readString();
    if (PROTOCOL.isEnabled())
    {
      PROTOCOL.format("Read repositoryName: {0}", repositoryName);
    }
  }

  @Override
  protected void responding(ExtendedDataOutputStream out) throws IOException
  {
    Repository repository = RepositoryManager.INSTANCE.getRepository(repositoryName, true);
    if (repository == null)
    {
      if (PROTOCOL.isEnabled())
      {
        PROTOCOL.format("IRepository {0} not found", repositoryName);
      }

      out.writeInt(CDOProtocolConstants.ERROR_REPOSITORY_NOT_FOUND);
      return;
    }

    CDOServerProtocol serverProtocol = (CDOServerProtocol)getProtocol();
    SessionManager sessionManager = repository.getSessionManager();
    Session session = sessionManager.openSession(serverProtocol);
    if (session == null)
    {
      if (PROTOCOL.isEnabled())
      {
        PROTOCOL.format("Failed to open session for repository {0}", repositoryName);
      }

      out.writeInt(CDOProtocolConstants.ERROR_NO_SESSION);
      return;
    }

    serverProtocol.setSession(session);
    writeSessionID(out, session);
    writeRepositoryUUID(out, repository);
  }

  private void writeSessionID(ExtendedDataOutputStream out, ISession session) throws IOException
  {
    if (PROTOCOL.isEnabled())
    {
      PROTOCOL.format("Writing sessionID: {0}", session.getSessionID());
    }

    out.writeInt(session.getSessionID());
  }

  private void writeRepositoryUUID(ExtendedDataOutputStream out, IRepository repository) throws IOException
  {
    if (PROTOCOL.isEnabled())
    {
      PROTOCOL.format("Writing repositoryUUID: {0}", repository.getUUID());
    }

    out.writeString(repository.getUUID());
  }
}
