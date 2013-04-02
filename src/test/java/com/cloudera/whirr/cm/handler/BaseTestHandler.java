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
package com.cloudera.whirr.cm.handler;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.whirr.ClusterSpec;
import org.apache.whirr.service.BaseServiceDryRunTest;
import org.junit.Before;

import com.cloudera.whirr.cm.BaseTest;
import com.cloudera.whirr.cm.CmConstants;
import com.cloudera.whirr.cm.handler.cdh.BaseHandlerCmCdh;
import com.google.common.collect.ImmutableMap;
import com.jcraft.jsch.JSchException;

public abstract class BaseTestHandler extends BaseServiceDryRunTest implements BaseTest {

  @Override
  public ClusterSpec newClusterSpecForProperties(Map<String, String> properties) throws IOException,
      ConfigurationException, JSchException {
    ClusterSpec clusterSpec = super.newClusterSpecForProperties(ImmutableMap.<String, String> builder()
        .putAll(properties).put("whirr.cluster-user", CLUSTER_USER)
        .put(CmConstants.CONFIG_WHIRR_NAME, CmConstants.CONFIG_WHIRR_NAME_DEFAULT)
        .put(CmConstants.CONFIG_WHIRR_AUTO_VARIABLE, Boolean.FALSE.toString()).build());
    clusterSpec.setPrivateKey(FILE_KEY_PRIVATE);
    clusterSpec.setPublicKey(FILE_KEY_PUBLIC);
    return clusterSpec;
  }

  @Before
  public void clearClusterSingleton() {
    BaseHandlerCmCdh.CmServerClusterSingleton.getInstance().clear();
  }



}
