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

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.cloudera.whirr.cm.server.CmServerCommand;
import com.cloudera.whirr.cm.server.CmServerException;

public class CmServerCommandTest extends BaseTestIntegrationServer {

  @Override
  @Before
  public void provisionCluster() throws CmServerException {
    super.provisionCluster();
    Assert.assertTrue(server.configure(cluster));
    Assert.assertTrue(server.isConfigured(cluster));
  }

  @Test
  public void testClean() throws CmServerException {
    Assert.assertTrue(true);
  }

  @Test
  public void testClient() throws CmServerException, InterruptedException {
    Assert.assertTrue(CmServerCommand.get().host(CM_HOST).cluster(cluster).client(DIR_CLIENT_CONFIG.getAbsolutePath())
        .command("client").execute());
  }

  @Test
  public void testHosts() throws CmServerException, InterruptedException {
    Assert.assertTrue(CmServerCommand.get().host(CM_HOST).cluster(cluster).command("hosts").execute());
  }

  @Test
  public void testServices() throws CmServerException, InterruptedException {
    Assert.assertTrue(CmServerCommand.get().host(CM_HOST).cluster(cluster).command("hosts").execute());
    Assert.assertTrue(CmServerCommand.get().host(CM_HOST).cluster(cluster).command("services").execute());
  }
  
}
