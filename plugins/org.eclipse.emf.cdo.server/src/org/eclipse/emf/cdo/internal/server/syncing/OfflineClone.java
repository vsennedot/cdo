/**
 * Copyright (c) 2004 - 2010 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.internal.server.syncing;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchVersion;
import org.eclipse.emf.cdo.common.commit.CDOChangeSetData;
import org.eclipse.emf.cdo.common.commit.CDOCommitData;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDAndVersion;
import org.eclipse.emf.cdo.common.id.CDOIDUtil;
import org.eclipse.emf.cdo.common.model.CDOPackageInfo;
import org.eclipse.emf.cdo.common.model.CDOPackageUnit;
import org.eclipse.emf.cdo.common.revision.CDORevisionKey;
import org.eclipse.emf.cdo.internal.server.TransactionCommitContext;
import org.eclipse.emf.cdo.server.StoreThreadLocal;
import org.eclipse.emf.cdo.spi.common.branch.InternalCDOBranch;
import org.eclipse.emf.cdo.spi.common.branch.InternalCDOBranchManager;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageUnit;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevisionDelta;
import org.eclipse.emf.cdo.spi.server.InternalCommitContext;
import org.eclipse.emf.cdo.spi.server.InternalTransaction;

import org.eclipse.net4j.util.collection.IndexedList;
import org.eclipse.net4j.util.om.monitor.Monitor;
import org.eclipse.net4j.util.om.monitor.OMMonitor;
import org.eclipse.net4j.util.transaction.TransactionException;

import org.eclipse.emf.spi.cdo.CDOSessionProtocol;
import org.eclipse.emf.spi.cdo.CDOSessionProtocol.CommitTransactionResult;

import java.util.List;
import java.util.Map;

/**
 * @author Eike Stepper
 */
public class OfflineClone extends SynchronizableRepository
{
  public OfflineClone()
  {
    setState(OFFLINE);
  }

  @Override
  public final Type getType()
  {
    return CLONE;
  }

  @Override
  public final void setType(Type type)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public InternalCommitContext createCommitContext(InternalTransaction transaction)
  {
    CDOBranch branch = transaction.getBranch();
    if (branch.isLocal())
    {
      return createNormalCommitContext(transaction);
    }

    if (getState() != ONLINE)
    {
      return createBranchingCommitContext(transaction, branch);
    }

    return createWriteThroughCommitContext(transaction);
  }

  protected InternalCommitContext createBranchingCommitContext(InternalTransaction transaction, CDOBranch branch)
  {
    long timeStamp = createCommitTimeStamp(null, new Monitor());
    CDOBranch offlineBranch = createOfflineBranch(branch, timeStamp - 1L);
    transaction.setBranchPoint(offlineBranch.getHead());
    return new BranchingCommitContext(transaction, timeStamp);
  }

  protected InternalCommitContext createWriteThroughCommitContext(InternalTransaction transaction)
  {
    return new WriteThroughCommitContext(transaction);
  }

  protected CDOBranch createOfflineBranch(CDOBranch baseBranch, long baseTimeStamp)
  {
    try
    {
      StoreThreadLocal.setSession(getReplicatorSession());
      InternalCDOBranchManager branchManager = getBranchManager();
      return branchManager.createBranch(NEW_LOCAL_BRANCH,
          "Offline-" + baseTimeStamp, (InternalCDOBranch)baseBranch, baseTimeStamp); //$NON-NLS-1$
    }
    finally
    {
      StoreThreadLocal.release();
    }
  }

  /**
   * @author Eike Stepper
   */
  protected static final class CommitContextData implements CDOCommitData
  {
    private InternalCommitContext commitContext;

    public CommitContextData(InternalCommitContext commitContext)
    {
      this.commitContext = commitContext;
    }

    public boolean isEmpty()
    {
      return false;
    }

    public CDOChangeSetData copy()
    {
      throw new UnsupportedOperationException();
    }

    public void merge(CDOChangeSetData changeSetData)
    {
      throw new UnsupportedOperationException();
    }

    public List<CDOPackageUnit> getNewPackageUnits()
    {
      final InternalCDOPackageUnit[] newPackageUnits = commitContext.getNewPackageUnits();
      return new IndexedList<CDOPackageUnit>()
      {
        @Override
        public CDOPackageUnit get(int index)
        {
          return newPackageUnits[index];
        }

        @Override
        public int size()
        {
          return newPackageUnits.length;
        }
      };
    }

    public List<CDOIDAndVersion> getNewObjects()
    {
      final InternalCDORevision[] newObjects = commitContext.getNewObjects();
      return new IndexedList<CDOIDAndVersion>()
      {
        @Override
        public CDOIDAndVersion get(int index)
        {
          return newObjects[index];
        }

        @Override
        public int size()
        {
          return newObjects.length;
        }
      };
    }

    public List<CDORevisionKey> getChangedObjects()
    {
      final InternalCDORevisionDelta[] changedObjects = commitContext.getDirtyObjectDeltas();
      return new IndexedList<CDORevisionKey>()
      {
        @Override
        public CDORevisionKey get(int index)
        {
          return changedObjects[index];
        }

        @Override
        public int size()
        {
          return changedObjects.length;
        }
      };
    }

    public List<CDOIDAndVersion> getDetachedObjects()
    {
      final CDOID[] detachedObjects = commitContext.getDetachedObjects();
      return new IndexedList<CDOIDAndVersion>()
      {
        @Override
        public CDOIDAndVersion get(int index)
        {
          return CDOIDUtil.createIDAndVersion(detachedObjects[index], CDOBranchVersion.UNSPECIFIED_VERSION);
        }

        @Override
        public int size()
        {
          return detachedObjects.length;
        }
      };
    }
  }

  /**
   * @author Eike Stepper
   */
  protected final class WriteThroughCommitContext extends TransactionCommitContext
  {
    public WriteThroughCommitContext(InternalTransaction transaction)
    {
      super(transaction);
    }

    @Override
    public void preWrite()
    {
      // Do nothing
    }

    @Override
    public void write(OMMonitor monitor)
    {
      // Do nothing
    }

    @Override
    public void commit(OMMonitor monitor)
    {
      InternalTransaction transaction = getTransaction();

      // Prepare commit to the master
      CDOBranch branch = transaction.getBranch();
      String userID = getUserID();
      String comment = getCommitComment();
      CDOCommitData commitData = new CommitContextData(this);

      // Delegate commit to the master
      CDOSessionProtocol sessionProtocol = getSynchronizer().getRemoteSession().getSessionProtocol();
      CommitTransactionResult result = sessionProtocol.commitDelegation(branch, userID, comment, commitData, monitor);

      // Stop if commit to master failed
      String rollbackMessage = result.getRollbackMessage();
      if (rollbackMessage != null)
      {
        throw new TransactionException(rollbackMessage);
      }

      // Prepare data needed for commit result and commit notifications
      long timeStamp = result.getTimeStamp();
      setTimeStamp(timeStamp);
      addMetaIDRanges(commitData.getNewPackageUnits());
      addIDMappings(result.getIDMappings());
      applyIDMappings(new Monitor());

      // Commit to the local repository
      super.preWrite();
      super.write(new Monitor());
      super.commit(new Monitor());

      // Remember commit time in the local repository
      setLastCommitTimeStamp(timeStamp);
      setLastReplicatedCommitTime(timeStamp);
    }

    @Override
    protected long createTimeStamp(OMMonitor monitor)
    {
      // Already set after commit to the master
      return WriteThroughCommitContext.this.getTimeStamp(); // Do not call getTimeStamp() of the enclosing Repo class!!!
    }

    @Override
    protected void lockObjects() throws InterruptedException
    {
      // Do nothing
    }

    @Override
    protected void adjustMetaRanges()
    {
      // Do nothing
    }

    private void addMetaIDRanges(List<CDOPackageUnit> newPackageUnits)
    {
      for (CDOPackageUnit newPackageUnit : newPackageUnits)
      {
        for (CDOPackageInfo packageInfo : newPackageUnit.getPackageInfos())
        {
          addMetaIDRange(packageInfo.getMetaIDRange());
        }
      }
    }

    private void addIDMappings(Map<CDOID, CDOID> idMappings)
    {
      for (Map.Entry<CDOID, CDOID> idMapping : idMappings.entrySet())
      {
        CDOID oldID = idMapping.getKey();
        CDOID newID = idMapping.getValue();
        addIDMapping(oldID, newID);
      }
    }
  }

  /**
   * @author Eike Stepper
   */
  protected final class BranchingCommitContext extends TransactionCommitContext
  {
    private long timeStamp;

    public BranchingCommitContext(InternalTransaction transaction, long timeStamp)
    {
      super(transaction);
      this.timeStamp = timeStamp;
    }

    @Override
    protected void lockObjects() throws InterruptedException
    {
      // Do nothing
    }

    @Override
    protected long createTimeStamp(OMMonitor monitor)
    {
      return timeStamp;
    }
  }
}
