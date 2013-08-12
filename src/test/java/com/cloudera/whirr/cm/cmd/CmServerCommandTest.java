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
package com.cloudera.whirr.cm.cmd;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import org.apache.whirr.Cluster.Instance;
import org.apache.whirr.ClusterSpec;
import org.apache.whirr.command.Command;
import org.apache.whirr.util.KeyPair;
import org.junit.Assert;
import org.junit.Test;

import com.cloudera.whirr.cm.handler.CmAgentHandler;
import com.cloudera.whirr.cm.handler.CmServerHandler;
import com.cloudera.whirr.cm.handler.cdh.CmCdhHdfsDataNodeHandler;
import com.cloudera.whirr.cm.handler.cdh.CmCdhHdfsNameNodeHandler;
import com.cloudera.whirr.cm.handler.cdh.CmCdhHdfsSecondaryNameNodeHandler;
import com.cloudera.whirr.cm.server.CmServerBuilder;
import com.cloudera.whirr.cm.server.CmServerCluster;
import com.cloudera.whirr.cm.server.CmServerServiceType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class CmServerCommandTest extends BaseTestCommand {

  private static final List<Class<? extends BaseCommand>> COMMANDS = ImmutableList.<Class<? extends BaseCommand>> of(
      CmServerInitClusterCommand.class, CmServerCreateServicesCommand.class, CmServerDownloadConfigCommand.class,
      CmServerListServicesCommand.class, CmServerDestroyServicesCommand.class, CmServerCleanClusterCommand.class);

  @Test
  public void testCommandServiceLoader() throws Exception {

    List<Class<? extends BaseCommand>> commands = new ArrayList<Class<? extends BaseCommand>>();
    for (Command command : ServiceLoader.load(Command.class)) {
      if (command instanceof BaseCommand) {
        commands.add(((BaseCommand) command).getClass());
      }
    }
    Assert.assertArrayEquals(COMMANDS.toArray(), commands.toArray());

  }

  @Test
  public void testBaseCommandCmServerNoRoles() throws Exception {

    initialiseCluster(new String[0][0]);

    BaseCommandCmServer clusterCommand = new BaseCommandCmServer("name", "description", factory, stateStoreFactory) {
      @Override
      public int run(ClusterSpec specification, Set<Instance> instances, CmServerCluster cluster,
          CmServerBuilder serverCommand) throws Exception {
        return 0;
      }
    };

    ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
    PrintStream err = new PrintStream(errBytes);

    Map<String, File> keys = KeyPair.generateTemporaryFiles();
    int rc = clusterCommand.run(null, System.out, err, Lists.newArrayList("--instance-templates", "1 noop",
        "--service-name", "test-service", "--cluster-name", "test-cluster", "--cluster-user", "whirr", "--identity",
        "myusername", "--quiet", "--private-key-file", keys.get("private").getAbsolutePath()));

    assertThat(rc, is(-1));
    assertThat(errBytes.toString(), containsString("Could not find cm-server."));
    verify(factory).create("test-service");
  }

  @Test
  public void testBaseCommandCmServerNoCmCdhRoles() throws Exception {

    final int rolesNumberCmCdh = initialiseCluster(new String[][] { { CmServerHandler.ROLE, CmAgentHandler.ROLE },
        { CmAgentHandler.ROLE } });

    BaseCommandCmServer clusterCommand = new BaseCommandCmServer("name", "description", factory, stateStoreFactory) {
      @Override
      public int run(ClusterSpec specification, Set<Instance> instances, CmServerCluster cluster,
          CmServerBuilder serverCommand) throws Exception {
        assertThat(cluster.getServices(CmServerServiceType.CLUSTER).size(), is(rolesNumberCmCdh));
        assertThat(true, is(serverCommand != null));
        return 0;
      }
    };

    Map<String, File> keys = KeyPair.generateTemporaryFiles();
    int rc = clusterCommand.run(null, System.out, System.err, Lists.newArrayList("--instance-templates", "1 noop",
        "--service-name", "test-service", "--cluster-name", "test-cluster", "--cluster-user", "whirr", "--identity",
        "myusername", "--quiet", "--private-key-file", keys.get("private").getAbsolutePath()));

    assertThat(rc, is(0));
    verify(factory).create("test-service");

  }

  @Test
  public void testBaseCommandCmServerHdfsRoles() throws Exception {

    final int rolesNumberCmCdh = initialiseCluster(new String[][] {
        { CmServerHandler.ROLE, CmAgentHandler.ROLE, CmCdhHdfsNameNodeHandler.ROLE,
            CmCdhHdfsSecondaryNameNodeHandler.ROLE }, { CmAgentHandler.ROLE, CmCdhHdfsDataNodeHandler.ROLE } });

    BaseCommandCmServer clusterCommand = new BaseCommandCmServer("name", "description", factory, stateStoreFactory) {
      @Override
      public int run(ClusterSpec specification, Set<Instance> instances, CmServerCluster cluster,
          CmServerBuilder serverCommand) throws Exception {
        assertThat(cluster.getServices(CmServerServiceType.CLUSTER).size(), is(rolesNumberCmCdh));
        assertThat(true, is(serverCommand != null));
        return 0;
      }
    };

    Map<String, File> keys = KeyPair.generateTemporaryFiles();
    int rc = clusterCommand.run(null, System.out, System.err, Lists.newArrayList("--instance-templates", "1 noop",
        "--service-name", "test-service", "--cluster-name", "test-cluster", "--cluster-user", "whirr", "--identity",
        "myusername", "--quiet", "--private-key-file", keys.get("private").getAbsolutePath()));

    assertThat(rc, is(0));
    verify(factory).create("test-service");

  }

  @Test
  public void testBaseCommandCmServerHdfsRolesClusterName() throws Exception {

    final String name = "My Cluster - Yay for me!";
    final int rolesNumberCmCdh = initialiseCluster(new String[][] {
        { CmServerHandler.ROLE, CmAgentHandler.ROLE, CmCdhHdfsNameNodeHandler.ROLE,
            CmCdhHdfsSecondaryNameNodeHandler.ROLE }, { CmAgentHandler.ROLE, CmCdhHdfsDataNodeHandler.ROLE } });

    BaseCommandCmServer clusterCommand = new BaseCommandCmServer("name", "description", factory, stateStoreFactory) {
      @Override
      public int run(ClusterSpec specification, Set<Instance> instances, CmServerCluster cluster,
          CmServerBuilder serverCommand) throws Exception {
        assertThat(cluster.getServiceName(CmServerServiceType.CLUSTER), is(name));
        assertThat(cluster.getServices(CmServerServiceType.CLUSTER).size(), is(rolesNumberCmCdh));
        assertThat(true, is(serverCommand != null));
        return 0;
      }
    };

    Map<String, File> keys = KeyPair.generateTemporaryFiles();
    int rc = clusterCommand.run(null, System.out, System.err, Lists
        .newArrayList("--instance-templates", "1 noop", "--service-name", "test-service", "--cluster-name",
            "test-cluster", "--cluster-user", "whirr", "--identity", "myusername", "--cm-cluster-name", name,
            "--quiet", "--private-key-file", keys.get("private").getAbsolutePath()));

    assertThat(rc, is(0));
    verify(factory).create("test-service");

  }

  @Test
  public void testBaseCommandCmServerHdfsRolesFilteredEroneous() throws Exception {

    initialiseCluster(new String[][] {
        { CmServerHandler.ROLE, CmAgentHandler.ROLE, CmCdhHdfsNameNodeHandler.ROLE,
            CmCdhHdfsSecondaryNameNodeHandler.ROLE }, { CmAgentHandler.ROLE, CmCdhHdfsDataNodeHandler.ROLE } });

    BaseCommandCmServer clusterCommand = new BaseCommandCmServer("name", "description", factory, stateStoreFactory) {
      @Override
      public int run(ClusterSpec specification, Set<Instance> instances, CmServerCluster cluster,
          CmServerBuilder serverCommand) throws Exception {
        assertThat(cluster.getServices(CmServerServiceType.CLUSTER).size(), is(1));
        assertThat(true, is(serverCommand != null));
        return 0;
      }
    };

    ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
    PrintStream err = new PrintStream(errBytes);

    Map<String, File> keys = KeyPair.generateTemporaryFiles();
    int rc = clusterCommand.run(null, System.out, err, Lists.newArrayList("--instance-templates", "1 noop",
        "--service-name", "test-service", "--cluster-name", "test-cluster", "--cluster-user", "whirr", "--identity",
        "myusername", "--roles", "some-fake-role", "--quiet", "--private-key-file", keys.get("private")
            .getAbsolutePath()));

    assertThat(rc, is(-1));
    assertThat(errBytes.toString(), containsString("'roles' is not a recognized option"));

  }

  @Test
  public void testBaseCommandCmServerHdfsRolesFiltered() throws Exception {

    initialiseCluster(new String[][] {
        { CmServerHandler.ROLE, CmAgentHandler.ROLE, CmCdhHdfsNameNodeHandler.ROLE,
            CmCdhHdfsSecondaryNameNodeHandler.ROLE }, { CmAgentHandler.ROLE, CmCdhHdfsDataNodeHandler.ROLE } });

    BaseCommandCmServer clusterCommand = new BaseCommandCmServer("name", "description", factory, stateStoreFactory) {
      @Override
      public boolean isRoleFilterable() {
        return true;
      }

      @Override
      public int run(ClusterSpec specification, Set<Instance> instances, CmServerCluster cluster,
          CmServerBuilder serverCommand) throws Exception {
        assertThat(cluster.getServices(CmServerServiceType.CLUSTER).size(), is(1));
        assertThat(true, is(serverCommand != null));
        return 0;
      }
    };

    Map<String, File> keys = KeyPair.generateTemporaryFiles();
    int rc = clusterCommand.run(null, System.out, System.err, Lists.newArrayList("--instance-templates", "1 noop",
        "--service-name", "test-service", "--cluster-name", "test-cluster", "--cluster-user", "whirr", "--identity",
        "myusername", "--roles", CmCdhHdfsDataNodeHandler.ROLE, "--quiet", "--private-key-file", keys.get("private")
            .getAbsolutePath()));

    assertThat(rc, is(0));
    verify(factory).create("test-service");

  }

  @Test
  public void testBaseCommandCmServerHdfsRolesFilteredOut() throws Exception {

    initialiseCluster(new String[][] {
        { CmServerHandler.ROLE, CmAgentHandler.ROLE, CmCdhHdfsNameNodeHandler.ROLE,
            CmCdhHdfsSecondaryNameNodeHandler.ROLE }, { CmAgentHandler.ROLE, CmCdhHdfsDataNodeHandler.ROLE } });

    BaseCommandCmServer clusterCommand = new BaseCommandCmServer("name", "description", factory, stateStoreFactory) {
      @Override
      public boolean isRoleFilterable() {
        return true;
      }

      @Override
      public int run(ClusterSpec specification, Set<Instance> instances, CmServerCluster cluster,
          CmServerBuilder serverCommand) throws Exception {
        assertThat(cluster.getServices(CmServerServiceType.CLUSTER).size(), is(3));
        assertThat(true, is(serverCommand != null));
        return 0;
      }
    };

    ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
    PrintStream err = new PrintStream(errBytes);

    Map<String, File> keys = KeyPair.generateTemporaryFiles();
    int rc = clusterCommand.run(null, System.out, err, Lists.newArrayList("--instance-templates", "1 noop",
        "--service-name", "test-service", "--cluster-name", "test-cluster", "--cluster-user", "whirr", "--identity",
        "myusername", "--roles", "some-fake-role", "--quiet", "--private-key-file", keys.get("private")
            .getAbsolutePath()));

    assertThat(rc, is(-1));
    assertThat(errBytes.toString(), containsString("Role filter does not include any appropriate roles."));
    verify(factory).create("test-service");

  }

}
