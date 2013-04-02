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

import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.containsPattern;

import java.util.Set;

import org.apache.whirr.service.DryRunModule.DryRun;
import org.junit.Test;

import com.cloudera.whirr.cm.handler.CmNodeHandler;
import com.cloudera.whirr.cm.handler.CmServerHandler;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class CmNodeHandlerTest extends BaseTestHandler {

  @Override
  protected Set<String> getInstanceRoles() {
    return ImmutableSet.of(CmNodeHandler.ROLE);
  }

  @Override
  protected Predicate<CharSequence> bootstrapPredicate() {
    return and(containsPattern("configure_hostnames"), containsPattern("install_cm"));
  }

  @Override
  protected Predicate<CharSequence> configurePredicate() {
    return Predicates.alwaysTrue();
  }

  @Override
  @Test
  public void testBootstrapAndConfigure() throws Exception {
    DryRun dryRun = launchWithClusterSpec(newClusterSpecForProperties(ImmutableMap.of("whirr.instance-templates", "1 "
        + CmNodeHandler.ROLE)));
    assertScriptPredicateOnPhase(dryRun, "bootstrap", bootstrapPredicate());
  }

  @Test
  public void testWithCmServer() throws Exception {
    DryRun dryRun = launchWithClusterSpec(newClusterSpecForProperties(ImmutableMap.of("whirr.instance-templates", "1 "
        + CmServerHandler.ROLE + ",1 " + CmNodeHandler.ROLE)));
    assertScriptPredicateOnPhase(dryRun, "bootstrap", bootstrapPredicate());
  }

  @Test
  public void testNoCmServer() throws Exception {
    DryRun dryRun = launchWithClusterSpec(newClusterSpecForProperties(ImmutableMap.of("whirr.instance-templates", "1 "
        + CmNodeHandler.ROLE)));
    assertScriptPredicateOnPhase(dryRun, "bootstrap", bootstrapPredicate());
  }
  
}
