/**
 * Copyright (c) 2004 - 2010 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Andre Dietisheim - initial API and implementation
 */
package org.eclipse.emf.cdo.common.commit;

import org.eclipse.emf.cdo.common.branch.CDOBranch;

/**
 * @author Andre Dietisheim
 * @since 3.0
 */
public interface CDOCommitInfoManager
{
  public CDOCommitInfo getCommitInfo(long timeStamp);

  public void getCommitInfos(CDOBranch branch, long startTime, long endTime, CDOCommitInfoHandler handler);

  public void getCommitInfos(CDOBranch branch, CDOCommitInfoHandler handler);

  public void getCommitInfos(long startTime, long endTime, CDOCommitInfoHandler handler);
}
