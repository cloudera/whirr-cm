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
package com.cloudera.whirr.cm.integration;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.WordUtils;
import org.apache.whirr.Cluster.Instance;
import org.apache.whirr.ClusterController;
import org.apache.whirr.ClusterSpec;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.cloudera.whirr.cm.BaseTest;
import com.cloudera.whirr.cm.CmConstants;
import com.cloudera.whirr.cm.CmServerClusterInstance;
import com.cloudera.whirr.cm.server.CmServer;
import com.cloudera.whirr.cm.server.CmServerCluster;
import com.cloudera.whirr.cm.server.impl.CmServerFactory;
import com.cloudera.whirr.cm.server.impl.CmServerLog;
import com.cloudera.whirr.cm.server.impl.CmServerLog.CmServerLogSyncCommand;
import com.google.common.collect.ImmutableMap;

public abstract class BaseITServer implements BaseTest {

  private static final String PLATFORM_DEFAULT = "example";

  protected static CmServerLog log = new CmServerLog.CmServerLogSysOut(LOG_TAG_CM_SERVER_API_TEST, false);

  protected String cm;
  protected String api;
  protected String cdh;
  protected String platform;

  protected Configuration configuration;
  protected ClusterSpec specification;
  protected ClusterController controller;
  protected Set<Instance> instances;

  protected CmServer serverTest;
  protected CmServer serverTestBootstrap;
  protected CmServerCluster cluster;

  private static boolean clusterSetupAndTeardown = false;

  private static final File clusterStateStoreFile = new File(new File(System.getProperty("user.home")),
      ".whirr/whirrcmtest/instances");

  @Rule
  public TestName test = new TestName();

  @BeforeClass
  public static void setupClass() throws Exception {
    clusterSetupAndTeardown = !isClusterBootstrapped();
  }

  @Before
  public void setupMethod() throws Exception {
    CmServerClusterInstance.clear();
    setSystemProperty(TEST_CM_VERSION, cm);
    setSystemProperty(TEST_CM_API_VERSION, api);
    setSystemProperty(TEST_CM_CDH_VERSION, cdh);
    setSystemProperty(TEST_PLATFORM, platform);
    if (!clusterSetupAndTeardown && !isClusterBootstrappedStatically()) {
      clusterDestroy();
    }
    if (!isClusterBootstrapped()) {
      clusterBootstrap(ImmutableMap.of(CONFIG_WHIRR_AUTO, "" + isClusterBootstrappedAuto()));
    }
    Assert.assertNotNull(configuration = clusterConfig());
    Assert.assertNotNull(specification = ClusterSpec.withNoDefaults(clusterConfig()));
    Assert.assertNotNull(controller = new ClusterController());
    Assert.assertNotNull(cluster = CmServerClusterInstance.getCluster(specification,
        CmServerClusterInstance.getConfiguration(specification),
        controller.getInstances(specification, controller.getClusterStateStore(specification)), new TreeSet<String>(),
        new HashSet<String>()));
    Assert.assertNotNull(instances = controller.getInstances(specification,
        controller.getClusterStateStore(specification)));
    Assert.assertNotNull(serverTestBootstrap = new CmServerFactory().getCmServer(CmServerClusterInstance
        .getVersion(configuration), CmServerClusterInstance.getVersionApi(configuration), CmServerClusterInstance
        .getVersionCdh(configuration), cluster.getServer().getIp(), cluster.getServer().getIpInternal(), CM_PORT,
        CmConstants.CM_USER, CmConstants.CM_PASSWORD, log));
    Assert.assertNotNull(serverTest = new CmServerFactory().getCmServer(CmServerClusterInstance
        .getVersion(configuration), CmServerClusterInstance.getVersionApi(configuration), CmServerClusterInstance
        .getVersionCdh(configuration), cluster.getServer().getIp(), cluster.getServer().getIpInternal(), CM_PORT,
        CmConstants.CM_USER, CmConstants.CM_PASSWORD, new CmServerLog.CmServerLogSysOut(LOG_TAG_CM_SERVER_API, false)));
    Assert.assertTrue(serverTestBootstrap.initialise(cluster));
    if (isClusterBootstrappedStatically()) {
      try {
        serverTestBootstrap.unconfigure(cluster);
      } catch (Exception e) {
        e.printStackTrace();
      }
      try {
        serverTestBootstrap.provision(cluster);
      } catch (Exception e) {
        e.printStackTrace();
      }
      Assert.assertTrue(serverTestBootstrap.isProvisioned(cluster));
    }
    log.logOperationStartedSync("PreTestServices");
    CmServerClusterInstance.logCluster(log, "PreTestServices", CmServerClusterInstance.getConfiguration(specification),
        serverTestBootstrap.getServices(cluster), instances);
    log.logOperationFinishedSync("PreTestServices");
    log.logSpacer();
    log.logSpacerDashed();
    log.logOperationStartedSync(getTestName());
    log.logSpacerDashed();
    log.logSpacer();
    CmServerClusterInstance.clear();
  }

  @After
  public void teardownMethod() throws Exception {
    log.logSpacer();
    log.logSpacerDashed();
    log.logOperationFinishedSync(getTestName());
    log.logSpacerDashed();
    log.logSpacer();
    if (serverTestBootstrap != null) {
      log.logOperationStartedSync("PostTestServices");
      CmServerClusterInstance.logCluster(log, "PostTestServices",
          CmServerClusterInstance.getConfiguration(specification), serverTestBootstrap.getServices(cluster), instances);
      log.logOperationFinishedSync("PostTestServices");
    }
    if (!isClusterBootstrappedStatically()) {
      clusterDestroy();
    }
  }

  @AfterClass
  public static void teardownClass() throws Exception {
    if (clusterSetupAndTeardown) {
      clusterDestroy();
    }
  }

  private String getTestName() {
    String qualifier = WordUtils.capitalize(System.getProperty(TEST_CM_VERSION))
        + WordUtils.capitalize(System.getProperty(TEST_CM_API_VERSION))
        + WordUtils.capitalize(System.getProperty(TEST_CM_CDH_VERSION))
        + WordUtils.capitalize(System.getProperty(TEST_PLATFORM));
    return WordUtils.capitalize(test.getMethodName()) + (qualifier.equals("") ? "" : ("-" + qualifier));
  }

  protected boolean isClusterBootstrappedStatically() {
    return true;
  }

  protected boolean isClusterBootstrappedAuto() {
    return false;
  }

  private static boolean isClusterBootstrapped() {
    return clusterStateStoreFile.exists();
  }

  private static Configuration clusterConfig() throws ConfigurationException {
    CompositeConfiguration configuration = new CompositeConfiguration();
    if (System.getProperty("config") != null) {
      configuration.addConfiguration(new PropertiesConfiguration(System.getProperty("config")));
    }
    configuration.addConfiguration(new PropertiesConfiguration(TEST_CM_PREFIX_PROPERTIES
        + (System.getProperty(TEST_PLATFORM) == null || System.getProperty(TEST_PLATFORM).equals("") ? PLATFORM_DEFAULT
            : System.getProperty(TEST_PLATFORM)) + ".properties"));
    configuration.addConfiguration(new PropertiesConfiguration(TEST_CM_TEST_PROPERTIES));
    configuration.addConfiguration(new PropertiesConfiguration(TEST_CM_EXAMPLE_PROPERTIES));
    configuration.addConfiguration(new PropertiesConfiguration(CmServerClusterInstance.class.getClassLoader()
        .getResource(CONFIG_WHIRR_DEFAULT_FILE)));
    return configuration;
  }

  private static void clusterBootstrap(Map<String, String> configuration) throws Exception {
    if (!isClusterBootstrapped()) {
      log.logOperationStartedSync("ClusterBootstrap");
      Configuration configurationAggregate = clusterConfig();
      for (String key : configuration.keySet()) {
        if (configurationAggregate.containsKey(key)) {
          configurationAggregate.clearProperty(key);
        }
        configurationAggregate.addProperty(key, configuration.get(key));
      }
      new ClusterController().launchCluster(ClusterSpec.withNoDefaults(configurationAggregate));
      log.logOperationFinishedSync("ClusterBootstrap");
    }
  }

  private static void clusterDestroy() throws Exception {
    if (isClusterBootstrapped()) {
      log.logOperation("ClusterDestroy", new CmServerLogSyncCommand() {
        @Override
        public void execute() throws Exception {
          new ClusterController().destroyCluster(ClusterSpec.withNoDefaults(clusterConfig()));
        }
      });
    }
  }

  public static class ClusterBoostrap {

    public static void main(String[] args) {
      int returnValue = 0;
      try {
        BaseITServer.clusterBootstrap(ImmutableMap.of(CONFIG_WHIRR_AUTO, Boolean.FALSE.toString()));
      } catch (Exception e) {
        e.printStackTrace();
        returnValue = 1;
      }
      System.exit(returnValue);
    }

  }

  public static class ClusterDestroy {

    public static void main(String[] args) {
      int returnValue = 0;
      try {
        BaseITServer.clusterDestroy();
      } catch (Exception e) {
        e.printStackTrace();
        returnValue = 1;
      }
      System.exit(returnValue);
    }

  }

  protected static void setSystemProperty(String name, String value) {
    if (name == null) {
      throw new IllegalArgumentException("Null name passed");
    }
    if (value == null || value.equals("")) {
      value = System.getProperty(name);
    }
    value = value == null ? "" : value;
    System.setProperty(name, value);
  }

}
