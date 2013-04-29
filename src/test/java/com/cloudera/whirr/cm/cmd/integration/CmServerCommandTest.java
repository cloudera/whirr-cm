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
package com.cloudera.whirr.cm.cmd.integration;

import java.util.Collections;

import org.apache.whirr.Cluster.Instance;
import org.junit.Assert;
import org.junit.Test;

import com.cloudera.whirr.cm.cmd.CmServerCleanClusterCommand;
import com.cloudera.whirr.cm.cmd.CmServerCreateServicesCommand;
import com.cloudera.whirr.cm.cmd.CmServerDestroyServicesCommand;
import com.cloudera.whirr.cm.cmd.CmServerDownloadConfigCommand;
import com.cloudera.whirr.cm.cmd.CmServerInitClusterCommand;
import com.cloudera.whirr.cm.cmd.CmServerListServicesCommand;
import com.cloudera.whirr.cm.server.CmServerServiceType;

public class CmServerCommandTest extends BaseTestIntegrationCommand {

  @Test
  public void testClean() throws Exception {
    Assert.assertTrue(true);
  }

  @Test
  public void testCommandInitCluster() throws Exception {
    Assert.assertEquals(0, new CmServerInitClusterCommand(null, null).run(specification,
        Collections.<Instance> emptySet(), cluster, command));
  }

  @Test
  public void testCommandCreateServices() throws Exception {
    Assert.assertEquals(0, new CmServerCreateServicesCommand(null, null).run(specification,
        Collections.<Instance> emptySet(), cluster, command));
  }

  @Test
  public void testCommandDestroyServices() throws Exception {
    Assert.assertTrue(serverBootstrap.configure(cluster));
    Assert.assertEquals(0, new CmServerDestroyServicesCommand(null, null).run(specification,
        Collections.<Instance> emptySet(), cluster, command));
  }

  @Test
  public void testCommandDownloadConfig() throws Exception {
    Assert.assertTrue(serverBootstrap.configure(cluster));
    cluster.setName(cluster.getServiceName(CmServerServiceType.CLUSTER));
    Assert.assertEquals(0, new CmServerDownloadConfigCommand(null, null).run(specification,
        Collections.<Instance> emptySet(), cluster, command));
  }

  @Test
  public void testListServices() throws Exception {
    Assert.assertTrue(serverBootstrap.configure(cluster));
    Assert.assertEquals(0, new CmServerListServicesCommand(null, null).run(specification,
        Collections.<Instance> emptySet(), cluster, command));
  }

  @Test
  public void testCommandLifecycle() throws Exception {
    Assert.assertEquals(0, new CmServerListServicesCommand(null, null).run(specification,
        Collections.<Instance> emptySet(), cluster, command));
    Assert.assertEquals(-1, new CmServerDestroyServicesCommand(null, null).run(specification,
        Collections.<Instance> emptySet(), cluster, command));
    Assert.assertEquals(-1, new CmServerDownloadConfigCommand(null, null).run(specification,
        Collections.<Instance> emptySet(), cluster, command));
    Assert.assertEquals(0, new CmServerCreateServicesCommand(null, null).run(specification,
        Collections.<Instance> emptySet(), cluster, command));
    Assert.assertEquals(-1, new CmServerCreateServicesCommand(null, null).run(specification,
        Collections.<Instance> emptySet(), cluster, command));
    Assert.assertEquals(0, new CmServerDownloadConfigCommand(null, null).run(specification,
        Collections.<Instance> emptySet(), cluster, command));
    Assert.assertEquals(0, new CmServerDestroyServicesCommand(null, null).run(specification,
        Collections.<Instance> emptySet(), cluster, command));
    Assert.assertEquals(0, new CmServerCreateServicesCommand(null, null).run(specification,
        Collections.<Instance> emptySet(), cluster, command));
    Assert.assertEquals(0, new CmServerListServicesCommand(null, null).run(specification,
        Collections.<Instance> emptySet(), cluster, command));
    Assert.assertEquals(0, new CmServerDestroyServicesCommand(null, null).run(specification,
        Collections.<Instance> emptySet(), cluster, command));
    Assert.assertEquals(-1, new CmServerDestroyServicesCommand(null, null).run(specification,
        Collections.<Instance> emptySet(), cluster, command));
  }

  @Test
  public void testCommandCleanCluster() throws Exception {
    Assert.assertEquals(0, new CmServerCleanClusterCommand(null, null).run(specification,
        Collections.<Instance> emptySet(), cluster, command));
  }

}
