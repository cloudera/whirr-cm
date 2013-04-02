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

import static org.apache.whirr.RolePredicates.role;
import static org.jclouds.scriptbuilder.domain.Statements.call;

import java.io.IOException;

import org.apache.whirr.Cluster.Instance;
import org.apache.whirr.service.ClusterActionEvent;

public class CmAgentHandler extends CmNodeHandler {

  public static final String ROLE = "cm-agent";

  @Override
  public String getRole() {
    return ROLE;
  }

  @Override
  protected void beforeBootstrap(ClusterActionEvent event) throws IOException, InterruptedException {
    super.beforeBootstrap(event);
    addStatement(event, call("install_cm_agent"));
  }

  @Override
  protected void beforeConfigure(ClusterActionEvent event) throws IOException, InterruptedException {
    super.beforeConfigure(event);
    Instance cmServerInstance = null;
    try {
      cmServerInstance = event.getCluster().getInstanceMatching(role(CmServerHandler.ROLE));
    } catch (Exception exception) {
    }
    if (cmServerInstance != null) {
      addStatement(
          event,
          call("configure_cm_agent", "-h", event.getCluster().getInstanceMatching(role(CmServerHandler.ROLE))
              .getPublicIp(), "-p",
              getConfiguration(event.getClusterSpec()).getString(CmServerHandler.PROPERTY_PORT_COMMS)));
    } else {
      logLineItem("Warning, not configuring agent, because failed to find CM server");
    }
  }

}
