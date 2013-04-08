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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Set;

import org.apache.whirr.Cluster;
import org.apache.whirr.ClusterController;
import org.apache.whirr.ClusterControllerFactory;
import org.apache.whirr.ClusterSpec;
import org.apache.whirr.state.ClusterStateStore;
import org.apache.whirr.state.ClusterStateStoreFactory;
import org.apache.whirr.state.MemoryClusterStateStore;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadata.Status;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.domain.Credentials;
import org.jclouds.domain.LocationBuilder;
import org.jclouds.domain.LocationScope;

import com.cloudera.whirr.cm.BaseTest;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public abstract class BaseTestCommand implements BaseTest {

  protected ClusterController controller;
  protected ClusterControllerFactory factory;
  protected ClusterStateStoreFactory stateStoreFactory;

  @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
  public int initialiseCluster(String[][] roles) throws IOException, InterruptedException {

    int rolesCount = 0;
    for (int i = 0; i < roles.length; i++) {
      for (int j = 0; j < roles[i].length; j++) {
        if (roles[i][j].startsWith("cm-cdh")) {
          rolesCount++;
        }
      }
    }

    factory = mock(ClusterControllerFactory.class);
    controller = mock(ClusterController.class);
    when(factory.create((String) any())).thenReturn(controller);

    NodeMetadata[] nodes = new NodeMetadata[roles.length];
    for (int i = 0; i < roles.length; i++) {
      nodes[i] = new NodeMetadataBuilder()
          .name("name" + (i + 1))
          .ids("id" + (i + 1))
          .location(
              new LocationBuilder().scope(LocationScope.PROVIDER).id("stub").description("stub description").build())
          .imageId("image-id").status(Status.RUNNING).publicAddresses(Lists.newArrayList("127.0.0." + (i + 1)))
          .privateAddresses(Lists.newArrayList("127.0.0." + (i + 1))).build();
    }
    when(controller.getNodes((ClusterSpec) any())).thenReturn((Set) Sets.newLinkedHashSet(Lists.newArrayList(nodes)));
    when(controller.getInstances((ClusterSpec) any(), (ClusterStateStore) any())).thenCallRealMethod();

    stateStoreFactory = mock(ClusterStateStoreFactory.class);
    Credentials credentials = new Credentials("dummy", "dummy");
    Set<Cluster.Instance> instances = Sets.newHashSet();
    for (int i = 0; i < roles.length; i++) {
      String ip = "127.0.0." + (i + 1);
      instances.add(new Cluster.Instance(credentials, Sets.newHashSet(roles[i]), ip, ip, "id" + (i + 1), null));
    }
    ClusterStateStore memStore = new MemoryClusterStateStore();
    memStore.save(new Cluster(instances));
    when(stateStoreFactory.create((ClusterSpec) any())).thenReturn(memStore);

    return rolesCount;

  }

}
