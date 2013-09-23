package com.cloudera.whirr.cm.integration;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.cloudera.whirr.cm.server.CmServerException;
import com.cloudera.whirr.cm.server.CmServerService;
import com.cloudera.whirr.cm.server.CmServerService.CmServerServiceStatus;
import com.cloudera.whirr.cm.server.CmServerServiceBuilder;
import com.cloudera.whirr.cm.server.CmServerServiceType;

public abstract class CmServerClusterIT extends BaseITServer {

  @Test
  public void testGetServiceHost() throws CmServerException {
    List<CmServerService> serviceHosts = serverTest.getServiceHosts();
    Assert.assertEquals(7, serviceHosts.size());
    Assert.assertTrue(serviceHosts.size() > 2);
    Assert.assertEquals(serviceHosts.get(2).getHost(),
        serverTest.getServiceHost(new CmServerServiceBuilder().host(serviceHosts.get(2).getHost()).build()).getHost());
    Assert.assertEquals(serviceHosts.get(2).getHost(),
        serverTest.getServiceHost(new CmServerServiceBuilder().host(null).ip(serviceHosts.get(2).getIp()).build())
            .getHost());
    Assert.assertEquals(
        serviceHosts.get(2).getHost(),
        serverTest.getServiceHost(
            new CmServerServiceBuilder().host(null).ipInternal(serviceHosts.get(2).getIp())
                .status(CmServerServiceStatus.UNKNOWN).build()).getHost());
    Assert.assertEquals(
        serviceHosts.get(2).getHost(),
        serverTest.getServiceHost(
            new CmServerServiceBuilder().host("some-rubbish").ip("192.168.1.89")
                .ipInternal(serviceHosts.get(2).getIp()).status(CmServerServiceStatus.UNKNOWN).build()).getHost());
    Assert.assertEquals(
        null,
        serverTest.getServiceHost(new CmServerServiceBuilder().host("some-rubbish").ip("192.168.1.89")
            .ipInternal("192.168.1.90").status(CmServerServiceStatus.UNKNOWN).build()));
  }

  @Test
  public void testGetService() throws CmServerException {
    Assert.assertTrue(serverTest.getServices(cluster).isEmpty());
    Assert.assertTrue(serverTest.configure(cluster));
    Assert.assertFalse(serverTest.getServices(cluster).isEmpty());
    Assert.assertEquals(
        new CmServerServiceBuilder().type(CmServerServiceType.HDFS_NAMENODE)
            .tag(configuration.getString("whirr.cluster-name")).qualifier(CmServerService.NAME_QUALIFIER_DEFAULT)
            .build().getName(), serverTest.getService(cluster, CmServerServiceType.HDFS_NAMENODE).getName());
  }

  @Test
  public void testServiceLifecycle() throws Exception {

    Assert.assertTrue(serverTest.isProvisioned(cluster));
    Assert.assertFalse(serverTest.isConfigured(cluster));
    Assert.assertFalse(serverTest.isStarted(cluster));
    Assert.assertTrue(serverTest.isStopped(cluster));

    Assert.assertFalse(serverTest.stop(cluster));

    Assert.assertTrue(serverTest.isProvisioned(cluster));
    Assert.assertFalse(serverTest.isConfigured(cluster));
    Assert.assertFalse(serverTest.isStarted(cluster));
    Assert.assertTrue(serverTest.isStopped(cluster));

    Assert.assertFalse(serverTest.unconfigure(cluster));

    Assert.assertTrue(serverTest.isProvisioned(cluster));
    Assert.assertFalse(serverTest.isConfigured(cluster));
    Assert.assertFalse(serverTest.isStarted(cluster));
    Assert.assertTrue(serverTest.isStopped(cluster));

    Assert.assertFalse(serverTest.stop(cluster));

    Assert.assertTrue(serverTest.isProvisioned(cluster));
    Assert.assertFalse(serverTest.isConfigured(cluster));
    Assert.assertFalse(serverTest.isStarted(cluster));
    Assert.assertTrue(serverTest.isStopped(cluster));

    Assert.assertTrue(serverTest.start(cluster));

    Assert.assertTrue(serverTest.isProvisioned(cluster));
    Assert.assertTrue(serverTest.isConfigured(cluster));
    Assert.assertTrue(serverTest.isStarted(cluster));
    Assert.assertFalse(serverTest.isStopped(cluster));

    Assert.assertFalse(serverTest.start(cluster));

    Assert.assertTrue(serverTest.isProvisioned(cluster));
    Assert.assertTrue(serverTest.isConfigured(cluster));
    Assert.assertTrue(serverTest.isStarted(cluster));
    Assert.assertFalse(serverTest.isStopped(cluster));

    Assert.assertTrue(serverTest.unconfigure(cluster));

    Assert.assertTrue(serverTest.isProvisioned(cluster));
    Assert.assertFalse(serverTest.isConfigured(cluster));
    Assert.assertFalse(serverTest.isStarted(cluster));
    Assert.assertTrue(serverTest.isStopped(cluster));

    Assert.assertFalse(serverTest.unconfigure(cluster));

    Assert.assertTrue(serverTest.isProvisioned(cluster));
    Assert.assertFalse(serverTest.isConfigured(cluster));
    Assert.assertFalse(serverTest.isStarted(cluster));
    Assert.assertTrue(serverTest.isStopped(cluster));

    Assert.assertTrue(serverTest.configure(cluster));

    Assert.assertTrue(serverTest.isProvisioned(cluster));
    Assert.assertTrue(serverTest.isConfigured(cluster));
    Assert.assertFalse(serverTest.isStarted(cluster));
    Assert.assertTrue(serverTest.isStopped(cluster));

    Assert.assertFalse(serverTest.configure(cluster));

    Assert.assertTrue(serverTest.isProvisioned(cluster));
    Assert.assertTrue(serverTest.isConfigured(cluster));
    Assert.assertFalse(serverTest.isStarted(cluster));
    Assert.assertTrue(serverTest.isStopped(cluster));

    Assert.assertFalse(serverTest.stop(cluster));

    Assert.assertTrue(serverTest.isProvisioned(cluster));
    Assert.assertTrue(serverTest.isConfigured(cluster));
    Assert.assertFalse(serverTest.isStarted(cluster));
    Assert.assertTrue(serverTest.isStopped(cluster));

    Assert.assertTrue(serverTest.unprovision(cluster));

    Assert.assertFalse(serverTest.isProvisioned(cluster));
    Assert.assertFalse(serverTest.isConfigured(cluster));
    Assert.assertFalse(serverTest.isStarted(cluster));
    Assert.assertTrue(serverTest.isStopped(cluster));

  }

}