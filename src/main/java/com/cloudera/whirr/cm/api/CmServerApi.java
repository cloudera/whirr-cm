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
package com.cloudera.whirr.cm.api;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface CmServerApi {

  public String getName(CmServerCluster cluster);

  public boolean getServiceConfigs(CmServerCluster cluster, File directory) throws CmServerApiException;

  public List<CmServerService> getServiceHosts() throws CmServerApiException;

  public CmServerService getServiceHost(CmServerService service) throws CmServerApiException;

  public CmServerService getServiceHost(CmServerService service, List<CmServerService> services)
      throws CmServerApiException;

  public List<CmServerService> getServices(CmServerCluster cluster) throws CmServerApiException;

  public CmServerService getService(CmServerCluster cluster, CmServerServiceType type) throws CmServerApiException;

  public List<CmServerService> getServices(CmServerCluster cluster, CmServerServiceType type)
      throws CmServerApiException;

  public boolean isProvisioned(CmServerCluster cluster) throws CmServerApiException;

  public boolean isConfigured(CmServerCluster cluster) throws CmServerApiException;

  public boolean isStarted(CmServerCluster cluster) throws CmServerApiException;

  public boolean isStopped(CmServerCluster cluster) throws CmServerApiException;

  public Map<String, String> initialise(Map<String, String> config) throws CmServerApiException;

  public boolean provision(CmServerCluster cluster) throws CmServerApiException;

  public boolean configure(CmServerCluster cluster) throws CmServerApiException;

  public boolean start(CmServerCluster cluster) throws CmServerApiException;

  public boolean stop(CmServerCluster cluster) throws CmServerApiException;

  public boolean unconfigure(CmServerCluster cluster) throws CmServerApiException;

  public boolean unprovision(CmServerCluster cluster) throws CmServerApiException;

}