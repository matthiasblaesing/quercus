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
 * @author Emil Ong
 */

package com.caucho.quercus.lib.bam;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Logger;

import com.caucho.bam.BamError;
import com.caucho.bam.actor.ActorSender;
import com.caucho.bam.actor.SimpleActorSender;
import com.caucho.bam.stream.MessageStream;
import com.caucho.bam.stream.NullMessageStream;
import com.caucho.hemp.broker.HempBroker;
import com.caucho.hmtp.HmtpClient;
import com.caucho.quercus.annotation.ClassImplementation;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.ConstStringValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.xmpp.im.ImMessage;
import com.caucho.xmpp.im.ImPresence;
import com.caucho.xmpp.im.RosterItem;
import com.caucho.xmpp.im.RosterQuery;
import com.caucho.xmpp.im.Text;

/**
 * BAM functions
 */
@ClassImplementation
public class BamModule extends AbstractQuercusModule
{
  private static final Logger log
    = Logger.getLogger(BamModule.class.getName());
  private static final L10N L = new L10N(BamModule.class);

  private static final StringValue PHP_SELF 
    = new ConstStringValue("PHP_SELF");

  private static final StringValue SERVER_NAME 
    = new ConstStringValue("SERVER_NAME");

  private static BamPhpActor getActor(Env env)
  {
    Value actorValue = env.getGlobalValue("_quercus_bam_actor");

    if (actorValue != null && ! actorValue.isNull())
      return (BamPhpActor) actorValue.toJavaObject();

    return null;
  }

  private static ActorSender getActorClient(Env env)
  {
    ActorSender connection
      = (ActorSender) env.getSpecialValue("_quercus_bam_connection");

    // create a connection lazily
    if (connection == null) {
      HempBroker broker = HempBroker.getCurrent();

      String address = "php@" + env.getGlobalVar("_SERVER").get(SERVER_NAME);
      String resource = env.getGlobalVar("_SERVER").get(PHP_SELF).toString();

      if (resource.indexOf('/') == 0)
        resource = resource.substring(1);
      
      NullMessageStream stream = new NullMessageStream(address, broker);

      connection = new SimpleActorSender(stream, broker, address, resource);
      env.addCleanup(new BamConnectionResource(connection));
      env.setSpecialValue("_quercus_bam_connection", connection);
    }

    return connection;
  }

  private static BamPhpServiceManager getServiceManager(Env env)
  {
    Value managerValue = env.getGlobalValue("_quercus_bam_service_manager");

    if (managerValue != null && ! managerValue.isNull())
      return (BamPhpServiceManager) managerValue.toJavaObject();

    return null;
  }

  private static MessageStream getBrokerStream(Env env)
  {
    BamPhpActor actor = getActor(env);

    if (actor != null)
      return actor.getBroker();

    ActorSender connection = getActorClient(env);

    return connection.getBroker();
  }

  private static String getAddress(Env env)
  {
    BamPhpActor actor = getActor(env);

    if (actor != null)
      return actor.getAddress();

    ActorSender connection = getActorClient(env);

    return connection.getAddress();
  }

  public static Value bam_login(Env env, 
                                String url, 
                                String username, 
                                String password)
  {
    BamPhpActor actor = getActor(env);

    if (actor != null)
      return env.error("bam_login not available from actor script");

    HmtpClient client = null;//new HmtpClient(url);

    BamConnectionResource resource = new BamConnectionResource(client);
    env.addCleanup(resource);

    try {
      client.connect(username, password);
    }
    catch (Exception e) {
      e.printStackTrace();
      return env.error("Unable to connect to BAM server", e);
    }

    env.setSpecialValue("_quercus_bam_connection", client);

    return BooleanValue.TRUE;
  }

  public static Value bam_service_exists(Env env, String address)
  {
    BamPhpServiceManager manager = getServiceManager(env);

    if (manager == null)
      return env.error("bam_service_exists must be called from " + 
                       "service manager script");

    return BooleanValue.create(manager.hasChild(address));
  }

  /**
   * Registers a "child" service that is represented by the given script.
   **/
  public static Value bam_register_service(Env env, String address, String script)
  {
    BamPhpServiceManager manager = getServiceManager(env);

    if (manager == null)
      return env.error("bam_register_service must be called from " + 
                       "service manager script");

    Path path = env.getSelfDirectory().lookup(script);

    if (! path.exists())
      return env.error("script not found: " + script);

    BamPhpActor child = new BamPhpActor();
    child.setAddress(address);
    child.setScript(path);
    // child.setBroker(manager.getBroker());

    //InjectManager container = InjectManager.getCurrent();
    //container.injectObject(child);

    manager.addChild(address, child);

    return BooleanValue.TRUE;
  }

  /**
   * Registers a "child" service that is represented by the given script.
   **/
  public static Value bam_unregister_service(Env env, String address)
  {
    BamPhpServiceManager manager = getServiceManager(env);

    if (manager == null)
      return env.error("bam_unregister_service must be called from " + 
                       "service manager script");

    BamPhpActor service = manager.removeChild(address);

    if (service == null)
      return BooleanValue.FALSE;

    // XXX: manager.getBroker().removeMailbox(service);

    return BooleanValue.TRUE;
  }

  public static Value bam_actor_exists(Env env, String address)
  {
    BamPhpActor actor = getActor(env);

    if (actor == null)
      return env.error("bam_actor_exists must be called from actor script");

    return BooleanValue.create(actor.hasChild(address));
  }

  /**
   * Registers a "child" actor that is represented by the given script.
   **/
  public static Value bam_register_actor(Env env, String address, String script)
  {
    BamPhpActor actor = getActor(env);

    if (actor == null)
      return env.error("bam_register_actor must be called from actor script");

    BamPhpActor child = new BamPhpActor();
    child.setAddress(address);

    Path path = env.getSelfDirectory().lookup(script);

    if (! path.exists())
      return env.error("script not found: " + script);

    child.setScript(path);

    //InjectManager container = InjectManager.getCurrent();
    //container.injectObject(child);

    actor.addChild(address, child);

    return BooleanValue.TRUE;
  }

  public static String bam_my_address(Env env)
  {
    return getAddress(env);
  }

  //
  // Utilities
  //

  public static String bam_bare_address(Env env, String uri)
  {
    int slash = uri.indexOf('/');

    if (slash < 0)
      return uri;

    return uri.substring(0, slash);
  }

  public static String bam_address_resource(Env env, String uri)
  {
    int slash = uri.indexOf('/');

    if (slash < 0 || slash == uri.length() - 1)
      return "";

    return uri.substring(slash + 1);
  }

  //
  // Transmit
  //

  public static void bam_send_message(Env env, String to, Serializable value)
  {
    getBrokerStream(env).message(to, getAddress(env), value);
  }

  public static void bam_send_message_error(Env env, 
                                            String to, 
                                            Serializable value, 
                                            BamError error)
  {
    getBrokerStream(env).messageError(to, getAddress(env), value, error);
  }

  public static Value bam_send_query(Env env, 
                                         long id, 
                                         String to, 
                                         Serializable value)
  {
    String from = getAddress(env);
    getBrokerStream(env).query(id, to, from, value);

    return BooleanValue.TRUE;
  }

  public static void bam_send_query_result(Env env, 
                                           long id, 
                                           String to,
                                           Serializable value)
  {
    getBrokerStream(env).queryResult(id, to, getAddress(env), value);
  }

  public static void bam_send_query_error(Env env, 
                                          long id, String to,
                                          Serializable value, BamError error)
  {
    getBrokerStream(env).queryError(id, to, getAddress(env), value, error);
  }

  public static Value im_send_message(Env env, 
                                      String to, 
                                      String from, 
                                      Value body,
                                      @Optional("chat") String type,
                                      @Optional Value subject,
                                      @Optional String thread,
                                      @Optional Serializable[] extras)
  {
    Text[] subjects = null;

    // extract subject text
    if (subject != null) {
      if (subject.isArray()) {
        subjects = new Text[subject.getSize()];

        int i = 0;
        Iterator<Value> iterator = subject.getValueIterator(env);

        while (iterator.hasNext()) {
          Value subjectValue = iterator.next();

          if (! subjectValue.isString())
            return env.error("subject values must be strings");

          subjects[i++] = new Text(subjectValue.toString());
        }
      }
      else if (subject.isString()) {
        if (! subject.isString())
          return env.error("subject values must be strings");

        subjects = new Text[] { new Text(subject.toString()) };
      }
    }

    // extract body text
    Text[] bodies = null;

    if (body.isArray()) {
      bodies = new Text[body.getSize()];

      int i = 0;
      Iterator<Value> iterator = body.getValueIterator(env);

      while (iterator.hasNext()) {
        Value bodyValue = iterator.next();

        if (! bodyValue.isString())
          return env.error("body values must be strings");

        bodies[i++] = new Text(bodyValue.toString());
      }
    }
    else if (body.isString()) {
      if (! body.isString())
        return env.error("body values must be strings");

      bodies = new Text[] { new Text(body.toString()) };
    }

    ImMessage message = new ImMessage(to, from, type, 
                                      subjects, bodies, thread, extras);
    
    bam_send_message(env, to, message);

    return BooleanValue.TRUE;
  }

  public static RosterItem im_create_roster_item(Env env, 
                                                 String address,
                                                 @Optional String name,
                                                 @Optional String subscription,
                                                 @Optional 
                                                 ArrayList<String> groupList)
  {
    if ("".equals(subscription))
      subscription = "to";

    return new RosterItem(null, address, name, subscription, groupList);
  }

  public static void im_send_roster(Env env, 
                                    long id, String to, 
                                    ArrayList<RosterItem> roster)
  {
    bam_send_query_result(env, id, to, new RosterQuery(roster));
  }
  
  private static ImPresence createPresence(Env env, 
                                           String to, 
                                           String from,
                                           String show,
                                           String status,
                                           int priority,
                                           ArrayList<Serializable> extras)
  {
    if ("".equals(from))
      from = getAddress(env);

    if ("".equals(show))
      show = null;

    Text statusText = null;

    if (! "".equals(status))
      statusText = new Text(status);

    return new ImPresence(to, from, show, statusText, priority, extras);
  }


  public static void im_send_presence(Env env, 
                                      String to, 
                                      @Optional String from,
                                      @Optional String show,
                                      @Optional String status,
                                      @Optional int priority,
                                      @Optional ArrayList<Serializable> extras)
  {
    ImPresence presence = 
      createPresence(env, to, from, show, status, priority, extras);

    bam_send_message(env, to, presence);
  }

  public static void im_send_presence_unavailable(Env env, 
                                                  String to, 
                                                  @Optional String from,
                                                  @Optional String show,
                                                  @Optional String status,
                                                  @Optional int priority,
                                                  @Optional 
                                                  ArrayList<Serializable> 
                                                  extras)
  {
    // XXX: needs to be ImPresenceUnavailable
    ImPresence presence = 
      createPresence(env, to, from, show, status, priority, extras);

    bam_send_message(env, to, presence);
  }

  /**
   * Makes a subscription request.
   **/
  public static void im_send_presence_subscribe(Env env, 
                                                String to, 
                                                @Optional String from,
                                                @Optional String show,
                                                @Optional String status,
                                                @Optional int priority,
                                                @Optional 
                                                ArrayList<Serializable> extras)
  {
    // XXX: presenceSubscribe
    ImPresence presence = 
      createPresence(env, to, from, show, status, priority, extras);

    bam_send_message(env, to, presence);
  }

  /**
   * Approves a subscription request.
   **/
  public static void im_send_presence_subscribed(Env env, 
                                                 String to, 
                                                 @Optional String from,
                                                 @Optional String show,
                                                 @Optional String status,
                                                 @Optional int priority,
                                                 @Optional 
                                                 ArrayList<Serializable> extras)
  {
    // presenceSubscribed
    ImPresence presence = 
      createPresence(env, to, from, show, status, priority, extras);

    bam_send_message(env, to, presence);
  }

  /**
   * Makes an unsubscription request.
   **/
  public static void im_send_presence_unsubscribe(Env env, 
                                                  String to, 
                                                  @Optional String from,
                                                  @Optional String show,
                                                  @Optional String status,
                                                  @Optional int priority,
                                                  @Optional 
                                                  ArrayList<Serializable> 
                                                  extras)
  {
    // XXX: presenceUnsubscribe
    ImPresence presence = 
      createPresence(env, to, from, show, status, priority, extras);

    bam_send_message(env, to, presence);
  }

  /**
   * Rejects a subscription request.
   **/
  public static void im_send_presence_unsubscribed(Env env, 
                                                   String to, 
                                                   @Optional String from,
                                                   @Optional String show,
                                                   @Optional String status,
                                                   @Optional int priority,
                                                   @Optional 
                                                   ArrayList<Serializable> 
                                                   extras)
  {
    // presenceUnsubscribe
    ImPresence presence = 
      createPresence(env, to, from, show, status, priority, extras);

    bam_send_message(env, to, presence);
  }

  public static void im_send_presence_probe(Env env, 
                                            String to, 
                                            @Optional String from,
                                            @Optional String show,
                                            @Optional String status,
                                            @Optional int priority,
                                            @Optional 
                                            ArrayList<Serializable> extras)
  {
    // presenceProbe
    ImPresence presence = 
      createPresence(env, to, from, show, status, priority, extras);

    bam_send_message(env, to, presence);
  }

  /**
   * Dispatches messages, queries, and presences to handler functions based
   * on their prefixes.
   **/
  public static Value bam_dispatch(Env env)
  {
    // manager script dispatch
    BamPhpServiceManager manager = getServiceManager(env);

    if (manager != null) {
      AbstractFunction function = null;

      if (env.getGlobalValue("_quercus_bam_start_service") != null) {
        function = env.findFunction("bam_start_service");
      } 
      else if (env.getGlobalValue("_quercus_bam_stop_service") != null) {
        function = env.findFunction("bam_stop_service");
      }

      if (function == null) {
        env.setGlobalValue("_quercus_bam_function_return", BooleanValue.FALSE);

        return BooleanValue.FALSE;
      }

      Value address = env.getGlobalValue("_quercus_bam_service_address");
      Value ret = function.call(env, address);

      env.setGlobalValue("_quercus_bam_function_return", ret);

      return BooleanValue.TRUE;
    }

    // actor script dispatch

    Value eventTypeValue = env.getGlobalValue("_quercus_bam_event_type");

    if (eventTypeValue == null)
      return BooleanValue.FALSE;

    BamEventType eventType = (BamEventType) eventTypeValue.toJavaObject();

    Value to = env.getGlobalValue("_quercus_bam_to");
    Value from = env.getGlobalValue("_quercus_bam_from");
    Value value = env.getGlobalValue("_quercus_bam_value");

    AbstractFunction function = findFunction(env, eventType.getPrefix(), value);

    if (function == null) {
      log.fine(L.l("bam handler function not found for {0}", eventType));

      return BooleanValue.FALSE;
    }

    Value functionReturn = BooleanValue.FALSE;

    if (eventType.hasId() && eventType.hasError()) {
      Value id = env.getGlobalValue("_quercus_bam_id");
      Value error = env.getGlobalValue("_quercus_bam_error");

      functionReturn = function.call(env, id, to, from, value, error);
    } 
    else if (! eventType.hasId() && eventType.hasError()) {
      Value error = env.getGlobalValue("_quercus_bam_error");

      functionReturn = function.call(env, to, from, value, error);
    }
    else if (eventType.hasId() && ! eventType.hasError()) {
      Value id = env.getGlobalValue("_quercus_bam_id");

      functionReturn = function.call(env, id, to, from, value);
    }
    else {
      functionReturn = function.call(env, to, from, value);
    }

    env.setGlobalValue("_quercus_bam_function_return", functionReturn);

    return functionReturn;
  }

  /**
   * Finds the handler function for a value with the given prefix.  If there
   * is a specific handler for a specific value type, that is returned 
   * otherwise the generic handler (with the name of the prefix) is returned
   * if found.
   **/
  private static AbstractFunction findFunction(Env env, 
                                               String prefix, 
                                               Value value)
  {
    if (value == null)
      return env.findFunction(prefix);

    Object obj = value.toJavaObject();

    if (obj == null)
      return env.findFunction(prefix);

    String typeName = obj.getClass().getSimpleName().toLowerCase(Locale.ENGLISH);
    String functionName = prefix + '_' + typeName;

    AbstractFunction function = env.findFunction(functionName);

    if (function == null)
      function = env.findFunction(prefix);

    return function;
  }

}

