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

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.apache.whirr.Cluster.Instance;
import org.apache.whirr.ClusterSpec;

import com.cloudera.whirr.cm.handler.CmAgentHandler;
import com.cloudera.whirr.cm.handler.CmNodeHandler;
import com.cloudera.whirr.cm.handler.CmServerHandler;
import com.cloudera.whirr.cm.handler.cdh.BaseHandlerCmCdh;
import com.cloudera.whirr.cm.server.CmServerCluster;
import com.cloudera.whirr.cm.server.CmServerException;
import com.cloudera.whirr.cm.server.CmServerService;
import com.cloudera.whirr.cm.server.CmServerServiceBuilder;
import com.cloudera.whirr.cm.server.CmServerServiceType;
import com.cloudera.whirr.cm.server.impl.CmServerFactory;
import com.cloudera.whirr.cm.server.impl.CmServerLog;

public class CmServerClusterInstance implements CmConstants {

  private static CmServerFactory factory;
  private static CmServerCluster cluster;

  private CmServerClusterInstance() {
  }

  public static synchronized CmServerFactory getFactory() {
    return factory == null ? (factory = new CmServerFactory()) : factory;
  }

  public static synchronized CmServerFactory getFactory(CmServerFactory factory) {
    return CmServerClusterInstance.factory = factory;
  }

  public static synchronized CmServerCluster getCluster() {
    return getCluster(false);
  }

  public static synchronized CmServerCluster getCluster(boolean clear) {
    return clear ? (cluster = new CmServerCluster()) : (cluster == null ? (cluster = new CmServerCluster()) : cluster);
  }

  public static synchronized CmServerCluster getCluster(ClusterSpec specification, Set<Instance> instances)
      throws CmServerException, IOException {
    return getCluster(specification, instances, Collections.<String> emptySet(), Collections.<String> emptySet());
  }

  public static synchronized CmServerCluster getCluster(ClusterSpec specification, Set<Instance> instances,
      Set<String> mounts) throws CmServerException, IOException {
    return getCluster(specification, instances, mounts, Collections.<String> emptySet());
  }

  public static synchronized CmServerCluster getCluster(ClusterSpec specification, Set<Instance> instances,
      Set<String> mounts, Set<String> roles) throws CmServerException, IOException {
    cluster = new CmServerCluster();
    cluster.setIsParcel(!specification.getConfiguration().getBoolean(CONFIG_WHIRR_USE_PACKAGES, false));
    cluster.setMounts(mounts);
    for (Instance instance : instances) {
      for (String role : instance.getRoles()) {
        if (role.equals(CmServerHandler.ROLE)) {
          cluster.setServer(instance.getPublicIp());
        } else if (role.equals(CmAgentHandler.ROLE)) {
          cluster.addAgent(instance.getPublicIp());
        } else if (role.equals(CmNodeHandler.ROLE)) {
          cluster.addNode(instance.getPublicIp());
        } else {
          CmServerServiceType type = BaseHandlerCmCdh.getRolesToType().get(role);
          if (type != null && (roles == null || roles.isEmpty() || roles.contains(role))) {
            cluster.addService(getClusterService(specification, instance, type));
          }
        }
      }
    }
    return cluster;
  }

  public static CmServerService getClusterService(ClusterSpec specification, Instance instance, CmServerServiceType type)
      throws CmServerException, IOException {
    return new CmServerServiceBuilder().type(type)
        .tag(specification.getConfiguration().getString(CONFIG_WHIRR_NAME, CONFIG_WHIRR_NAME_DEFAULT))
        .qualifier("" + (cluster.getServices(type).size() + 1)).host(instance.getPublicHostName())
        .ip(instance.getPublicIp()).ipInternal(instance.getPrivateIp()).build();
  }

  public static CmServerCluster getCluster(CmServerCluster cluster) throws CmServerException {
    CmServerCluster clusterTo = new CmServerCluster();
    clusterTo.setServer(cluster.getServer());
    for (String agent : cluster.getAgents()) {
      clusterTo.addAgent(agent);
    }
    for (String node : cluster.getNodes()) {
      clusterTo.addAgent(node);
    }
    return clusterTo;
  }

  public static boolean logCluster(CmServerLog logger, String label, ClusterSpec specification, CmServerCluster cluster) {
    if (cluster.getServiceTypes(CmServerServiceType.CLUSTER).isEmpty()) {
      logger.logOperationInProgressSync(label, "NO CDH SERVICES");
    } else {
      for (CmServerServiceType type : cluster.getServiceTypes()) {
        logger.logOperationInProgressSync(label, "CDH " + type.toString() + " SERVICE");
        for (CmServerService service : cluster.getServices(type)) {
          logger.logOperationInProgressSync(label,
              "  " + service.getName() + "@" + service.getIp() + "=" + service.getStatus());
        }
      }
    }
    if (!cluster.getAgents().isEmpty()) {
      logger.logOperationInProgressSync(label, "CM AGENTS");
    }
    for (String cmAgent : cluster.getAgents()) {
      logger.logOperationInProgressSync(label, "  ssh -o StrictHostKeyChecking=no " + specification.getClusterUser()
          + "@" + cmAgent);
    }
    if (!cluster.getNodes().isEmpty()) {
      logger.logOperationInProgressSync(label, "CM NODES");
    }
    for (String cmNode : cluster.getNodes()) {
      logger.logOperationInProgressSync(label, "  ssh -o StrictHostKeyChecking=no " + specification.getClusterUser()
          + "@" + cmNode);
    }
    logger.logOperationInProgressSync(label, "CM SERVER");
    if (cluster.getServer() != null) {
      logger.logOperationInProgressSync(label, "  http://" + cluster.getServer() + ":7180");
      logger.logOperationInProgressSync(label, "  ssh -o StrictHostKeyChecking=no " + specification.getClusterUser()
          + "@" + cluster.getServer());
    } else {
      logger.logOperationInProgressSync(label, "NO CM SERVER");
    }
    return !cluster.isEmpty();
  }

  public static void logHeader(CmServerLog logger, String operation) {
    logger.logSpacer();
    logger.logSpacerDashed();
    logger.logOperation(operation, "");
    logger.logSpacerDashed();
  }

  public static void logLineItem(CmServerLog logger, String operation) {
    logger.logSpacer();
    logger.logOperationStartedSync(operation);
  }

  public static void logLineItem(CmServerLog logger, String operation, String detail) {
    logger.logSpacer();
    logger.logOperationInProgressSync(operation, detail);
  }

  public static void logLineItemDetail(CmServerLog logger, String operation, String detail) {
    logger.logOperationInProgressSync(operation, detail);
  }

  public static void logLineItemFooter(CmServerLog logger, String operation) {
    logger.logOperationFinishedSync(operation);
  }

  public static void logLineItemFooterFinal(CmServerLog logger) {
    logger.logSpacer();
    logger.logSpacerDashed();
    logger.logSpacer();
  }

  public static void logException(CmServerLog logger, String operation, String message, Throwable throwable) {
    logger.logOperationInProgressSync(operation, "Failed");
    logger.logOperationStackTrace(operation, throwable);
    logger.logSpacer();
    logger.logOperation(operation, message);
  }

}