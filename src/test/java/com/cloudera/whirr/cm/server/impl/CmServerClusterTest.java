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
package com.cloudera.whirr.cm.server.impl;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cloudera.whirr.cm.server.CmServerCluster;
import com.cloudera.whirr.cm.server.CmServerException;
import com.cloudera.whirr.cm.server.CmServerService;
import com.cloudera.whirr.cm.server.CmServerService.CmServerServiceStatus;
import com.cloudera.whirr.cm.server.CmServerServiceType;

public class CmServerClusterTest extends BaseTestServerImpl {

  private CmServerCluster cluster;

  @Before
  public void setupCluster() throws CmServerException {
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
  public void testService() throws CmServerException {
    Assert.assertTrue(new CmServerService(CmServerServiceType.CLUSTER).equals(new CmServerService(
        CmServerServiceType.CLUSTER)));
    Assert.assertTrue(new CmServerService(CmServerServiceType.CLUSTER, CLUSTER_TAG).equals(new CmServerService(
        CmServerServiceType.CLUSTER, CLUSTER_TAG)));
    Assert.assertTrue(new CmServerService("host", null).equals(new CmServerService("host", null)));
    Assert.assertTrue(new CmServerService("host", "127.0.0.1").equals(new CmServerService("host", "127.0.0.1")));
    Assert.assertTrue(new CmServerService(CLUSTER_TAG + CmServerService.NAME_TOKEN_DELIM
        + CmServerServiceType.HDFS_NAMENODE.toString().toLowerCase() + CmServerService.NAME_TOKEN_DELIM
        + CmServerService.NAME_QUALIFIER_DEFAULT, "host", "127.0.0.1", null, CmServerServiceStatus.UNKNOWN)
        .equals(new CmServerService(CmServerServiceType.HDFS_NAMENODE, CLUSTER_TAG,
            CmServerService.NAME_QUALIFIER_DEFAULT, "host", "127.0.0.1")));
    boolean caught = false;
    try {
      Assert
          .assertTrue(new CmServerService("", "host", CmServerServiceStatus.UNKNOWN).equals(new CmServerService(
              CmServerServiceType.HDFS_NAMENODE, CLUSTER_TAG, CmServerService.NAME_QUALIFIER_DEFAULT, "host",
              "127.0.0.1")));
    } catch (IllegalArgumentException e) {
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      Assert.assertTrue(new CmServerService(CLUSTER_TAG + CmServerService.NAME_TOKEN_DELIM
          + CmServerService.NAME_TOKEN_DELIM + CmServerServiceType.HDFS_NAMENODE.toString().toLowerCase()
          + CmServerService.NAME_TOKEN_DELIM + CmServerService.NAME_QUALIFIER_DEFAULT, "host",
          CmServerServiceStatus.UNKNOWN).equals(new CmServerService(CmServerServiceType.HDFS_NAMENODE, CLUSTER_TAG
          + CmServerService.NAME_TOKEN_DELIM, CmServerService.NAME_QUALIFIER_DEFAULT, "host", "127.0.0.1")));
    } catch (IllegalArgumentException e) {
      caught = true;
    }
    Assert.assertTrue(caught);
  }

  @Test
  public void testIsEmpty() throws CmServerException {
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
  public void testAdd() throws CmServerException {
    boolean caught = false;
    try {
      cluster.add(CmServerServiceType.CLUSTER);
    } catch (CmServerException e) {
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      cluster.add(CmServerServiceType.HDFS);
    } catch (CmServerException e) {
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      cluster.add(new CmServerService(CmServerServiceType.CLUSTER, CLUSTER_TAG));
    } catch (CmServerException e) {
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      cluster.add(new CmServerService(CmServerServiceType.HDFS, CLUSTER_TAG));
    } catch (CmServerException e) {
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      cluster.add(CmServerServiceType.HDFS_NAMENODE);
    } catch (CmServerException e) {
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      cluster.add(new CmServerService(CmServerServiceType.HDFS_NAMENODE, CLUSTER_TAG));
    } catch (CmServerException e) {
      caught = true;
    }
    Assert.assertTrue(caught);
  }

  @Test
  public void testGetTypes() throws CmServerException {
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
    Assert.assertArrayEquals(new CmServerServiceType[] { CmServerServiceType.HDFS_NAMENODE,
        CmServerServiceType.HDFS_SECONDARY_NAMENODE, CmServerServiceType.HDFS_DATANODE,
        CmServerServiceType.HBASE_REGIONSERVER, CmServerServiceType.IMPALA_DAEMON },
        cluster.getServiceTypes(CmServerServiceType.CLUSTER).toArray());
    Assert.assertArrayEquals(new CmServerServiceType[] { CmServerServiceType.HDFS_NAMENODE,
        CmServerServiceType.HDFS_SECONDARY_NAMENODE, CmServerServiceType.HDFS_DATANODE },
        cluster.getServiceTypes(CmServerServiceType.HDFS).toArray());
  }

  @Test
  public void testGetServices() throws InterruptedException, IOException {
    Assert.assertEquals(8, cluster.getServices(CmServerServiceType.CLUSTER).size());
    Assert.assertEquals(6, cluster.getServices(CmServerServiceType.HDFS).size());
    Assert.assertEquals(1, cluster.getServices(CmServerServiceType.HDFS_NAMENODE).size());
    Assert.assertEquals(4, cluster.getServices(CmServerServiceType.HDFS_DATANODE).size());
    Assert.assertEquals(0, cluster.getServices(CmServerServiceType.CLIENT).size());
    int i = 0;
    CmServerServiceType[] serviceTypes = new CmServerServiceType[] { CmServerServiceType.HDFS_NAMENODE,
        CmServerServiceType.HDFS_SECONDARY_NAMENODE, CmServerServiceType.HDFS_DATANODE,
        CmServerServiceType.HDFS_DATANODE, CmServerServiceType.HDFS_DATANODE, CmServerServiceType.HDFS_DATANODE,
        CmServerServiceType.HBASE_REGIONSERVER, CmServerServiceType.IMPALA_DAEMON };
    for (CmServerService service : cluster.getServices(CmServerServiceType.CLUSTER)) {
      Assert.assertEquals(serviceTypes[i++], service.getType());
    }
    Assert.assertEquals(8, i);
    i = 0;
    for (CmServerService service : cluster.getServices(CmServerServiceType.HDFS)) {
      Assert.assertEquals(serviceTypes[i++], service.getType());
    }
    Assert.assertEquals(6, i);
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
