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
    cluster.add(new CmServerService(CmServerServiceType.HDFS_NAMENODE, CLUSTER_TAG, "1", "host-1"));
    cluster.add(new CmServerService(CmServerServiceType.HDFS_SECONDARY_NAMENODE, CLUSTER_TAG, "1", "host-1"));
    cluster.add(new CmServerService(CmServerServiceType.HDFS_DATANODE, CLUSTER_TAG, "1", "host-1"));
  }

  @Test
  public void testGetValid1() throws CmServerException {
    Assert.assertFalse(((Boolean) CmServerCommand.get().host("host-1").cluster(cluster)
        .client(DIR_CLIENT_CONFIG.getAbsolutePath()).command("client").execute()).booleanValue());
  }

  @Test
  public void testGetValid2() throws CmServerException {
    Assert
        .assertFalse(((Boolean) CmServerCommand
            .get()
            .arguments(
                new String[] { "--host", "host-1", "--client", DIR_CLIENT_CONFIG.getAbsolutePath(), "--command",
                    "client" }).cluster(cluster).execute()).booleanValue());
  }

  @Test(expected = CmServerException.class)
  public void testGetException1() throws CmServerException {
    Assert.assertNotNull(CmServerCommand.get().execute());
  }

  @Test(expected = CmServerException.class)
  public void testGetException2() throws CmServerException {
    Assert.assertNotNull(CmServerCommand.get().host(""));
  }

  @Test(expected = CmServerException.class)
  public void testGetException3() throws CmServerException {
    Assert.assertNotNull(CmServerCommand.get().host("").port(""));
  }

  @Test(expected = CmServerException.class)
  public void testGetException4() throws CmServerException {
    Assert.assertNotNull(CmServerCommand.get().host("host-1").port(""));
  }

  @Test(expected = CmServerException.class)
  public void testGetException5() throws CmServerException {
    Assert.assertNotNull(CmServerCommand.get().host("host-1").command(""));
  }

  @Test(expected = CmServerException.class)
  public void testGetException6() throws CmServerException {
    Assert.assertNotNull(CmServerCommand.get().host("host-1").execute());
  }

  @Test(expected = CmServerException.class)
  public void testGetException7() throws CmServerException {
    Assert.assertNotNull(CmServerCommand.get().host("host-1").command("").execute());
  }

  @Test(expected = CmServerException.class)
  public void testGetException8() throws CmServerException {
    Assert.assertNotNull(CmServerCommand.get().host("host-1").command("some-rubbish").execute());
  }

  @Test(expected = CmServerException.class)
  public void testGetException9() throws CmServerException {
    Assert.assertNotNull(CmServerCommand.get().host("host-1").command("client").execute());
  }

  @Test(expected = CmServerException.class)
  public void testGetException10() throws CmServerException {
    Assert.assertNotNull(CmServerCommand.get().host("host-1").cluster(cluster).execute());
  }

  @Test(expected = CmServerException.class)
  public void testGetException11() throws CmServerException {
    Assert.assertNotNull(CmServerCommand.get().host("host-1").client(DIR_CLIENT_CONFIG.getAbsolutePath()).execute());
  }

  @Test(expected = CmServerException.class)
  public void testGetException12() throws CmServerException {
    Assert.assertFalse(((Boolean) CmServerCommand.get()
        .arguments(new String[] { "--host", "host-1", "--client", DIR_CLIENT_CONFIG.getAbsolutePath(), "--command" })
        .cluster(cluster).execute()).booleanValue());
  }

  @Test(expected = CmServerException.class)
  public void testGetException13() throws CmServerException {
    Assert.assertFalse(((Boolean) CmServerCommand.get()
        .arguments(new String[] { "--host", "host-1", "--client", DIR_CLIENT_CONFIG.getAbsolutePath() })
        .cluster(cluster).execute()).booleanValue());
  }

  @Test
  public void testProcessArgumnetsValid1() throws CmServerException {
    Assert.assertEquals(0, CmServerCommand.argumentsPreProcess(new String[] {}).size());
    Assert.assertEquals(1, CmServerCommand.argumentsPreProcess(new String[] { "--client", "/tmp/whirr.properties" })
        .size());
    Assert.assertEquals(1, CmServerCommand.argumentsPreProcess(new String[] { "--client", "/tmp/whirr.properties" })
        .size());
    Assert.assertEquals(2,
        CmServerCommand.argumentsPreProcess(new String[] { "--client", "/tmp/whirr.properties", "--host", "host-1" })
            .size());
  }

  @Test(expected = CmServerException.class)
  public void testProcessArgumentsException1() throws CmServerException {
    Assert.assertEquals(1, CmServerCommand.argumentsPreProcess(new String[] { "--client" }).size());
  }

  @Test(expected = CmServerException.class)
  public void testProcessArgumentsException2() throws CmServerException {
    Assert.assertEquals(2,
        CmServerCommand.argumentsPreProcess(new String[] { "--client", "/tmp/whirr.properties", "host" }).size());
  }

  @Test(expected = CmServerException.class)
  public void testProcessArgumentsException3() throws CmServerException {
    Assert.assertEquals(1, CmServerCommand.argumentsPreProcess(new String[] { "client", "/tmp/whirr.properties" })
        .size());
  }

  @Test(expected = CmServerException.class)
  public void testProcessArgumentsException4() throws CmServerException {
    Assert.assertEquals(1, CmServerCommand.argumentsPreProcess(new String[] { "", "" }).size());
  }

  @Test(expected = CmServerException.class)
  public void testProcessArgumentsException5() throws CmServerException {
    Assert.assertEquals(2,
        CmServerCommand.argumentsPreProcess(new String[] { "--client", "/tmp/whirr.properties", "host", "host-1" })
            .size());
  }

}
