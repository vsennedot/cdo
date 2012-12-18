/*
 * Copyright (c) 2004 - 2012 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.compare;

import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.compare.scope.IComparisonScope;

/**
 * @author Eike Stepper
 */
public final class ComparisonScopeAdapter extends AdapterImpl
{
  private IComparisonScope scope;

  public ComparisonScopeAdapter(IComparisonScope scope)
  {
    this.scope = scope;
  }

  public final IComparisonScope getScope()
  {
    return scope;
  }

  @Override
  public boolean isAdapterForType(Object type)
  {
    return type == ComparisonScopeAdapter.class;
  }
}
