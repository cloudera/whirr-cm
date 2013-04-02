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
package com.cloudera.whirr.cm.server;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface CmServer {

  public boolean getServiceConfigs(CmServerCluster cluster, File directory) throws CmServerException;

  public List<CmServerService> getServiceHosts() throws CmServerException;

  public CmServerService getServiceHost(CmServerService service) throws CmServerException;

  public CmServerService getServiceHost(CmServerService service, List<CmServerService> services)
      throws CmServerException;

  public CmServerCluster getServices(CmServerCluster cluster) throws CmServerException;

  public CmServerService getService(CmServerCluster cluster, CmServerServiceType type) throws CmServerException;

  public CmServerCluster getServices(CmServerCluster cluster, CmServerServiceType type) throws CmServerException;

  public boolean isProvisioned(CmServerCluster cluster) throws CmServerException;

  public boolean isConfigured(CmServerCluster cluster) throws CmServerException;

  public boolean isStarted(CmServerCluster cluster) throws CmServerException;

  public boolean isStopped(CmServerCluster cluster) throws CmServerException;

  public Map<String, String> initialise(Map<String, String> config) throws CmServerException;

  public boolean provision(CmServerCluster cluster) throws CmServerException;

  public boolean configure(CmServerCluster cluster) throws CmServerException;

  public boolean start(CmServerCluster cluster) throws CmServerException;

  public boolean stop(CmServerCluster cluster) throws CmServerException;

  public boolean unconfigure(CmServerCluster cluster) throws CmServerException;

  public boolean unprovision(CmServerCluster cluster) throws CmServerException;

}