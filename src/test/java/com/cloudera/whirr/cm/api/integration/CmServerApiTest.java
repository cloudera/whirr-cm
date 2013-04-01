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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.cloudera.whirr.cm.BaseTest;
import com.cloudera.whirr.cm.CmServerHandler;
import com.cloudera.whirr.cm.api.CmServerApi;
import com.cloudera.whirr.cm.api.CmServerApiException;
import com.cloudera.whirr.cm.api.CmServerApiFactory;
import com.cloudera.whirr.cm.api.CmServerApiLog;
import com.cloudera.whirr.cm.api.CmServerCluster;
import com.cloudera.whirr.cm.api.CmServerService;
import com.cloudera.whirr.cm.api.CmServerServiceType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class CmServerApiTest implements BaseTest {

  private static final String CLUSTER_TAG = "whirr";

  // The CM Server and database host and port
  private static String CM_HOST = getSystemProperty("whirr.test.cm.host", "37-188-123-45.static.cloud-ips.co.uk");
  private static int CM_PORT = Integer.valueOf(getSystemProperty("whirr.test.cm.port", "7180"));

  // The CM Server config to be uploaded
  private static Map<String, String> CM_CONFIG = ImmutableMap.of("REMOTE_PARCEL_REPO_URLS",
      getSystemProperty("whirr.test.cm.repos",
          "http://10.178.197.160/tmph3l7m2vv103/cloudera-repos/cdh4/parcels/4.2.0.10/" + ","
              + "http://10.178.197.160/tmph3l7m2vv103/cloudera-repos/impala/parcels/0.6.109/"));

  private static CmServerApi api;
  private static CmServerCluster cluster;
  private static int clusterSize;

  @BeforeClass
  public static void initialiseCluster() throws CmServerApiException {

    Assert.assertNotNull(api = CmServerApiFactory.getCmServerApi(CM_HOST, CM_PORT, CmServerHandler.CM_USER, CmServerHandler.CM_PASSWORD,
        new CmServerApiLog.CmServerApiLogSysOut()));
    Assert.assertTrue(api.initialise(CM_CONFIG).size() > 0);

    Set<String> hosts = new HashSet<String>();
    for (CmServerService service : api.getServiceHosts()) {
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
  public void provisionCluster() throws CmServerApiException {

    try {
      api.unconfigure(cluster);
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      api.provision(cluster);
    } catch (Exception e) {
      e.printStackTrace();
    }
    Assert.assertTrue(api.isProvisioned(cluster));

  }

  @After
  public void unconfigureCluster() throws CmServerApiException {

    try {
      api.unconfigure(cluster);
    } catch (Exception e) {
      e.printStackTrace();
    }
    Assert.assertFalse(api.isConfigured(cluster));

  }

  @Test
  public void testClean() throws CmServerApiException {

  }

  @Test
  public void testGetServiceHost() throws CmServerApiException {

    List<CmServerService> serviceHosts = api.getServiceHosts();
    Assert.assertEquals(clusterSize, serviceHosts.size());
    Assert.assertTrue(serviceHosts.size() > 2);
    Assert.assertEquals(serviceHosts.get(2).getHost(),
        api.getServiceHost(new CmServerService(serviceHosts.get(2).getHost(), null)).getHost());
    Assert.assertEquals(serviceHosts.get(2).getHost(),
        api.getServiceHost(new CmServerService((String) null, serviceHosts.get(2).getIp())).getHost());
    Assert.assertEquals(serviceHosts.get(2).getHost(),
        api.getServiceHost(new CmServerService((String) null, null, serviceHosts.get(2).getIp())).getHost());
    Assert.assertEquals(serviceHosts.get(2).getHost(),
        api.getServiceHost(new CmServerService("some-rubbish", "192.168.1.89", serviceHosts.get(2).getIp())).getHost());
    boolean caught = false;
    try {
      Assert.assertEquals(serviceHosts.get(2).getHost(),
          api.getServiceHost(new CmServerService("some-rubbish", "192.168.1.89", "192.168.1.90")).getHost());
    } catch (CmServerApiException e) {
      caught = true;
    }
    Assert.assertTrue(caught);
    Assert.assertEquals(
        serviceHosts.get(2).getHost(),
        api.getServiceHost(new CmServerService("some-rubbish", "192.168.1.89", serviceHosts.get(2).getIp()),
            serviceHosts).getHost());

  }

  @Test
  public void testGetServiceConfig() throws CmServerApiException {

    Assert.assertFalse(api.getServiceConfigs(cluster, DIR_CONFIG));
    Assert.assertTrue(api.configure(cluster));
    Assert.assertTrue(api.getServiceConfigs(cluster, DIR_CONFIG));

  }

  @Test
  public void testGetService() throws CmServerApiException {

    Assert.assertEquals(0, api.getServices(cluster).size());
    Assert.assertTrue(api.configure(cluster));
    Assert.assertEquals(34, api.getServices(cluster).size());
    Assert.assertEquals(new CmServerService(CmServerServiceType.HDFS_NAMENODE, CLUSTER_TAG,
        CmServerService.NAME_QUALIFIER_DEFAULT, api.getService(cluster, CmServerServiceType.HDFS_NAMENODE).getHost()),
        api.getService(cluster, CmServerServiceType.HDFS_NAMENODE));

  }

  @Test
  public void testInitialise() throws CmServerApiException {

    Assert.assertEquals("whirr_cluster_1", api.getName(cluster));
    Assert.assertTrue(api.initialise(CM_CONFIG).size() > 0);

  }

  @Test
  public void testProvision() throws CmServerApiException {

    Assert.assertFalse(api.provision(cluster));

  }

  @Test
  public void testConfigure() throws CmServerApiException {

    Assert.assertTrue(api.configure(cluster));

  }

  @Test
  public void testStart() throws CmServerApiException {

    Assert.assertTrue(api.start(cluster));

  }

  @Test
  public void testLifecycle() throws CmServerApiException {

    Assert.assertTrue(api.isProvisioned(cluster));
    Assert.assertTrue(api.unprovision(cluster));
    Assert.assertFalse(api.isProvisioned(cluster));
    Assert.assertFalse(api.unprovision(cluster));
    Assert.assertFalse(api.isProvisioned(cluster));
    Assert.assertFalse(api.isConfigured(cluster));
    Assert.assertFalse(api.isStarted(cluster));
    Assert.assertTrue(api.isStopped(cluster));
    Assert.assertFalse(api.stop(cluster));
    Assert.assertFalse(api.unconfigure(cluster));
    Assert.assertTrue(api.configure(cluster));
    Assert.assertTrue(api.isProvisioned(cluster));
    Assert.assertTrue(api.isConfigured(cluster));
    Assert.assertFalse(api.isStarted(cluster));
    Assert.assertTrue(api.isStopped(cluster));
    Assert.assertFalse(api.configure(cluster));
    Assert.assertTrue(api.isProvisioned(cluster));
    Assert.assertTrue(api.isConfigured(cluster));
    Assert.assertFalse(api.isStarted(cluster));
    Assert.assertTrue(api.isStopped(cluster));
    Assert.assertTrue(api.unprovision(cluster));
    Assert.assertFalse(api.isProvisioned(cluster));
    Assert.assertFalse(api.isConfigured(cluster));
    Assert.assertFalse(api.isStarted(cluster));
    Assert.assertTrue(api.isStopped(cluster));
    Assert.assertTrue(api.start(cluster));
    Assert.assertTrue(api.isProvisioned(cluster));
    Assert.assertTrue(api.isConfigured(cluster));
    Assert.assertTrue(api.isStarted(cluster));
    Assert.assertFalse(api.isStopped(cluster));
    Assert.assertFalse(api.start(cluster));
    Assert.assertTrue(api.isStarted(cluster));
    Assert.assertFalse(api.isStopped(cluster));
    Assert.assertFalse(api.start(cluster));
    Assert.assertTrue(api.isStarted(cluster));
    Assert.assertFalse(api.isStopped(cluster));
    Assert.assertTrue(api.stop(cluster));
    Assert.assertFalse(api.isStarted(cluster));
    Assert.assertTrue(api.isStopped(cluster));
    Assert.assertFalse(api.stop(cluster));
    Assert.assertFalse(api.isStarted(cluster));
    Assert.assertTrue(api.isStopped(cluster));
    Assert.assertFalse(api.stop(cluster));
    Assert.assertFalse(api.isStarted(cluster));
    Assert.assertTrue(api.isStopped(cluster));
    Assert.assertFalse(api.stop(cluster));
    Assert.assertFalse(api.isStarted(cluster));
    Assert.assertTrue(api.isStopped(cluster));
    Assert.assertTrue(api.start(cluster));
    Assert.assertTrue(api.isStarted(cluster));
    Assert.assertFalse(api.isStopped(cluster));
    Assert.assertTrue(api.unconfigure(cluster));
    Assert.assertTrue(api.isProvisioned(cluster));
    Assert.assertFalse(api.isConfigured(cluster));
    Assert.assertFalse(api.isStarted(cluster));
    Assert.assertTrue(api.isStopped(cluster));
    Assert.assertFalse(api.unconfigure(cluster));
    Assert.assertTrue(api.isProvisioned(cluster));
    Assert.assertFalse(api.isConfigured(cluster));
    Assert.assertFalse(api.isStarted(cluster));
    Assert.assertTrue(api.isStopped(cluster));

  }

  private static String getSystemProperty(String key, String value) {
    return System.getProperty(key) == null ? value : System.getProperty(key);
  }
}
