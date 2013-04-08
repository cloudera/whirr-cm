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
package com.cloudera.whirr.cm.handler.cdh;

import static org.jclouds.scriptbuilder.domain.Statements.call;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import org.apache.whirr.service.ClusterActionEvent;
import org.apache.whirr.service.ClusterActionHandler;

import com.cloudera.whirr.cm.CmServerClusterInstance;
import com.cloudera.whirr.cm.handler.BaseHandler;
import com.cloudera.whirr.cm.handler.CmAgentHandler;
import com.cloudera.whirr.cm.server.CmServerException;
import com.cloudera.whirr.cm.server.CmServerService;
import com.cloudera.whirr.cm.server.CmServerServiceBuilder;
import com.cloudera.whirr.cm.server.CmServerServiceType;

public abstract class BaseHandlerCmCdh extends BaseHandler {

  public abstract CmServerServiceType getType();

  @Override
  protected void beforeBootstrap(ClusterActionEvent event) throws IOException, InterruptedException {
    super.beforeBootstrap(event);
    try {
      if (!event.getInstanceTemplate().getRoles().contains(CmAgentHandler.ROLE)) {
        throw new CmServerException("Role [" + getRole() + "] requires colocated role [" + CmAgentHandler.ROLE + "]");
      }
      CmServerClusterInstance.getCluster().addService(
          new CmServerServiceBuilder().type(getType())
              .tag(event.getClusterSpec().getConfiguration().getString(CONFIG_WHIRR_NAME, CONFIG_WHIRR_NAME_DEFAULT))
              .build());
    } catch (CmServerException e) {
      throw new IOException("Unexpected error building cluster", e);
    }
    if (event.getClusterSpec().getConfiguration().getBoolean(CONFIG_WHIRR_USE_PACKAGES, false)) {
      addStatement(event, call("register_cdh_repo"));
      addStatement(event, call("install_cdh_packages"));
    }
  }

  @Override
  protected void afterBootstrap(ClusterActionEvent event) throws IOException, InterruptedException {
    super.afterBootstrap(event);
    try {
      if (CmServerClusterInstance.getCluster().getServer() == null) {
        throw new CmServerException("Role [" + getRole() + "] requires cluster to have role [" + CmAgentHandler.ROLE
            + "]");
      }
      if (CmServerClusterInstance.getCluster().isEmpty()) {
        throw new CmServerException("Cluster is not consistent");
      }
    } catch (CmServerException e) {
      throw new IOException("Unexpected error building cluster", e);
    }
  }

  @Override
  protected void beforeStart(ClusterActionEvent event) throws IOException, InterruptedException {
    super.beforeStart(event);
    try {
      CmServerClusterInstance.getCluster().addService(
          new CmServerServiceBuilder().type(getType())
              .tag(event.getClusterSpec().getConfiguration().getString(CONFIG_WHIRR_NAME, CONFIG_WHIRR_NAME_DEFAULT))
              .status(CmServerService.CmServerServiceStatus.STARTING).build());
    } catch (CmServerException e) {
      throw new IOException("Unexpected error building cluster", e);
    }
  }

  @Override
  protected void beforeStop(ClusterActionEvent event) throws IOException, InterruptedException {
    super.beforeStop(event);
    try {
      CmServerClusterInstance.getCluster().addService(
          new CmServerServiceBuilder().type(getType())
              .tag(event.getClusterSpec().getConfiguration().getString(CONFIG_WHIRR_NAME, CONFIG_WHIRR_NAME_DEFAULT))
              .status(CmServerService.CmServerServiceStatus.STOPPING).build());
    } catch (CmServerException e) {
      throw new IOException("Unexpected error building cluster", e);
    }
  }

  public static Map<String, CmServerServiceType> getRolesToType() {
    Map<String, CmServerServiceType> roleToType = new HashMap<String, CmServerServiceType>();
    // It is OK to do this every time, since ServiceLoader creates a cache, which must be weakly
    // referenced since I had issues with this when caching this under memory pressure (eg maven tests)
    for (ClusterActionHandler handler : ServiceLoader.load(ClusterActionHandler.class)) {
      if (handler instanceof BaseHandlerCmCdh) {
        roleToType.put(handler.getRole(), ((BaseHandlerCmCdh) handler).getType());
      }
    }
    return roleToType;
  }

}
