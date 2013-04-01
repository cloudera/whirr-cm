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
package com.cloudera.whirr.cm;

import static org.jclouds.scriptbuilder.domain.Statements.call;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.whirr.Cluster.Instance;
import org.apache.whirr.service.ClusterActionEvent;
import org.apache.whirr.service.hadoop.VolumeManager;

import com.google.common.collect.Iterables;

public abstract class BaseHandlerCm extends BaseHandler {

  public static final String DATA_DIRS_ROOT = "cm.data.dirs.root";
  public static final String DATA_DIRS_DEFAULT = "cm.data.dirs.default";

  public static final String CONSOLE_SPACER = "-------------------------------------------------------------------------------";

  protected Map<String, String> deviceMappings = new HashMap<String, String>();

  @Override
  protected void beforeBootstrap(ClusterActionEvent event) throws IOException, InterruptedException {
    logOperation("Host Pre Bootstrap");
    super.beforeBootstrap(event);
    addStatement(event, call("configure_hostnames"));
    addStatement(event, call("retry_helpers"));
  }

  @Override
  protected void afterBootstrap(ClusterActionEvent event) throws IOException, InterruptedException {
    logOperation("Host Post Bootstrap");
    super.afterBootstrap(event);
  }

  @Override
  protected void beforeConfigure(ClusterActionEvent event) throws IOException, InterruptedException {
    logOperation("Host Pre Configure");
    super.beforeConfigure(event);
    addStatement(event, call("retry_helpers"));
    if (getConfiguration(event.getClusterSpec()).getString(DATA_DIRS_ROOT) == null) {
      getDeviceMappings(event);
      String devMappings = VolumeManager.asString(deviceMappings);
      addStatement(event, call("prepare_all_disks", "'" + devMappings + "'"));
    }
    logOperation("Host Post Configure");
  }

  public Map<String, String> getDeviceMappings(ClusterActionEvent event) {
    if (deviceMappings.isEmpty()) {
      Instance prototype = Iterables.getFirst(event.getCluster().getInstances(), null);
      if (prototype == null) {
        throw new IllegalStateException("No instances found.");
      }
      VolumeManager volumeManager = new VolumeManager();
      deviceMappings.putAll(volumeManager.getDeviceMappings(event.getClusterSpec(), prototype));
    }

    return deviceMappings;
  }

  private void logOperation(String operation) {

    logHeader("Whirr " + operation);
    logLineItem("Role:");
    logLineItemDetail(getRole());

  }

  public void logHeader(String message) {

    System.out.println();
    System.out.println(CONSOLE_SPACER);
    System.out.println(message);
    System.out.println(CONSOLE_SPACER);

  }

  public void logFooter() {

    System.out.println();
    System.out.println(CONSOLE_SPACER);

  }

  public void logLineItem(String message) {

    System.out.println();
    System.out.println(message);

  }

  public void logLineItemDetail(String message) {

    System.out.println(message);

  }

  public void logException(String message, Throwable throwable) {

    System.out.println();
    throwable.printStackTrace();
    System.out.println();
    System.out.println(message);

  }

}
