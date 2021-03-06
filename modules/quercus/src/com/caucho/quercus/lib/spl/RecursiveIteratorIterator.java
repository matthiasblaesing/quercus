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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.spl;

import java.util.ArrayList;

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.ConstStringValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;

public class RecursiveIteratorIterator
  implements OuterIterator, Traversable, Iterator
{
  public static final int LEAVES_ONLY = 0;
  public static final int SELF_FIRST = 1;
  public static final int CHILD_FIRST = 2;
  public static final int CATCH_GET_CHILD = 16;

  private final ArrayList<RecursiveIterator> _iterStack;
  private final int _mode;
  private int _maxDepth;

  public RecursiveIteratorIterator(Env env,
                                   Value value,
                                   @Optional int mode,  //0 == LEAVES_ONLY
                                   @Optional int flags)
  {
    RecursiveIterator iter = RecursiveIteratorProxy.create(env, value);

    _iterStack = new ArrayList<RecursiveIterator>();
    _iterStack.add(iter);

    _mode = mode;
  }

  //
  // OuterIterator
  //

  @Override
  public RecursiveIterator getInnerIterator()
  {
    int i = _iterStack.size() - 1;

    return _iterStack.get(i);
  }

  //
  // Iterator
  //

  @Override
  public boolean valid(Env env)
  {
    return getInnerIterator().valid(env);
  }

  @Override
  public Value current(Env env)
  {
    return getInnerIterator().current(env);
  }

  @Override
  public Value key(Env env)
  {
    return getInnerIterator().key(env);
  }

  @Override
  public void next(Env env)
  {
    RecursiveIterator currentIter = getInnerIterator();

    if (currentIter.hasChildren(env)) {
      Value recursiveIter = currentIter.getChildren(env);
      _iterStack.add(RecursiveIteratorProxy.create(env, recursiveIter));
      currentIter.next(env);
      return;
    }

    currentIter.next(env);

    if (! currentIter.valid(env) && _iterStack.size() > 1) {
      _iterStack.remove(_iterStack.size() - 1);

      next(env);

      return;
    }
  }

  @Override
  public void rewind(Env env)
  {
    for (int i = _iterStack.size() - 1; i > 0; i--) {
      _iterStack.remove(i);
    }

    _iterStack.get(0).rewind(env);
  }

  /**
   * (PHP 5)<br/>
   * Get the current depth of the recursive iteration
   * @link http://php.net/manual/en/recursiveiteratoriterator.getdepth.php
   * @return int The current depth of the recursive iteration.
   */
  public Value getDepth()
  { 
    return LongValue.create(_iterStack.size());
  }
  
  /**
   * (PHP 5 &gt;= 5.1.0)<br/>
   * Set max depth
   * @link http://php.net/manual/en/recursiveiteratoriterator.setmaxdepth.php
   * @param string $max_depth [optional] <p>
   * The maximum allowed depth. -1 is used
   * for any depth.
   * </p>
   * @return void
   */
  public void setMaxDepth(Env env, @Optional(value="-1") Value maxDepth) 
  { 
    _maxDepth = maxDepth.toInt();
  }

  /**
   * (PHP 5 &gt;= 5.1.0)<br/>
   * Get max depth
   * @link http://php.net/manual/en/recursiveiteratoriterator.getmaxdepth.php
   * @return mixed The maximum accepted depth, or false if any depth is allowed.
   */
  public Value getMaxDepth(Env env) 
  { 
    if (_maxDepth < 0) {
      return BooleanValue.FALSE;
    } else {
      return LongValue.create(_maxDepth);
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _iterStack + "]";
  }

  static class RecursiveIteratorProxy implements RecursiveIterator {
    private final Value _obj;

    private static final StringValue HAS_CHILDREN
      = new ConstStringValue("hasChildren");

    private static final StringValue GET_CHILDREN
      = new ConstStringValue("getChildren");

    private static final StringValue VALID
      = new ConstStringValue("valid");

    private static final StringValue CURRENT
      = new ConstStringValue("current");

    private static final StringValue KEY
      = new ConstStringValue("key");

    private static final StringValue NEXT
      = new ConstStringValue("next");

    private static final StringValue REWIND
      = new ConstStringValue("rewind");

    RecursiveIteratorProxy(Value obj)
    {
      _obj = obj;
    }

    public static RecursiveIterator create(Env env, Value value)
    {
      
      if (value.getQuercusClass() == env.getClass("RecursiveDirectoryIterator")) {
        return (RecursiveIterator) value.toJavaObject();
      }
      else {
        return new RecursiveIteratorProxy(value);
      }
    }

    @Override
    public boolean hasChildren(Env env)
    {
      return _obj.callMethod(env, HAS_CHILDREN).toBoolean();
    }

    @Override
    public Value getChildren(Env env)
    {
      return _obj.callMethod(env, GET_CHILDREN);
    }

    @Override
    public boolean valid(Env env)
    {
      return _obj.callMethod(env, VALID).toBoolean();
    }

    @Override
    public Value current(Env env)
    {
      return _obj.callMethod(env, CURRENT);
    }

    @Override
    public Value key(Env env)
    {
      return _obj.callMethod(env, KEY);
    }

    @Override
    public void next(Env env)
    {
      _obj.callMethod(env, NEXT);
    }

    @Override
    public void rewind(Env env)
    {
      _obj.callMethod(env, REWIND);
    }
    
  }
}
