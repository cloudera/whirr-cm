/**
 * Licensed to Cloudera, Inc. under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Cloudera, Inc. licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudera.whirr.cm.server;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.WordUtils;
import org.codehaus.plexus.util.StringUtils;

import com.cloudera.whirr.cm.server.impl.CmServerFactory;
import com.cloudera.whirr.cm.server.impl.CmServerImpl;
import com.cloudera.whirr.cm.server.impl.CmServerLog;

public class CmServerCommand {

  public static final String ARGUMENT_PREFIX = "--";

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface CmServerCommandMethod {
    String name();
  }

  private String host;
  private int port = 7180;
  private String user = "admin";
  private String password = "admin";

  private File client;
  private String command;

  private CmServerCluster cluster;

  private CmServerLog logger = new CmServerLog.CmServerLogSysOut();

  private CmServer server;

  private static final Map<String, Method> COMMANDS = new HashMap<String, Method>();
  static {
    for (Method method : CmServerImpl.class.getMethods()) {
      if (method.isAnnotationPresent(CmServerCommandMethod.class)) {
        COMMANDS.put(method.getAnnotation(CmServerCommandMethod.class).name(), method);
      }
    }
  }

  private static final Map<String, Method> CONFIG_COMMANDS = new HashMap<String, Method>();
  static {
    for (Method method : CmServerCommand.class.getMethods()) {
      if (method.isAnnotationPresent(CmServerCommandMethod.class)) {
        CONFIG_COMMANDS.put(method.getAnnotation(CmServerCommandMethod.class).name(), method);
      }
    }
  }

  private CmServerCommand() throws CmServerException {
  }

  public static Set<String> getCommands() {
    return new HashSet<String>(COMMANDS.keySet());
  }

  public static CmServerCommand get() throws CmServerException {
    return new CmServerCommand();
  }

  public CmServerCommand arguments(String[] arguments) throws CmServerException {
    return arguments(argumentsPreProcess(arguments));
  }

  public CmServerCommand arguments(Map<String, String> arguments) throws CmServerException {
    for (String argument : arguments.keySet()) {
      if (CONFIG_COMMANDS.containsKey(argument)) {
        try {
          CONFIG_COMMANDS.get(argument).invoke(this, new Object[] { arguments.get(argument) });
        } catch (Exception exception) {
          throw new CmServerException("Unexpected runtime exception setting argument", exception);
        }
      }
    }
    return this;
  }

  @CmServerCommandMethod(name = "host")
  public CmServerCommand host(String host) throws CmServerException {
    if (host == null || host.equals("")) {
      throw new CmServerException("Illegal host argument passed [" + host + "]");
    }
    this.host = host;
    this.server = null;
    return this;
  }

  @CmServerCommandMethod(name = "port")
  public CmServerCommand port(String port) throws CmServerException {
    if (port == null || port.equals("") || !StringUtils.isNumeric(port)) {
      throw new CmServerException("Illegal port argument passed [" + port + "]");
    }
    this.port = Integer.parseInt(port);
    this.server = null;
    return this;
  }

  @CmServerCommandMethod(name = "user")
  public CmServerCommand user(String user) throws CmServerException {
    if (user == null || user.equals("")) {
      throw new CmServerException("Illegal user argument passed [" + user + "]");
    }
    this.user = user;
    this.server = null;
    return this;
  }

  @CmServerCommandMethod(name = "password")
  public CmServerCommand password(String password) throws CmServerException {
    if (password == null || password.equals("")) {
      throw new CmServerException("Illegal password argument passed [" + password + "]");
    }
    this.password = password;
    this.server = null;
    return this;
  }

  @CmServerCommandMethod(name = "client")
  public CmServerCommand client(String client) throws CmServerException {
    if (client == null) {
      throw new CmServerException("Illegal client argument passed [" + client + "]");
    }
    this.client = new File(client);
    return this;
  }

  @CmServerCommandMethod(name = "command")
  public CmServerCommand command(String command) throws CmServerException {
    if (command == null || !COMMANDS.containsKey(command)) {
      throw new CmServerException("Illegal command argument passed [" + command + "]");
    }
    this.command = command;
    return this;
  }

  public CmServerCommand cluster(CmServerCluster cluster) throws CmServerException {
    if (cluster == null || cluster.isEmpty()) {
      throw new CmServerException("Illegal cluster argument passed [" + cluster + "]");
    }
    this.cluster = cluster;
    return this;
  }

  public CmServerCommand logger(CmServerLog logger) throws CmServerException {
    if (logger == null) {
      throw new CmServerException("Illegal logger argument passed [" + logger + "]");
    }
    this.logger = logger;
    return this;
  }

  @SuppressWarnings("unchecked")
  public boolean execute() throws CmServerException {
    if (host == null) {
      throw new CmServerException("Required paramater [host] not set");
    }
    if (command == null) {
      throw new CmServerException("Required paramater [command] not set");
    }
    if (server == null) {
      server = CmServerFactory.getCmServer(host, port, user, password, new CmServerLog.CmServerLogNull());
    }
    List<Object> paramaters = new ArrayList<Object>();
    for (Class<?> clazz : COMMANDS.get(command).getParameterTypes()) {
      if (clazz.equals(CmServerCluster.class)) {
        if (cluster == null) {
          throw new CmServerException("Required paramater [cluster] not set");
        }
        paramaters.add(cluster);
      } else if (clazz.equals(File.class)) {
        if (client == null) {
          throw new CmServerException("Required paramater [client] not set");
        }
        paramaters.add(client);
      } else {
        throw new CmServerException("Unexpected paramater type [" + clazz.getName() + "]");
      }
    }
    try {

      logger.logOperationStartedSync(WordUtils.capitalize(command));

      Object commandReturn = COMMANDS.get(command).invoke(server, paramaters.toArray());

      boolean commandReturnBoolean = false;
      if (commandReturn instanceof Boolean) {

        commandReturnBoolean = ((Boolean) commandReturn).booleanValue();

      } else if (commandReturn instanceof List<?>) {

        List<?> commandReturnList = ((List<?>) commandReturn);
        commandReturnBoolean = !commandReturnList.isEmpty();
        if (commandReturnBoolean && commandReturnList.get(0) instanceof CmServerService) {
          for (CmServerService service : ((List<CmServerService>) commandReturnList)) {
            logger.logOperationInProgressSync(WordUtils.capitalize(command),
                "  " + (service.getName() + "@" + service.getHost() + "=" + service.getStatus()));
          }
        }

      } else if (commandReturn instanceof CmServerCluster) {

        CmServerCluster commandReturnCluster = ((CmServerCluster) commandReturn);
        commandReturnBoolean = !commandReturnCluster.isEmpty();
        for (CmServerServiceType type : commandReturnCluster.getServiceTypes()) {
          logger.logOperationInProgressSync(WordUtils.capitalize(command), "  " + type.toString());
          for (CmServerService service : commandReturnCluster.getServices(type)) {
            logger.logOperationInProgressSync(WordUtils.capitalize(command),
                "    " + service.getName() + "@" + service.getHost() + "=" + service.getStatus());
          }
        }

      }

      logger.logOperationFinishedSync(WordUtils.capitalize(command));

      return commandReturnBoolean;

    } catch (Exception exception) {
      throw new CmServerException("Unexpected runtime exception executing CM Server", exception);
    }
  }

  public static Map<String, String> argumentsPreProcess(String[] arguments) throws CmServerException {
    Map<String, String> argumentsProcessed = new HashMap<String, String>();
    try {
      for (int i = 0; i < arguments.length; i++) {
        if (arguments[i] == null || arguments[i].indexOf(ARGUMENT_PREFIX) != 0) {
          throw new IllegalArgumentException("Argument [" + arguments[i] + "] does not include valid prefix ["
              + ARGUMENT_PREFIX + "]");
        }
        argumentsProcessed.put(arguments[i].replaceFirst(ARGUMENT_PREFIX, ""), arguments[++i]);
      }
    } catch (Exception exception) {
      throw new CmServerException("Illegal arguments " + Arrays.asList(arguments)
          + " must be of the form [--name1, value1, --name2, value2, ...]", exception);
    }
    return argumentsProcessed;
  }

}