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
 * @author Alex Rojkov
 */

package com.caucho.boot;

import com.caucho.VersionFactory;
import com.caucho.util.L10N;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command to stop Resin server
 * bin/resin.sh stop -server a
 */
public class StopCommand extends AbstractStartCommand
{
  private static Logger _log;
  private static L10N _L;

  @Override
  public String getName()
  {
    return "stop";
  }

  @Override
  public int doCommand(WatchdogArgs args,
                       WatchdogClient client)
    throws BootArgumentException
  {
    validateArgs(args.getArgv());

    try {
      client.stopWatchdog(args.getServerId());

      System.out.println(L().l(
        "Resin/{0} stopped -server '{1}' for watchdog at {2}:{3}",
        VersionFactory.getVersion(),
        args.getServerId(),
        client.getWatchdogAddress(),
        client.getWatchdogPort()));
    } catch (Exception e) {
      System.out.println(L().l(
        "Resin/{0} can't stop -server '{1}' for watchdog at {2}:{3}.\n{4}",
        VersionFactory.getVersion(),
        args.getServerId(),
        client.getWatchdogAddress(),
        client.getWatchdogPort(),
        e.toString()));

      log().log(Level.FINE, e.toString(), e);

      System.exit(1);
    }

    return 0;
  }

  @Override
  public boolean isRetry()
  {
    return true;
  }

  private static Logger log()
  {
    if (_log == null)
      _log = Logger.getLogger(StopCommand.class.getName());

    return _log;
  }

  private static L10N L()
  {
    if (_L == null)
      _L = new L10N(StopCommand.class);

    return _L;
  }

  @Override
  public void usage()
  {
    System.out.println("usage: bin/resin.sh [-options] stop");
    System.out.println();
    System.out.println("where options include:");
    System.out.println("   -conf <file>          : select a configuration file");
    System.out.println("   -data-directory <dir> : select a resin-data directory");
    System.out.println("   -log-directory <dir>  : select a logging directory");
    System.out.println("   -resin-home <dir>     : select a resin home directory");
    System.out.println("   -root-directory <dir> : select a root directory");
    System.out.println("   -server <id>          : select a <server> to run");
    System.out.println("   -watchdog-port <port> : override the watchdog-port");
    System.out.println("   -verbose              : print verbose starting information");
  }
}