/*
 * Copyright (c) 2004-2014 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.explorer.ui.checkouts.wizards;

import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchManager;
import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.explorer.repositories.CDORepository;
import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.emf.cdo.ui.widgets.ComposeBranchPointComposite;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

import java.util.Properties;

/**
 * @author Eike Stepper
 */
public class CheckoutBranchPointPage extends CheckoutWizardPage
{
  private CDORepository repository;

  private CDOBranchPoint branchPoint;

  private ComposeBranchPointComposite branchPointComposite;

  private String timeStampError;

  public CheckoutBranchPointPage()
  {
    super("Branch Point", "Select the branch point of the new checkout.");
  }

  public final CDOBranchPoint getBranchPoint()
  {
    return branchPoint;
  }

  public final void setBranchPoint(CDOBranchPoint branchPoint)
  {
    if (this.branchPoint != branchPoint)
    {
      log("Setting branch point to " + branchPoint);
      this.branchPoint = branchPoint;

      if (branchPointComposite != null)
      {
        if (branchPoint != null)
        {
          CDOBranch branch = branchPoint.getBranch();

          TreeViewer branchViewer = branchPointComposite.getBranchViewer();
          branchViewer.setSelection(new StructuredSelection(branch));
          branchViewer.setExpandedState(branch, true);
        }
      }

      branchPointChanged(branchPoint);
    }
  }

  public void setBranchPoint(int branchID, long timeStamp)
  {
    CheckoutRepositoryPage repositoryPage = getWizard().getRepositoryPage();
    CDOSession session = repositoryPage.getSession();

    CDOBranchManager branchManager = session.getBranchManager();
    CDOBranch branch = branchManager.getBranch(branchID);

    setBranchPoint(branch.getPoint(timeStamp));
  }

  @Override
  protected void createUI(Composite parent)
  {
    branchPointComposite = new ComposeBranchPointComposite(parent, true, branchPoint)
    {
      @Override
      protected void timeStampError(String message)
      {
        timeStampError = message;
        validate();
      }

      @Override
      protected void branchPointChanged(CDOBranchPoint branchPoint)
      {
        CheckoutBranchPointPage.this.setBranchPoint(branchPoint);
        validate();
      }

      @Override
      protected void doubleClicked()
      {
        showNextPage();
      }
    };

    branchPointComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
  }

  @Override
  protected void repositoryChanged(CDORepository repository)
  {
    this.repository = repository;

    TreeViewer branchViewer = branchPointComposite.getBranchViewer();
    branchViewer.setSelection(StructuredSelection.EMPTY);
    branchViewer.setInput(null);
    setBranchPoint(null);

    super.repositoryChanged(repository);
  }

  @Override
  protected void pageActivated()
  {
    CDOSession session = getWizard().getRepositoryPage().getSession();

    TreeViewer branchViewer = branchPointComposite.getBranchViewer();
    branchViewer.setInput(session.getBranchManager());

    setBranchPoint(CDOBranch.MAIN_BRANCH_ID, CDOBranchPoint.UNSPECIFIED_DATE);
  }

  @Override
  protected boolean doValidate() throws ValidationProblem
  {
    if (timeStampError != null)
    {
      throw new ValidationProblem(timeStampError);
    }

    return true;
  }

  @Override
  protected void fillProperties(Properties properties)
  {
    properties.setProperty("branchID", Integer.toString(branchPoint.getBranch().getID()));
    properties.setProperty("timeStamp", Long.toString(branchPoint.getTimeStamp()));
  }
}