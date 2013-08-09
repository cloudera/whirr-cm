package com.cloudera.whirr.cm.integration;

import java.util.Collections;

import org.apache.whirr.Cluster.Instance;
import org.junit.Assert;
import org.junit.Test;

import com.cloudera.whirr.cm.cmd.CmServerCreateServicesCommand;
import com.cloudera.whirr.cm.cmd.CmServerDestroyServicesCommand;
import com.cloudera.whirr.cm.cmd.CmServerDownloadConfigCommand;
import com.cloudera.whirr.cm.cmd.CmServerInitClusterCommand;
import com.cloudera.whirr.cm.cmd.CmServerListServicesCommand;

public class CmServerCommandIT extends CmServerClusterIT {

  @Test
  public void testInitCluster() throws Exception {
    Assert.assertEquals(0, new CmServerInitClusterCommand(null, null).run(specification,
        Collections.<Instance> emptySet(), cluster, serverTestBuilder));
  }

  @Test
  public void testCreateServices() throws Exception {
    Assert.assertEquals(0, new CmServerCreateServicesCommand(null, null).run(specification,
        Collections.<Instance> emptySet(), cluster, serverTestBuilder));
  }

  @Test
  public void testDownloadConfig() throws Exception {
    Assert.assertTrue(serverBootstrap.configure(cluster));
    Assert.assertEquals(0, new CmServerDownloadConfigCommand(null, null).run(specification,
        Collections.<Instance> emptySet(), cluster, serverTestBuilder));
    Assert.assertTrue(DIR_CLIENT_CONFIG.list().length > 0);
  }

  @Test
  public void testListServices() throws Exception {
    Assert.assertTrue(serverBootstrap.configure(cluster));
    Assert.assertEquals(0, new CmServerListServicesCommand(null, null).run(specification,
        Collections.<Instance> emptySet(), cluster, serverTestBuilder));
  }

  @Test
  public void testStartServices() throws Exception {
    // TODO
  }

  @Test
  public void testStopServices() throws Exception {
    Assert.assertTrue(serverBootstrap.start(cluster));
    // TODO
  }

  @Test
  public void testDestroyServices() throws Exception {
    Assert.assertTrue(serverBootstrap.configure(cluster));
    Assert.assertEquals(0, new CmServerDestroyServicesCommand(null, null).run(specification,
        Collections.<Instance> emptySet(), cluster, serverTestBuilder));
  }

  @Test
  public void testCleanCluster() throws Exception {
    // TODO
    // Assert.assertEquals(0, new CmServerCleanClusterCommand(null, null).run(specification,
    // Collections.<Instance> emptySet(), cluster, serverTestBuilder));
  }

}