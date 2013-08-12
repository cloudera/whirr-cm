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
import java.util.Collections;
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

public class CmServerBuilder implements CmServerConstants {

  public static final String ARGUMENT_PREFIX = "--";

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface CmServerCommandMethod {
    String name();
  }

  private String version;
  private String versionApi;
  private String versionCdh;

  private String ip;
  private String ipInternal;
  private int port = 7180;
  private String user = "admin";
  private String password = "admin";

  private File path;
  private String command;

  private CmServerCluster cluster;

  private CmServerFactory factory = new CmServerFactory();

  private CmServerLog logger = new CmServerLog.CmServerLogSysOut(LOG_TAG_CM_SERVER_CMD, false);

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
    for (Method method : CmServerBuilder.class.getMethods()) {
      if (method.isAnnotationPresent(CmServerCommandMethod.class)) {
        CONFIG_COMMANDS.put(method.getAnnotation(CmServerCommandMethod.class).name(), method);
      }
    }
  }

  public CmServerBuilder() throws CmServerException {
  }

  public static Set<String> getCommands() {
    return new HashSet<String>(COMMANDS.keySet());
  }

  public CmServerBuilder arguments(String[] arguments) throws CmServerException {
    return arguments(argumentsPreProcess(arguments));
  }

  public CmServerBuilder arguments(Map<String, String> arguments) throws CmServerException {
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

  @CmServerCommandMethod(name = "version")
  public CmServerBuilder version(String version) throws CmServerException {
    this.version = version;
    this.server = null;
    return this;
  }

  @CmServerCommandMethod(name = "versionApi")
  public CmServerBuilder versionApi(String versionApi) throws CmServerException {
    this.versionApi = versionApi;
    this.server = null;
    return this;
  }

  @CmServerCommandMethod(name = "versionCdh")
  public CmServerBuilder versionCdh(String versionCdh) throws CmServerException {
    this.versionCdh = versionCdh;
    this.server = null;
    return this;
  }
  
  @CmServerCommandMethod(name = "ip")
  public CmServerBuilder ip(String ip) throws CmServerException {
    if (ip == null || ip.equals("")) {
      throw new CmServerException("Illegal IP argument passed [" + ip + "]");
    }
    this.ip = ip;
    this.server = null;
    return this;
  }

  @CmServerCommandMethod(name = "ipInternal")
  public CmServerBuilder ipInternal(String ipInternal) throws CmServerException {
    if (ipInternal == null || ipInternal.equals("")) {
      throw new CmServerException("Illegal IP argument passed [" + ipInternal + "]");
    }
    this.ipInternal = ipInternal;
    return this;
  }

  @CmServerCommandMethod(name = "port")
  public CmServerBuilder port(String port) throws CmServerException {
    if (port == null || port.equals("") || !StringUtils.isNumeric(port)) {
      throw new CmServerException("Illegal port argument passed [" + port + "]");
    }
    this.port = Integer.parseInt(port);
    this.server = null;
    return this;
  }

  @CmServerCommandMethod(name = "user")
  public CmServerBuilder user(String user) throws CmServerException {
    if (user == null || user.equals("")) {
      throw new CmServerException("Illegal user argument passed [" + user + "]");
    }
    this.user = user;
    this.server = null;
    return this;
  }

  @CmServerCommandMethod(name = "password")
  public CmServerBuilder password(String password) throws CmServerException {
    if (password == null || password.equals("")) {
      throw new CmServerException("Illegal password argument passed [" + password + "]");
    }
    this.password = password;
    this.server = null;
    return this;
  }

  @CmServerCommandMethod(name = "path")
  public CmServerBuilder path(String path) throws CmServerException {
    if (path == null) {
      throw new CmServerException("Illegal path argument passed [" + path + "]");
    }
    this.path = new File(path);
    return this;
  }

  @CmServerCommandMethod(name = "command")
  public CmServerBuilder command(String command) throws CmServerException {
    if (command == null || !COMMANDS.containsKey(command)) {
      throw new CmServerException("Illegal command argument passed [" + command + "]");
    }
    this.command = command;
    return this;
  }

  public CmServerBuilder cluster(CmServerCluster cluster) throws CmServerException {
    if (cluster == null) {
      throw new CmServerException("Illegal cluster argument passed [" + cluster + "]");
    }
    this.cluster = cluster;
    return this;
  }

  public CmServerBuilder logger(CmServerLog logger) throws CmServerException {
    if (logger == null) {
      throw new CmServerException("Illegal logger argument passed [" + logger + "]");
    }
    this.logger = logger;
    return this;
  }

  public void execute() throws CmServerException {
    executeObject();
  }

  public boolean executeBoolean() throws CmServerException {
    Object object = executeObject();
    if (object instanceof Boolean) {
      return ((Boolean) object).booleanValue();
    } else {
      return object != null;
    }
  }

  @SuppressWarnings("unchecked")
  public List<CmServerService> executeServices() throws CmServerException {
    Object object = executeObject();
    if (object instanceof List
        && (((List<?>) object).isEmpty() || ((List<?>) object).get(0) instanceof CmServerService)) {
      return (List<CmServerService>) object;
    } else {
      return Collections.emptyList();
    }
  }

  public CmServerCluster executeCluster() throws CmServerException {
    Object object = executeObject();
    if (object instanceof CmServerCluster) {
      return (CmServerCluster) object;
    } else {
      return new CmServerCluster();
    }
  }

  private Object executeObject() throws CmServerException {
    if (ip == null) {
      throw new CmServerException("Required paramater [ip] not set");
    }
    if (command == null) {
      throw new CmServerException("Required paramater [command] not set");
    }
    if (server == null) {
      server = factory.getCmServer(version, versionApi, versionCdh, ip, ipInternal, port, user, password,
          new CmServerLog.CmServerLogSysOut(LOG_TAG_CM_SERVER_API, false));
    }
    List<Object> paramaters = new ArrayList<Object>();
    for (Class<?> clazz : COMMANDS.get(command).getParameterTypes()) {
      if (clazz.equals(CmServerCluster.class)) {
        if (cluster == null) {
          throw new CmServerException("Required paramater [cluster] not set");
        }
        paramaters.add(cluster);
      } else if (clazz.equals(File.class)) {
        if (path == null) {
          throw new CmServerException("Required paramater [path] not set");
        }
        paramaters.add(path);
      } else {
        throw new CmServerException("Unexpected paramater type [" + clazz.getName() + "]");
      }
    }
    String label = WordUtils.capitalize(command);
    try {
      logger.logOperationStartedSync(label);
      Object commandReturn = COMMANDS.get(command).invoke(server, paramaters.toArray());
      logger.logOperationFinishedSync(label);
      return commandReturn;
    } catch (Exception exception) {
      logger.logOperationFailedSync(label, exception);
      throw new CmServerException("Unexpected runtime exception executing CM Server command", exception);
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