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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.whirr.service.ClusterActionEvent;
import org.apache.whirr.service.FirewallManager.Rule;

import com.cloudera.whirr.cm.CmServerClusterInstance;
import com.cloudera.whirr.cm.server.CmServer;
import com.cloudera.whirr.cm.server.CmServerCluster;
import com.cloudera.whirr.cm.server.CmServerException;
import com.cloudera.whirr.cm.server.CmServerService;
import com.cloudera.whirr.cm.server.CmServerService.CmServerServiceStatus;
import com.cloudera.whirr.cm.server.CmServerServiceType;
import com.cloudera.whirr.cm.server.impl.CmServerLog;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.CharStreams;
import com.google.common.io.Resources;

public class CmServerHandler extends BaseHandlerCm {

  public static final String ROLE = "cm-server";

  public static final String LICENSE_FILE = "cm-license.txt";

  public static final String PROPERTY_PARCEL_PRODUCT = "cm-server.parcel.product";
  public static final String PROPERTY_PARCEL_VERSION = "cm-server.parcel.version";
  public static final String PROPERTY_PORTS = "cm-server.ports";
  public static final String PROPERTY_PORT_WEB = "cm-server.port.web";
  public static final String PROPERTY_PORT_COMMS = "cm-server.port.comms";

  @Override
  public String getRole() {
    return ROLE;
  }

  @Override
  protected void beforeBootstrap(ClusterActionEvent event) throws IOException, InterruptedException {
    super.beforeBootstrap(event);
    try {
      CmServerClusterInstance.getCluster().setServer(getInstanceId());
    } catch (CmServerException e) {
      throw new IOException("Unexpected error building cluster", e);
    }
    addStatement(event, call("install_cm"));
    addStatement(event, call("install_cm_server"));
  }

  @Override
  protected void beforeConfigure(ClusterActionEvent event) throws IOException, InterruptedException {
    super.beforeConfigure(event);
    URL licenceConfigUri = null;
    if ((licenceConfigUri = CmServerHandler.class.getClassLoader().getResource(LICENSE_FILE)) != null) {
      addStatement(
          event,
          createOrOverwriteFile(
              "/tmp/" + LICENSE_FILE,
              Splitter.on('\n').split(
                  CharStreams.toString(Resources.newReaderSupplier(licenceConfigUri, Charsets.UTF_8)))));
    }
    addStatement(event, call("configure_cm_server"));
    @SuppressWarnings("unchecked")
    List<String> ports = getConfiguration(event.getClusterSpec()).getList(PROPERTY_PORTS);
    ports.add(getConfiguration(event.getClusterSpec()).getString(PROPERTY_PORT_WEB));
    ports.add(getConfiguration(event.getClusterSpec()).getString(PROPERTY_PORT_COMMS));
    for (String port : ports) {
      if (port != null && !"".equals(port))
        event.getFirewallManager().addRule(Rule.create().destination(role(ROLE)).port(Integer.parseInt(port)));
    }
    handleFirewallRules(event);
  }

  @Override
  protected void afterConfigure(ClusterActionEvent event) throws IOException, InterruptedException {
    super.afterConfigure(event);
    executeServer("CMClusterProvision", event, null, new ServerCommand() {
      @Override
      public CmServerCluster execute(ClusterActionEvent event, CmServer server, CmServerCluster clusterInput)
          throws Exception {
        Map<String, String> config = new HashMap<String, String>();
        @SuppressWarnings("unchecked")
        Iterator<String> keys = event.getClusterSpec().getConfiguration().getKeys();
        while (keys.hasNext()) {
          String key = keys.next();
          if (key.startsWith(CONFIG_WHIRR_CM_PREFIX)) {
            config.put(key.replaceFirst(CONFIG_WHIRR_CM_PREFIX, ""), event.getClusterSpec().getConfiguration()
                .getString(key));
          }
        }
        boolean success = false;
        server.initialise(config);
        if (server.provision(clusterInput)) {
          if (server.configure(clusterInput)) {
            if (server.getServiceConfigs(clusterInput, event.getClusterSpec().getClusterDirectory())) {
              success = true;
            }
          }
        }
        if (!success) {
          throw new CmServerException("Unexepcted error attempting to provision cluster");
        }
        return clusterInput;
      }
    }, false);
  }

  @Override
  protected void afterStart(ClusterActionEvent event) throws IOException, InterruptedException {
    super.afterStart(event);
    executeServer("CMClusterStart", event, CmServerServiceStatus.STARTING, new ServerCommand() {
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
    }, true);
  }

  @Override
  protected void afterStop(ClusterActionEvent event) throws IOException, InterruptedException {
    super.afterStop(event);
    executeServer("CMClusterStop", event, CmServerServiceStatus.STOPPING, new ServerCommand() {
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
    }, true);
  }

  private void executeServer(String operation, ClusterActionEvent event, CmServerServiceStatus status,
      ServerCommand command, boolean footer) throws IOException, InterruptedException {
    super.afterStart(event);
    try {
      CmServerClusterInstance.logHeader(logger, operation);
      CmServerCluster cluster = getCluster(event, status);
      if (!cluster.isEmpty()) {
        if (!event.getClusterSpec().getConfiguration().getBoolean(CONFIG_WHIRR_AUTO_VARIABLE, true)) {
          CmServerClusterInstance.logLineItem(logger, operation, "Warning, services found, but whirr property");
          CmServerClusterInstance.logLineItemDetail(logger, operation, "[" + CONFIG_WHIRR_AUTO_VARIABLE
              + "] is false so not executing");
        } else if (event.getClusterSpec().getConfiguration().getBoolean(CONFIG_WHIRR_AUTO_VARIABLE, true)) {
          CmServerClusterInstance.logLineItem(logger, operation);
          CmServer server = CmServerClusterInstance.getFactory().getCmServer(
              event.getCluster().getInstanceMatching(role(ROLE)).getPublicIp(), 7180, CM_USER, CM_PASSWORD,
              new CmServerLog.CmServerLogSysOut(LOG_TAG_CM_SERVER_API, false));
          cluster = command.execute(event, server, cluster);
          CmServerClusterInstance.logLineItemFooter(logger, operation);
          CmServerClusterInstance.logLineItem(logger, operation);
          CmServerClusterInstance.logCluster(logger, operation, event.getClusterSpec(), cluster);
          CmServerClusterInstance.logLineItemFooter(logger, operation);
        }
      }
    } catch (Exception e) {
      CmServerClusterInstance.logException(logger, operation,
          "Failed to execute (see above), log into the web console to resolve", e);
    } finally {
      CmServerClusterInstance.getCluster(true);
    }
    if (footer) {
      CmServerClusterInstance.logLineItemFooterFinal(logger);
    }
  }

  private CmServerCluster getCluster(ClusterActionEvent event, CmServerServiceStatus status) throws CmServerException,
      IOException {
    CmServerCluster clusterStale = CmServerClusterInstance.getCluster();
    CmServerCluster cluster, clusterCurrent = cluster = CmServerClusterInstance.getCluster(event.getClusterSpec(),
        event.getCluster().getInstances(), getDataMounts(event));
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
    CmServerClusterInstance.getCluster(true);
    return cluster;
  }

  private Set<String> getDataMounts(ClusterActionEvent event) throws IOException {
    Set<String> mounts = new HashSet<String>();
    String overirdeMounts = getConfiguration(event.getClusterSpec()).getString(DATA_DIRS_ROOT);
    if (overirdeMounts != null) {
      mounts.add(overirdeMounts);
    } else if (!getDeviceMappings(event).isEmpty()) {
      mounts.addAll(getDeviceMappings(event).keySet());
    } else {
      mounts.add(getConfiguration(event.getClusterSpec()).getString(DATA_DIRS_DEFAULT));
    }
    return mounts;
  }

  public static abstract class ServerCommand {
    public abstract CmServerCluster execute(ClusterActionEvent event, CmServer server, CmServerCluster clusterInput)
        throws Exception;
  }

}
