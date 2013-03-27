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
package com.cloudera.whirr.cm.api;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cloudera.whirr.cm.BaseTest;

public class CmServerClusterTest implements BaseTest {

  private static final String CLUSTER_TAG = "whirr_test";

  private CmServerCluster cluster;

  @Before
  public void setupCluster() throws CmServerApiException {
    cluster = new CmServerCluster();
    cluster.add(new CmServerService(CmServerServiceType.HDFS_DATANODE, CLUSTER_TAG, "2", "host-2"));
    cluster.add(new CmServerService(CmServerServiceType.HDFS_NAMENODE, CLUSTER_TAG, "1", "host-1"));
    cluster.add(new CmServerService(CmServerServiceType.HDFS_DATANODE, CLUSTER_TAG, "1", "host-1"));
    cluster.add(new CmServerService(CmServerServiceType.HDFS_SECONDARY_NAMENODE, CLUSTER_TAG, "1", "host-1"));
    cluster.add(new CmServerService(CmServerServiceType.HDFS_DATANODE, CLUSTER_TAG, "3", "host-3"));
    cluster.add(new CmServerService(CmServerServiceType.HDFS_DATANODE, CLUSTER_TAG, "4", "host-4"));
    cluster.add(new CmServerService(CmServerServiceType.HBASE_REGIONSERVER, CLUSTER_TAG, "1", "host-4"));
    cluster.add(new CmServerService(CmServerServiceType.IMPALA_DAEMON, CLUSTER_TAG, "1", "host-4"));
  }

  @Test
  public void testIsEmpty() throws CmServerApiException {
    Assert.assertFalse(cluster.isEmpty());
    Assert.assertFalse(cluster.isEmptyServices());
    cluster.clear();
    Assert.assertTrue(cluster.isEmpty());
    Assert.assertTrue(cluster.isEmptyServices());
    cluster.add(CmServerServiceType.HDFS_NAMENODE);
    Assert.assertFalse(cluster.isEmpty());
    Assert.assertTrue(cluster.isEmptyServices());
    cluster.add(new CmServerService(CmServerServiceType.HDFS_NAMENODE, CLUSTER_TAG, "1", "host-1"));
    Assert.assertFalse(cluster.isEmpty());
    Assert.assertFalse(cluster.isEmptyServices());
    cluster.clearServices();
    Assert.assertFalse(cluster.isEmpty());
    Assert.assertTrue(cluster.isEmptyServices());
    cluster.clear();
    Assert.assertTrue(cluster.isEmpty());
    Assert.assertTrue(cluster.isEmptyServices());
  }

  @Test
  public void testAdd() throws CmServerApiException {
    boolean caught = false;
    try {
      cluster.add(CmServerServiceType.CLUSTER);
    } catch (CmServerApiException e) {
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      cluster.add(CmServerServiceType.HDFS);
    } catch (CmServerApiException e) {
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      cluster.add(new CmServerService(CmServerServiceType.CLUSTER, CLUSTER_TAG));
    } catch (CmServerApiException e) {
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      cluster.add(new CmServerService(CmServerServiceType.HDFS, CLUSTER_TAG));
    } catch (CmServerApiException e) {
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      cluster.add(CmServerServiceType.HDFS_NAMENODE);
    } catch (CmServerApiException e) {
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      cluster.add(new CmServerService(CmServerServiceType.HDFS_NAMENODE, CLUSTER_TAG));
    } catch (CmServerApiException e) {
      caught = true;
    }
    Assert.assertTrue(caught);
  }

  @Test
  public void testGetTypes() throws CmServerApiException {
    Assert.assertArrayEquals(new CmServerServiceType[] { CmServerServiceType.HDFS, CmServerServiceType.HBASE,
        CmServerServiceType.IMPALA }, cluster.getServiceTypes().toArray());
  }

  @Test
  public void testGetServiceTypes() throws InterruptedException, IOException {
    Assert.assertEquals(5, cluster.getServiceTypes(CmServerServiceType.CLUSTER).size());
    Assert.assertEquals(3, cluster.getServiceTypes(CmServerServiceType.HDFS).size());
    Assert.assertEquals(1, cluster.getServiceTypes(CmServerServiceType.HDFS_NAMENODE).size());
    Assert.assertEquals(1, cluster.getServiceTypes(CmServerServiceType.HDFS_DATANODE).size());
    Assert.assertEquals(0, cluster.getServiceTypes(CmServerServiceType.CLIENT).size());
  }

  @Test
  public void testGetServices() throws InterruptedException, IOException {
    Assert.assertEquals(8, cluster.getServices(CmServerServiceType.CLUSTER).size());
    Assert.assertEquals(6, cluster.getServices(CmServerServiceType.HDFS).size());
    Assert.assertEquals(1, cluster.getServices(CmServerServiceType.HDFS_NAMENODE).size());
    Assert.assertEquals(4, cluster.getServices(CmServerServiceType.HDFS_DATANODE).size());
    Assert.assertEquals(0, cluster.getServices(CmServerServiceType.CLIENT).size());
  }

  @Test
  public void testGetService() throws InterruptedException, IOException {
    Assert.assertNotNull(cluster.getService(CmServerServiceType.CLUSTER));
    Assert.assertNotNull(cluster.getService(CmServerServiceType.HDFS));
    Assert.assertNotNull(cluster.getService(CmServerServiceType.HDFS_NAMENODE));
    Assert.assertNotNull(cluster.getService(CmServerServiceType.HDFS_DATANODE));
    Assert.assertNull(cluster.getService(CmServerServiceType.CLIENT));
  }

  @Test
  public void testGetNames() throws InterruptedException, IOException {
    Assert.assertEquals(CLUSTER_TAG + CmServerService.NAME_TOKEN_DELIM
        + CmServerServiceType.CLUSTER.toString().toLowerCase() + CmServerService.NAME_TOKEN_DELIM + "1",
        cluster.getServiceName(CmServerServiceType.CLUSTER));
    Assert.assertEquals(CLUSTER_TAG + CmServerService.NAME_TOKEN_DELIM
        + CmServerServiceType.HDFS.toString().toLowerCase() + CmServerService.NAME_TOKEN_DELIM + "1",
        cluster.getServiceName(CmServerServiceType.HDFS));
    Assert.assertEquals(CLUSTER_TAG + CmServerService.NAME_TOKEN_DELIM
        + CmServerServiceType.HDFS_NAMENODE.toString().toLowerCase() + CmServerService.NAME_TOKEN_DELIM + "1",
        cluster.getServiceName(CmServerServiceType.HDFS_NAMENODE));
    Assert.assertEquals(CLUSTER_TAG + CmServerService.NAME_TOKEN_DELIM
        + CmServerServiceType.HDFS_DATANODE.toString().toLowerCase() + CmServerService.NAME_TOKEN_DELIM + "1",
        cluster.getServiceName(CmServerServiceType.HDFS_DATANODE));
    Assert.assertEquals(CLUSTER_TAG + CmServerService.NAME_TOKEN_DELIM
        + CmServerServiceType.CLIENT.toString().toLowerCase() + CmServerService.NAME_TOKEN_DELIM + "1",
        cluster.getServiceName(CmServerServiceType.CLIENT));
    boolean caught = false;
    try {
      Assert.assertEquals(CLUSTER_TAG + CmServerService.NAME_TOKEN_DELIM
          + CmServerServiceType.CLUSTER.toString().toLowerCase() + CmServerService.NAME_TOKEN_DELIM + "1",
          new CmServerCluster().getServiceName(CmServerServiceType.CLUSTER));
    } catch (IOException e) {
      caught = true;
    }
    Assert.assertTrue(caught);
  }

}
