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

public class CmServerService implements Comparable<CmServerService> {

  public enum CmServerServiceStatus {
    STARTING, STARTED, STOPPING, STOPPED, BUSY, UNKNOWN
  }

  public static final String NAME_TOKEN_DELIM = "_";
  public static final String NAME_TAG_DEFAULT = "cdh";
  public static final String NAME_QUALIFIER_DEFAULT = "1";
  public static final String NAME_QUALIFIER_GROUP = "group";

  private String name;
  private String group;
  private CmServerServiceType type;
  private String tag;
  private String qualifier;
  private String host;
  private String ip;
  private String ipInternal;

  private transient CmServerServiceStatus status = CmServerServiceStatus.UNKNOWN;

  private String toString;

  protected CmServerService(String name, String host, String ip, String ipInternal, CmServerServiceStatus status) {
    if (name == null) {
      throw new IllegalArgumentException("Illegal argumnents passed to constructor");
    }
    String tag = _getTag(name);
    String qualifier = _getQualifier(name);
    CmServerServiceType type = _getType(name);
    if (tag == null || qualifier == null || type == null || !_getName(type, tag, qualifier).equals(name)) {
      throw new IllegalArgumentException("Illgal argumnents passed to constructor");
    }
    this.name = name;
    this.group = _getName(type, tag, NAME_QUALIFIER_GROUP);
    this.type = type;
    this.tag = tag;
    this.qualifier = qualifier;
    this.host = host;
    this.ip = ip;
    this.ipInternal = ipInternal;
    this.status = status;
  }

  protected CmServerService(CmServerServiceType type, String tag, String qualifier, String host, String ip,
      String ipInternal, CmServerServiceStatus status) {
    if (type == null || tag == null || tag.contains(NAME_TOKEN_DELIM) || qualifier == null
        || qualifier.contains(NAME_TOKEN_DELIM)) {
      throw new IllegalArgumentException("Illgal argumnents passed to constructor");
    }
    this.name = _getName(type, tag, qualifier);
    this.group = _getName(type, tag, NAME_QUALIFIER_GROUP);
    this.type = type;
    this.tag = tag;
    this.qualifier = qualifier;
    this.host = host;
    this.ip = ip;
    this.ipInternal = ipInternal;
    this.status = status;
  }

  private static String _getName(CmServerServiceType type, String tag, String qualifier) {
    return tag + NAME_TOKEN_DELIM + type.toString().toLowerCase() + NAME_TOKEN_DELIM + qualifier;
  }

  private static String _getTag(String name) {
    String tag;
    try {
      tag = name.substring(0, name.indexOf(NAME_TOKEN_DELIM));
    } catch (Exception e) {
      throw new IllegalArgumentException("Illegal name [" + name + "]");
    }
    if (tag.contains(NAME_TOKEN_DELIM)) {
      throw new IllegalArgumentException("Illegal name [" + name + "]");
    }
    return tag;
  }

  private static String _getQualifier(String name) {
    String qualifier;
    try {
      qualifier = name.substring(name.lastIndexOf(NAME_TOKEN_DELIM) + 1, name.length());
    } catch (Exception e) {
      throw new IllegalArgumentException("Illegal name [" + name + "]");
    }
    if (qualifier.contains(NAME_TOKEN_DELIM)) {
      throw new IllegalArgumentException("Illegal name [" + name + "]");
    }
    return qualifier;
  }

  private static CmServerServiceType _getType(String name) {
    try {
      return CmServerServiceType.valueOf(name.substring(name.indexOf(NAME_TOKEN_DELIM) + 1,
          name.lastIndexOf(NAME_TOKEN_DELIM)).toUpperCase());
    } catch (Exception e) {
      throw new IllegalArgumentException("Illegal name [" + name + "]");
    }
  }

  @Override
  public int compareTo(CmServerService service) {
    int compareTo = 0;
    if (getType() != null && service.getType() != null) {
      compareTo = getType().compareTo(service.getType());
    }
    if (compareTo == 0) {
      compareTo = toString().compareTo(service.toString());
    }
    return compareTo;
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof CmServerService) {
      return toString().equals(object.toString());
    }
    return false;
  }

  @Override
  public String toString() {
    // toString can be cached given object is immutable
    if (toString == null) {
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
      toString = string.toString();
    }
    return toString;
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

  public String getIpInternal() {
    return ipInternal;
  }

  public CmServerServiceStatus getStatus() {
    return status;
  }

}
