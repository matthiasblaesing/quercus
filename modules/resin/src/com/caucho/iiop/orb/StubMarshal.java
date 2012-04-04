/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.iiop.orb;

import java.util.*;
import java.lang.reflect.*;

/**
 * Marshal for a client stub.
 */
public class StubMarshal {
  private final HashMap<Method,MethodMarshal> _methodMap
    = new HashMap<Method,MethodMarshal>();

  public StubMarshal(Class cl)
  {
    introspect(cl);
  }

  private void introspect(Class cl)
  {
    MarshalFactory factory = MarshalFactory.create();
    
    for (Method method : cl.getMethods()) {
      if (Modifier.isStatic(method.getModifiers()))
	continue;
      
      if (method.getDeclaringClass().equals(Object.class))
	continue;
      
      if (method.getDeclaringClass().equals(org.omg.CORBA.Object.class))
	continue;

      _methodMap.put(method, new MethodMarshal(factory, method, cl));
    }
  }

  MethodMarshal get(Method method)
  {
    return _methodMap.get(method);
  }
}