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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.cloudera.whirr.cm.server.CmServerException;
import com.cloudera.whirr.cm.server.CmServerService;
import com.cloudera.whirr.cm.server.CmServerServiceType;
import com.cloudera.whirr.cm.server.CmServerService.CmServerServiceStatus;

public class CmServerTest extends BaseTestIntegrationServer {

  @Test
  public void testClean() throws CmServerException {
    Assert.assertTrue(true);
  }

  @Test
  public void testGetServiceHost() throws CmServerException {
    List<CmServerService> serviceHosts = server.getServiceHosts();
    Assert.assertEquals(clusterSize, serviceHosts.size());
    Assert.assertTrue(serviceHosts.size() > 2);
    Assert.assertEquals(serviceHosts.get(2).getHost(),
        server.getServiceHost(new CmServerService(serviceHosts.get(2).getHost(), null)).getHost());
    Assert.assertEquals(serviceHosts.get(2).getHost(),
        server.getServiceHost(new CmServerService((String) null, serviceHosts.get(2).getIp())).getHost());
    Assert.assertEquals(
        serviceHosts.get(2).getHost(),
        server.getServiceHost(
            new CmServerService((String) null, null, serviceHosts.get(2).getIp(), CmServerServiceStatus.UNKNOWN))
            .getHost());
    Assert.assertEquals(
        serviceHosts.get(2).getHost(),
        server.getServiceHost(
            new CmServerService("some-rubbish", "192.168.1.89", serviceHosts.get(2).getIp(),
                CmServerServiceStatus.UNKNOWN)).getHost());
    boolean caught = false;
    try {
      Assert.assertEquals(
          serviceHosts.get(2).getHost(),
          server.getServiceHost(
              new CmServerService("some-rubbish", "192.168.1.89", "192.168.1.90", CmServerServiceStatus.UNKNOWN))
              .getHost());
    } catch (CmServerException e) {
      caught = true;
    }
    Assert.assertTrue(caught);
    Assert.assertEquals(
        serviceHosts.get(2).getHost(),
        server.getServiceHost(
            new CmServerService("some-rubbish", "192.168.1.89", serviceHosts.get(2).getIp(),
                CmServerServiceStatus.UNKNOWN), serviceHosts).getHost());
  }

  @Test
  public void testGetServiceConfig() throws CmServerException {
    Assert.assertFalse(server.getServiceConfigs(cluster, DIR_CLIENT_CONFIG));
    Assert.assertTrue(server.configure(cluster));
    Assert.assertTrue(server.getServiceConfigs(cluster, DIR_CLIENT_CONFIG));
  }

  @Test
  public void testGetService() throws CmServerException {
    Assert.assertTrue(server.getServices(cluster).isEmpty());
    Assert.assertTrue(server.configure(cluster));  
    Assert.assertFalse(server.getServices(cluster).isEmpty());
    Assert.assertEquals(new CmServerService(CmServerServiceType.HDFS_NAMENODE, CLUSTER_TAG,
        CmServerService.NAME_QUALIFIER_DEFAULT, server.getService(cluster, CmServerServiceType.HDFS_NAMENODE).getHost()),
        server.getService(cluster, CmServerServiceType.HDFS_NAMENODE));
  }

  @Test
  public void testInitialise() throws CmServerException {
    Assert.assertTrue(server.initialise(CM_CONFIG).size() > 0);
  }

  @Test
  public void testProvision() throws CmServerException {
    Assert.assertFalse(server.provision(cluster));
  }

  @Test
  public void testConfigure() throws CmServerException {
    Assert.assertTrue(server.configure(cluster));
  }

  @Test
  public void testStart() throws CmServerException {
    Assert.assertTrue(server.start(cluster));
  }

  @Test
  public void testLifecycle() throws CmServerException {
    Assert.assertTrue(server.isProvisioned(cluster));
    Assert.assertTrue(server.unprovision(cluster));
    Assert.assertFalse(server.isProvisioned(cluster));
    Assert.assertFalse(server.unprovision(cluster));
    Assert.assertFalse(server.isProvisioned(cluster));
    Assert.assertFalse(server.isConfigured(cluster));
    Assert.assertFalse(server.isStarted(cluster));
    Assert.assertTrue(server.isStopped(cluster));
    Assert.assertFalse(server.stop(cluster));
    Assert.assertFalse(server.unconfigure(cluster));
    Assert.assertTrue(server.configure(cluster));
    Assert.assertTrue(server.isProvisioned(cluster));
    Assert.assertTrue(server.isConfigured(cluster));
    Assert.assertFalse(server.isStarted(cluster));
    Assert.assertTrue(server.isStopped(cluster));
    Assert.assertFalse(server.configure(cluster));
    Assert.assertTrue(server.isProvisioned(cluster));
    Assert.assertTrue(server.isConfigured(cluster));
    Assert.assertFalse(server.isStarted(cluster));
    Assert.assertTrue(server.isStopped(cluster));
    Assert.assertTrue(server.unprovision(cluster));
    Assert.assertFalse(server.isProvisioned(cluster));
    Assert.assertFalse(server.isConfigured(cluster));
    Assert.assertFalse(server.isStarted(cluster));
    Assert.assertTrue(server.isStopped(cluster));
    Assert.assertTrue(server.start(cluster));
    Assert.assertTrue(server.isProvisioned(cluster));
    Assert.assertTrue(server.isConfigured(cluster));
    Assert.assertTrue(server.isStarted(cluster));
    Assert.assertFalse(server.isStopped(cluster));
    Assert.assertFalse(server.start(cluster));
    Assert.assertTrue(server.isStarted(cluster));
    Assert.assertFalse(server.isStopped(cluster));
    Assert.assertFalse(server.start(cluster));
    Assert.assertTrue(server.isStarted(cluster));
    Assert.assertFalse(server.isStopped(cluster));
    Assert.assertTrue(server.stop(cluster));
    Assert.assertFalse(server.isStarted(cluster));
    Assert.assertTrue(server.isStopped(cluster));
    Assert.assertFalse(server.stop(cluster));
    Assert.assertFalse(server.isStarted(cluster));
    Assert.assertTrue(server.isStopped(cluster));
    Assert.assertFalse(server.stop(cluster));
    Assert.assertFalse(server.isStarted(cluster));
    Assert.assertTrue(server.isStopped(cluster));
    Assert.assertFalse(server.stop(cluster));
    Assert.assertFalse(server.isStarted(cluster));
    Assert.assertTrue(server.isStopped(cluster));
    Assert.assertTrue(server.start(cluster));
    Assert.assertTrue(server.isStarted(cluster));
    Assert.assertFalse(server.isStopped(cluster));
    Assert.assertTrue(server.unconfigure(cluster));
    Assert.assertTrue(server.isProvisioned(cluster));
    Assert.assertFalse(server.isConfigured(cluster));
    Assert.assertFalse(server.isStarted(cluster));
    Assert.assertTrue(server.isStopped(cluster));
    Assert.assertFalse(server.unconfigure(cluster));
    Assert.assertTrue(server.isProvisioned(cluster));
    Assert.assertFalse(server.isConfigured(cluster));
    Assert.assertFalse(server.isStarted(cluster));
    Assert.assertTrue(server.isStopped(cluster));
  }

}
