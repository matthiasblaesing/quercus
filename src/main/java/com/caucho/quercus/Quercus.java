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

package com.caucho.quercus;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.vfs.InputStreamStream;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.StdoutStream;
import com.caucho.vfs.StringPath;
import com.caucho.vfs.WriteStream;

public class Quercus
  extends QuercusContext
{
  private static final Logger log
    = Logger.getLogger(Quercus.class.getName());

  private String _fileName;
  private String []_argv;

  public Quercus()
  {
    super();
  }

  //
  // command-line main
  //

  public static void main(String []args)
    throws IOException
  {
    Quercus quercus = new Quercus();

    startMain(args, quercus);
  }

  public static int startMain(String []args, Quercus quercus)
    throws IOException
  {
    if (! quercus.parseArgs(args)) {
      quercus.printUsage();
      return 1;
    }

    quercus.init();
    quercus.start();

    if (quercus.getFileName() != null) {
      return quercus.execute();
    }
    else {
      InputStream is = System.in;

      ReadStream stream = new ReadStream(new InputStreamStream(is));

      return quercus.execute(stream);
    }
  }

  public void printUsage()
  {
    System.out.println("usage: " + getClass().getName() + " [flags] <file> [php-args]");
    System.out.println(" -f            : Explicitly set the script filename.");
    System.out.println(" -d name=value : Sets a php ini value.");
  }

  @Override
  public String getSapiName()
  {
    return "cli";
  }

  public String getFileName()
  {
    return _fileName;
  }

  public void setFileName(String name)
  {
    _fileName = name;
  }

  protected boolean parseArgs(String []args)
  {
    ArrayList<String> phpArgList = new ArrayList<String>();

    int i = 0;
    for (; i < args.length; i++) {
      if (args[i].startsWith("-d")) {
        String arg;
        if ("-d".equals(args[i])) {
          arg = args[i + 1];
          i++;
        } else {
          // e.g. -dxdebug.remote_enable=1
          arg = args[i].substring(2);
        }
        int eqIndex = arg.indexOf('=');

        String name;
        String value;

        if (eqIndex >= 0) {
          name = arg.substring(0, eqIndex);
          value = arg.substring(eqIndex + 1);
        }
        else {
          name = arg;
          value = "";
        }

        setIni(name, value);
      }
      else if ("-f".equals(args[i])) {
        _fileName = args[++i];
      }
      else if ("-q".equals(args[i])) {
        // quiet
      }
      else if ("-n".equals(args[i])) {
        // no php-pip
      }
      else if ("--".equals(args[i])) {
        break;
      }
      else if ("-h".equals(args[i])
               || "--help".equals(args[i])) {
        return false;
      }
      else if (args[i].startsWith("-")) {
        System.out.println("unknown option: " + args[i]);
        return false;
      }
      else {
        break;
      }
    }

    for (; i < args.length; i++) {
      phpArgList.add(args[i]);
    }

    _argv = phpArgList.toArray(new String[phpArgList.size()]);

    if (_fileName == null && _argv.length > 0)
      _fileName = _argv[0];

    return true;
  }

  protected String[] getArgv() {
    return _argv;
  }

  public int execute()
    throws IOException
  {
    Path path = getPwd().lookup(_fileName);

    return execute(path);
  }

  public int execute(String code)
    throws IOException
  {
    Path path = new StringPath(code);

    return execute(path);
  }

  public int execute(Path path)
    throws IOException
  {
    QuercusPage page = parse(path);

    WriteStream os = new WriteStream(StdoutStream.create());

    os.setNewlineString("\n");
    os.setEncoding("iso-8859-1");

    Env env = createEnv(page, os, null, null);
    env.start();

    try {
      env.execute();
      return 0;
    } catch (QuercusDieException e) {
      log.log(Level.FINER, e.toString(), e);
      return e.getExitValue();
    } catch (QuercusExitException e) {
      log.log(Level.FINER, e.toString(), e);
      return e.getExitValue();
    } catch (QuercusErrorException e) {
      log.log(Level.FINER, e.toString(), e);
      return 1;
    } finally {
      env.close();

      os.flush();
    }
  }

  public int execute(ReadStream stream)
    throws IOException
  {
    QuercusPage page = parse(stream);

    WriteStream os = new WriteStream(StdoutStream.create());

    os.setNewlineString("\n");
    os.setEncoding("iso-8859-1");

    Env env = createEnv(page, os, null, null);
    env.start();

    try {
      env.execute();
      return 0;
    } catch (QuercusDieException e) {
      log.log(Level.FINER, e.toString(), e);
      return e.getExitValue();
    } catch (QuercusExitException e) {
      log.log(Level.FINER, e.toString(), e);
      return e.getExitValue();
    } catch (QuercusErrorException e) {
      log.log(Level.FINER, e.toString(), e);
      return 1;
    } finally {
      env.close();

      os.flush();
    }
  }
}