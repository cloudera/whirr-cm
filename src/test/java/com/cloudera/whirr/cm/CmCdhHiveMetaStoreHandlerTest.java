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

import static com.google.common.base.Predicates.containsPattern;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.cloudera.whirr.cm.cdh.CmCdhHiveMetaStoreHandler;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class CmCdhHiveMetaStoreHandlerTest extends BaseTestHandler {

  @Override
  protected Set<String> getInstanceRoles() {
    return ImmutableSet.of(CmServerHandler.ROLE, CmAgentHandler.ROLE, CmCdhHiveMetaStoreHandler.ROLE);
  }

  @Override
  protected Predicate<CharSequence> bootstrapPredicate() {
    return containsPattern("install_cmcdh_hivemetastore");
  }

  @Override
  protected Predicate<CharSequence> configurePredicate() {
    return containsPattern("configure_cmcdh_hivemetastore");
  }

  @Test
  public void testNoAgent() throws Exception {
    boolean caught = false;
    try {
      launchWithClusterSpec(newClusterSpecForProperties(ImmutableMap.of("whirr.instance-templates", "1 "
          + CmServerHandler.ROLE + "+" + CmCdhHiveMetaStoreHandler.ROLE)));
    } catch (Exception e) {
      caught = true;
    }
    Assert.assertTrue(caught);
  }

  @Test
  public void testNoCmServer() throws Exception {
    boolean caught = false;
    try {
      launchWithClusterSpec(newClusterSpecForProperties(ImmutableMap.of("whirr.instance-templates", "1 "
          + CmAgentHandler.ROLE + "+" + CmCdhHiveMetaStoreHandler.ROLE)));
    } catch (Exception e) {
      caught = true;
    }
    Assert.assertTrue(caught);
  }
}
