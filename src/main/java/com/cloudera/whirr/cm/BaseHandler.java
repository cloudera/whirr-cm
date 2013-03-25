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

import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.apache.whirr.ClusterSpec;
import org.apache.whirr.service.ClusterActionEvent;
import org.apache.whirr.service.ClusterActionHandlerSupport;

public abstract class BaseHandler extends ClusterActionHandlerSupport {

  public static final String CONFIG_WHIRR_NAME = "whirr.cluster-name";
  public static final String CONFIG_WHIRR_AUTO_VARIABLE = "whirr.env.cmauto";
  public static final String CONFIG_WHIRR_CM_PREFIX = "whirr.cm.config.";

  public static final String CM_CLUSTER_NAME = "whirr";

  // jclouds allows '-', CM does not, CM allows '_', jclouds does not, so lets restrict to alphanumeric
  private static final Pattern CM_CLUSTER_NAME_REGEX = Pattern.compile("[A-Za-z0-9]+");

  protected static final String CONFIG_IMPORT_PATH = "functions/cmf/";

  private static final String PROPERTIES_FILE = "whirr-cm-default.properties";

  protected Configuration getConfiguration(ClusterSpec spec) throws IOException {
    return getConfiguration(spec, PROPERTIES_FILE);
  }

  @Override
  protected void beforeBootstrap(ClusterActionEvent event) throws IOException, InterruptedException {
    super.beforeBootstrap(event);
    if (!CM_CLUSTER_NAME_REGEX.matcher(
        event.getClusterSpec().getConfiguration().getString(CONFIG_WHIRR_NAME, CM_CLUSTER_NAME)).matches()) {
      throw new IOException("Illegal cluster name ["
          + event.getClusterSpec().getConfiguration().getString(CONFIG_WHIRR_NAME, CM_CLUSTER_NAME)
          + "] passed in variable [" + CONFIG_WHIRR_NAME + "] with default [" + CM_CLUSTER_NAME
          + "]. Please use only alphanumeric characters.");
    }
  }

}
