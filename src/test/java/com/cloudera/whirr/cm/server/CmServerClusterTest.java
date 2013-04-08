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
package com.cloudera.whirr.cm.server;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cloudera.whirr.cm.server.CmServerService.CmServerServiceStatus;

public class CmServerClusterTest extends BaseTestServer {

  private CmServerCluster cluster;

  @Before
  public void setupCluster() throws CmServerException {
    cluster = new CmServerCluster();
    cluster.setServer("some-host");
    cluster.addAgent("some-host");
    cluster.addService(new CmServerServiceBuilder().type(CmServerServiceType.HDFS_DATANODE).tag(CLUSTER_TAG)
        .qualifier("2").host("host-2").build());
    cluster.addService(new CmServerServiceBuilder().type(CmServerServiceType.HDFS_NAMENODE).tag(CLUSTER_TAG)
        .qualifier("1").host("host-1").build());
    cluster.addService(new CmServerServiceBuilder().type(CmServerServiceType.HDFS_DATANODE).tag(CLUSTER_TAG)
        .qualifier("1").host("host-1").build());
    cluster.addService(new CmServerServiceBuilder().type(CmServerServiceType.HDFS_SECONDARY_NAMENODE).tag(CLUSTER_TAG)
        .qualifier("1").host("host-1").build());
    cluster.addService(new CmServerServiceBuilder().type(CmServerServiceType.HDFS_DATANODE).tag(CLUSTER_TAG)
        .qualifier("3").host("host-3").build());
    cluster.addService(new CmServerServiceBuilder().type(CmServerServiceType.HDFS_DATANODE).tag(CLUSTER_TAG)
        .qualifier("4").host("host-4").build());
    cluster.addService(new CmServerServiceBuilder().type(CmServerServiceType.HBASE_REGIONSERVER).tag(CLUSTER_TAG)
        .qualifier("1").host("host-4").build());
    cluster.addService(new CmServerServiceBuilder().type(CmServerServiceType.IMPALA_DAEMON).tag(CLUSTER_TAG)
        .qualifier("1").host("host-4").build());
  }

  @Test
  public void testService() throws CmServerException {
    Assert.assertTrue(new CmServerServiceBuilder().type(CmServerServiceType.CLUSTER).build()
        .equals(new CmServerServiceBuilder().build()));
    Assert.assertTrue(new CmServerServiceBuilder().type(CmServerServiceType.CLUSTER).tag(CLUSTER_TAG).build()
        .equals(new CmServerServiceBuilder().tag(CLUSTER_TAG).build()));
    Assert.assertTrue(new CmServerServiceBuilder().host("host").ip(null).build()
        .equals(new CmServerServiceBuilder().host("host").build()));
    Assert.assertTrue(new CmServerServiceBuilder()
        .name(
            CLUSTER_TAG + CmServerService.NAME_TOKEN_DELIM + CmServerServiceType.HDFS_NAMENODE.toString().toLowerCase()
                + CmServerService.NAME_TOKEN_DELIM + CmServerService.NAME_QUALIFIER_DEFAULT)
        .host("host")
        .ip("127.0.0.1")
        .status(CmServerServiceStatus.UNKNOWN)
        .build()
        .equals(
            new CmServerServiceBuilder().type(CmServerServiceType.HDFS_NAMENODE).tag(CLUSTER_TAG)
                .qualifier(CmServerService.NAME_QUALIFIER_DEFAULT).host("host").ip("127.0.0.1").build()));
    boolean caught = false;
    try {
      Assert.assertTrue(new CmServerServiceBuilder()
          .name("")
          .host("host")
          .status(CmServerServiceStatus.UNKNOWN)
          .build()
          .equals(
              new CmServerServiceBuilder().type(CmServerServiceType.HDFS_NAMENODE).tag(CLUSTER_TAG)
                  .qualifier(CmServerService.NAME_QUALIFIER_DEFAULT).host("host").ip("127.0.0.1").build()));
    } catch (IllegalArgumentException e) {
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      Assert.assertTrue(new CmServerServiceBuilder()
          .name(
              CLUSTER_TAG + CmServerService.NAME_TOKEN_DELIM + CmServerService.NAME_TOKEN_DELIM
                  + CmServerServiceType.HDFS_NAMENODE.toString().toLowerCase() + CmServerService.NAME_TOKEN_DELIM
                  + CmServerService.NAME_QUALIFIER_DEFAULT)
          .host("host")
          .status(CmServerServiceStatus.UNKNOWN)
          .build()
          .equals(
              new CmServerServiceBuilder().type(CmServerServiceType.HDFS_NAMENODE)
                  .tag(CLUSTER_TAG + CmServerService.NAME_TOKEN_DELIM)
                  .qualifier(CmServerService.NAME_QUALIFIER_DEFAULT).host("host").ip("127.0.0.1").build()));
    } catch (IllegalArgumentException e) {
      caught = true;
    }
    Assert.assertTrue(caught);
  }

  @Test
  public void testIsEmpty() throws CmServerException {
    CmServerCluster cluster = new CmServerCluster();
    Assert.assertTrue(cluster.isEmpty());
    cluster.addService(new CmServerServiceBuilder().type(CmServerServiceType.HDFS_NAMENODE).tag(CLUSTER_TAG)
        .qualifier("1").host("host-1").build());
    Assert.assertTrue(cluster.isEmpty());
    cluster.setServer("some-host");
    Assert.assertTrue(cluster.isEmpty());
    cluster.addAgent("some-host");
    Assert.assertFalse(cluster.isEmpty());
  }

  @Test
  public void testAdd() throws CmServerException {
    boolean caught = false;
    try {
      cluster.addServiceType(CmServerServiceType.CLUSTER);
    } catch (CmServerException e) {
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      cluster.addServiceType(CmServerServiceType.HDFS);
    } catch (CmServerException e) {
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      cluster.addService(new CmServerServiceBuilder().type(CmServerServiceType.CLUSTER).tag(CLUSTER_TAG).build());
    } catch (CmServerException e) {
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      cluster.addService(new CmServerServiceBuilder().type(CmServerServiceType.HDFS).tag(CLUSTER_TAG).build());
    } catch (CmServerException e) {
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      cluster.addServiceType(CmServerServiceType.HDFS_NAMENODE);
    } catch (CmServerException e) {
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      cluster.addService(new CmServerServiceBuilder().type(CmServerServiceType.HDFS_NAMENODE).tag(CLUSTER_TAG).build());
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
