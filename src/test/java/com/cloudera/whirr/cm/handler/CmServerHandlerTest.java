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

import org.apache.whirr.Cluster;
import org.apache.whirr.ClusterController;
import org.apache.whirr.ClusterSpec;
import org.junit.Assert;
import org.junit.Test;

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
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class CmServerHandlerTest extends BaseTestHandler {

  private static final String WHIRR_INSTANCE_TEMPLATE_ALL = "1 " + CmServerHandler.ROLE + "+" + CmAgentHandler.ROLE
      + ",1 " + CmAgentHandler.ROLE + "+" + CmCdhHdfsNameNodeHandler.ROLE + "+"
      + CmCdhHdfsSecondaryNameNodeHandler.ROLE + "+" + CmCdhHueServerHandler.ROLE + "+"
      + CmCdhHueBeeswaxServerHandler.ROLE + "+" + CmCdhMapReduceJobTrackerHandler.ROLE + "+"
      + CmCdhHBaseMasterHandler.ROLE + "+" + CmCdhHiveMetaStoreHandler.ROLE + "+" + CmCdhImpalaStateStoreHandler.ROLE
      + "+" + CmCdhOozieServerHandler.ROLE + ",3 " + CmAgentHandler.ROLE + "+" + CmCdhHdfsDataNodeHandler.ROLE + "+"
      + CmCdhMapReduceTaskTrackerHandler.ROLE + "+" + CmCdhZookeeperServerHandler.ROLE + "+"
      + CmCdhHBaseRegionServerHandler.ROLE + "+" + CmCdhImpalaDaemonHandler.ROLE + "+" + CmCdhFlumeAgentHandler.ROLE;

  @Override
  protected Set<String> getInstanceRoles() {
    return ImmutableSet.of(CmServerHandler.ROLE);
  }

  @Override
  protected Predicate<CharSequence> bootstrapPredicate() {
    return and(containsPattern("configure_hostnames"),
        and(containsPattern("install_cm"), containsPattern("install_cm_server")));
  }

  @Override
  protected Predicate<CharSequence> configurePredicate() {
    return containsPattern("configure_cm_server");
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
    Assert
        .assertNotNull(launchWithClusterSpec(newClusterSpecForProperties(ImmutableMap
            .of("whirr.instance-templates",
                WHIRR_INSTANCE_TEMPLATE_ALL,
                CONFIG_WHIRR_CM_PREFIX + "REMOTE_PARCEL_REPO_URLS",
                "http://10.178.197.160/tmph3l7m2vv103/cloudera-repos/cdh4/parcels/4.2.0.10/\\,http://10.178.197.160/tmph3l7m2vv103/cloudera-repos/impala/parcels/0.6.109/"))));
    Assert.assertTrue(countersAssertAndReset(27, 27, 27, 0));
  }

  @Test
  public void testNodesAndAgentsAndClusterLifecycle() throws Exception {
    ClusterSpec clusterSpec = newClusterSpecForProperties(ImmutableMap
        .of("whirr.instance-templates",
            WHIRR_INSTANCE_TEMPLATE_ALL,
            CONFIG_WHIRR_CM_PREFIX + "REMOTE_PARCEL_REPO_URLS",
            "http://10.178.197.160/tmph3l7m2vv103/cloudera-repos/cdh4/parcels/4.2.0.10/\\,http://10.178.197.160/tmph3l7m2vv103/cloudera-repos/impala/parcels/0.6.109/"));
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
  public void testNodesAndAgentsAndClusterLifecycleFilteredHdfs() throws Exception {
    ClusterSpec clusterSpec = newClusterSpecForProperties(ImmutableMap
        .of("whirr.instance-templates",
            WHIRR_INSTANCE_TEMPLATE_ALL,
            CONFIG_WHIRR_CM_PREFIX + "REMOTE_PARCEL_REPO_URLS",
            "http://10.178.197.160/tmph3l7m2vv103/cloudera-repos/cdh4/parcels/4.2.0.10/\\,http://10.178.197.160/tmph3l7m2vv103/cloudera-repos/impala/parcels/0.6.109/"));
    Set<String> roles = BaseCommandCmServer.filterRoles(CmCdhHdfsNameNodeHandler.ROLE);
    ClusterController controller = getController(clusterSpec);
    Cluster cluster = launchWithClusterSpecAndWithController(clusterSpec, controller);
    Assert.assertNotNull(cluster);
    Assert.assertTrue(countersAssertAndReset(27, 27, 27, 0));
    Assert.assertNotNull(controller.startServices(clusterSpec, cluster, roles, Collections.<String> emptySet()));
    Assert.assertTrue(countersAssertAndReset(0, 0, 5, 0));
    Assert.assertNotNull(controller.stopServices(clusterSpec, cluster, roles, Collections.<String> emptySet()));
    Assert.assertTrue(countersAssertAndReset(0, 0, 0, 5));
    Assert.assertNotNull(controller.stopServices(clusterSpec, cluster, roles, Collections.<String> emptySet()));
    Assert.assertTrue(countersAssertAndReset(0, 0, 0, 5));
  }

  @Test
  public void testNodesAndAgentsAndClusterNotAuto() throws Exception {
    Assert
        .assertNotNull(launchWithClusterSpec(newClusterSpecForProperties(ImmutableMap
            .of("whirr.instance-templates",
                WHIRR_INSTANCE_TEMPLATE_ALL,
                CONFIG_WHIRR_CM_PREFIX + "REMOTE_PARCEL_REPO_URLS",
                "http://10.178.197.160/tmph3l7m2vv103/cloudera-repos/cdh4/parcels/4.2.0.10/\\,http://10.178.197.160/tmph3l7m2vv103/cloudera-repos/impala/parcels/0.6.109/",
                CONFIG_WHIRR_AUTO_VARIABLE, Boolean.FALSE.toString()))));
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
      clusterSpec.getConfiguration().setProperty(CONFIG_WHIRR_NAME, "some_cluster_name");
      Assert.assertNotNull(launchWithClusterSpec(clusterSpec));
    } catch (Exception e) {
      caught = true;
    }
    Assert.assertTrue(caught);
  }

}
