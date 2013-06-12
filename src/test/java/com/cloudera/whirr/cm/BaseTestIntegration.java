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

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.TreeSet;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.whirr.ClusterController;
import org.apache.whirr.ClusterSpec;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

import com.cloudera.whirr.cm.server.CmServer;
import com.cloudera.whirr.cm.server.CmServerCluster;
import com.cloudera.whirr.cm.server.CmServerException;
import com.cloudera.whirr.cm.server.impl.CmServerFactory;
import com.cloudera.whirr.cm.server.impl.CmServerLog;

public abstract class BaseTestIntegration implements BaseTest {

  protected static CmServer serverBootstrap;
  protected static CmServerCluster cluster;
  private static boolean setupAndTearDownCluster = false;

  @BeforeClass
  public static void setupCluster() throws Exception {
    setupAndTearDownCluster = !clusterInitialised();
    clusterBootstrap();
    cluster = clusterTopology();
    Assert.assertNotNull(serverBootstrap = new CmServerFactory().getCmServer(cluster.getServer().getIp(), cluster
        .getServer().getIpInternal(), CM_PORT, CmConstants.CM_USER, CmConstants.CM_PASSWORD,
        new CmServerLog.CmServerLogSysOut(LOG_TAG_CM_SERVER_API_TEST, false)));
    Assert.assertTrue(serverBootstrap.initialise(cluster));
  }

  @Before
  public void provisionCluster() throws Exception {
    try {
      serverBootstrap.unconfigure(cluster);
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      serverBootstrap.provision(cluster);
    } catch (Exception e) {
      e.printStackTrace();
    }
    Assert.assertTrue(serverBootstrap.isProvisioned(cluster));
  }

  @AfterClass
  public static void teardownCluster() throws Exception {
    if (setupAndTearDownCluster) {
      clusterDestroy();
    }
  }

  public static boolean clusterInitialised() {
    return new File(new File(System.getProperty("user.home")), ".whirr/whirrcmtest/instances").exists();
  }

  public static Configuration clusterConfig() throws ConfigurationException {
    CompositeConfiguration config = new CompositeConfiguration();
    if (System.getProperty("config") != null) {
      config.addConfiguration(new PropertiesConfiguration(System.getProperty("config")));
    }
    config.addConfiguration(new PropertiesConfiguration("cm.properties"));
    return config;
  }

  public static CmServerCluster clusterTopology() throws ConfigurationException, IOException, InterruptedException,
      CmServerException {
    ClusterController clusterController = new ClusterController();
    ClusterSpec clusterSpec = ClusterSpec.withNoDefaults(clusterConfig());
    return CmServerClusterInstance.getCluster(clusterSpec, CmServerClusterInstance.getConfiguration(clusterSpec),
        clusterController.getInstances(clusterSpec, clusterController.getClusterStateStore(clusterSpec)),
        new TreeSet<String>(), new HashSet<String>());
  }

  public static void clusterBootstrap() throws Exception {
    if (!clusterInitialised()) {
      new ClusterController().launchCluster(ClusterSpec.withNoDefaults(clusterConfig()));
    }
  }

  public static void clusterDestroy() throws Exception {
    if (clusterInitialised()) {
      new ClusterController().destroyCluster(ClusterSpec.withNoDefaults(clusterConfig()));
    }
  }

}
