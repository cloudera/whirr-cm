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
package com.cloudera.whirr.cm;

import com.cloudera.whirr.cm.handler.CmServerHandler;
import com.cloudera.whirr.cm.server.CmServerConstants;

public interface CmConstants extends CmServerConstants {

  public static final String PROPERTIES_FILE = "whirr-cm-default.properties";

  public static final String CM_USER = "admin";
  public static final String CM_PASSWORD = "admin";

  public static final String CM_LICENSE_FILE = "cm-license.txt";
  
  public static final String CONFIG_WHIRR_AUTO = "whirr.cm.auto";
  public static final String CONFIG_WHIRR_USE_PACKAGES = "whirr.cm.use.packages";
  public static final String CONFIG_WHIRR_DATA_DIRS_ROOT = "whirr.cm.data.dirs.root";
  public static final String CONFIG_WHIRR_FIREWALL_ENABLE = "whirr.cm.firewall.enable";
  public static final String CONFIG_WHIRR_DB_TYPE = "whirr.cm.db.type";
  public static final String CONFIG_WHIRR_CM_CONFIG_PREFIX = "whirr.cm.config.";

  public static final String CONFIG_WHIRR_INTERNAL_AGENT_LOG_FILE = "whirr.cm.agent.log.file";
  public static final String CONFIG_WHIRR_INTERNAL_DATA_DIRS_DEFAULT = "whirr.cm.data.dirs.root.default";
  public static final String CONFIG_WHIRR_INTERNAL_CM_CONFIG_DEFAULT_PREFIX = "whirr.cm.default.config.";
  public static final String CONFIG_WHIRR_INTERNAL_PORTS_DB_PREFIX = "database.port.";
  public static final String CONFIG_WHIRR_INTERNAL_PORTS_CLIENT_SUFFIX = ".client.ports";
  public static final String CONFIG_WHIRR_INTERNAL_PORT_WEB = CmServerHandler.ROLE + ".port.web";
  public static final String CONFIG_WHIRR_INTERNAL_PORT_COMMS = CmServerHandler.ROLE + ".port.comms";

  public static final String CONFIG_WHIRR_NAME_DEFAULT = "whirr";

  public static final String CONFIG_CM_DIR_SUFFIX = "dir";
  public static final String CONFIG_CM_DIR_SUFFIX_LIST = CONFIG_CM_DIR_SUFFIX + "_list";

  public static final String CONFIG_CM_DB_SUFFIX_TYPE = "database_type";
  public static final String CONFIG_CM_DB_SUFFIX_HOST = "database_host";
  public static final String CONFIG_CM_DB_SUFFIX_PORT = "database_port";
  public static final String CONFIG_CM_DB_SUFFIX_NAME = "database_name";

  public static final String CONFIG_CM_LICENSE_PROVIDED = "license_provided";
  public static final String CONFIG_CM_TASKTRACKER_INSTRUMENTATION = "mapred_tasktracker_instrumentation";

  public static final String LOG_TAG_WHIRR_HANDLER = "Whirr Handler";
  public static final String LOG_TAG_WHIRR_COMMAND = "Whirr Command";

}
