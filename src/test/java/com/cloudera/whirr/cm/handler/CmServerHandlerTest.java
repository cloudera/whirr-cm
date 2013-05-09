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

import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.containsPattern;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.configuration.Configuration;
import org.apache.whirr.Cluster;
import org.apache.whirr.ClusterController;
import org.apache.whirr.ClusterSpec;
import org.junit.Assert;
import org.junit.Test;

import com.cloudera.whirr.cm.CmServerClusterInstance;
import com.cloudera.whirr.cm.cmd.BaseCommandCmServer;
import com.cloudera.whirr.cm.handler.cdh.CmCdhFlumeAgentHandler;
import com.cloudera.whirr.cm.handler.cdh.CmCdhHBaseMasterHandler;
import com.cloudera.whirr.cm.handler.cdh.CmCdhHBaseRegionServerHandler;
import com.cloudera.whirr.cm.handler.cdh.CmCdhHdfsDataNodeHandler;
import com.cloudera.whirr.cm.handler.cdh.CmCdhHdfsNameNodeHandler;
import com.cloudera.whirr.cm.handler.cdh.CmCdhHdfsSecondaryNameNodeHandler;
import com.cloudera.whirr.cm.handler.cdh.CmCdhHiveMetaStoreHandler;
import com.cloudera.whirr.cm.handler.cdh.CmCdhHueBeeswaxServerHandler;
import com.cloudera.whirr.cm.handler.cdh.CmCdhHueServerHandler;
import com.cloudera.whirr.cm.handler.cdh.CmCdhImpalaDaemonHandler;
import com.cloudera.whirr.cm.handler.cdh.CmCdhImpalaStateStoreHandler;
import com.cloudera.whirr.cm.handler.cdh.CmCdhMapReduceJobTrackerHandler;
import com.cloudera.whirr.cm.handler.cdh.CmCdhMapReduceTaskTrackerHandler;
import com.cloudera.whirr.cm.handler.cdh.CmCdhOozieServerHandler;
import com.cloudera.whirr.cm.handler.cdh.CmCdhZookeeperServerHandler;
import com.cloudera.whirr.cm.server.CmServerServiceType;
import com.cloudera.whirr.cm.server.CmServerServiceTypeCms;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;

public class CmServerHandlerTest extends BaseTestHandler {

  private static final String WHIRR_INSTANCE_TEMPLATE_ALL = "1 " + CmServerHandler.ROLE + "+" + CmAgentHandler.ROLE
      + ",1 " + CmAgentHandler.ROLE + "+" + CmCdhHdfsNameNodeHandler.ROLE + "+"
      + CmCdhHdfsSecondaryNameNodeHandler.ROLE + "+" + CmCdhHueServerHandler.ROLE + "+"
      + CmCdhHueBeeswaxServerHandler.ROLE + "+" + CmCdhMapReduceJobTrackerHandler.ROLE + "+" + ",1 "
      + CmAgentHandler.ROLE + "+" + CmCdhHBaseMasterHandler.ROLE + "+" + CmCdhHiveMetaStoreHandler.ROLE + "+"
      + CmCdhImpalaStateStoreHandler.ROLE + "+" + CmCdhOozieServerHandler.ROLE + ",3 " + CmAgentHandler.ROLE + "+"
      + CmCdhHdfsDataNodeHandler.ROLE + "+" + CmCdhMapReduceTaskTrackerHandler.ROLE + "+"
      + CmCdhZookeeperServerHandler.ROLE + "+" + CmCdhHBaseRegionServerHandler.ROLE + "+"
      + CmCdhImpalaDaemonHandler.ROLE + "+" + CmCdhFlumeAgentHandler.ROLE;

  @Override
  protected Set<String> getInstanceRoles() {
    return ImmutableSortedSet.of(CmServerHandler.ROLE);
  }

  @Override
  protected Predicate<CharSequence> bootstrapPredicate() {
    return and(
        containsPattern("configure_hostnames"),
        and(containsPattern("install_database"),
            and(containsPattern("install_cm"), containsPattern("install_cm_server"))));
  }

  @Override
  protected Predicate<CharSequence> configurePredicate() {
    return containsPattern("configure_cm_server");
  }

  @Test
  public void testConfiguration() throws Exception {

    Configuration configuration = CmServerClusterInstance.getConfiguration(newClusterSpecForProperties(ImmutableMap.of(
        "whirr.instance-templates", "1 " + CmServerHandler.ROLE + ",2 " + CmNodeHandler.ROLE)));
    Assert.assertEquals(
        "impala",
        configuration.getString(CONFIG_WHIRR_CM_CONFIG_PREFIX + CmServerServiceType.HDFS.getId().toLowerCase()
            + ".dfs_block_local_path_access_user"));
    Assert.assertEquals("/manager/agent/log/agent.log",
        CmServerClusterInstance.getConfiguration(newClusterSpecForProperties(Collections.<String, String> emptyMap()))
            .getString(CONFIG_WHIRR_INTERNAL_AGENT_LOG_FILE));
    Assert.assertEquals(
        "100000",
        configuration.getString(CONFIG_WHIRR_CM_CONFIG_PREFIX + CmServerServiceTypeCms.CM.getId().toLowerCase()
            + ".parcel_distribute_rate_limit_kbs_per_second"));

    configuration = CmServerClusterInstance.getConfiguration(newClusterSpecForProperties(ImmutableMap
        .of("whirr.instance-templates", "1 " + CmServerHandler.ROLE + ",2 " + CmNodeHandler.ROLE,
            CONFIG_WHIRR_CM_CONFIG_PREFIX + CmServerServiceType.HDFS.getId().toLowerCase()
                + ".dfs_block_local_path_access_user", "some_user", CONFIG_WHIRR_CM_CONFIG_PREFIX
                + CmServerServiceType.HDFS.getId().toLowerCase() + ".some_setting", "some_value",
            CONFIG_WHIRR_CM_CONFIG_PREFIX + CmServerServiceType.HDFS_NAMENODE.getId().toLowerCase()
                + ".some_other_setting", "some_other_value")));
    Assert.assertEquals(
        "some_user",
        configuration.getString(CONFIG_WHIRR_CM_CONFIG_PREFIX + CmServerServiceType.HDFS.getId().toLowerCase()
            + ".dfs_block_local_path_access_user"));
    Assert.assertEquals(
        "some_value",
        configuration.getString(CONFIG_WHIRR_CM_CONFIG_PREFIX + CmServerServiceType.HDFS.getId().toLowerCase()
            + ".some_setting"));
    Assert.assertEquals(
        "some_value",
        CmServerClusterInstance.getClusterConfiguration(configuration, new TreeSet<String>(),
            CmServerServiceType.HDFS.getId(), null, "some_setting"));
    Assert.assertEquals("some_value", CmServerClusterInstance.getClusterConfiguration(configuration,
        new TreeSet<String>(), CmServerServiceType.HDFS_NAMENODE.getId(), CmServerServiceType.HDFS.getId(),
        "some_setting"));
    Assert.assertEquals("some_other_value", CmServerClusterInstance.getClusterConfiguration(configuration,
        new TreeSet<String>(), CmServerServiceType.HDFS_NAMENODE.getId(), CmServerServiceType.HDFS.getId(),
        "some_other_setting"));
    Assert.assertEquals(
        "mysql",
        CmServerClusterInstance.getClusterConfiguration(configuration, new TreeSet<String>(),
            CmServerServiceTypeCms.CM.getId(), null, CONFIG_CM_DB_SUFFIX_TYPE));
    Assert.assertEquals("mysql", CmServerClusterInstance.getClusterConfiguration(configuration, new TreeSet<String>(),
        CmServerServiceTypeCms.HOSTMONITOR.getId(), null, CONFIG_CM_DB_SUFFIX_TYPE));
    Assert.assertEquals("localhost:3306", CmServerClusterInstance.getClusterConfiguration(configuration,
        new TreeSet<String>(), CmServerServiceTypeCms.HOSTMONITOR.getId(), null, CONFIG_CM_DB_SUFFIX_HOST));
    Assert.assertEquals(
        "mysql",
        CmServerClusterInstance.getClusterConfiguration(configuration, new TreeSet<String>(),
            CmServerServiceType.HIVE.getId(), null, CONFIG_CM_DB_SUFFIX_TYPE));
    Assert.assertEquals(
        "3306",
        CmServerClusterInstance.getClusterConfiguration(configuration, new TreeSet<String>(),
            CmServerServiceType.HIVE.getId(), null, CONFIG_CM_DB_SUFFIX_PORT));
    boolean caught = false;
    try {
      Assert.assertEquals("some_other_value", CmServerClusterInstance.getClusterConfiguration(configuration,
          new TreeSet<String>(), CmServerServiceType.HDFS_NAMENODE.getId(), CmServerServiceType.HDFS.getId(),
          "some_unknown_setting"));
    } catch (Exception e) {
      caught = true;
    }
    Assert.assertTrue(caught);
    Assert.assertEquals(
        configuration.getString(CONFIG_WHIRR_INTERNAL_DATA_DIRS_DEFAULT)
            + configuration.getString(CONFIG_WHIRR_INTERNAL_CM_CONFIG_DEFAULT_PREFIX
                + CmServerServiceTypeCms.NAVIGATOR.getId().toLowerCase() + ".mgmt_log_dir"), CmServerClusterInstance
            .getClusterConfiguration(configuration, new TreeSet<String>())
            .get(CmServerServiceTypeCms.NAVIGATOR.getId()).get("mgmt_log_dir"));
    Assert.assertEquals(
        configuration.getString(CONFIG_WHIRR_INTERNAL_DATA_DIRS_DEFAULT)
            + configuration.getString(CONFIG_WHIRR_INTERNAL_CM_CONFIG_DEFAULT_PREFIX
                + CmServerServiceType.HDFS_NAMENODE.getId().toLowerCase() + ".dfs_name_dir_list"),
        CmServerClusterInstance.getClusterConfiguration(configuration, new TreeSet<String>())
            .get(CmServerServiceType.HDFS_NAMENODE.getId()).get("dfs_name_dir_list"));
    Assert.assertEquals(
        "/mnt/1"
            + configuration.getString(CONFIG_WHIRR_INTERNAL_CM_CONFIG_DEFAULT_PREFIX
                + CmServerServiceType.HDFS_NAMENODE.getId().toLowerCase() + ".dfs_name_dir_list"),
        CmServerClusterInstance.getClusterConfiguration(configuration, ImmutableSortedSet.of("/mnt/1"))
            .get(CmServerServiceType.HDFS_NAMENODE.getId()).get("dfs_name_dir_list"));
    Assert.assertEquals(
        "/mnt/1"
            + configuration.getString(CONFIG_WHIRR_INTERNAL_CM_CONFIG_DEFAULT_PREFIX
                + CmServerServiceType.HDFS_NAMENODE.getId().toLowerCase() + ".dfs_name_dir_list")
            + ","
            + "/mnt/2"
            + configuration.getString(CONFIG_WHIRR_INTERNAL_CM_CONFIG_DEFAULT_PREFIX
                + CmServerServiceType.HDFS_NAMENODE.getId().toLowerCase() + ".dfs_name_dir_list"),
        CmServerClusterInstance.getClusterConfiguration(configuration, ImmutableSortedSet.of("/mnt/1", "/mnt/2"))
            .get(CmServerServiceType.HDFS_NAMENODE.getId()).get("dfs_name_dir_list"));
    Assert.assertNull(CmServerClusterInstance
        .getClusterConfiguration(configuration, ImmutableSortedSet.of("/mnt/1", "/mnt/2"))
        .get(CmServerServiceType.MAPREDUCE_TASK_TRACKER.getId()).get(CONFIG_CM_TASKTRACKER_INSTRUMENTATION));

    configuration = CmServerClusterInstance.getConfiguration(newClusterSpecForProperties(ImmutableMap.of(
        "whirr.instance-templates", "1 " + CmServerHandler.ROLE + ",2 " + CmNodeHandler.ROLE,
        CONFIG_WHIRR_CM_CONFIG_PREFIX + CmServerServiceTypeCms.CM.getId().toLowerCase() + "."
            + CONFIG_CM_DB_SUFFIX_NAME, "cman", CONFIG_WHIRR_CM_CONFIG_PREFIX
            + CmServerServiceTypeCms.CM.getId().toLowerCase() + "." + CONFIG_CM_DB_SUFFIX_TYPE, "postgres",
        CONFIG_WHIRR_CM_CONFIG_PREFIX + CmServerServiceType.HIVE.getId().toLowerCase() + ".hive_metastore_"
            + CONFIG_CM_DB_SUFFIX_PORT, "9999", CONFIG_WHIRR_CM_CONFIG_PREFIX
            + CmServerServiceType.CLUSTER.getId().toLowerCase() + "." + CONFIG_CM_LICENSE_PROVIDED, "true")));
    Assert.assertEquals(
        "/data1"
            + configuration.getString(CONFIG_WHIRR_INTERNAL_CM_CONFIG_DEFAULT_PREFIX
                + CmServerServiceTypeCms.NAVIGATOR.getId().toLowerCase() + ".mgmt_log_dir"),
        CmServerClusterInstance.getClusterConfiguration(configuration, ImmutableSortedSet.of("/data1", "/data2"))
            .get(CmServerServiceTypeCms.NAVIGATOR.getId()).get("mgmt_log_dir"));
    Assert.assertEquals(
        "/data1"
            + configuration.getString(CONFIG_WHIRR_INTERNAL_CM_CONFIG_DEFAULT_PREFIX
                + CmServerServiceType.HDFS_NAMENODE.getId().toLowerCase() + ".dfs_name_dir_list")
            + ",/data2"
            + configuration.getString(CONFIG_WHIRR_INTERNAL_CM_CONFIG_DEFAULT_PREFIX
                + CmServerServiceType.HDFS_NAMENODE.getId().toLowerCase() + ".dfs_name_dir_list"),
        CmServerClusterInstance.getClusterConfiguration(configuration, ImmutableSortedSet.of("/data1", "/data2"))
            .get(CmServerServiceType.HDFS_NAMENODE.getId()).get("dfs_name_dir_list"));
    Assert.assertEquals(
        "org.apache.hadoop.mapred.TaskTrackerCmonInst",
        CmServerClusterInstance.getClusterConfiguration(configuration, ImmutableSortedSet.of("/mnt/1", "/mnt/2"))
            .get(CmServerServiceType.MAPREDUCE_TASK_TRACKER.getId()).get(CONFIG_CM_TASKTRACKER_INSTRUMENTATION));

    configuration = CmServerClusterInstance.getConfiguration(newClusterSpecForProperties(ImmutableMap.of(
        "whirr.instance-templates", "1 " + CmServerHandler.ROLE + ",2 " + CmNodeHandler.ROLE,
        CONFIG_WHIRR_CM_CONFIG_PREFIX + CmServerServiceTypeCms.CM.getId().toLowerCase() + "."
            + CONFIG_CM_DB_SUFFIX_NAME, "cman", CONFIG_WHIRR_CM_CONFIG_PREFIX
            + CmServerServiceTypeCms.CM.getId().toLowerCase() + "." + CONFIG_CM_DB_SUFFIX_TYPE, "postgres",
        CONFIG_WHIRR_CM_CONFIG_PREFIX + CmServerServiceType.HIVE.getId().toLowerCase() + ".hive_metastore_"
            + CONFIG_CM_DB_SUFFIX_PORT, "9999")));
    Assert.assertEquals(
        "/tmp"
            + configuration.getString(CONFIG_WHIRR_INTERNAL_CM_CONFIG_DEFAULT_PREFIX
                + CmServerServiceTypeCms.NAVIGATOR.getId().toLowerCase() + ".mgmt_log_dir"),
        CmServerClusterInstance.getClusterConfiguration(configuration, ImmutableSortedSet.of("/tmp"))
            .get(CmServerServiceTypeCms.NAVIGATOR.getId()).get("mgmt_log_dir"));
    Assert.assertEquals(
        "/tmp"
            + configuration.getString(CONFIG_WHIRR_INTERNAL_CM_CONFIG_DEFAULT_PREFIX
                + CmServerServiceType.HDFS_NAMENODE.getId().toLowerCase() + ".dfs_name_dir_list"),
        CmServerClusterInstance.getClusterConfiguration(configuration, ImmutableSortedSet.of("/tmp"))
            .get(CmServerServiceType.HDFS_NAMENODE.getId()).get("dfs_name_dir_list"));
    Assert.assertEquals(
        "/mnt/1"
            + configuration.getString(CONFIG_WHIRR_INTERNAL_CM_CONFIG_DEFAULT_PREFIX
                + CmServerServiceType.HDFS_NAMENODE.getId().toLowerCase() + ".dfs_name_dir_list")
            + ",/mnt/2"
            + configuration.getString(CONFIG_WHIRR_INTERNAL_CM_CONFIG_DEFAULT_PREFIX
                + CmServerServiceType.HDFS_NAMENODE.getId().toLowerCase() + ".dfs_name_dir_list"),
        CmServerClusterInstance.getClusterConfiguration(configuration, ImmutableSortedSet.of("/mnt/1", "/mnt/2"))
            .get(CmServerServiceType.HDFS_NAMENODE.getId()).get("dfs_name_dir_list"));
    Assert.assertEquals("postgres", CmServerClusterInstance.getClusterConfiguration(configuration,
        ImmutableSortedSet.of("/tmp"), CmServerServiceTypeCms.CM.getId(), null, CONFIG_CM_DB_SUFFIX_TYPE));
    Assert.assertEquals("mysql", CmServerClusterInstance.getClusterConfiguration(configuration, new TreeSet<String>(),
        CmServerServiceTypeCms.HOSTMONITOR.getId(), null, CONFIG_CM_DB_SUFFIX_TYPE));
    Assert.assertEquals("localhost:3306", CmServerClusterInstance.getClusterConfiguration(configuration,
        new TreeSet<String>(), CmServerServiceTypeCms.HOSTMONITOR.getId(), null, CONFIG_CM_DB_SUFFIX_HOST));
    Assert.assertEquals(
        "mysql",
        CmServerClusterInstance.getClusterConfiguration(configuration, new TreeSet<String>(),
            CmServerServiceType.HIVE.getId(), null, CONFIG_CM_DB_SUFFIX_TYPE));
    Assert.assertEquals(
        "9999",
        CmServerClusterInstance.getClusterConfiguration(configuration, new TreeSet<String>(),
            CmServerServiceType.HIVE.getId(), null, CONFIG_CM_DB_SUFFIX_PORT));

    configuration = CmServerClusterInstance.getConfiguration(newClusterSpecForProperties(ImmutableMap.of(
        "whirr.instance-templates", "1 " + CmServerHandler.ROLE + ",2 " + CmNodeHandler.ROLE,
        CONFIG_WHIRR_CM_CONFIG_PREFIX + CmServerServiceType.HDFS_NAMENODE.getId().toLowerCase() + ".dfs_name_dir_list",
        "/mynn")));
    Assert.assertEquals(
        "/mnt/1"
            + configuration.getString(CONFIG_WHIRR_INTERNAL_CM_CONFIG_DEFAULT_PREFIX
                + CmServerServiceTypeCms.NAVIGATOR.getId().toLowerCase() + ".mgmt_log_dir"),
        CmServerClusterInstance.getClusterConfiguration(configuration, ImmutableSortedSet.of("/mnt/1", "/mnt/2"))
            .get(CmServerServiceTypeCms.NAVIGATOR.getId()).get("mgmt_log_dir"));
    Assert.assertEquals("/mynn", CmServerClusterInstance.getClusterConfiguration(configuration, new TreeSet<String>())
        .get(CmServerServiceType.HDFS_NAMENODE.getId()).get("dfs_name_dir_list"));
    Assert.assertEquals(
        "/mynn",
        CmServerClusterInstance.getClusterConfiguration(configuration, ImmutableSortedSet.of("/mnt/1", "/mnt/2"))
            .get(CmServerServiceType.HDFS_NAMENODE.getId()).get("dfs_name_dir_list"));
    Assert.assertEquals(
        configuration.getString(CONFIG_WHIRR_INTERNAL_DATA_DIRS_DEFAULT)
            + configuration.getString(CONFIG_WHIRR_INTERNAL_CM_CONFIG_DEFAULT_PREFIX
                + CmServerServiceType.HDFS_SECONDARY_NAMENODE.getId().toLowerCase() + ".fs_checkpoint_dir_list"),
        CmServerClusterInstance.getClusterConfiguration(configuration, new TreeSet<String>())
            .get(CmServerServiceType.HDFS_SECONDARY_NAMENODE.getId()).get("fs_checkpoint_dir_list"));

    configuration = CmServerClusterInstance.getConfiguration(newClusterSpecForProperties(ImmutableMap.of(
        "whirr.instance-templates", "1 " + CmServerHandler.ROLE + ",2 " + CmNodeHandler.ROLE,
        CONFIG_WHIRR_CM_CONFIG_PREFIX + CmServerServiceType.HDFS_NAMENODE.getId().toLowerCase() + ".dfs_name_dir_list",
        "/mynn", CONFIG_WHIRR_DATA_DIRS_ROOT, "/tmp")));
    Assert.assertEquals("/mynn", CmServerClusterInstance.getClusterConfiguration(configuration, new TreeSet<String>())
        .get(CmServerServiceType.HDFS_NAMENODE.getId()).get("dfs_name_dir_list"));
    Assert.assertEquals(
        "/mynn",
        CmServerClusterInstance.getClusterConfiguration(configuration, ImmutableSortedSet.of("/mnt/1", "/mnt/2"))
            .get(CmServerServiceType.HDFS_NAMENODE.getId()).get("dfs_name_dir_list"));
    Assert.assertEquals(
        "/data"
            + configuration.getString(CONFIG_WHIRR_INTERNAL_CM_CONFIG_DEFAULT_PREFIX
                + CmServerServiceType.HDFS_SECONDARY_NAMENODE.getId().toLowerCase() + ".fs_checkpoint_dir_list"),
        CmServerClusterInstance.getClusterConfiguration(configuration, new TreeSet<String>())
            .get(CmServerServiceType.HDFS_SECONDARY_NAMENODE.getId()).get("fs_checkpoint_dir_list"));

  }

  @Test
  public void testNodes() throws Exception {
    Assert.assertNotNull(launchWithClusterSpec(newClusterSpecForProperties(ImmutableMap.of("whirr.instance-templates",
        "1 " + CmServerHandler.ROLE + ",2 " + CmNodeHandler.ROLE))));
  }

  @Test
  public void testAgents() throws Exception {
    Assert.assertNotNull(launchWithClusterSpec(newClusterSpecForProperties(ImmutableMap.of("whirr.instance-templates",
        "1 " + CmServerHandler.ROLE + ",2 " + CmAgentHandler.ROLE))));
  }

  @Test
  public void testNodesAndAgents() throws Exception {
    Assert.assertNotNull(launchWithClusterSpec(newClusterSpecForProperties(ImmutableMap.of("whirr.instance-templates",
        "1 " + CmServerHandler.ROLE + ",2 " + CmNodeHandler.ROLE + ",2 " + CmAgentHandler.ROLE))));
  }

  @Test
  public void testNodesAndAgentsAndCluster() throws Exception {
    Assert.assertNotNull(launchWithClusterSpec(newClusterSpecForProperties(ImmutableMap.of("whirr.instance-templates",
        WHIRR_INSTANCE_TEMPLATE_ALL))));
    Assert.assertTrue(countersAssertAndReset(27, 27, 27, 0));
  }

  @Test
  public void testNodesAndAgentsAndClusterLifecycle() throws Exception {
    ClusterSpec clusterSpec = newClusterSpecForProperties(ImmutableMap.of("whirr.instance-templates",
        WHIRR_INSTANCE_TEMPLATE_ALL));
    ClusterController controller = getController(clusterSpec);
    Cluster cluster = launchWithClusterSpecAndWithController(clusterSpec, controller);
    Assert.assertNotNull(cluster);
    Assert.assertTrue(countersAssertAndReset(27, 27, 27, 0));
    Assert.assertNotNull(controller.startServices(clusterSpec, cluster));
    Assert.assertTrue(countersAssertAndReset(0, 0, 27, 0));
    Assert.assertNotNull(controller.stopServices(clusterSpec, cluster));
    Assert.assertTrue(countersAssertAndReset(0, 0, 0, 27));
    Assert.assertNotNull(controller.stopServices(clusterSpec, cluster));
    Assert.assertTrue(countersAssertAndReset(0, 0, 0, 27));
  }

  @Test
  public void testNodesAndAgentsAndClusterBootstrapMultiCluster() throws Exception {
    ClusterSpec clusterSpec = newClusterSpecForProperties(ImmutableMap.of("whirr.instance-templates",
        WHIRR_INSTANCE_TEMPLATE_ALL));
    ClusterController controller = getController(clusterSpec);
    Cluster cluster = controller.bootstrapCluster(clusterSpec);

    ClusterSpec clusterSpecTwo = newClusterSpecForProperties(ImmutableMap.of("whirr.instance-templates",
        WHIRR_INSTANCE_TEMPLATE_ALL));
    clusterSpecTwo.setVersion("different-version");
    ClusterController controllerTwo = getController(clusterSpecTwo);
    Cluster clusterTwo = controllerTwo.bootstrapCluster(clusterSpecTwo);

    Assert.assertNotNull(cluster);
    Assert.assertNotNull(clusterTwo);
  }

  @Test
  public void testNodesAndAgentsAndClusterLifecycleFilteredHdfs() throws Exception {
    ClusterSpec clusterSpec = newClusterSpecForProperties(ImmutableMap.of("whirr.instance-templates",
        WHIRR_INSTANCE_TEMPLATE_ALL));
    Set<String> roles = BaseCommandCmServer.filterRoles(CmCdhHdfsNameNodeHandler.ROLE);
    ClusterController controller = getController(clusterSpec);
    Cluster cluster = launchWithClusterSpecAndWithController(clusterSpec, controller);
    Assert.assertNotNull(cluster);
    Assert.assertTrue(countersAssertAndReset(27, 27, 27, 0));
    Assert.assertNotNull(controller.startServices(clusterSpec, cluster, roles, new TreeSet<String>()));
    Assert.assertTrue(countersAssertAndReset(0, 0, 5, 0));
    Assert.assertNotNull(controller.stopServices(clusterSpec, cluster, roles, new TreeSet<String>()));
    Assert.assertTrue(countersAssertAndReset(0, 0, 0, 5));
    Assert.assertNotNull(controller.stopServices(clusterSpec, cluster, roles, new TreeSet<String>()));
    Assert.assertTrue(countersAssertAndReset(0, 0, 0, 5));
  }

  @Test
  public void testNodesAndAgentsAndClusterNotAuto() throws Exception {
    Assert.assertNotNull(launchWithClusterSpec(newClusterSpecForProperties(ImmutableMap.of("whirr.instance-templates",
        WHIRR_INSTANCE_TEMPLATE_ALL, CONFIG_WHIRR_AUTO, Boolean.FALSE.toString()))));
  }

  @Test
  public void testNodesAndAgentsAndClusterFirewall() throws Exception {
    Assert.assertNotNull(launchWithClusterSpec(newClusterSpecForProperties(ImmutableMap.of("whirr.instance-templates",
        WHIRR_INSTANCE_TEMPLATE_ALL, CONFIG_WHIRR_AUTO, Boolean.FALSE.toString(), CONFIG_WHIRR_FIREWALL_ENABLE,
        Boolean.TRUE.toString()))));
  }

  @Test
  public void testNodesAndAgentsAndClusterConfiguration() throws Exception {
    Assert.assertNotNull(launchWithClusterSpec(newClusterSpecForProperties(ImmutableMap.of("whirr.instance-templates",
        WHIRR_INSTANCE_TEMPLATE_ALL, CONFIG_WHIRR_CM_CONFIG_PREFIX + CmServerServiceType.HDFS.getId().toLowerCase()
            + ".some_setting", "some_value"))));
    Assert.assertTrue(countersAssertAndReset(27, 27, 27, 0));
  }

  @Test
  public void testNodesAndAgentsAndClusterConfigurationOverride() throws Exception {
    Assert.assertNotNull(launchWithClusterSpec(newClusterSpecForProperties(ImmutableMap.of("whirr.instance-templates",
        WHIRR_INSTANCE_TEMPLATE_ALL, CONFIG_WHIRR_CM_CONFIG_PREFIX + CmServerServiceType.HDFS.getId().toLowerCase()
            + ".dfs_block_local_path_access_user", "someuser"))));
    Assert.assertTrue(countersAssertAndReset(27, 27, 27, 0));
  }

  @Test
  public void testNodesAndAgentsAndClusterConfigurationInvalid() throws Exception {
    boolean caught = false;
    try {
      Assert.assertNotNull(launchWithClusterSpec(newClusterSpecForProperties(ImmutableMap.of(
          "whirr.instance-templates", WHIRR_INSTANCE_TEMPLATE_ALL, CONFIG_WHIRR_CM_CONFIG_PREFIX
              + CmServerServiceType.HDFS.getId().toLowerCase(), "some_value"))));
    } catch (Exception e) {
      caught = true;
    }
    Assert.assertTrue(caught);
  }

  @Test
  public void testNoAgentsAndCluster() throws Exception {
    boolean caught = false;
    try {
      Assert.assertNotNull(launchWithClusterSpec(newClusterSpecForProperties(ImmutableMap.of(
          "whirr.instance-templates", "1 " + CmServerHandler.ROLE + ",2 " + CmCdhHdfsNameNodeHandler.ROLE))));
    } catch (Exception e) {
      caught = true;
    }
    Assert.assertTrue(caught);
  }

  @Test
  public void testAgentsAndMultipleNameNodes() throws Exception {
    boolean caught = false;
    try {
      Assert.assertNotNull(launchWithClusterSpec(newClusterSpecForProperties(ImmutableMap.of(
          "whirr.instance-templates", "1 " + CmServerHandler.ROLE + ",1 " + CmAgentHandler.ROLE + "+"
              + CmCdhHdfsNameNodeHandler.ROLE + ",1 " + CmAgentHandler.ROLE + "+" + CmCdhHdfsNameNodeHandler.ROLE))));
    } catch (Exception e) {
      caught = true;
    }
    Assert.assertTrue(caught);
  }

  @Test
  public void testMultipleCmServers() throws Exception {
    boolean caught = false;
    try {
      Assert.assertNotNull(launchWithClusterSpec(newClusterSpecForProperties(ImmutableMap.of(
          "whirr.instance-templates", "1 " + CmServerHandler.ROLE + ",1 " + CmServerHandler.ROLE))));
    } catch (Exception e) {
      caught = true;
    }
    Assert.assertTrue(caught);
  }

  @Test
  public void testInValidClusterName() throws Exception {
    boolean caught = false;
    try {
      ClusterSpec clusterSpec = newClusterSpecForProperties(ImmutableMap.of("whirr.instance-templates", "1 "
          + CmServerHandler.ROLE + ",1 " + CmAgentHandler.ROLE + ",1 " + CmAgentHandler.ROLE + "+"
          + CmCdhHdfsNameNodeHandler.ROLE));
      clusterSpec.getConfiguration()
          .setProperty(ClusterSpec.Property.CLUSTER_NAME.getConfigName(), "some_cluster_name");
      Assert.assertNotNull(launchWithClusterSpec(clusterSpec));
    } catch (Exception e) {
      caught = true;
    }
    Assert.assertTrue(caught);
  }

}
