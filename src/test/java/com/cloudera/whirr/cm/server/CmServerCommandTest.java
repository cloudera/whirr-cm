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

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

public class CmServerCommandTest extends BaseTestServer {

  private CmServerCluster cluster;

  @Before
  public void setupCluster() throws CmServerException {
    cluster = new CmServerCluster();
    cluster.setServer("some-host");
    cluster.addAgent("some-host");
    cluster.addService(new CmServerServiceBuilder().type(CmServerServiceType.HDFS_NAMENODE).tag(CLUSTER_TAG)
        .qualifier("1").host("host-1").build());
    cluster.addService(new CmServerServiceBuilder().type(CmServerServiceType.HDFS_SECONDARY_NAMENODE).tag(CLUSTER_TAG)
        .qualifier("1").host("host-1").build());
    cluster.addService(new CmServerServiceBuilder().type(CmServerServiceType.HDFS_DATANODE).tag(CLUSTER_TAG)
        .qualifier("1").host("host-1").build());
  }

  @Test
  public void testGetValid1() throws CmServerException {
    Assert.assertNotNull(new CmServerBuilder().host("host-1").cluster(cluster)
        .client(DIR_CLIENT_CONFIG.getAbsolutePath()).command("client"));
  }

  @Test
  public void testGetValid2() throws CmServerException {
    Assert.assertNotNull(new CmServerBuilder().arguments(
        new String[] { "--host", "host-1", "--client", DIR_CLIENT_CONFIG.getAbsolutePath(), "--command", "client" })
        .cluster(cluster));
  }

  @Test(expected = CmServerException.class)
  public void testGetException1() throws CmServerException {
    Assert.assertNotNull(new CmServerBuilder().executeBoolean());
  }

  @Test(expected = CmServerException.class)
  public void testGetException2() throws CmServerException {
    Assert.assertNotNull(new CmServerBuilder().host(""));
  }

  @Test(expected = CmServerException.class)
  public void testGetException3() throws CmServerException {
    Assert.assertNotNull(new CmServerBuilder().host("").port(""));
  }

  @Test(expected = CmServerException.class)
  public void testGetException4() throws CmServerException {
    Assert.assertNotNull(new CmServerBuilder().host("host-1").port(""));
  }

  @Test(expected = CmServerException.class)
  public void testGetException5() throws CmServerException {
    Assert.assertNotNull(new CmServerBuilder().host("host-1").command(""));
  }

  @Test(expected = CmServerException.class)
  public void testGetException6() throws CmServerException {
    Assert.assertNotNull(new CmServerBuilder().host("host-1").executeBoolean());
  }

  @Test(expected = CmServerException.class)
  public void testGetException7() throws CmServerException {
    Assert.assertNotNull(new CmServerBuilder().host("host-1").command("").executeBoolean());
  }

  @Test(expected = CmServerException.class)
  public void testGetException8() throws CmServerException {
    Assert.assertNotNull(new CmServerBuilder().host("host-1").command("some-rubbish").executeBoolean());
  }

  @Test(expected = CmServerException.class)
  public void testGetException9() throws CmServerException {
    Assert.assertNotNull(new CmServerBuilder().host("host-1").command("client").executeBoolean());
  }

  @Test(expected = CmServerException.class)
  public void testGetException10() throws CmServerException {
    Assert.assertNotNull(new CmServerBuilder().host("host-1").cluster(cluster).executeBoolean());
  }

  @Test(expected = CmServerException.class)
  public void testGetException11() throws CmServerException {
    Assert.assertNotNull(new CmServerBuilder().host("host-1").client(DIR_CLIENT_CONFIG.getAbsolutePath())
        .executeBoolean());
  }

  @Test(expected = CmServerException.class)
  public void testGetException12() throws CmServerException {
    Assert.assertFalse(((Boolean) new CmServerBuilder()
        .arguments(new String[] { "--host", "host-1", "--client", DIR_CLIENT_CONFIG.getAbsolutePath(), "--command" })
        .cluster(cluster).executeBoolean()).booleanValue());
  }

  @Test(expected = CmServerException.class)
  public void testGetException13() throws CmServerException {
    Assert.assertFalse(((Boolean) new CmServerBuilder()
        .arguments(new String[] { "--host", "host-1", "--client", DIR_CLIENT_CONFIG.getAbsolutePath() })
        .cluster(cluster).executeBoolean()).booleanValue());
  }

  @Test
  public void testProcessArgumnetsValid1() throws CmServerException {
    Assert.assertEquals(0, CmServerBuilder.argumentsPreProcess(new String[] {}).size());
    Assert.assertEquals(1, CmServerBuilder.argumentsPreProcess(new String[] { "--client", "/tmp/whirr.properties" })
        .size());
    Assert.assertEquals(1, CmServerBuilder.argumentsPreProcess(new String[] { "--client", "/tmp/whirr.properties" })
        .size());
    Assert.assertEquals(2,
        CmServerBuilder.argumentsPreProcess(new String[] { "--client", "/tmp/whirr.properties", "--host", "host-1" })
            .size());
  }

  @Test(expected = CmServerException.class)
  public void testProcessArgumentsException1() throws CmServerException {
    Assert.assertEquals(1, CmServerBuilder.argumentsPreProcess(new String[] { "--client" }).size());
  }

  @Test(expected = CmServerException.class)
  public void testProcessArgumentsException2() throws CmServerException {
    Assert.assertEquals(2,
        CmServerBuilder.argumentsPreProcess(new String[] { "--client", "/tmp/whirr.properties", "host" }).size());
  }

  @Test(expected = CmServerException.class)
  public void testProcessArgumentsException3() throws CmServerException {
    Assert.assertEquals(1, CmServerBuilder.argumentsPreProcess(new String[] { "client", "/tmp/whirr.properties" })
        .size());
  }

  @Test(expected = CmServerException.class)
  public void testProcessArgumentsException4() throws CmServerException {
    Assert.assertEquals(1, CmServerBuilder.argumentsPreProcess(new String[] { "", "" }).size());
  }

  @Test(expected = CmServerException.class)
  public void testProcessArgumentsException5() throws CmServerException {
    Assert.assertEquals(2,
        CmServerBuilder.argumentsPreProcess(new String[] { "--client", "/tmp/whirr.properties", "host", "host-1" })
            .size());
  }

}
