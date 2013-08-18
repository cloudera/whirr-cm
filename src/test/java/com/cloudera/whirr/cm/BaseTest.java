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

import java.io.File;

public interface BaseTest extends CmConstants {

  public static final String CLUSTER_TAG = "whirr";

  public static final String CLUSTER_USER = "whirr";

  public static final String TEST_PLATFORM = "whirr.test.platform";
  public static final String TEST_PLATFORM_DESTROY = "whirr.test.platform.destroy";
  public static final String TEST_CM_VERSION = "whirr.test.cm.version";
  public static final String TEST_CM_API_VERSION = "whirr.test.cm.api.version";
  public static final String TEST_CM_CDH_VERSION = "whirr.test.cm.cdh.version";

  public static final String TEST_CM_TEST_PROPERTIES = "test-whirrcm.properties";
  public static final String TEST_CM_TEST_GLOBAL_PROPERTIES = "cm-test.properties";
  public static final String TEST_CM_TEST_PREFIX_PROPERTIES = "test-whirrcm-";
  public static final String TEST_CM_EXAMPLE_PROPERTIES = "cm-ec2.properties";

  public static final String TEST_PLATFORM_DEFAULT = "example";

  public static final File TEST_DIR_CLIENT_CONFIG = new File(new File(".").getAbsolutePath() + "/target/test-client");
  public static final File TEST_FILE_KEY_PRIVATE = new File(new File(".").getAbsolutePath()
      + "/src/test/resources/test-key");
  public static final File TEST_FILE_KEY_PUBLIC = new File(new File(".").getAbsolutePath()
      + "/src/test/resources/test-key.pub");

  public static final String TEST_LOG_TAG_CM_SERVER_API_TEST = "TestBootstrap";

}
