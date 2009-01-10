/**
 * <copyright>
 * </copyright>
 *
 * $Id: SingleContainedElementValidator.java,v 1.2 2009-01-10 07:56:19 estepper Exp $
 */
package org.eclipse.emf.cdo.tests.model4.validation;

import org.eclipse.emf.cdo.tests.model4.RefSingleContained;

/**
 * A sample validator interface for {@link org.eclipse.emf.cdo.tests.model4.SingleContainedElement}. This doesn't really
 * do anything, and it's not a real EMF artifact. It was generated by the org.eclipse.emf.examples.generator.validator
 * plug-in to illustrate how EMF's code generator can be extended. This can be disabled with -vmargs
 * -Dorg.eclipse.emf.examples.generator.validator=false.
 */
public interface SingleContainedElementValidator
{
  boolean validate();

  boolean validateName(String value);

  boolean validateParent(RefSingleContained value);
}
