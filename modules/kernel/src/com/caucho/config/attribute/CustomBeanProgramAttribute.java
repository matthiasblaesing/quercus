/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
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

package com.caucho.config.attribute;

import java.lang.reflect.*;

import com.caucho.config.*;
import com.caucho.config.inject.ConfigContext;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.type.*;
import com.caucho.config.types.CustomBeanConfig;
import com.caucho.util.L10N;
import com.caucho.xml.QName;

public class CustomBeanProgramAttribute extends Attribute {
  private static final L10N L = new L10N(CustomBeanProgramAttribute.class);

  public static final CustomBeanProgramAttribute ATTRIBUTE
    = new CustomBeanProgramAttribute();

  private CustomBeanProgramAttribute()
  {
  }

  public ConfigType getConfigType()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns true for a program-style attribute.
   */
  public boolean isProgram()
  {
    return true;
  }
  
  /**
   * Sets the value of the attribute
   */
  public void setValue(Object bean, QName name, Object value)
    throws ConfigException
  {
    try {
      CustomBeanConfig customBean = (CustomBeanConfig) bean;

      customBean.addInitProgram((ConfigProgram) value);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  public String toString()
  {
    return getClass().getSimpleName();
  }
  
  /**
   * Sets the value of the attribute
   */
  @Override
  public void setText(Object bean, QName name, String text)
    throws ConfigException
  {
    try {
      CustomBeanConfig customBean = (CustomBeanConfig) bean;

      customBean.addInitProgram(new TextArgProgram(text));
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
  
  static class TextArgProgram extends ConfigProgram {
    private String _arg;

    TextArgProgram(String arg)
    {
      _arg = arg;
    }
    
    public void inject(Object bean, ConfigContext env)
    {
      throw new UnsupportedOperationException(getClass().getName());
    }

    public Object configure(ConfigType type, ConfigContext env)
      throws ConfigException
    {
      return type.valueOf(_arg);
    }
  }
}
