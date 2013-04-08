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

  public static final File DIR_CLIENT_CONFIG = new File(new File(".").getAbsolutePath() + "/target/test-client");
  public static final File FILE_KEY_PRIVATE = new File(new File(".").getAbsolutePath() + "/src/test/resources/test-key");
  public static final File FILE_KEY_PUBLIC = new File(new File(".").getAbsolutePath()
      + "/src/test/resources/test-key.pub");

  public static final String LOG_TAG_CM_SERVER_API_TEST = "TestBootstrap";

}
