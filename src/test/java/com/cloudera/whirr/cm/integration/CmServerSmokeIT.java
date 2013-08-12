package com.cloudera.whirr.cm.integration;

import org.junit.Assert;
import org.junit.Test;

public class CmServerSmokeIT extends BaseITServer {

  @Test
  public void testLaunchCluster() throws Exception {
    Assert.assertTrue(serverTest.isProvisioned(cluster));
    Assert.assertTrue(serverTest.isConfigured(cluster));
    Assert.assertTrue(serverTest.isStarted(cluster));
    Assert.assertFalse(serverTest.isStopped(cluster));
  }

  @Override
  protected boolean isClusterBootstrappedStatically() {
    return false;
  }

  @Override
  protected boolean isClusterBootstrappedAuto() {
    return true;
  }

}