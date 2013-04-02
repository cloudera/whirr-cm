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
package com.cloudera.whirr.cm.server.integration;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

import com.cloudera.whirr.cm.BaseTest;
import com.cloudera.whirr.cm.handler.CmServerHandler;
import com.cloudera.whirr.cm.server.CmServer;
import com.cloudera.whirr.cm.server.CmServerCluster;
import com.cloudera.whirr.cm.server.CmServerException;
import com.cloudera.whirr.cm.server.CmServerService;
import com.cloudera.whirr.cm.server.CmServerServiceType;
import com.cloudera.whirr.cm.server.impl.CmServerFactory;
import com.cloudera.whirr.cm.server.impl.CmServerLog;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class BaseTestIntegrationServer implements BaseTest {

  // The CM Server and database host and port
  protected static String CM_HOST = getSystemProperty("whirr.test.cm.host", "212-64-152-210.static.cloud-ips.co.uk");
  protected static int CM_PORT = Integer.valueOf(getSystemProperty("whirr.test.cm.port", "7180"));

  // The CM Server config to be uploaded
  protected static Map<String, String> CM_CONFIG = ImmutableMap.of("REMOTE_PARCEL_REPO_URLS",
      getSystemProperty("whirr.test.cm.repos",
          "http://10.178.197.160/tmph3l7m2vv103/cloudera-repos/cdh4/parcels/4.2.0.10/" + ","
              + "http://10.178.197.160/tmph3l7m2vv103/cloudera-repos/impala/parcels/0.6.109/"));

  protected static CmServer server;
  protected static CmServerCluster cluster;
  protected static int clusterSize;

  @BeforeClass
  public static void initialiseCluster() throws CmServerException {
    Assert.assertNotNull(server = CmServerFactory.getCmServer(CM_HOST, CM_PORT, CmServerHandler.CM_USER,
        CmServerHandler.CM_PASSWORD, new CmServerLog.CmServerLogSysOut()));
    Assert.assertTrue(server.initialise(CM_CONFIG).size() > 0);
    Set<String> hosts = new HashSet<String>();
    for (CmServerService service : server.getServiceHosts()) {
      hosts.add(service.getHost());
    }
    Assert.assertFalse(hosts.isEmpty());
    Assert.assertTrue("Integration test cluster requires at least 4 nodes", hosts.size() >= 4);
    clusterSize = hosts.size();
    hosts.remove(CM_HOST);
    String[] hostSlaves = hosts.toArray(new String[hosts.size()]);
    cluster = new CmServerCluster();
    cluster.setDataMounts(ImmutableSet.<String> builder().add("/data/" + CLUSTER_TAG).build());
    cluster.add(new CmServerService(CmServerServiceType.HIVE_METASTORE, CLUSTER_TAG, "1", CM_HOST));
    cluster.add(new CmServerService(CmServerServiceType.HUE_SERVER, CLUSTER_TAG, "1", CM_HOST));
    cluster.add(new CmServerService(CmServerServiceType.HUE_BEESWAX_SERVER, CLUSTER_TAG, "1", CM_HOST));
    cluster.add(new CmServerService(CmServerServiceType.OOZIE_SERVER, CLUSTER_TAG, "1", CM_HOST));
    cluster.add(new CmServerService(CmServerServiceType.HBASE_MASTER, CLUSTER_TAG, "1", CM_HOST));
    cluster.add(new CmServerService(CmServerServiceType.HDFS_NAMENODE, CLUSTER_TAG, "1", CM_HOST));
    cluster.add(new CmServerService(CmServerServiceType.HDFS_SECONDARY_NAMENODE, CLUSTER_TAG, "1", CM_HOST));
    cluster.add(new CmServerService(CmServerServiceType.MAPREDUCE_JOB_TRACKER, CLUSTER_TAG, "1", CM_HOST));
    cluster.add(new CmServerService(CmServerServiceType.IMPALA_STATE_STORE, CLUSTER_TAG, "1", CM_HOST));
    for (int i = 0; i < hostSlaves.length; i++) {
      cluster
          .add(new CmServerService(CmServerServiceType.HBASE_REGIONSERVER, CLUSTER_TAG, "" + (i + 1), hostSlaves[i]));
      cluster.add(new CmServerService(CmServerServiceType.MAPREDUCE_TASK_TRACKER, CLUSTER_TAG, "" + (i + 1),
          hostSlaves[i]));
      cluster.add(new CmServerService(CmServerServiceType.HDFS_DATANODE, CLUSTER_TAG, "" + (i + 1), hostSlaves[i]));
      cluster.add(new CmServerService(CmServerServiceType.ZOOKEEPER_SERVER, CLUSTER_TAG, "" + (i + 1), hostSlaves[i]));
      cluster.add(new CmServerService(CmServerServiceType.IMPALA_DAEMON, CLUSTER_TAG, "" + (i + 1), hostSlaves[i]));
    }
  }

  @Before
  public void provisionCluster() throws CmServerException {
    try {
      server.unconfigure(cluster);
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      server.provision(cluster);
    } catch (Exception e) {
      e.printStackTrace();
    }
    Assert.assertTrue(server.isProvisioned(cluster));
  }

  @After
  public void unconfigureCluster() throws CmServerException {
    try {
      server.unconfigure(cluster);
    } catch (Exception e) {
      e.printStackTrace();
    }
    Assert.assertFalse(server.isConfigured(cluster));
  }

  private static String getSystemProperty(String key, String value) {
    return System.getProperty(key) == null ? value : System.getProperty(key);
  }

}
