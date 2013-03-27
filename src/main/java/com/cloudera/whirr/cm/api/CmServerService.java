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

public class CmServerService {

  public static final String NAME_TOKEN_DELIM = "_";

  private static final String NAME_QUALIFIER_GROUP = "group";

  private String name;
  private String group;
  private CmServerServiceType type;
  private String tag;
  private String qualifier;
  private String host;
  private String ip;
  private String ipInternal;

  public CmServerService(CmServerServiceType type) {
    this(type, null);
  }

  public CmServerService(CmServerServiceType type, String tag) {
    this(type, tag, "1", null, null, null);
  }

  public CmServerService(String host, String ip) {
    this(null, null, null, host, ip, null);
  }

  public CmServerService(CmServerServiceType type, String tag, String qualifier, String host) {
    this(type, tag, qualifier, host, null, null);
  }

  public CmServerService(CmServerServiceType type, String tag, String qualifier, String host, String ip,
      String ipInternal) {
    this.name = type == null ? null : ((tag == null ? "" : (tag + NAME_TOKEN_DELIM))
        + (type == null ? null : (type.toString().toLowerCase() + NAME_TOKEN_DELIM)) + (qualifier == null ? ""
        : qualifier));
    this.group = type == null ? "" : ((tag == null ? "" : (tag + NAME_TOKEN_DELIM))
        + (type == null ? "" : (type.toString().toLowerCase() + NAME_TOKEN_DELIM)) + NAME_QUALIFIER_GROUP);
    this.type = type;
    this.tag = tag;
    this.qualifier = qualifier;
    this.host = host;
    this.ip = ip;
    this.ipInternal = ipInternal;
  }

  @Override
  public String toString() {
    StringBuilder string = new StringBuilder();
    string.append("{");
    string.append("name=");
    string.append(name);
    string.append(", ");
    string.append("group=");
    string.append(group);
    string.append(", ");
    string.append("type=");
    string.append(type);
    string.append(", ");
    string.append("tag=");
    string.append(tag);
    string.append(", ");
    string.append("qualifier=");
    string.append(qualifier);
    string.append(", ");
    string.append("host=");
    string.append(host);
    string.append(", ");
    string.append("ip=");
    string.append(ip);
    string.append(", ");
    string.append("ipInternal=");
    string.append(ipInternal);
    string.append("}");
    return string.toString();
  }

  public String getName() {
    return name;
  }

  public String getGroup() {
    return group;
  }

  public CmServerServiceType getType() {
    return type;
  }

  public String getTag() {
    return tag;
  }

  public String getQualifier() {
    return qualifier;
  }

  public String getHost() {
    return host;
  }

  public String getIp() {
    return ip;
  }

  public String geIpInternal() {
    return ipInternal;
  }

}
