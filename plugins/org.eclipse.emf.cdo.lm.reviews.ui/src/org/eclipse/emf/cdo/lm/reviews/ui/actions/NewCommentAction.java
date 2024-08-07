/*
 * Copyright (c) 2024 Eike Stepper (Loehne, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.lm.reviews.ui.actions;

import org.eclipse.emf.cdo.CDOObject;
import org.eclipse.emf.cdo.lm.client.ISystemDescriptor;
import org.eclipse.emf.cdo.lm.client.ISystemManager;
import org.eclipse.emf.cdo.lm.reviews.Comment;
import org.eclipse.emf.cdo.lm.reviews.CommentStatus;
import org.eclipse.emf.cdo.lm.reviews.Commentable;
import org.eclipse.emf.cdo.lm.reviews.ReviewsFactory;
import org.eclipse.emf.cdo.lm.reviews.provider.ReviewsEditPlugin;
import org.eclipse.emf.cdo.lm.reviews.ui.bundle.OM;
import org.eclipse.emf.cdo.lm.ui.actions.LMAction;

import org.eclipse.net4j.util.StringUtil;

import org.eclipse.emf.edit.ui.provider.ExtendedImageRegistry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPage;

/**
 * @author Eike Stepper
 */
public class NewCommentAction extends LMAction.NewElement<Commentable>
{
  private final boolean heading;

  private String text;

  private CommentStatus status = CommentStatus.NONE;

  public NewCommentAction(IWorkbenchPage page, StructuredViewer viewer, Commentable commentable, boolean heading)
  {
    super(page, viewer, //
        "New " + getTypeString(heading) + INTERACTIVE, //
        "Add a new " + getTypeString(heading).toLowerCase(), //
        ExtendedImageRegistry.INSTANCE.getImageDescriptor(ReviewsEditPlugin.INSTANCE.getImage("full/obj16/" + getTypeString(heading))), //
        "Add a new " + getTypeString(heading).toLowerCase() + ".", //
        "icons/wizban/New" + getTypeString(heading) + ".png", //
        commentable);
    this.heading = heading;
  }

  @Override
  protected ImageDescriptor getImageDescriptor(String imagePath)
  {
    return OM.getImageDescriptor(imagePath);
  }

  @Override
  protected void fillDialogArea(LMDialog dialog, Composite parent)
  {
    {
      Label label = new Label(parent, SWT.NONE);
      label.setLayoutData(GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).create());
      label.setText("Text:");

      int style = SWT.BORDER;
      if (!heading)
      {
        style |= SWT.MULTI;
      }

      Text textArea = new Text(parent, style);
      textArea.setLayoutData(GridDataFactory.fillDefaults().grab(true, !heading).align(SWT.FILL, SWT.FILL).create());
      textArea.addModifyListener(event -> {
        text = textArea.getText();
        validateDialog();
      });
    }

    {
      Button button = newCheckBox(parent, "Needs Resolution");
      button.addSelectionListener(new SelectionAdapter()
      {
        @Override
        public void widgetSelected(SelectionEvent e)
        {
          status = button.getSelection() ? CommentStatus.UNRESOLVED : CommentStatus.NONE;
          validateDialog();
        }
      });
    }
  }

  @Override
  protected String doValidate(LMDialog dialog)
  {
    if (StringUtil.isEmpty(text))
    {
      return "A text must be entered.";
    }

    return super.doValidate(dialog);
  }

  @Override
  protected CDOObject newElement(Commentable commentable, IProgressMonitor monitor) throws Exception
  {
    ISystemDescriptor systemDescriptor = ISystemManager.INSTANCE.getDescriptor(commentable);
    String author = systemDescriptor.getSystemRepository().getCredentials().getUserID();

    Comment comment = createComment();
    comment.setAuthor(author);
    comment.setText(text);
    comment.setStatus(status);

    return systemDescriptor.modify(commentable, c -> {
      c.getComments().add(comment);
      return comment;
    }, monitor);
  }

  private Comment createComment()
  {
    if (heading)
    {
      return ReviewsFactory.eINSTANCE.createHeading();
    }

    return ReviewsFactory.eINSTANCE.createComment();
  }

  private static String getTypeString(boolean heading)
  {
    return heading ? "Heading" : "Comment";
  }
}
