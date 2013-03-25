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

import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.containsPattern;

import java.util.Set;

import org.apache.whirr.ClusterSpec;
import org.junit.Assert;
import org.junit.Test;

import com.cloudera.whirr.cm.cdh.BaseHandlerCmCdh;
import com.cloudera.whirr.cm.cdh.CmCdhDataNodeHandler;
import com.cloudera.whirr.cm.cdh.CmCdhHBaseMasterHandler;
import com.cloudera.whirr.cm.cdh.CmCdhHBaseRegionServerHandler;
import com.cloudera.whirr.cm.cdh.CmCdhHiveMetaStoreHandler;
import com.cloudera.whirr.cm.cdh.CmCdhImpalaDaemonHandler;
import com.cloudera.whirr.cm.cdh.CmCdhImpalaStateStoreHandler;
import com.cloudera.whirr.cm.cdh.CmCdhJobTrackerHandler;
import com.cloudera.whirr.cm.cdh.CmCdhNameNodeHandler;
import com.cloudera.whirr.cm.cdh.CmCdhSecondaryNameNodeHandler;
import com.cloudera.whirr.cm.cdh.CmCdhTaskTrackerHandler;
import com.cloudera.whirr.cm.cdh.CmCdhZookeeperServerHandler;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class CmServerHandlerTest extends BaseTestHandler {

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
                "1 " + CmServerHandler.ROLE + "+" + CmAgentHandler.ROLE + ",1 " + CmAgentHandler.ROLE + "+"
                    + CmCdhNameNodeHandler.ROLE + "+" + CmCdhSecondaryNameNodeHandler.ROLE + "+"
                    + CmCdhJobTrackerHandler.ROLE + "+" + CmCdhHBaseMasterHandler.ROLE + "+"
                    + CmCdhHiveMetaStoreHandler.ROLE + "+" + CmCdhImpalaStateStoreHandler.ROLE + ",3 "
                    + CmAgentHandler.ROLE + "+" + CmCdhDataNodeHandler.ROLE + "+" + CmCdhTaskTrackerHandler.ROLE + "+"
                    + CmCdhZookeeperServerHandler.ROLE + "+" + CmCdhHBaseRegionServerHandler.ROLE + "+"
                    + CmCdhImpalaDaemonHandler.ROLE,
                BaseHandler.CONFIG_WHIRR_CM_PREFIX + "REMOTE_PARCEL_REPO_URLS",
                "http://10.178.197.160/tmph3l7m2vv103/cloudera-repos/cdh4/parcels/4.2.0.10/\\,http://10.178.197.160/tmph3l7m2vv103/cloudera-repos/impala/parcels/0.6.109/"))));
    Assert.assertEquals(6, BaseHandlerCmCdh.CmServerClusterSingleton.getInstance().getServiceTypes().size());
  }

  @Test
  public void testNoAgentsAndCluster() throws Exception {
    boolean caught = false;
    try {
      Assert.assertNotNull(launchWithClusterSpec(newClusterSpecForProperties(ImmutableMap.of(
          "whirr.instance-templates", "1 " + CmServerHandler.ROLE + ",2 " + CmCdhNameNodeHandler.ROLE,
          BaseHandler.CONFIG_WHIRR_AUTO_VARIABLE, Boolean.TRUE.toString()))));
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
              + CmCdhNameNodeHandler.ROLE + ",1 " + CmAgentHandler.ROLE + "+" + CmCdhNameNodeHandler.ROLE))));
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
          + CmCdhNameNodeHandler.ROLE));
      clusterSpec.getConfiguration().setProperty(BaseHandler.CONFIG_WHIRR_NAME, "some_cluster_name");
      Assert.assertNotNull(launchWithClusterSpec(clusterSpec));
    } catch (Exception e) {
      caught = true;
    }
    Assert.assertTrue(caught);
  }
}
