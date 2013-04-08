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

import com.cloudera.whirr.cm.server.CmServerService.CmServerServiceStatus;

public class CmServerServiceBuilder {

  private String name;
  private CmServerServiceType type = CmServerServiceType.CLUSTER;
  private String tag = CmServerService.NAME_TAG_DEFAULT;
  private String qualifier = CmServerService.NAME_QUALIFIER_DEFAULT;
  private String host;
  private String ip;
  private String ipInternal;
  private CmServerServiceStatus status = CmServerServiceStatus.UNKNOWN;

  public CmServerServiceBuilder() {
  }

  public CmServerServiceBuilder name(String name) {
    this.name = name;
    return this;
  }

  public CmServerServiceBuilder type(CmServerServiceType type) {
    this.type = type;
    return this;
  }

  public CmServerServiceBuilder tag(String tag) {
    this.tag = tag;
    return this;
  }

  public CmServerServiceBuilder qualifier(String qualifier) {
    this.qualifier = qualifier;
    return this;
  }

  public CmServerServiceBuilder host(String host) {
    this.host = host;
    return this;
  }

  public CmServerServiceBuilder ip(String ip) {
    this.ip = ip;
    return this;
  }

  public CmServerServiceBuilder ipInternal(String ipInternal) {
    this.ipInternal = ipInternal;
    return this;
  }

  public CmServerServiceBuilder status(CmServerServiceStatus status) {
    this.status = status;
    return this;
  }

  public CmServerService build() {
    return name == null ? new CmServerService(type, tag, qualifier, host, ip, ipInternal, status)
        : new CmServerService(name, host, ip, ipInternal, status);
  }

}
