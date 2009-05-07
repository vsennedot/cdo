/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.common.model;

import org.eclipse.emf.cdo.common.io.CDODataInput;
import org.eclipse.emf.cdo.common.io.CDODataOutput;
import org.eclipse.emf.cdo.internal.common.messages.Messages;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;

import java.io.IOException;
import java.text.MessageFormat;

/**
 * @author Eike Stepper
 * @since 2.0
 */
public final class CDOClassifierRef
{
  public static final String URI_SEPARATOR = "#"; //$NON-NLS-1$

  private String packageURI;

  private String classifierName;

  public CDOClassifierRef()
  {
  }

  public CDOClassifierRef(EClassifier classifier)
  {
    this(classifier.getEPackage().getNsURI(), classifier.getName());
  }

  public CDOClassifierRef(String packageURI, String classifierName)
  {
    this.packageURI = packageURI;
    this.classifierName = classifierName;
  }

  public CDOClassifierRef(CDODataInput in) throws IOException
  {
    String uri = in.readCDOPackageURI();
    int hash = uri.lastIndexOf(URI_SEPARATOR);
    if (hash == -1)
    {
      throw new IOException("Invalid classifier URI: " + uri); //$NON-NLS-1$
    }

    packageURI = uri.substring(0, hash);
    classifierName = uri.substring(hash + 1);
  }

  public void write(CDODataOutput out) throws IOException
  {
    out.writeCDOPackageURI(packageURI + URI_SEPARATOR + classifierName);
  }

  public String getPackageURI()
  {
    return packageURI;
  }

  public String getClassifierName()
  {
    return classifierName;
  }

  public EClassifier resolve(EPackage.Registry packageRegistry)
  {
    EPackage ePackage = packageRegistry.getEPackage(packageURI);
    if (ePackage == null)
    {
      throw new IllegalStateException(MessageFormat.format(Messages.getString("CDOClassifierRef.0"), packageURI)); //$NON-NLS-1$
    }

    return ePackage.getEClassifier(classifierName);
  }

  @Override
  public String toString()
  {
    return MessageFormat.format("CDOClassifierRef({0}, {1})", packageURI, classifierName); //$NON-NLS-1$
  }
}
