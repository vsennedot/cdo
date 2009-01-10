/**
 * <copyright>
 * Copyright (c) 2004 - 2008 André Dietisheim, Switzerland.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    André Dietisheim - initial API and implementation
 * </copyright>
 *
 * $Id: CDOEagerPackageRegistryDefImpl.java,v 1.2 2009-01-10 07:56:10 estepper Exp $
 */
package org.eclipse.emf.cdo.defs.impl;

import org.eclipse.emf.cdo.defs.CDODefsPackage;
import org.eclipse.emf.cdo.defs.CDOEagerPackageRegistryDef;
import org.eclipse.emf.cdo.util.CDOUtil;

import org.eclipse.emf.internal.cdo.session.CDOPackageRegistryImpl.Eager;

import org.eclipse.emf.ecore.EClass;

/**
 * <!-- begin-user-doc --> An implementation of the model object '<em><b>Eager Package Registry Def</b></em>'. <!--
 * end-user-doc -->
 * <p>
 * </p>
 * 
 * @generated
 */
public class CDOEagerPackageRegistryDefImpl extends CDOPackageRegistryDefImpl implements CDOEagerPackageRegistryDef
{
  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  protected CDOEagerPackageRegistryDefImpl()
  {
    super();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  protected EClass eStaticClass()
  {
    return CDODefsPackage.Literals.CDO_EAGER_PACKAGE_REGISTRY_DEF;
  }

  /**
   * Creates a {@link Eager}.
   * 
   * @return the CDO package registry
   */
  protected Object createInstance()
  {
    return addPackages(CDOUtil.createEagerPackageRegistry());
  }

} // EagerPackageRegistryDefImpl
