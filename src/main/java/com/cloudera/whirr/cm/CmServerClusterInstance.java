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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.whirr.Cluster;
import org.apache.whirr.Cluster.Instance;
import org.apache.whirr.ClusterSpec;
import org.apache.whirr.service.ClusterActionEvent;
import org.apache.whirr.service.hadoop.VolumeManager;

import com.cloudera.whirr.cm.handler.CmAgentHandler;
import com.cloudera.whirr.cm.handler.CmNodeHandler;
import com.cloudera.whirr.cm.handler.CmServerHandler;
import com.cloudera.whirr.cm.handler.cdh.BaseHandlerCmCdh;
import com.cloudera.whirr.cm.server.CmServerCluster;
import com.cloudera.whirr.cm.server.CmServerException;
import com.cloudera.whirr.cm.server.CmServerService;
import com.cloudera.whirr.cm.server.CmServerServiceBuilder;
import com.cloudera.whirr.cm.server.CmServerServiceType;
import com.cloudera.whirr.cm.server.impl.CmServerFactory;
import com.cloudera.whirr.cm.server.impl.CmServerLog;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;

public class CmServerClusterInstance implements CmConstants {

  private static CmServerFactory factory;
  private static boolean isStandaloneCommand = true;
  private static Future<?> logExecutorFuture;
  private static ExecutorService logExecutor = Executors.newSingleThreadScheduledExecutor();
  private static Map<ClusterActionEvent, Set<Integer>> ports = new HashMap<ClusterActionEvent, Set<Integer>>();

  private static int LOG_POLL_PERIOD_MS = 7500;
  private static int LOG_POLL_PERIOD_BACKOFF_NUMBER = 3;
  private static int LOG_POLL_PERIOD_BACKOFF_INCRAMENT = 2;

  private CmServerClusterInstance() {
  }

  private static final LoadingCache<Key, CmServerCluster> clusterCache = CacheBuilder.newBuilder().build(
      new CacheLoader<Key, CmServerCluster>() {

        @Override
        public CmServerCluster load(Key arg0) {
          return new CmServerCluster();
        }
      });

  public static synchronized void clear() {
    clusterCache.invalidateAll();
    ports.clear();
  }

  public synchronized static Configuration getConfiguration(ClusterSpec clusterSpec) throws IOException {
    try {
      CompositeConfiguration configuration = new CompositeConfiguration();
      if (clusterSpec != null) {
        configuration.addConfiguration(clusterSpec.getConfiguration());
      }
      configuration.addConfiguration(new PropertiesConfiguration(CmServerClusterInstance.class.getClassLoader()
          .getResource(CONFIG_WHIRR_DEFAULT_FILE)));
      return configuration;
    } catch (ConfigurationException e) {
      throw new IOException("Error loading " + CONFIG_WHIRR_DEFAULT_FILE, e);
    }
  }

  public static synchronized boolean isStandaloneCommand() {
    return isStandaloneCommand;
  }

  public static synchronized void setIsStandaloneCommand(boolean isStandaloneCommand) {
    CmServerClusterInstance.isStandaloneCommand = isStandaloneCommand;
  }

  public static String getVersion(Configuration configuration) throws IOException {
    return getVersionSubstring(configuration, CONFIG_WHIRR_CM_VERSION, "cm");
  }

  public static String getVersionApi(Configuration configuration) throws IOException {
    return getVersionSubstring(configuration, CONFIG_WHIRR_CM_API_VERSION, "v");
  }

  public static String getVersionCdh(Configuration configuration) throws IOException {
    return getVersionSubstring(configuration, CONFIG_WHIRR_CM_CDH_VERSION, "cdh");
  }

  private static String getVersionSubstring(Configuration configuration, String property, String prefix)
      throws IOException {
    String version = configuration.getString(property);
    if (version == null || !version.startsWith(prefix)) {
      return null;
    } else {
      return version.substring(prefix.length());
    }
  }

  public static synchronized Set<Integer> portsPush(ClusterActionEvent event, Set<String> ports) {
    Set<Integer> portsNew = new HashSet<Integer>();
    if (CmServerClusterInstance.ports.get(event) == null) {
      CmServerClusterInstance.ports.put(event, new HashSet<Integer>());
    }
    for (String port : ports) {
      if (ports != null && !ports.equals("")) {
        try {
          Integer portInteger = Integer.parseInt(port);
          if (!CmServerClusterInstance.ports.get(event).contains(portInteger)) {
            portsNew.add(portInteger);
            CmServerClusterInstance.ports.get(event).add(portInteger);
          }
        } catch (NumberFormatException e) {
          // ignore
        }
      }
    }
    return portsNew;
  }

  public static synchronized Set<Integer> portsPop(ClusterActionEvent event) {
    return ports.remove(event);
  }

  public static synchronized CmServerFactory getFactory() {
    return factory == null ? (factory = new CmServerFactory()) : factory;
  }

  public static synchronized CmServerFactory getFactory(CmServerFactory factory) {
    return CmServerClusterInstance.factory = factory;
  }

  public static synchronized CmServerCluster getCluster(ClusterSpec spec) {
    return clusterCache.getUnchecked(new Key(spec));
  }

  public static synchronized void getCluster(ClusterSpec spec, boolean clear) {
    if (clear) {
      clusterCache.invalidate(new Key(spec));
    }
  }

  public static synchronized CmServerCluster getCluster(ClusterSpec spec, Configuration configuration,
      Set<Instance> instances) throws CmServerException, IOException {
    return getCluster(spec, configuration, instances, new TreeSet<String>(), Collections.<String> emptySet());
  }

  public static synchronized CmServerCluster getCluster(ClusterSpec spec, Configuration configuration,
      Set<Instance> instances, SortedSet<String> mounts) throws CmServerException, IOException {
    return getCluster(spec, configuration, instances, mounts, Collections.<String> emptySet());
  }

  public static synchronized CmServerCluster getCluster(ClusterSpec spec, Configuration configuration,
      Set<Instance> instances, SortedSet<String> mounts, Set<String> roles) throws CmServerException, IOException {
    CmServerCluster cluster = new CmServerCluster();
    clusterCache.put(new Key(spec), cluster);
    cluster.setIsParcel(!configuration.getBoolean(CONFIG_WHIRR_USE_PACKAGES, false));
    cluster.addServiceConfigurationAll(getClusterConfiguration(configuration, mounts));
    for (Instance instance : instances) {
      for (String role : instance.getRoles()) {
        if (role.equals(CmServerHandler.ROLE)) {
          cluster.setServer(new CmServerServiceBuilder().host(instance.getPublicHostName()).ip(instance.getPublicIp())
              .ipInternal(instance.getPrivateIp()).build());
        } else if (role.equals(CmAgentHandler.ROLE)) {
          cluster.addAgent(new CmServerServiceBuilder().ip(instance.getPublicIp()).ipInternal(instance.getPrivateIp())
              .build());
        } else if (role.equals(CmNodeHandler.ROLE)) {
          cluster.addNode(new CmServerServiceBuilder().ip(instance.getPublicIp()).ipInternal(instance.getPrivateIp())
              .build());
        } else {
          CmServerServiceType type = BaseHandlerCmCdh.getRolesToType().get(role);
          if (type != null && (roles == null || roles.isEmpty() || roles.contains(role))) {
            cluster.addService(new CmServerServiceBuilder()
                .type(type)
                .tag(
                    configuration.getString(ClusterSpec.Property.CLUSTER_NAME.getConfigName(),
                        CONFIG_WHIRR_NAME_DEFAULT)).qualifier("" + (cluster.getServices(type).size() + 1))
                .ip(instance.getPublicIp()).ipInternal(instance.getPrivateIp()).build());
          }
        }
      }
    }
    return cluster;
  }

  public static CmServerCluster getCluster(CmServerCluster cluster) throws CmServerException {
    CmServerCluster clusterTo = new CmServerCluster();
    clusterTo.setIsParcel(cluster.getIsParcel());
    clusterTo.addServiceConfigurationAll(cluster.getServiceConfiguration());
    clusterTo.setServer(cluster.getServer());
    for (CmServerService agent : cluster.getAgents()) {
      clusterTo.addAgent(agent);
    }
    for (CmServerService node : cluster.getNodes()) {
      clusterTo.addAgent(node);
    }
    return clusterTo;
  }

  public static String getClusterConfiguration(ClusterSpec clusterSpec, SortedSet<String> mounts, String type,
      String typeParent, String settingSuffix) throws IOException {
    return getClusterConfiguration(getConfiguration(clusterSpec), mounts, type, typeParent, settingSuffix);
  }

  public static String getClusterConfiguration(Configuration configuration, SortedSet<String> mounts, String type,
      String typeParent, String settingSuffix) throws IOException {
    String databaseSettingValue = null;
    Map<String, Map<String, String>> clusterConfiguration = getClusterConfiguration(configuration, mounts);
    if (clusterConfiguration.get(type) != null) {
      for (String setting : clusterConfiguration.get(type).keySet()) {
        if (setting.endsWith(settingSuffix)) {
          databaseSettingValue = clusterConfiguration.get(type).get(setting);
        }
      }
    }
    if (databaseSettingValue == null && typeParent != null) {
      if (clusterConfiguration.get(typeParent) != null) {
        for (String setting : clusterConfiguration.get(typeParent).keySet()) {
          if (setting.endsWith(settingSuffix)) {
            databaseSettingValue = clusterConfiguration.get(typeParent).get(setting);
          }
        }
      }
    }
    if (databaseSettingValue == null) {
      throw new IOException("Could not find setting [" + settingSuffix + "] for type [" + type + "] with parent type ["
          + typeParent + "] from configuration");
    }
    return databaseSettingValue;
  }

  public static Map<String, Map<String, String>> getClusterConfiguration(ClusterSpec clusterSpec,
      SortedSet<String> mounts) throws IOException {
    return getClusterConfiguration(getConfiguration(clusterSpec), mounts);
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Map<String, String>> getClusterConfiguration(final Configuration configuration,
      SortedSet<String> mounts) throws IOException {

    Map<String, Map<String, String>> clusterConfiguration = new HashMap<String, Map<String, String>>();

    Iterator<String> keys = configuration.getKeys();
    while (keys.hasNext()) {
      String key = keys.next();
      if (key.startsWith(CONFIG_WHIRR_CM_CONFIG_PREFIX)) {
        String[] keyTokens = key.substring(CONFIG_WHIRR_CM_CONFIG_PREFIX.length(), key.length()).split("\\.");
        if (keyTokens == null || keyTokens.length != 2) {
          throw new IOException("Invalid key [" + key + "], expected to be of format [" + CONFIG_WHIRR_CM_CONFIG_PREFIX
              + "<role>.<setting>]");
        }
        keyTokens[0] = keyTokens[0].toUpperCase();
        if (clusterConfiguration.get(keyTokens[0]) == null) {
          clusterConfiguration.put(keyTokens[0], new HashMap<String, String>());
        }
        clusterConfiguration.get(keyTokens[0]).put(keyTokens[1], configuration.getString(key));
      }
    }

    keys = configuration.getKeys();
    while (keys.hasNext()) {
      final String key = keys.next();
      if (key.startsWith(CONFIG_WHIRR_INTERNAL_CM_CONFIG_DEFAULT_PREFIX)) {
        String[] keyTokens = key.substring(CONFIG_WHIRR_INTERNAL_CM_CONFIG_DEFAULT_PREFIX.length(), key.length())
            .split("\\.");
        if (keyTokens == null || keyTokens.length != 2) {
          throw new IOException("Invalid key [" + key + "], expected to be of format ["
              + CONFIG_WHIRR_INTERNAL_CM_CONFIG_DEFAULT_PREFIX + "<role>.<setting>]");
        }
        keyTokens[0] = keyTokens[0].toUpperCase();
        if (configuration.getString(CONFIG_WHIRR_CM_CONFIG_PREFIX + keyTokens[0].toLowerCase() + "." + keyTokens[1]) == null) {
          if (clusterConfiguration.get(keyTokens[0]) == null) {
            clusterConfiguration.put(keyTokens[0], new HashMap<String, String>());
          }
          if (keyTokens[1].endsWith(CONFIG_CM_DIR_SUFFIX_LIST) && !mounts.isEmpty()) {
            clusterConfiguration.get(keyTokens[0]).put(keyTokens[1],
                Joiner.on(',').join(Lists.transform(Lists.newArrayList(mounts), new Function<String, String>() {
                  @Override
                  public String apply(String input) {
                    return input + configuration.getString(key);
                  }
                })));
          } else {
            clusterConfiguration.get(keyTokens[0]).put(
                keyTokens[1],
                (mounts.isEmpty() ? configuration.getString(CONFIG_WHIRR_INTERNAL_DATA_DIRS_DEFAULT) : mounts
                    .iterator().next()) + configuration.getString(key));
          }
        }
      }
    }

    keys = configuration.getKeys();
    while (keys.hasNext()) {
      final String key = keys.next();
      if (key.startsWith(CONFIG_WHIRR_CM_CONFIG_PREFIX) && key.endsWith(CONFIG_CM_DB_SUFFIX_TYPE)) {
        String[] keyTokens = key.substring(CONFIG_WHIRR_CM_CONFIG_PREFIX.length(), key.length()).split("\\.");
        if (keyTokens == null || keyTokens.length != 2) {
          throw new IOException("Invalid key [" + key + "], expected to be of format ["
              + CONFIG_WHIRR_INTERNAL_CM_CONFIG_DEFAULT_PREFIX + "<role>.<setting>]");
        }
        keyTokens[0] = keyTokens[0].toUpperCase();
        if (configuration.getString(key) != null && configuration.getString(key).length() == 0) {
          clusterConfiguration.get(keyTokens[0]).put(keyTokens[1], configuration.getString(CONFIG_WHIRR_DB_TYPE));
          if (configuration.getString(key.replace(CONFIG_CM_DB_SUFFIX_TYPE, CONFIG_CM_DB_SUFFIX_PORT)) != null
              && configuration.getString(key.replace(CONFIG_CM_DB_SUFFIX_TYPE, CONFIG_CM_DB_SUFFIX_PORT)).length() == 0) {
            clusterConfiguration.get(keyTokens[0]).put(
                keyTokens[1].replace(CONFIG_CM_DB_SUFFIX_TYPE, CONFIG_CM_DB_SUFFIX_PORT),
                configuration.getString(CONFIG_WHIRR_INTERNAL_PORTS_DB_PREFIX
                    + configuration.getString(CONFIG_WHIRR_DB_TYPE)));
          } else if (configuration.getString(key.replace(CONFIG_CM_DB_SUFFIX_TYPE, CONFIG_CM_DB_SUFFIX_HOST)) != null
              && !configuration.getString(key.replace(CONFIG_CM_DB_SUFFIX_TYPE, CONFIG_CM_DB_SUFFIX_HOST))
                  .contains(":")) {
            clusterConfiguration.get(keyTokens[0]).put(
                keyTokens[1].replace(CONFIG_CM_DB_SUFFIX_TYPE, CONFIG_CM_DB_SUFFIX_HOST),
                configuration.getString(key.replace(CONFIG_CM_DB_SUFFIX_TYPE, CONFIG_CM_DB_SUFFIX_HOST))
                    + ":"
                    + configuration.getString(CONFIG_WHIRR_INTERNAL_PORTS_DB_PREFIX
                        + configuration.getString(CONFIG_WHIRR_DB_TYPE)));
          }
        }
      }
    }

    if (clusterConfiguration.get(CmServerServiceType.CLUSTER.getId()) == null) {
      clusterConfiguration.put(CmServerServiceType.CLUSTER.getId(), new HashMap<String, String>());
    }
    if (clusterConfiguration.get(CmServerServiceType.CLUSTER.getId()).get(CONFIG_CM_LICENSE_PROVIDED) == null) {
      if (Utils.urlForURI(configuration.getString(CONFIG_WHIRR_CM_LICENSE_URI)) != null) {
        clusterConfiguration.get(CmServerServiceType.CLUSTER.getId()).put(CONFIG_CM_LICENSE_PROVIDED,
            Boolean.TRUE.toString());
      } else {
        clusterConfiguration.get(CmServerServiceType.CLUSTER.getId()).put(CONFIG_CM_LICENSE_PROVIDED,
            Boolean.FALSE.toString());
      }
    }

    if (clusterConfiguration.get(CmServerServiceType.CLUSTER.getId()).get(CONFIG_CM_LICENSE_PROVIDED)
        .equals(Boolean.TRUE.toString())) {
      if (clusterConfiguration.get(CmServerServiceType.MAPREDUCE_TASK_TRACKER.getId()) == null) {
        clusterConfiguration.put(CmServerServiceType.MAPREDUCE_TASK_TRACKER.getId(), new HashMap<String, String>());
      }
      clusterConfiguration.get(CmServerServiceType.MAPREDUCE_TASK_TRACKER.getId()).put(
          CONFIG_CM_TASKTRACKER_INSTRUMENTATION, "org.apache.hadoop.mapred.TaskTrackerCmonInst");
    }

    return clusterConfiguration;

  }

  public static boolean logCluster(CmServerLog logger, String label, Configuration configuration,
      CmServerCluster cluster, Set<Instance> instances) throws IOException {
    if (!instances.isEmpty()) {
      logger.logOperationInProgressSync(label, "HOSTS");
      for (Instance instance : instances) {
        logger.logOperationInProgressSync(label, "  " + instance.getId() + "@" + instance.getPublicHostName() + "@"
            + instance.getPublicIp() + "@" + instance.getPrivateIp());
      }
    }
    if (!cluster.getServiceTypes(CmServerServiceType.CLUSTER).isEmpty()) {
      for (CmServerServiceType type : cluster.getServiceTypes()) {
        logger.logOperationInProgressSync(label, "CDH " + type.toString() + " SERVICE");
        for (CmServerService service : cluster.getServices(type)) {
          logger.logOperationInProgressSync(label,
              "  " + service.getName() + "@" + service.getIp() + "=" + service.getStatus());
        }
      }
    }
    if (!cluster.getAgents().isEmpty()) {
      logger.logOperationInProgressSync(label, "CM AGENTS");
    }
    SortedSet<String> cmAgentsSorted = new TreeSet<String>();
    for (CmServerService cmAgent : cluster.getAgents()) {
      cmAgentsSorted.add("  ssh -o StrictHostKeyChecking=no -i "
          + configuration.getString(ClusterSpec.Property.PRIVATE_KEY_FILE.getConfigName()) + " "
          + configuration.getString(ClusterSpec.Property.CLUSTER_USER.getConfigName()) + "@" + cmAgent.getIp());
    }
    for (String cmAgentSorted : cmAgentsSorted) {
      logger.logOperationInProgressSync(label, cmAgentSorted);
    }
    if (!cluster.getNodes().isEmpty()) {
      logger.logOperationInProgressSync(label, "CM NODES");
    }
    SortedSet<String> cmNodesSorted = new TreeSet<String>();
    for (CmServerService cmNode : cluster.getNodes()) {
      cmNodesSorted.add("  ssh -o StrictHostKeyChecking=no -i "
          + configuration.getString(ClusterSpec.Property.PRIVATE_KEY_FILE.getConfigName()) + " "
          + configuration.getString(ClusterSpec.Property.CLUSTER_USER.getConfigName()) + "@" + cmNode.getIp());
    }
    for (String cmNodeSorted : cmNodesSorted) {
      logger.logOperationInProgressSync(label, cmNodeSorted);
    }
    logger.logOperationInProgressSync(label, "CM SERVER");
    if (cluster.getServer() != null) {
      logger.logOperationInProgressSync(
          label,
          "  http://" + cluster.getServer().getHost() + ":"
              + configuration.getString(CmConstants.CONFIG_WHIRR_INTERNAL_PORT_WEB));
      logger.logOperationInProgressSync(
          label,
          "  ssh -o StrictHostKeyChecking=no -i "
              + configuration.getString(ClusterSpec.Property.PRIVATE_KEY_FILE.getConfigName()) + " "
              + configuration.getString(ClusterSpec.Property.CLUSTER_USER.getConfigName()) + "@"
              + cluster.getServer().getIp());
    } else {
      logger.logOperationInProgressSync(label, "NO CM SERVER");
    }
    return !cluster.isEmpty();
  }

  public static void logHeader(CmServerLog logger, String operation) {
    logger.logSpacer();
    logger.logSpacerDashed();
    logger.logOperation(operation, "");
    logger.logSpacerDashed();
    logger.logSpacer();
  }

  public static void logLineItem(CmServerLog logger, String operation) {
    logger.logOperationStartedSync(operation);
  }

  public static void logLineItem(CmServerLog logger, String operation, String detail) {
    logger.logOperationInProgressSync(operation, detail);
  }

  public static void logLineItemAsync(final CmServerLog logger, final String operation) {
    logger.logOperationStartedAsync(operation);
    logExecutorFuture = logExecutor.submit(new Runnable() {
      @Override
      public void run() {
        int sleep = LOG_POLL_PERIOD_MS;
        int sleepBackoffNumber = LOG_POLL_PERIOD_BACKOFF_NUMBER;
        while (true) {
          try {
            logger.logOperationInProgressAsync(operation);
            if (sleepBackoffNumber-- == 0) {
              sleep += LOG_POLL_PERIOD_BACKOFF_INCRAMENT;
              sleepBackoffNumber = LOG_POLL_PERIOD_BACKOFF_NUMBER;
            }
            Thread.sleep(sleep);
          } catch (InterruptedException e) {
            return;
          }
        }
      }
    });
  }

  public static void logLineItemDetail(CmServerLog logger, String operation, String detail) {
    logger.logOperationInProgressSync(operation, detail);
  }

  public static void logLineItemFooter(CmServerLog logger, String operation) {
    logger.logOperationFinishedSync(operation);
  }

  public static void logLineItemFooterAsync(CmServerLog logger, String operation) {
    if (logExecutorFuture != null) {
      logExecutorFuture.cancel(true);
    }
    logger.logOperationFinishedAsync(operation);
  }

  public static void logLineItemFooterFinal(CmServerLog logger) {
    logger.logSpacer();
    logger.logSpacerDashed();
    logger.logSpacer();
  }

  public static void logException(CmServerLog logger, String operation, String message, Throwable throwable) {
    if (logExecutorFuture != null) {
      logExecutorFuture.cancel(true);
    }
    logger.logOperationInProgressSync(operation, "failed");
    logger.logOperationStackTrace(operation, throwable);
    logger.logSpacer();
    logger.logOperation(operation, message);
  }

  private static class Key {
    private String provider;
    private String endpoint;
    private String identity;
    private String clusterName;
    private String version;

    private final String key;

    public Key(ClusterSpec spec) {
      provider = spec.getProvider();
      endpoint = spec.getEndpoint();
      identity = spec.getIdentity();
      clusterName = spec.getClusterName();
      version = spec.getVersion();

      key = Objects.toStringHelper("").omitNullValues().add("provider", provider).add("endpoint", endpoint)
          .add("identity", identity).add("clusterName", clusterName).add("version", version).toString();
    }

    @Override
    public boolean equals(Object that) {
      if (that instanceof Key) {
        return Objects.equal(this.key, ((Key) that).key);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(key);
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this).add("provider", provider).add("endpoint", identity).add("identity", identity)
          .add("clusterName", clusterName).add("version", version).toString();
    }
  }

  public static SortedSet<String> getMounts(ClusterSpec specification, Cluster cluster) throws IOException {
    return getMounts(specification, cluster == null ? null : cluster.getInstances());
  }

  @SuppressWarnings("unchecked")
  public static SortedSet<String> getMounts(ClusterSpec specification, Set<Instance> instances) throws IOException {
    Configuration configuration = getConfiguration(specification);
    SortedSet<String> mounts = new TreeSet<String>();
    Set<String> deviceMappings = CmServerClusterInstance.getDeviceMappings(specification, instances).keySet();
    if (!configuration.getList(CONFIG_WHIRR_DATA_DIRS_ROOT).isEmpty()) {
      mounts.addAll(configuration.getList(CONFIG_WHIRR_DATA_DIRS_ROOT));
    } else if (!deviceMappings.isEmpty()) {
      mounts.addAll(deviceMappings);
    } else {
      mounts.add(configuration.getString(CONFIG_WHIRR_INTERNAL_DATA_DIRS_DEFAULT));
    }
    return mounts;
  }

  public static Map<String, String> getDeviceMappings(ClusterSpec specification, Cluster cluster) {
    return getDeviceMappings(specification, cluster == null ? null : cluster.getInstances());
  }

  public static Map<String, String> getDeviceMappings(ClusterSpec specification, Set<Instance> instances) {
    Map<String, String> deviceMappings = new HashMap<String, String>();
    if (specification != null && instances != null && !instances.isEmpty()) {
      deviceMappings.putAll(new VolumeManager().getDeviceMappings(specification, instances.iterator().next()));
    }
    return deviceMappings;
  }

}