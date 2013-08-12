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

import java.io.IOException;
import java.util.Set;

import org.apache.whirr.Cluster.Instance;
import org.apache.whirr.ClusterControllerFactory;
import org.apache.whirr.ClusterSpec;
import org.apache.whirr.state.ClusterStateStoreFactory;

import com.cloudera.whirr.cm.server.CmServerBuilder;
import com.cloudera.whirr.cm.server.CmServerCluster;

public class CmServerCleanClusterCommand extends BaseCommandCmServer {

  public static final String NAME = "clean-cluster";
  public static final String DESCRIPTION = "Remove cluster and all services";

  public CmServerCleanClusterCommand() throws IOException {
    this(new ClusterControllerFactory());
  }

  public CmServerCleanClusterCommand(ClusterControllerFactory factory) {
    this(factory, new ClusterStateStoreFactory());
  }

  public CmServerCleanClusterCommand(ClusterControllerFactory factory, ClusterStateStoreFactory stateStoreFactory) {
    super(NAME, DESCRIPTION, factory, stateStoreFactory);
  }

  @Override
  public int run(ClusterSpec specification, Set<Instance> instances, CmServerCluster cluster,
      CmServerBuilder serverCommand) throws Exception {
    return serverCommand.command("unprovision").executeBoolean() ? 0 : -1;
  }

}