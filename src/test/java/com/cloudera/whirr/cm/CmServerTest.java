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

import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.containsPattern;

import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class CmServerTest extends BaseCmTest {

  @Override
  protected Set<String> getInstanceRoles() {
    return ImmutableSet.of(CmServerHandler.ROLE);
  }

  @Override
  protected Predicate<CharSequence> bootstrapPredicate() {
    return and(
      containsPattern("configure_hostnames"),
      and(containsPattern("install_cdh_hadoop"),
        and(containsPattern("install_cm"), containsPattern("install_cm_server"))));
  }

  @Override
  protected Predicate<CharSequence> configurePredicate() {
    return containsPattern("configure_cm_server");
  }

  @Test
  public void testNodes() throws Exception {
    Assert.assertNotNull(launchWithClusterSpec(newClusterSpecForProperties(ImmutableMap.of("whirr.instance-templates",
      "1 " + CmServerHandler.ROLE + ",2 " + CmNodeHandler.ROLE))));
  }

  @Test
  public void testAgents() throws Exception {
    Assert.assertNotNull(launchWithClusterSpec(newClusterSpecForProperties(ImmutableMap.of("whirr.instance-templates",
      "1 " + CmServerHandler.ROLE + ",2 " + CmAgentHandler.ROLE))));
  }

  @Test
  public void testNodesAndAgents() throws Exception {
    Assert.assertNotNull(launchWithClusterSpec(newClusterSpecForProperties(ImmutableMap.of("whirr.instance-templates",
      "1 " + CmServerHandler.ROLE + ",2 " + CmNodeHandler.ROLE + ",2 " + CmAgentHandler.ROLE))));
  }

}
