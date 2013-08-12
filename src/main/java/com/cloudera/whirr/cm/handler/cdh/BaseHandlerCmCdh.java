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
import java.util.HashSet;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import org.apache.whirr.ClusterSpec;
import org.apache.whirr.service.ClusterActionEvent;
import org.apache.whirr.service.ClusterActionHandler;

import com.cloudera.whirr.cm.CmServerClusterInstance;
import com.cloudera.whirr.cm.handler.BaseHandler;
import com.cloudera.whirr.cm.handler.CmAgentHandler;
import com.cloudera.whirr.cm.server.CmServerException;
import com.cloudera.whirr.cm.server.CmServerService;
import com.cloudera.whirr.cm.server.CmServerServiceBuilder;
import com.cloudera.whirr.cm.server.CmServerServiceType;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public abstract class BaseHandlerCmCdh extends BaseHandler {

  public abstract CmServerServiceType getType();

  @Override
  @SuppressWarnings("unchecked")
  public Set<String> getPortsClient(ClusterActionEvent event) throws IOException {
    return new HashSet<String>(CmServerClusterInstance.getConfiguration(event.getClusterSpec()).getList(
        getRole() + CONFIG_WHIRR_INTERNAL_PORTS_CLIENT_SUFFIX));
  }

  public boolean isDatabaseDependent() {
    return false;
  }

  @Override
  protected void beforeBootstrap(ClusterActionEvent event) throws IOException, InterruptedException {
    super.beforeBootstrap(event);
    try {
      if (!event.getInstanceTemplate().getRoles().contains(CmAgentHandler.ROLE)) {
        throw new CmServerException("Role [" + getRole() + "] requires colocated role [" + CmAgentHandler.ROLE + "]");
      }
      CmServerClusterInstance.getCluster(event.getClusterSpec()).addService(
          new CmServerServiceBuilder()
              .type(getType())
              .tag(
                  CmServerClusterInstance.getConfiguration(event.getClusterSpec()).getString(
                      ClusterSpec.Property.CLUSTER_NAME.getConfigName(), CONFIG_WHIRR_NAME_DEFAULT)).build());
    } catch (CmServerException e) {
      throw new IOException("Unexpected error building cluster", e);
    }
    if (isDatabaseDependent()) {
      addStatement(
          event,
          call("install_database", "-t", CmServerClusterInstance.getClusterConfiguration(event.getClusterSpec(),
              CmServerClusterInstance.getMounts(event.getClusterSpec(), event.getCluster()), getType().getId(),
              getType().getParent() == null ? null : getType().getParent().getId(), CONFIG_CM_DB_SUFFIX_TYPE), "-d",
              CmServerClusterInstance.getClusterConfiguration(event.getClusterSpec(), CmServerClusterInstance.getMounts(event.getClusterSpec(), event.getCluster()), getType()
                  .getId(), getType().getParent() == null ? null : getType().getParent().getId(), "database_name")));
    }
    if (CmServerClusterInstance.getConfiguration(event.getClusterSpec()).getBoolean(CONFIG_WHIRR_USE_PACKAGES, false)) {
      addStatement(event, call("register_cdh_repo"));
      addStatement(event, call("install_cdh_packages"));
    }
  }

  @Override
  protected void afterBootstrap(ClusterActionEvent event) throws IOException, InterruptedException {
    super.afterBootstrap(event);
    try {
      if (CmServerClusterInstance.getCluster(event.getClusterSpec()).getServer() == null) {
        throw new CmServerException("Role [" + getRole() + "] requires cluster to have role [" + CmAgentHandler.ROLE
            + "]");
      }
      if (CmServerClusterInstance.getCluster(event.getClusterSpec()).isEmpty()) {
        throw new CmServerException("Cluster is not consistent");
      }
    } catch (CmServerException e) {
      throw new IOException("Unexpected error building cluster", e);
    }
  }

  @Override
  protected void beforeConfigure(ClusterActionEvent event) throws IOException, InterruptedException {
    super.beforeConfigure(event);
    addStatement(
        event,
        call("configure_cm_cdh", "-r", getRole(), "-d",
            Joiner.on(',').join(Lists.transform(Lists.newArrayList(CmServerClusterInstance.getMounts(event.getClusterSpec(), event.getCluster())), new Function<String, String>() {
              @Override
              public String apply(String input) {
                return input;
              }
            }))));
  }

  @Override
  protected void beforeStart(ClusterActionEvent event) throws IOException, InterruptedException {
    super.beforeStart(event);
    try {
      CmServerClusterInstance.getCluster(event.getClusterSpec()).addService(
          new CmServerServiceBuilder()
              .type(getType())
              .tag(
                  CmServerClusterInstance.getConfiguration(event.getClusterSpec()).getString(
                      ClusterSpec.Property.CLUSTER_NAME.getConfigName(), CONFIG_WHIRR_NAME_DEFAULT))
              .status(CmServerService.CmServerServiceStatus.STARTING).build());
    } catch (CmServerException e) {
      throw new IOException("Unexpected error building cluster", e);
    }
  }

  @Override
  protected void beforeStop(ClusterActionEvent event) throws IOException, InterruptedException {
    super.beforeStop(event);
    try {
      CmServerClusterInstance.getCluster(event.getClusterSpec()).addService(
          new CmServerServiceBuilder()
              .type(getType())
              .tag(
                  CmServerClusterInstance.getConfiguration(event.getClusterSpec()).getString(
                      ClusterSpec.Property.CLUSTER_NAME.getConfigName(), CONFIG_WHIRR_NAME_DEFAULT))
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
