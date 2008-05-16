/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.faces.application;

import java.util.*;

import javax.el.*;

import javax.faces.*;
import javax.faces.component.*;
import javax.faces.context.*;
import javax.faces.convert.*;
import javax.faces.el.*;
import javax.faces.event.*;
import javax.faces.validator.*;

public abstract class Application
{
  public abstract ActionListener getActionListener();

  public abstract void setActionListener(ActionListener listener);

  public abstract Locale getDefaultLocale();

  public abstract void setDefaultLocale(Locale locale);

  public abstract String getDefaultRenderKitId();

  public abstract void setDefaultRenderKitId(String renderKitId);

  public abstract String getMessageBundle();

  public abstract void setMessageBundle(String bundle);

  public abstract NavigationHandler getNavigationHandler();

  public abstract void setNavigationHandler(NavigationHandler handler);

  @Deprecated
  public abstract PropertyResolver getPropertyResolver();

  @Deprecated
  public abstract void setPropertyResolver(PropertyResolver resolver);

  /**
   * @Since 1.2
   */
  public ResourceBundle getResourceBundle(FacesContext ctx, String name)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Deprecated
  public abstract VariableResolver getVariableResolver();

  @Deprecated
  public abstract void setVariableResolver(VariableResolver resolver);

  /**
   * @Since 1.2
   */
  public void addELResolver(ELResolver resolver)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * @Since 1.2
   */
  public ELResolver getELResolver()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public abstract ViewHandler getViewHandler();

  public abstract void setViewHandler(ViewHandler handler);

  public abstract StateManager getStateManager();

  public abstract void setStateManager(StateManager manager);

  public abstract void addComponent(String componentType,
				    String componentClass);

  public abstract UIComponent createComponent(String componentType)
    throws FacesException;

  @Deprecated
  public abstract UIComponent createComponent(ValueBinding componentBinding,
					      FacesContext context,
					      String componentType)
    throws FacesException;

  /**
   * @Since 1.2
   */
  public UIComponent createComponent(ValueExpression componentExpr,
				     FacesContext context,
				     String componentType)
    throws FacesException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public abstract Iterator<String> getComponentTypes();

  public abstract void addConverter(String converterId,
				    String converterClass);

  public abstract void addConverter(Class targetClass,
				    String converterClass);

  public abstract Converter createConverter(String converterId)
    throws FacesException;

  public abstract Converter createConverter(Class targetClass)
    throws FacesException;

  public abstract Iterator<String> getConverterIds();

  public abstract Iterator<Class> getConverterTypes();

  /**
   * @Since 1.2
   */
  public ExpressionFactory getExpressionFactory()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public Object evaluateExpressionGet(FacesContext context,
				      String expression,
				      Class expectedType)
    throws ELException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Deprecated
  public abstract MethodBinding createMethodBinding(String ref,
						    Class []param)
    throws ReferenceSyntaxException;

  public abstract Iterator<Locale> getSupportedLocales();

  public abstract void setSupportedLocales(Collection<Locale> locales);

  /**
   * @Since 1.2
   */
  public void addELContextListener(ELContextListener listener)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * @Since 1.2
   */
  public void removeELContextListener(ELContextListener listener)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * @Since 1.2
   */
  public ELContextListener []getELContextListeners()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public abstract void addValidator(String validatorId, String validatorClass);

  public abstract Validator createValidator(String validatorId)
    throws FacesException;

  public abstract Iterator<String> getValidatorIds();

  @Deprecated
  public abstract ValueBinding createValueBinding(String ref)
    throws ReferenceSyntaxException;

  /**
   * @Since 2.0
   */
  public ResourceHandler getResourceHandler(){
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * @Since 2.0
   */
  public ProjectStage getProjectStage() {
    throw new UnsupportedOperationException(getClass().getName());
  }
}
