/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is finishThread software; you can redistribute it and/or modify
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

package com.caucho.network.listen;

import java.util.concurrent.atomic.AtomicReference;

import com.caucho.inject.Module;

/**
 * State for an external request.
 */
@Module
enum SocketLinkRequestState {
  /**
   * The allocated, ready to accept state
   */
  INIT {
    @Override
    boolean toAccept(AtomicReference<SocketLinkRequestState> stateRef)
    {
      if (! stateRef.compareAndSet(INIT, REQUEST)) {
        throw new IllegalStateException(this + " to " + stateRef.get());
      }
      
      return true;
    }
  },
  
  /**
   * Handling a request
   */
  REQUEST {
    @Override
    boolean toIdle(AtomicReference<SocketLinkRequestState> stateRef)
    {
      if (! stateRef.compareAndSet(REQUEST, INIT)) {
        throw new IllegalStateException(this + " to " + stateRef.get());
      }
      
      return true;
    }
    
    @Override
    boolean toKeepalive(AtomicReference<SocketLinkRequestState> stateRef)
    {
      if (! stateRef.compareAndSet(REQUEST, KEEPALIVE)) {
        throw new IllegalStateException(this + " to " + stateRef.get());
      }
      
      return true;
    }
    
    @Override
    boolean toAsyncStart(AtomicReference<SocketLinkRequestState> stateRef)
    {
      if (! stateRef.compareAndSet(REQUEST, ASYNC_START)) {
        throw new IllegalStateException(this + " to " + stateRef.get());
      }
      
      return true;
    }
  },
  
  /**
   * Keepalive detached
   */
  KEEPALIVE {
    @Override
    boolean toWakeKeepalive(AtomicReference<SocketLinkRequestState> stateRef)
    {
      if (! stateRef.compareAndSet(KEEPALIVE, REQUEST)) {
        throw new IllegalStateException(this + " to " + stateRef.get());
      }
      
      return true;
    }
  },
  
  ASYNC_START {
    @Override
    boolean toAsyncSuspend(AtomicReference<SocketLinkRequestState> stateRef)
    {
      if (stateRef.compareAndSet(ASYNC_START, SUSPEND)) {
        return true;
      }
      
      if (stateRef.compareAndSet(ASYNC_WAKE, REQUEST)) {
        return false;
      }

      throw new IllegalStateException(this + " to " + stateRef.get());
    }
    
    @Override
    boolean toAsyncWake(AtomicReference<SocketLinkRequestState> stateRef)
    {
      if (stateRef.compareAndSet(ASYNC_START, ASYNC_WAKE)) {
        return false;
      }
      
      return stateRef.get().toAsyncWake(stateRef);
    }
  },
  
  ASYNC_WAKE {
    @Override
    boolean toAsyncSuspend(AtomicReference<SocketLinkRequestState> stateRef)
    {
      if (stateRef.compareAndSet(ASYNC_WAKE, REQUEST)) {
        return false;
      }

      throw new IllegalStateException(this + " to " + stateRef.get());
    }
  },
  
  /**
   * Comet suspend
   */
  SUSPEND {
    @Override
    boolean toAsyncWake(AtomicReference<SocketLinkRequestState> stateRef)
    {
      if (! stateRef.compareAndSet(SUSPEND, REQUEST)) {
        throw new IllegalStateException(this + " to " + stateRef.get());
      }
      
      return true;
    }
  };

  boolean toAccept(AtomicReference<SocketLinkRequestState> stateRef)
  {
    throw new IllegalStateException(toString());
  }

  boolean toKeepalive(AtomicReference<SocketLinkRequestState> stateRef)
  {
    throw new IllegalStateException(toString());
  }

  boolean toWakeKeepalive(AtomicReference<SocketLinkRequestState> stateRef)
  {
    throw new IllegalStateException(toString());
  }

  boolean toAsyncStart(AtomicReference<SocketLinkRequestState> stateRef)
  {
    throw new IllegalStateException(toString());
  }

  boolean toAsyncSuspend(AtomicReference<SocketLinkRequestState> stateRef)
  {
    throw new IllegalStateException(toString());
  }

  boolean toAsyncWake(AtomicReference<SocketLinkRequestState> stateRef)
  {
    throw new IllegalStateException(toString());
  }

  boolean toIdle(AtomicReference<SocketLinkRequestState> stateRef)
  {
    throw new IllegalStateException(toString());
  }
}
