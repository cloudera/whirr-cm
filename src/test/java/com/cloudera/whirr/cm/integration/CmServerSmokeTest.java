package com.cloudera.whirr.cm.integration;

import java.io.IOException;

import org.apache.commons.configuration.ConfigurationException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.cloudera.whirr.cm.BaseTestIntegration;
import com.cloudera.whirr.cm.CmConstants;
import com.cloudera.whirr.cm.server.CmServer;
import com.cloudera.whirr.cm.server.CmServerException;
import com.cloudera.whirr.cm.server.impl.CmServerFactory;
import com.cloudera.whirr.cm.server.impl.CmServerLog;

public class CmServerSmokeTest extends BaseTestIntegration {

  protected static CmServer server;

  @BeforeClass
  public static void initialiseServer() throws CmServerException {
    Assert.assertNotNull(server = new CmServerFactory().getCmServer(cluster.getServer().getIp(), cluster.getServer()
        .getIpInternal(), CM_PORT, CmConstants.CM_USER, CmConstants.CM_PASSWORD, new CmServerLog.CmServerLogSysOut(
        LOG_TAG_CM_SERVER_API, false)));
  }

  @Test
  public void testCleanClusterStart() throws ConfigurationException, IOException, InterruptedException,
      CmServerException {
    Assert.assertTrue(server.start(cluster));
  }

  @Test
  public void testCleanClusterLifecycle() throws ConfigurationException, IOException, InterruptedException,
      CmServerException {

    Assert.assertTrue(server.isProvisioned(cluster));
    Assert.assertFalse(server.isConfigured(cluster));
    Assert.assertFalse(server.isStarted(cluster));
    Assert.assertTrue(server.isStopped(cluster));

    Assert.assertTrue(server.configure(cluster));

    Assert.assertTrue(server.isProvisioned(cluster));
    Assert.assertTrue(server.isConfigured(cluster));
    Assert.assertFalse(server.isStarted(cluster));
    Assert.assertTrue(server.isStopped(cluster));

    Assert.assertTrue(server.start(cluster));

    Assert.assertTrue(server.isProvisioned(cluster));
    Assert.assertTrue(server.isConfigured(cluster));
    Assert.assertTrue(server.isStarted(cluster));
    Assert.assertFalse(server.isStopped(cluster));

    Assert.assertTrue(server.stop(cluster));

    Assert.assertTrue(server.isProvisioned(cluster));
    Assert.assertTrue(server.isConfigured(cluster));
    Assert.assertFalse(server.isStarted(cluster));
    Assert.assertTrue(server.isStopped(cluster));

  }

}