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
package com.cloudera.whirr.cm.cmd;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.apache.whirr.Cluster.Instance;
import org.apache.whirr.ClusterController;
import org.apache.whirr.ClusterControllerFactory;
import org.apache.whirr.ClusterSpec;
import org.apache.whirr.state.ClusterStateStoreFactory;

import com.cloudera.whirr.cm.CmServerClusterInstance;
import com.cloudera.whirr.cm.handler.CmAgentHandler;
import com.cloudera.whirr.cm.handler.CmNodeHandler;
import com.cloudera.whirr.cm.handler.CmServerHandler;
import com.cloudera.whirr.cm.handler.cdh.BaseHandlerCmCdh;
import com.cloudera.whirr.cm.server.CmServerBuilder;
import com.cloudera.whirr.cm.server.CmServerCluster;
import com.cloudera.whirr.cm.server.CmServerException;
import com.cloudera.whirr.cm.server.CmServerServiceType;
import com.google.common.base.Splitter;

public abstract class BaseCommandCmServer extends BaseCommand {

  public static final String OPTION_ROLES = "roles";
  public static final String OPTION_CLUSTER_NAME = "cm-cluster-name";

  protected OptionSpec<String> OPTIONSPEC_ROLES = isRoleFilterable() ? parser
      .accepts("roles", "Cluster roles to target").withRequiredArg().ofType(String.class) : null;

  protected OptionSpec<String> OPTIONSPEC_CLUSTER_NAME = parser.accepts("cm-cluster-name", "CM cluster name to target")
      .withRequiredArg().ofType(String.class);

  public BaseCommandCmServer(String name, String description, ClusterControllerFactory factory,
      ClusterStateStoreFactory stateStoreFactory) {
    super(name, description, factory, stateStoreFactory);
  }

  public BaseCommandCmServer(String name, String description, ClusterControllerFactory factory) {
    super(name, description, factory);
  }

  public boolean isRoleFilterable() {
    return false;
  }

  @Override
  public String getLabel() {
    return "CM" + super.getLabel();
  }

  public abstract int run(ClusterSpec specification, Set<Instance> instances, CmServerCluster cluster,
      CmServerBuilder serverCommand) throws Exception;

  @Override
  public int run(ClusterSpec specification, ClusterController clusterController, OptionSet optionSet) throws Exception {

    CmServerClusterInstance.logHeader(logger, getLabel());
    CmServerClusterInstance.logLineItem(logger, getLabel());

    Set<String> roles = new HashSet<String>();
    if (isRoleFilterable() && optionSet.hasArgument(OPTION_ROLES)) {
      if ((roles = filterRoles((String) optionSet.valueOf(OPTION_ROLES))).isEmpty()) {
        throw new CmServerException("Role filter does not include any appropriate roles.");
      }
    }

    Set<Instance> instances = clusterController.getInstances(specification, createClusterStateStore(specification));
    CmServerCluster cluster = CmServerClusterInstance.getCluster(specification, specification.getConfiguration(),
        instances, CmServerClusterInstance.getMounts(specification, instances), roles);

    if (optionSet.hasArgument(OPTION_CLUSTER_NAME)) {
      cluster.setName((String) optionSet.valueOf(OPTION_CLUSTER_NAME));
    }

    if (cluster.getServer() == null) {
      throw new CmServerException("Could not find " + CmServerHandler.ROLE + ".");
    }
    if (cluster.getAgents().isEmpty() && cluster.getNodes().isEmpty()) {
      throw new CmServerException("Could not find any " + CmAgentHandler.ROLE + "'s or " + CmNodeHandler.ROLE + "'s.");
    }

    CmServerBuilder command = new CmServerBuilder()
        .version(CmServerClusterInstance.getVersion(CmServerClusterInstance.getConfiguration(specification)))
        .versionApi(CmServerClusterInstance.getVersionApi(CmServerClusterInstance.getConfiguration(specification)))
        .versionCdh(CmServerClusterInstance.getVersionCdh(CmServerClusterInstance.getConfiguration(specification)))
        .ip(cluster.getServer().getIp()).ipInternal(cluster.getServer().getIpInternal()).cluster(cluster)
        .path(specification.getClusterDirectory().getAbsolutePath());

    int returnInt = run(specification, instances, cluster, command);

    CmServerClusterInstance.logLineItemFooter(logger, getLabel());
    CmServerClusterInstance.logLineItemFooterFinal(logger);

    return returnInt;
  }

  @Override
  public void printUsage(PrintStream stream) throws IOException {
    stream.println("Usage: whirr " + getName() + " [OPTIONS] [--cm-cluster-name \"My cluster\"]"
        + (isRoleFilterable() ? " [--roles role1,role2]" : ""));
    stream.println();
    parser.printHelpOn(stream);
  }

  public static Set<String> filterRoles(String rolesCsv) {
    Set<String> roles = new HashSet<String>();
    Map<String, CmServerServiceType> rolesToType = BaseHandlerCmCdh.getRolesToType();
    for (String role : Splitter.on(",").split(rolesCsv)) {
      if (rolesToType.containsKey(role)) {
        roles.add(role);
      }
    }
    if (!roles.isEmpty()) {
      roles.add(CmServerHandler.ROLE);
    }
    return roles;
  }
}