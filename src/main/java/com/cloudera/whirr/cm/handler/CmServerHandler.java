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
import static org.jclouds.scriptbuilder.domain.Statements.createOrOverwriteFile;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.whirr.Cluster.Instance;
import org.apache.whirr.service.ClusterActionEvent;

import com.cloudera.whirr.cm.CmConstants;
import com.cloudera.whirr.cm.CmServerClusterInstance;
import com.cloudera.whirr.cm.Utils;
import com.cloudera.whirr.cm.server.CmServer;
import com.cloudera.whirr.cm.server.CmServerCluster;
import com.cloudera.whirr.cm.server.CmServerException;
import com.cloudera.whirr.cm.server.CmServerService;
import com.cloudera.whirr.cm.server.CmServerService.CmServerServiceStatus;
import com.cloudera.whirr.cm.server.CmServerServiceBuilder;
import com.cloudera.whirr.cm.server.CmServerServiceType;
import com.cloudera.whirr.cm.server.CmServerServiceTypeCms;
import com.cloudera.whirr.cm.server.impl.CmServerLog;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.CharStreams;
import com.google.common.io.Resources;

public class CmServerHandler extends BaseHandlerCm {

  public static final String ROLE = "cm-server";

  @Override
  public String getRole() {
    return ROLE;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Set<String> getPortsClient(ClusterActionEvent event) throws IOException {
    Set<String> ports = new HashSet<String>(CmServerClusterInstance.getConfiguration(event.getClusterSpec()).getList(
        ROLE + CONFIG_WHIRR_INTERNAL_PORTS_CLIENT_SUFFIX));
    ports.add(CmServerClusterInstance.getConfiguration(event.getClusterSpec())
        .getString(CONFIG_WHIRR_INTERNAL_PORT_WEB));
    return ports;
  }

  @Override
  protected void beforeBootstrap(ClusterActionEvent event) throws IOException, InterruptedException {
    CmServerClusterInstance.logHeader(logger, "HostBootstrap");
    CmServerClusterInstance.logLineItemAsync(logger, "HostBootstrapInit");
    super.beforeBootstrap(event);
    try {
      CmServerClusterInstance.setIsStandaloneCommand(false);
      CmServerClusterInstance.getCluster(event.getClusterSpec()).setServer(
          new CmServerServiceBuilder().ip(getInstanceId(event.getClusterSpec())).build());
    } catch (CmServerException e) {
      throw new IOException("Unexpected error building cluster", e);
    }
    for (CmServerServiceTypeCms type : CmServerServiceTypeCms.values()) {
      switch (type) {
      case HOSTMONITOR:
      case SERVICEMONITOR:
      case ACTIVITYMONITOR:
      case REPORTSMANAGER:
      case NAVIGATOR:
        addStatement(
            event,
            call(
                "install_database",
                "-t",
                CmServerClusterInstance.getClusterConfiguration(event.getClusterSpec(),
                    CmServerClusterInstance.getMounts(event.getClusterSpec(), event.getCluster()), type.getId(),
                    type.getParent() == null ? null : type.getParent().getId(), CONFIG_CM_DB_SUFFIX_TYPE),
                "-d",
                CmServerClusterInstance.getClusterConfiguration(event.getClusterSpec(),
                    CmServerClusterInstance.getMounts(event.getClusterSpec(), event.getCluster()), type.getId(),
                    type.getParent() == null ? null : type.getParent().getId(), "database_name")));
        break;
      default:
        break;
      }
    }
    addStatement(event, call("install_cm"));
    addStatement(event, call("install_cm_server"));
    CmServerClusterInstance.logLineItemFooterAsync(logger, "HostBootstrapInit");
    CmServerClusterInstance.logLineItemAsync(logger, "HostBootstrapExecute");
  }

  @Override
  protected void afterBootstrap(ClusterActionEvent event) throws IOException, InterruptedException {
    super.afterBootstrap(event);
    CmServerClusterInstance.logLineItemFooterAsync(logger, "HostBootstrapExecute");
  }

  @Override
  protected void beforeConfigure(ClusterActionEvent event) throws IOException, InterruptedException {
    CmServerClusterInstance.logHeader(logger, "HostConfigure");
    CmServerClusterInstance.logLineItemAsync(logger, "HostConfigureInit");
    super.beforeConfigure(event);
    URL licenceConfigUri = null;
    if ((licenceConfigUri = Utils.urlForURI(CmServerClusterInstance.getConfiguration(event.getClusterSpec()).getString(
        CONFIG_WHIRR_CM_LICENSE_URI))) != null) {
      addStatement(
          event,
          createOrOverwriteFile(
              "/tmp/" + CM_LICENSE_FILE,
              Splitter.on('\n').split(
                  CharStreams.toString(Resources.newReaderSupplier(licenceConfigUri, Charsets.UTF_8)))));
    }
    addStatement(
        event,
        call("configure_cm_server", "-t", CmServerClusterInstance.getClusterConfiguration(event.getClusterSpec(),
            CmServerClusterInstance.getMounts(event.getClusterSpec(), event.getCluster()),
            CmServerServiceTypeCms.CM.getId(), null, CONFIG_CM_DB_SUFFIX_TYPE)));
    CmServerClusterInstance.logLineItemFooterAsync(logger, "HostConfigureInit");
    CmServerClusterInstance.logLineItemAsync(logger, "HostConfigureExecute");
  }

  @Override
  protected void afterConfigure(ClusterActionEvent event) throws IOException, InterruptedException {
    super.afterConfigure(event);
    CmServerClusterInstance.logLineItemFooterAsync(logger, "HostConfigureExecute");
    executeServer("CMClusterProvision", event, null, new ServerCommand() {
      @Override
      public CmServerCluster execute(ClusterActionEvent event, CmServer server, CmServerCluster clusterInput)
          throws Exception {
        server.initialise(clusterInput);
        return clusterInput;
      }
    }, false, true);
    executeServer("CMClusterConfigure", event, null, new ServerCommand() {
      @Override
      public CmServerCluster execute(ClusterActionEvent event, CmServer server, CmServerCluster clusterInput)
          throws Exception {
        boolean success = false;
        if (server.provision(clusterInput)) {
          if (server.configure(clusterInput)) {
            if (server.getServiceConfigs(clusterInput, event.getClusterSpec().getClusterDirectory())) {
              success = true;
            }
          }
        }
        if (!success) {
          throw new CmServerException("Unexepcted error attempting to configure cluster");
        }
        return clusterInput;
      }
    }, false, false);
  }

  @Override
  protected void afterStart(ClusterActionEvent event) throws IOException, InterruptedException {
    super.afterStart(event);
    executeServer("CMClusterStarting", event, CmServerServiceStatus.STARTING, new ServerCommand() {
      @Override
      public CmServerCluster execute(ClusterActionEvent event, CmServer server, CmServerCluster clusterInput)
          throws Exception {
        CmServerCluster clusterOutput = new CmServerCluster();
        server.start(clusterInput);
        if (server.isStarted(clusterInput)) {
          if ((clusterOutput = server.getServices(clusterInput)).isEmpty()) {
            throw new CmServerException("Unexpected error, empty cluster returned");
          }
        } else {
          throw new CmServerException("Unexpected error starting cluster, not correctly provisioned");
        }
        return clusterOutput;
      }
    }, true, false);
  }

  @Override
  protected void afterStop(ClusterActionEvent event) throws IOException, InterruptedException {
    super.afterStop(event);
    executeServer("CMClusterStopping", event, CmServerServiceStatus.STOPPING, new ServerCommand() {
      @Override
      public CmServerCluster execute(ClusterActionEvent event, CmServer server, CmServerCluster clusterInput)
          throws Exception {
        CmServerCluster clusterOutput = new CmServerCluster();
        if (server.isConfigured(clusterInput)) {
          server.stop(clusterInput);
          if ((clusterOutput = server.getServices(clusterInput)).isEmpty()) {
            throw new CmServerException("Unexpected error, empty cluster returned");
          }
        } else {
          throw new CmServerException("Unexpected error stopping cluster, not correctly provisioned");
        }
        return clusterOutput;
      }
    }, true, false);
  }

  private void executeServer(String operation, ClusterActionEvent event, CmServerServiceStatus status,
      ServerCommand command, boolean footer, boolean alwaysExecute) throws IOException, InterruptedException {
    super.afterStart(event);
    try {
      CmServerClusterInstance.logHeader(logger, operation);
      CmServerCluster cluster = getCluster(event, status);
      if (!cluster.isEmpty()) {
        if (!alwaysExecute && !CmServerClusterInstance.isStandaloneCommand()
            && !CmServerClusterInstance.getConfiguration(event.getClusterSpec()).getBoolean(CONFIG_WHIRR_AUTO, true)) {
          CmServerClusterInstance.logLineItem(logger, operation, "Warning, services found, but whirr property");
          CmServerClusterInstance.logLineItemDetail(logger, operation, "[" + CONFIG_WHIRR_AUTO
              + "] is false so not executing");
        } else {
          CmServerClusterInstance.logLineItem(
              logger,
              operation,
              "follow live at http://"
                  + event.getCluster().getInstanceMatching(role(ROLE)).getPublicHostName()
                  + ":"
                  + CmServerClusterInstance.getConfiguration(event.getClusterSpec()).getString(
                      CmConstants.CONFIG_WHIRR_INTERNAL_PORT_WEB));
          CmServerClusterInstance.logLineItem(logger, operation, "");
          CmServerClusterInstance.logLineItem(logger, operation);
          Instance serverInstance = event.getCluster().getInstanceMatching(role(ROLE));

          try {
            cluster = command.execute(
                event,
                CmServerClusterInstance.getFactory()
                    .getCmServer(
                        CmServerClusterInstance.getVersion(CmServerClusterInstance.getConfiguration(event
                            .getClusterSpec())),
                        CmServerClusterInstance.getVersionApi(CmServerClusterInstance.getConfiguration(event
                            .getClusterSpec())),
                        CmServerClusterInstance.getVersionCdh(CmServerClusterInstance.getConfiguration(event
                            .getClusterSpec())),
                        serverInstance.getPublicIp(),
                        serverInstance.getPrivateIp(),
                        CmServerClusterInstance.getConfiguration(event.getClusterSpec()).getInt(
                            CONFIG_WHIRR_INTERNAL_PORT_WEB), CM_USER, CM_PASSWORD,
                        new CmServerLog.CmServerLogSysOut(LOG_TAG_CM_SERVER_API, false)), cluster);
          } finally {
            CmServerClusterInstance.logLineItemFooter(logger, operation);
            CmServerClusterInstance.logLineItem(logger, operation, "");
            CmServerClusterInstance.logLineItem(logger, operation);
            CmServerClusterInstance.logCluster(logger, operation, CmServerClusterInstance.getConfiguration(event
                .getClusterSpec()), cluster, event.getCluster().getInstances());
            CmServerClusterInstance.logLineItemFooter(logger, operation);
          }
        }
      }
    } catch (Exception e) {
      CmServerClusterInstance.logException(logger, operation,
          "Failed to execute (see above), log into the web console to resolve", e);
    } finally {
      CmServerClusterInstance.getCluster(event.getClusterSpec(), true);
    }
    if (footer) {
      CmServerClusterInstance.logLineItemFooterFinal(logger);
    }
  }

  private CmServerCluster getCluster(ClusterActionEvent event, CmServerServiceStatus status) throws CmServerException,
      IOException, ConfigurationException {
    CmServerCluster clusterStale = CmServerClusterInstance.getCluster(event.getClusterSpec());
    CmServerCluster cluster, clusterCurrent = cluster = CmServerClusterInstance.getCluster(event.getClusterSpec(),
        CmServerClusterInstance.getConfiguration(event.getClusterSpec()), event.getCluster().getInstances(),
        CmServerClusterInstance.getMounts(event.getClusterSpec(), event.getCluster()));
    if (status != null) {
      CmServerCluster clusterFiltered = CmServerClusterInstance.getCluster(clusterCurrent);
      for (CmServerServiceType type : clusterStale.getServiceTypes()) {
        boolean typeFiltered = true;
        for (CmServerService service : clusterStale.getServices(type)) {
          if (!service.getStatus().equals(status)) {
            typeFiltered = false;
            break;
          }
        }
        if (typeFiltered) {
          for (CmServerService service : clusterCurrent.getServices(type)) {
            clusterFiltered.addService(service);
          }
        }
      }
      cluster = clusterFiltered;
    }
    CmServerClusterInstance.getCluster(event.getClusterSpec(), true);
    return cluster;
  }

  public static abstract class ServerCommand {
    public abstract CmServerCluster execute(ClusterActionEvent event, CmServer server, CmServerCluster clusterInput)
        throws Exception;
  }

}
