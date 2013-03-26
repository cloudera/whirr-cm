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
package com.cloudera.whirr.cm.api.integration;

import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.apache.whirr.Cluster.Instance;

import org.jclouds.domain.Credentials;

import org.junit.BeforeClass;
import org.junit.Test;

import com.cloudera.api.model.ApiHost;

import com.cloudera.whirr.cm.BaseTest;
import com.cloudera.whirr.cm.CmServerHandler;
import com.cloudera.whirr.cm.api.CmServerApi;
import com.cloudera.whirr.cm.api.CmServerApiException;
import com.cloudera.whirr.cm.api.CmServerApiLog;
import com.cloudera.whirr.cm.api.CmServerCluster;
import com.cloudera.whirr.cm.api.CmServerService;
import com.cloudera.whirr.cm.api.CmServerServiceType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

public class CmServerApiTest implements BaseTest {

  private static final String CLUSTER_TAG = "whirr_test";

  // The CDH Master Node, the tests assume a single master provisioned by whirr-cm with all database
  // dependent roles (eg cm-cdh-hivemetastore)
  private static String CDH_MASTER_HOST = getSystemProperty("whirr.test.cdh.master.host",
      "37-188-113-33.static.cloud-ips.co.uk");

  // The IP of CDH Master Node, the tests assume a single master provisioned by whirr-cm with all database
  // dependent roles (eg cm-cdh-hivemetastore)
  private static String CDH_MASTER_IP = getSystemProperty("whirr.test.cdh.master.ip",
      "37.188.113.33");

  // The CM Server host and port
  private static String CM_HOST = getSystemProperty("whirr.test.cm.host", "37-188-116-147.static.cloud-ips.co.uk");
  private static int CM_PORT = Integer.valueOf(getSystemProperty("whirr.test.cm.port", "7180"));

  // The CM Server config to be uploaded
  private static Map<String, String> CM_CONFIG = ImmutableMap.of("REMOTE_PARCEL_REPO_URLS",
      getSystemProperty("whirr.test.cm.repos",
          "http://10.178.197.160/tmph3l7m2vv103/cloudera-repos/cdh4/parcels/4.2.0.10/" + ","
              + "http://10.178.197.160/tmph3l7m2vv103/cloudera-repos/impala/parcels/0.6.109/"));

  private static CmServerApi api;
  private static CmServerCluster cluster;

  @Test
  public void testConfigure() throws CmServerApiException {

    api.configure(cluster);
    api.unconfigure(cluster);

  }

  @Test
  public void testConfigureAndStartFirst() throws CmServerApiException {

    api.configure(cluster);
    api.startFirst(cluster);
    api.stop(cluster);
    api.unconfigure(cluster);

  }

  @Test
  public void testConfigureAndStartFirstAndStart() throws CmServerApiException {

    api.configure(cluster);
    api.startFirst(cluster);
    api.stop(cluster);
    api.start(cluster);
    api.stop(cluster);
    api.unconfigure(cluster);

  }

  @BeforeClass
  public static void provisionCluster() throws CmServerApiException {

    Assert.assertNotNull(api = new CmServerApi(CM_HOST, CM_PORT, CmServerHandler.CM_USER, CmServerHandler.CM_PASSWORD,
        new CmServerApiLog.CmServerApiLogSysOut()));
    Assert.assertTrue(api.initialise(CM_CONFIG).size() > 0);

    Set<ApiHost> hosts = api.hosts();
    Assert.assertNotNull(hosts);
    Assert.assertTrue("Integration test cluster requires at least 4 (+ CM Server) nodes", hosts.size() >= 5);
    hosts.remove(CM_HOST);
    hosts.remove(CDH_MASTER_HOST);
    ApiHost[] hostSlaves = hosts.toArray(new ApiHost[] {});

    cluster = new CmServerCluster();
    cluster.add(new CmServerService(CmServerServiceType.HIVE_METASTORE, CLUSTER_TAG, "1",
                                    dummyInstance(CDH_MASTER_HOST, CDH_MASTER_IP)));
    cluster.add(new CmServerService(CmServerServiceType.HUE_SERVER, CLUSTER_TAG, "1",
                                    dummyInstance(CDH_MASTER_HOST, CDH_MASTER_IP)));
    cluster.add(new CmServerService(CmServerServiceType.HUE_BEESWAX_SERVER, CLUSTER_TAG, "1",
                                    dummyInstance(CDH_MASTER_HOST, CDH_MASTER_IP)));
    cluster.add(new CmServerService(CmServerServiceType.OOZIE_SERVER, CLUSTER_TAG, "1",
                                    dummyInstance(CDH_MASTER_HOST, CDH_MASTER_IP)));
    cluster.add(new CmServerService(CmServerServiceType.HBASE_MASTER, CLUSTER_TAG, "1",
                                    dummyInstance(CDH_MASTER_HOST, CDH_MASTER_IP)));
    cluster.add(new CmServerService(CmServerServiceType.HDFS_NAMENODE, CLUSTER_TAG, "1",
                                    dummyInstance(CDH_MASTER_HOST, CDH_MASTER_IP)));
    cluster.add(new CmServerService(CmServerServiceType.HDFS_SECONDARY_NAMENODE, CLUSTER_TAG, "1",
                                    dummyInstance(CDH_MASTER_HOST, CDH_MASTER_IP)));
    cluster.add(new CmServerService(CmServerServiceType.MAPREDUCE_JOB_TRACKER, CLUSTER_TAG, "1",
                                    dummyInstance(CDH_MASTER_HOST, CDH_MASTER_IP)));
    // cluster.add(new CmServerService(CmServerServiceType.IMPALA_STATE_STORE, CLUSTER_TAG, "1", CDH_MASTER_HOST));
    for (int i = 0; i < hostSlaves.length; i++) {
      cluster.add(new CmServerService(CmServerServiceType.HBASE_REGIONSERVER, CLUSTER_TAG, "" + i + 1,
                                      dummyInstance(hostSlaves[i].getHostname(), hostSlaves[i].getIpAddress())));
      cluster.add(new CmServerService(CmServerServiceType.MAPREDUCE_TASK_TRACKER, CLUSTER_TAG, "" + i + 1,
                                      dummyInstance(hostSlaves[i].getHostname(), hostSlaves[i].getIpAddress())));
      cluster.add(new CmServerService(CmServerServiceType.HDFS_DATANODE, CLUSTER_TAG, "" + i + 1, 
                                      dummyInstance(hostSlaves[i].getHostname(), hostSlaves[i].getIpAddress())));
      cluster.add(new CmServerService(CmServerServiceType.ZOOKEEPER_SERVER, CLUSTER_TAG, "" + i + 1,
                                      dummyInstance(hostSlaves[i].getHostname(), hostSlaves[i].getIpAddress())));
      // cluster.add(new CmServerService(CmServerServiceType.IMPALA_DAEMON, CLUSTER_TAG, "" + i+1, hostSlaves[1]));
    }

    api.provision(cluster);
  }

  private static Instance dummyInstance(String name, String ip) {
    Credentials credentials = new Credentials("dummy", "dummy");
    return new Instance(credentials, Sets.newHashSet("role1", "role2"), ip, ip, "i-" + ip, null);
  }
  
  private static String getSystemProperty(String key, String value) {
    return System.getProperty(key) == null ? value : System.getProperty(key);
  }
}
