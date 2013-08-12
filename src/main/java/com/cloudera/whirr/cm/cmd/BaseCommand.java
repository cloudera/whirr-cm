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
package com.cloudera.whirr.cm.cmd;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;

import joptsimple.OptionSet;

import org.apache.commons.lang.WordUtils;
import org.apache.whirr.ClusterController;
import org.apache.whirr.ClusterControllerFactory;
import org.apache.whirr.ClusterSpec;
import org.apache.whirr.command.AbstractClusterCommand;
import org.apache.whirr.state.ClusterStateStoreFactory;

import com.cloudera.whirr.cm.CmConstants;
import com.cloudera.whirr.cm.server.impl.CmServerLog;

public abstract class BaseCommand extends AbstractClusterCommand implements CmConstants {

  protected static final CmServerLog logger = new CmServerLog.CmServerLogSysOut(LOG_TAG_WHIRR_COMMAND, false);

  public BaseCommand(String name, String description, ClusterControllerFactory factory,
      ClusterStateStoreFactory stateStoreFactory) {
    super(name, description, factory, stateStoreFactory);
  }

  public BaseCommand(String name, String description, ClusterControllerFactory factory) {
    super(name, description, factory);
  }

  public abstract int run(ClusterSpec specification, ClusterController clusterController, OptionSet optionSet)
      throws Exception;

  public String getLabel() {
    return WordUtils.capitalize(getName().replace("-", " ").replace("_", " ")).replace(" ", "");
  }

  @Override
  public int run(InputStream in, PrintStream out, PrintStream err, List<String> args) throws Exception {

    try {

      OptionSet optionSet = parser.parse(args.toArray(new String[args.size()]));
      if (!optionSet.nonOptionArguments().isEmpty()) {
        printUsage(err);
        return -1;
      }

      ClusterSpec specification = getClusterSpec(optionSet);
      printProviderInfo(out, err, specification, optionSet);

      return run(specification, createClusterController(specification.getServiceName()), optionSet);

    } catch (Exception e) {
      printErrorAndHelpHint(err, e);
      return -1;
    }

  }

}
