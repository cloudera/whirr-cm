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
package com.cloudera.whirr.cm.handler;

import static org.jclouds.scriptbuilder.domain.Statements.call;

import java.io.IOException;

import org.apache.whirr.ClusterSpec;
import org.apache.whirr.service.ClusterActionEvent;
import org.apache.whirr.service.hadoop.VolumeManager;

import com.cloudera.whirr.cm.CmServerClusterInstance;
import com.cloudera.whirr.cm.server.impl.CmServerLog;

public abstract class BaseHandlerCm extends BaseHandler {

  protected static final CmServerLog logger = new CmServerLog.CmServerLogSysOut(LOG_TAG_WHIRR_HANDLER, false);

  protected String getInstanceId(ClusterSpec spec) {
    return getRole() + "-instance-id";
  }

  @Override
  protected void beforeBootstrap(ClusterActionEvent event) throws IOException, InterruptedException {
    super.beforeBootstrap(event);
    addStatement(event, call("configure_hostnames"));
    addStatement(event, call("retry_helpers"));
  }

  @Override
  protected void beforeConfigure(ClusterActionEvent event) throws IOException, InterruptedException {
    super.beforeConfigure(event);
    addStatement(event, call("retry_helpers"));
    if (CmServerClusterInstance.getConfiguration(event.getClusterSpec()).getList(CONFIG_WHIRR_DATA_DIRS_ROOT).isEmpty()) {
      addStatement(event, call("prepare_all_disks", "'" + VolumeManager.asString(getDeviceMappings(event)) + "'"));
    }
  }

}
