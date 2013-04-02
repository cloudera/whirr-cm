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
package com.cloudera.whirr.cm.handler.cdh;

import static org.jclouds.scriptbuilder.domain.Statements.call;

import java.io.IOException;

import org.apache.whirr.service.ClusterActionEvent;

import com.cloudera.whirr.cm.server.CmServerServiceType;

public class CmCdhHiveMetaStoreHandler extends BaseHandlerCmCdh {

  public static final String ROLE = "cm-cdh-hivemetastore";
  public static final CmServerServiceType TYPE = CmServerServiceType.HIVE_METASTORE;

  @Override
  public String getRole() {
    return ROLE;
  }

  @Override
  public CmServerServiceType getType() {
    return TYPE;
  }

  @Override
  protected void beforeBootstrap(ClusterActionEvent event) throws IOException, InterruptedException {
    super.beforeBootstrap(event);
    addStatement(event, call("install_cmcdh_hivemetastore"));
  }

  @Override
  protected void beforeConfigure(ClusterActionEvent event) throws IOException, InterruptedException {
    super.beforeConfigure(event);
    addStatement(event, call("configure_cmcdh_hivemetastore"));
  }

  @Override
  protected void beforeStart(ClusterActionEvent event) throws IOException, InterruptedException {
    super.beforeStart(event);
    addStatement(event, call("start_cmcdh_hivemetastore"));
  }

}
