package com.cloudera.whirr.cm.integration;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.apache.whirr.cli.command.StartServicesCommand;
import org.apache.whirr.cli.command.StopServicesCommand;
import org.codehaus.plexus.util.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import com.cloudera.whirr.cm.cmd.BaseCommandCmServer;
import com.cloudera.whirr.cm.cmd.CmServerCleanClusterCommand;
import com.cloudera.whirr.cm.cmd.CmServerCreateServicesCommand;
import com.cloudera.whirr.cm.cmd.CmServerDestroyServicesCommand;
import com.cloudera.whirr.cm.cmd.CmServerDownloadConfigCommand;
import com.cloudera.whirr.cm.cmd.CmServerInitClusterCommand;
import com.cloudera.whirr.cm.cmd.CmServerListServicesCommand;

public class CmServerCommandIT extends CmServerClusterIT {

  private static final String FILTER_ROLES = "cm-server,cm-cdh-namenode,cm-cdh-jobtracker";

  private static final OptionSet OPTIONS_EMPTY = new OptionParser().parse("");
  private static final OptionSet OPTIONS_ROLES_FILTER;
  static {
    OptionParser optionParser = new OptionParser();
    optionParser.accepts(BaseCommandCmServer.OPTION_ROLES).withRequiredArg().ofType(String.class);
    OPTIONS_ROLES_FILTER = optionParser.parse("--" + BaseCommandCmServer.OPTION_ROLES, FILTER_ROLES);
  }

  @Test
  public void testInitCluster() throws Exception {
    Assert.assertEquals(0, new CmServerInitClusterCommand().run(specification, controller, OPTIONS_EMPTY));
    Assert.assertTrue(serverTestBootstrap.isProvisioned(cluster));
    Assert.assertFalse(serverTestBootstrap.isConfigured(cluster));
    Assert.assertFalse(serverTestBootstrap.isStarted(cluster));
    Assert.assertTrue(serverTestBootstrap.isStopped(cluster));
    Assert.assertEquals(0, serverTestBootstrap.getServices(cluster).getServiceTypes().size());
  }

  @Test
  public void testCreateServices() throws Exception {
    Assert.assertEquals(0, new CmServerCreateServicesCommand().run(specification, controller, OPTIONS_EMPTY));
    Assert.assertTrue(serverTestBootstrap.isProvisioned(cluster));
    Assert.assertTrue(serverTestBootstrap.isConfigured(cluster));
    Assert.assertFalse(serverTestBootstrap.isStarted(cluster));
    Assert.assertTrue(serverTestBootstrap.isStopped(cluster));
  }

  @Test
  public void testDownloadConfig() throws Exception {
    Assert.assertTrue(serverTestBootstrap.configure(cluster));
    Assert.assertEquals(0, new CmServerDownloadConfigCommand().run(specification, controller, OPTIONS_EMPTY));
    Assert.assertTrue(specification.getClusterDirectory().exists());
    Assert.assertTrue(specification.getClusterDirectory().list().length > 0);
  }

  @Test
  public void testListServices() throws Exception {
    Assert.assertTrue(serverTestBootstrap.configure(cluster));
    Assert.assertEquals(0, new CmServerListServicesCommand().run(specification, controller, OPTIONS_EMPTY));
  }

  @Test
  public void testStartServices() throws Exception {
    Assert.assertEquals(0, new StartServicesCommand().runLifecycleStep(specification, controller, OPTIONS_EMPTY));
    Assert.assertTrue(serverTestBootstrap.isProvisioned(cluster));
    Assert.assertTrue(serverTestBootstrap.isConfigured(cluster));
    Assert.assertTrue(serverTestBootstrap.isStarted(cluster));
    Assert.assertFalse(serverTestBootstrap.isStopped(cluster));
  }

  @Test
  public void testStopServices() throws Exception {
    Assert.assertTrue(serverTestBootstrap.start(cluster));
    Assert.assertEquals(0, new StopServicesCommand().runLifecycleStep(specification, controller, OPTIONS_EMPTY));
    Assert.assertTrue(serverTestBootstrap.isProvisioned(cluster));
    Assert.assertTrue(serverTestBootstrap.isConfigured(cluster));
    Assert.assertFalse(serverTestBootstrap.isStarted(cluster));
    Assert.assertTrue(serverTestBootstrap.isStopped(cluster));
  }

  @Test
  public void testDestroyServices() throws Exception {
    Assert.assertTrue(serverTestBootstrap.configure(cluster));
    Assert.assertEquals(0, new CmServerDestroyServicesCommand().run(specification, controller, OPTIONS_EMPTY));
    Assert.assertTrue(serverTestBootstrap.isProvisioned(cluster));
    Assert.assertFalse(serverTestBootstrap.isConfigured(cluster));
    Assert.assertFalse(serverTestBootstrap.isStarted(cluster));
    Assert.assertTrue(serverTestBootstrap.isStopped(cluster));
    Assert.assertEquals(0, serverTestBootstrap.getServices(cluster).getServiceTypes().size());
  }

  @Test
  public void testCleanCluster() throws Exception {
    Assert.assertEquals(0, new CmServerCleanClusterCommand().run(specification, controller, OPTIONS_EMPTY));
    Assert.assertFalse(serverTestBootstrap.isProvisioned(cluster));
    Assert.assertFalse(serverTestBootstrap.isConfigured(cluster));
    Assert.assertFalse(serverTestBootstrap.isStarted(cluster));
    Assert.assertTrue(serverTestBootstrap.isStopped(cluster));
    Assert.assertEquals(0, serverTestBootstrap.getServices(cluster).getServiceTypes().size());
  }

  @Test
  public void testServiceLifecycleFiltered() throws Exception {
    Assert.assertEquals(0, new CmServerCreateServicesCommand().run(specification, controller, OPTIONS_ROLES_FILTER));
    Assert.assertTrue(serverTestBootstrap.isProvisioned(cluster));
    Assert.assertFalse(serverTestBootstrap.isConfigured(cluster));
    Assert.assertFalse(serverTestBootstrap.isStarted(cluster));
    Assert.assertTrue(serverTestBootstrap.isStopped(cluster));
    Assert.assertEquals(StringUtils.countMatches(FILTER_ROLES, ","), serverTestBootstrap.getServices(cluster)
        .getServiceTypes().size());
    Assert.assertEquals(0, new CmServerDestroyServicesCommand().run(specification, controller, OPTIONS_ROLES_FILTER));
  }

}