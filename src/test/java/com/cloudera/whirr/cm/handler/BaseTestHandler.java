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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.WordUtils;
import org.apache.whirr.Cluster;
import org.apache.whirr.ClusterController;
import org.apache.whirr.ClusterSpec;
import org.apache.whirr.service.BaseServiceDryRunTest;
import org.apache.whirr.service.DryRunModule.DryRun;
import org.junit.Before;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.cloudera.whirr.cm.BaseTest;
import com.cloudera.whirr.cm.CmServerClusterInstance;
import com.cloudera.whirr.cm.server.CmServer;
import com.cloudera.whirr.cm.server.CmServerCluster;
import com.cloudera.whirr.cm.server.CmServerException;
import com.cloudera.whirr.cm.server.CmServerService;
import com.cloudera.whirr.cm.server.CmServerServiceType;
import com.cloudera.whirr.cm.server.impl.CmServerFactory;
import com.cloudera.whirr.cm.server.impl.CmServerLog;
import com.cloudera.whirr.cm.server.impl.CmServerLog.CmServerLogSyncCommand;
import com.google.common.collect.ImmutableMap;
import com.jcraft.jsch.JSchException;

public abstract class BaseTestHandler extends BaseServiceDryRunTest implements BaseTest {

  protected int countProvisioned;
  protected int countConfigured;
  protected int countStarted;
  protected int countStopped;

  public void countersReset() {
    countProvisioned = countConfigured = countStarted = countStopped = 0;
  }

  protected boolean countersAssertAndReset(int countProvisioned, int countConfigured, int countStarted, int countStopped) {
    Assert.assertEquals(countProvisioned, this.countProvisioned);
    Assert.assertEquals(countConfigured, this.countConfigured);
    Assert.assertEquals(countStarted, this.countStarted);
    Assert.assertEquals(countStopped, this.countStopped);
    countersReset();
    return true;
  }

  @Before
  public void mockCmServer() {

    countersReset();

    CmServerFactory factory = Mockito.mock(CmServerFactory.class);
    CmServerClusterInstance.getFactory(factory);

    Mockito.when(
        factory.getCmServer(Matchers.anyString(), Matchers.anyInt(), Matchers.anyString(), Matchers.anyString(),
            Matchers.<CmServerLog> any())).thenReturn(new CmServer() {

      private boolean isProvisioned = false;
      private boolean isConfigured = false;
      private boolean isStarted = false;

      @Override
      public boolean getServiceConfigs(CmServerCluster cluster, File directory) throws CmServerException {
        return any(isConfigured);
      }

      @Override
      public List<CmServerService> getServiceHosts() throws CmServerException {
        return any(Collections.emptyList());
      }

      @Override
      public CmServerService getServiceHost(CmServerService service) throws CmServerException {
        return any(null);
      }

      @Override
      public CmServerService getServiceHost(CmServerService service, List<CmServerService> services)
          throws CmServerException {
        return any(null);
      }

      @Override
      public CmServerCluster getServices(CmServerCluster cluster) throws CmServerException {
        return any(cluster);
      }

      @Override
      public CmServerService getService(CmServerCluster cluster, CmServerServiceType type) throws CmServerException {
        return any(cluster.getServiceTypes(type));
      }

      @Override
      public CmServerCluster getServices(CmServerCluster cluster, CmServerServiceType type) throws CmServerException {
        return any(cluster);
      }

      @Override
      public boolean isProvisioned(CmServerCluster cluster) throws CmServerException {
        return any(isProvisioned);
      }

      @Override
      public boolean isConfigured(CmServerCluster cluster) throws CmServerException {
        return any(isConfigured);
      }

      @Override
      public boolean isStarted(CmServerCluster cluster) throws CmServerException {
        return any(isStarted);
      }

      @Override
      public boolean isStopped(CmServerCluster cluster) throws CmServerException {
        return any(!isStarted);
      }

      @Override
      public Map<String, String> initialise(Map<String, String> config) throws CmServerException {
        return any(Collections.emptyMap());
      }

      @Override
      public boolean provision(CmServerCluster cluster) throws CmServerException {
        countProvisioned += cluster.getServices(CmServerServiceType.CLUSTER).size();
        return any(isProvisioned = true);
      }

      @Override
      public boolean configure(CmServerCluster cluster) throws CmServerException {
        countConfigured += cluster.getServices(CmServerServiceType.CLUSTER).size();
        return any(isConfigured = true);
      }

      @Override
      public boolean start(CmServerCluster cluster) throws CmServerException {
        countStarted += cluster.getServices(CmServerServiceType.CLUSTER).size();
        return any(isStarted = true);
      }

      @Override
      public boolean stop(CmServerCluster cluster) throws CmServerException {
        countStopped += cluster.getServices(CmServerServiceType.CLUSTER).size();
        return any(isStarted = false);
      }

      @Override
      public boolean unconfigure(CmServerCluster cluster) throws CmServerException {
        return any(isConfigured = false);
      }

      @Override
      public boolean unprovision(CmServerCluster cluster) throws CmServerException {
        return any(isProvisioned = false);
      }
    });

  }

  @Override
  public ClusterSpec newClusterSpecForProperties(Map<String, String> properties) throws IOException,
      ConfigurationException, JSchException {
    ClusterSpec clusterSpec = super.newClusterSpecForProperties(ImmutableMap.<String, String> builder()
        .putAll(properties).put(CONFIG_WHIRR_USER, CLUSTER_USER).put(CONFIG_WHIRR_NAME, CONFIG_WHIRR_NAME_DEFAULT)
        .build());
    clusterSpec.setPrivateKey(FILE_KEY_PRIVATE);
    clusterSpec.setPublicKey(FILE_KEY_PUBLIC);
    return clusterSpec;
  }

  @Before
  public void clearClusterSingleton() {
    CmServerClusterInstance.getCluster(true);
  }

  protected ClusterController getController(ClusterSpec clusterSpec) {
    ClusterController controller = new ClusterController();
    DryRun dryRun = controller.getCompute().apply(clusterSpec).utils().injector().getInstance(DryRun.class);
    dryRun.reset();
    return controller;
  }

  protected Cluster launchWithClusterSpecAndWithController(ClusterSpec clusterSpec, ClusterController controller)
      throws IOException, InterruptedException {
    return controller.launchCluster(clusterSpec);
  }

  @SuppressWarnings("unchecked")
  private static <T> T any(Object value) {
    new CmServerLog.CmServerLogSysOut(LOG_TAG_CM_SERVER_API, false).logOperation(
        WordUtils.capitalize(Thread.currentThread().getStackTrace()[3].getMethodName()), new CmServerLogSyncCommand() {
          @Override
          public void execute() throws Exception {
          }
        });
    return (T) value;
  }

}
