/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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

package javax.servlet.http;

/**
 * The event object for adding or removing a session object.  See
 * HttpSessionBindingListener.
 */
public class HttpSessionBindingEvent extends HttpSessionEvent {
  private String name;
  private Object value;

  /**
   * Create a new session binding event.
   *
   * @param session the http session
   * @param name the session attribute name.
   */
  public HttpSessionBindingEvent(HttpSession session, String name)
  {
    super(session);
    this.name = name;
  }

  /**
   * Create a new session binding event.
   *
   * @param session the http session
   * @param name the session attribute name.
   * @param value the session attribute value.
   */
  public HttpSessionBindingEvent(HttpSession session, String name, Object value)
  {
    super(session);
    this.name = name;
    this.value = value;
  }

  /**
   * Returns the session variable name.
   */
  public String getName()
  {
    return name;
  }

  /**
   * Returns the session variable value.
   */
  public Object getValue()
  {
    return value;
  }

  /**
   * Returns the session.
   */
  public HttpSession getSession()
  {
    return (HttpSession) getSource();
  }
}
