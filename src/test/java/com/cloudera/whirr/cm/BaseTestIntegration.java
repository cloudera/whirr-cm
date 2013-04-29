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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

import com.cloudera.whirr.cm.server.CmServer;
import com.cloudera.whirr.cm.server.CmServerCluster;
import com.cloudera.whirr.cm.server.CmServerException;
import com.cloudera.whirr.cm.server.CmServerService;
import com.cloudera.whirr.cm.server.CmServerServiceBuilder;
import com.cloudera.whirr.cm.server.CmServerServiceType;
import com.cloudera.whirr.cm.server.CmServerServiceTypeCms;
import com.cloudera.whirr.cm.server.impl.CmServerFactory;
import com.cloudera.whirr.cm.server.impl.CmServerLog;
import com.google.common.collect.ImmutableMap;

public abstract class BaseTestIntegration implements BaseTest {

  // The CM Server and database host/IP and port
  protected static String CM_IP = getSystemProperty("whirr.test.cm.ip", "31.222.137.179");
  protected static int CM_PORT = Integer.valueOf(getSystemProperty("whirr.test.cm.port", "7180"));

  // The CM Server config to be uploaded
  protected static Map<String, Map<String, String>> CM_CONFIG = ImmutableMap.of(CmServerServiceTypeCms.CM.getId(),
      (Map<String, String>) ImmutableMap.of("remote_parcel_repo_urls", getSystemProperty("whirr.test.cm.repos",
          "http://10.178.197.160/tmph3l7m2vv103/cloudera-repos/cdh4/parcels/4.2.0.10/" + ","
              + "http://10.178.197.160/tmph3l7m2vv103/cloudera-repos/impala/parcels/0.6.109/")));

  protected static CmServer serverBootstrap;
  protected static CmServerCluster cluster;
  protected static Set<String> hosts;
  protected static int clusterSize;

  @BeforeClass
  public static void initialiseCluster() throws CmServerException, IOException {
    cluster = new CmServerCluster();
    cluster.addServiceConfigurationAll(CM_CONFIG);
    Assert.assertNotNull(serverBootstrap = new CmServerFactory().getCmServer(CM_IP, CM_PORT, CmConstants.CM_USER,
        CmConstants.CM_PASSWORD, new CmServerLog.CmServerLogSysOut(LOG_TAG_CM_SERVER_API_TEST, false)));
    Assert.assertTrue(serverBootstrap.initialise(cluster));
    hosts = new HashSet<String>();
    for (CmServerService service : serverBootstrap.getServiceHosts()) {
      hosts.add(service.getIpInternal());
      cluster.addAgent(service);
    }
    Assert.assertFalse(hosts.isEmpty());
    Assert.assertTrue("Integration test cluster requires at least 4 nodes", hosts.size() >= 4);
    clusterSize = hosts.size();
    if (!hosts.remove(CM_IP)) {
      throw new CmServerException("Could not find integration server with public IP [" + CM_IP + "] in host IP list "
          + hosts);
    }
    String[] hostSlaves = hosts.toArray(new String[hosts.size()]);
    cluster.setServer(new CmServerServiceBuilder().ip(CM_IP).build());
    cluster.addServiceConfigurationAll(CmServerClusterInstance.getClusterConfiguration(
        CmServerClusterInstance.getConfiguration(null),new TreeSet<String>()));
    cluster.addService(new CmServerServiceBuilder().type(CmServerServiceType.HIVE_METASTORE).tag(CLUSTER_TAG)
        .qualifier("1").ip(CM_IP).build());
    cluster.addService(new CmServerServiceBuilder().type(CmServerServiceType.HUE_SERVER).tag(CLUSTER_TAG)
        .qualifier("1").ip(CM_IP).build());
    cluster.addService(new CmServerServiceBuilder().type(CmServerServiceType.HUE_BEESWAX_SERVER).tag(CLUSTER_TAG)
        .qualifier("1").ip(CM_IP).build());
    cluster.addService(new CmServerServiceBuilder().type(CmServerServiceType.OOZIE_SERVER).tag(CLUSTER_TAG)
        .qualifier("1").ip(CM_IP).build());
    cluster.addService(new CmServerServiceBuilder().type(CmServerServiceType.HBASE_MASTER).tag(CLUSTER_TAG)
        .qualifier("1").ip(CM_IP).build());
    cluster.addService(new CmServerServiceBuilder().type(CmServerServiceType.HDFS_NAMENODE).tag(CLUSTER_TAG)
        .qualifier("1").ip(CM_IP).build());
    cluster.addService(new CmServerServiceBuilder().type(CmServerServiceType.HDFS_SECONDARY_NAMENODE).tag(CLUSTER_TAG)
        .qualifier("1").ip(CM_IP).build());
    cluster.addService(new CmServerServiceBuilder().type(CmServerServiceType.MAPREDUCE_JOB_TRACKER).tag(CLUSTER_TAG)
        .qualifier("1").ip(CM_IP).build());
    cluster.addService(new CmServerServiceBuilder().type(CmServerServiceType.IMPALA_STATE_STORE).tag(CLUSTER_TAG)
        .qualifier("1").ip(CM_IP).build());
    for (int i = 0; i < hostSlaves.length; i++) {
      cluster.addService(new CmServerServiceBuilder().type(CmServerServiceType.HBASE_REGIONSERVER).tag(CLUSTER_TAG)
          .qualifier("" + (i + 1)).ip(hostSlaves[i]).build());
      cluster.addService(new CmServerServiceBuilder().type(CmServerServiceType.MAPREDUCE_TASK_TRACKER).tag(CLUSTER_TAG)
          .qualifier("" + (i + 1)).ip(hostSlaves[i]).build());
      cluster.addService(new CmServerServiceBuilder().type(CmServerServiceType.HDFS_DATANODE).tag(CLUSTER_TAG)
          .qualifier("" + (i + 1)).ip(hostSlaves[i]).build());
      cluster.addService(new CmServerServiceBuilder().type(CmServerServiceType.ZOOKEEPER_SERVER).tag(CLUSTER_TAG)
          .qualifier("" + (i + 1)).ip(hostSlaves[i]).build());
      cluster.addService(new CmServerServiceBuilder().type(CmServerServiceType.IMPALA_DAEMON).tag(CLUSTER_TAG)
          .qualifier("" + (i + 1)).ip(hostSlaves[i]).build());
      cluster.addService(new CmServerServiceBuilder().type(CmServerServiceType.FLUME_AGENT).tag(CLUSTER_TAG)
          .qualifier("" + (i + 1)).ip(hostSlaves[i]).build());
    }
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

  private static String getSystemProperty(String key, String value) {
    return System.getProperty(key) == null ? value : System.getProperty(key);
  }

}
