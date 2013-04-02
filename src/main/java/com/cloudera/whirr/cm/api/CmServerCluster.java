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

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class CmServerCluster {

  private Map<CmServerServiceType, Set<CmServerService>> services = new HashMap<CmServerServiceType, Set<CmServerService>>();

  private Set<String> dataMounts = new HashSet<String>();

  private boolean isParcel = true;
  
  public CmServerCluster() {
  }

  public synchronized boolean isEmpty() {
    return services.isEmpty();
  }

  public synchronized boolean isEmptyServices() {
    for (CmServerServiceType type : services.keySet()) {
      if (!services.get(type).isEmpty()) {
        return false;
      }
    }
    return true;
  }

  public synchronized void clear() {
    services.clear();
  }

  public synchronized void clearServices() {
    for (CmServerServiceType type : services.keySet()) {
      services.get(type).clear();
    }
  }

  public synchronized boolean add(CmServerServiceType type) throws CmServerApiException {
    assertConsistentTopology(type);
    if (!services.containsKey(type.getParent())) {
      services.put(type.getParent(), new TreeSet<CmServerService>());
      return true;
    }
    return false;
  }

  public synchronized boolean add(CmServerService service) throws CmServerApiException {
    add(service.getType());
    services.get(service.getType().getParent()).add(service);
    return true;
  }

  public synchronized Set<CmServerServiceType> getServiceTypes() {
    return new TreeSet<CmServerServiceType>(services.keySet());
  }

  public synchronized Set<CmServerServiceType> getServiceTypes(CmServerServiceType type) {
    Set<CmServerServiceType> types = new TreeSet<CmServerServiceType>();
    if (type.equals(CmServerServiceType.CLUSTER)) {
      for (CmServerServiceType serviceType : services.keySet()) {
        for (CmServerService service : services.get(serviceType)) {
          types.add(service.getType());
        }
      }
    } else if (services.containsKey(type)) {
      for (CmServerService service : services.get(type)) {
        types.add(service.getType());
      }
    } else if (services.containsKey(type.getParent())) {
      for (CmServerService service : services.get(type.getParent())) {
        if (service.getType().equals(type)) {
          types.add(service.getType());
        }
      }
    }
    return types;
  }

  public synchronized CmServerService getService(CmServerServiceType type) throws IOException {
    Set<CmServerService> serviceCopy = getServices(type);
    return serviceCopy.size() == 0 ? null : serviceCopy.iterator().next();
  }

  public synchronized Set<CmServerService> getServices(CmServerServiceType type) {
    // TODO: Do Deep Copy
    Set<CmServerService> servicesCopy = new TreeSet<CmServerService>();
    if (type.equals(CmServerServiceType.CLUSTER)) {
      for (CmServerServiceType serviceType : services.keySet()) {
        servicesCopy.addAll(services.get(serviceType));
      }
    } else if (services.containsKey(type)) {
      servicesCopy.addAll(services.get(type));
    } else if (services.containsKey(type.getParent())) {
      for (CmServerService service : services.get(type.getParent())) {
        if (service.getType().equals(type)) {
          servicesCopy.add(service);
        }
      }
    }
    return servicesCopy;
  }

  public synchronized String getServiceName(CmServerServiceType type) throws IOException {
    if (services.get(type) != null) {
      CmServerService service = services.get(type).iterator().next();
      if (service.getType().equals(type)) {
        return service.getName();
      } else {
        return new CmServerService(type, service.getTag()).getName();
      }
    } else {
      Set<CmServerService> servicesChild = null;
      if (!services.isEmpty() && !(servicesChild = services.get(getServiceTypes().iterator().next())).isEmpty()) {
        return new CmServerService(type, servicesChild.iterator().next().getTag()).getName();
      }
    }
    throw new IOException("Cannot determine service name, cluster is empty");
  }

  public synchronized void setDataMounts(Set<String> mnts) {
    this.dataMounts.addAll(mnts);
  }

  public synchronized Set<String> getDataMounts() {
    return ImmutableSet.copyOf(dataMounts);
  }

  public String getDataDirsForSuffix(final Set<String> defaults, final String suffix) throws IOException {
    Set<String> dataMounts = getDataMounts();
    return Joiner.on(',').join(
        Lists.transform(Lists.newArrayList(dataMounts.isEmpty() ? defaults : dataMounts),
            new Function<String, String>() {
              @Override
              public String apply(String input) {
                return input + suffix;
              }
            }));
  }

  public void setIsParcel(boolean isParcel) {
    this.isParcel = isParcel;
  }

  public boolean getIsParcel() {
    return isParcel;
  }
    
  private void assertConsistentTopology(CmServerServiceType type) throws CmServerApiException {
    if (type.getParent() == null || type.getParent().getParent() == null) {
      throw new CmServerApiException("Invalid cluster topology: Attempt to add non leaf type [" + type + "]");
    }
    switch (type) {
    case HDFS_NAMENODE:
      if (getServices(CmServerServiceType.HDFS_NAMENODE).size() > 0) {
        throw new CmServerApiException("Invalid cluster topology: Attempt to add multiple types [" + type + "]");
      }
      break;
    default:
      break;
    }
  }

}
