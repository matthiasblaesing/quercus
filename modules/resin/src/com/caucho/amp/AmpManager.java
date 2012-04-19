/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.amp;

import com.caucho.amp.actor.AmpActor;
import com.caucho.amp.broker.AmpBroker;

/**
 * Manages an AMP domain.
 */
public interface AmpManager
{
  /**
   * Returns the domain's broker.
   */
  public AmpBroker getBroker();
  
  /**
   * Creates a client proxy to an api.
   */
  public <T> T createActorProxy(String address, Class<T> api);
  
  /**
   * Adds a bean to be proxied as an actor.
   */
  public void addActor(String address, Object bean);
  
  /**
   * Adds an actor stream, creating the mailbox for it 
   * using the default factory.
   */
  public void addActor(String address, AmpActor actor);
}