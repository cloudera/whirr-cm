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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import org.apache.whirr.Cluster.Instance;

import com.cloudera.api.ClouderaManagerClientBuilder;
import com.cloudera.api.DataView;
import com.cloudera.api.model.ApiBulkCommandList;
import com.cloudera.api.model.ApiCluster;
import com.cloudera.api.model.ApiClusterList;
import com.cloudera.api.model.ApiClusterVersion;
import com.cloudera.api.model.ApiCommand;
import com.cloudera.api.model.ApiConfig;
import com.cloudera.api.model.ApiConfigList;
import com.cloudera.api.model.ApiHost;
import com.cloudera.api.model.ApiHostRef;
import com.cloudera.api.model.ApiHostRefList;
import com.cloudera.api.model.ApiParcel;
import com.cloudera.api.model.ApiRole;
import com.cloudera.api.model.ApiRoleConfigGroup;
import com.cloudera.api.model.ApiRoleConfigGroupRef;
import com.cloudera.api.model.ApiRoleNameList;
import com.cloudera.api.model.ApiService;
import com.cloudera.api.model.ApiServiceConfig;
import com.cloudera.api.model.ApiServiceList;
import com.cloudera.api.v3.ParcelResource;
import com.cloudera.api.v3.RootResourceV3;
import com.cloudera.whirr.cm.api.CmServerApiLog.CmServerApiLogSyncCommand;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

public class CmServerApi {

  private static final String CM_PARCEL_STAGE_DOWNLOADED = "DOWNLOADED";
  private static final String CM_PARCEL_STAGE_DISTRIBUTED = "DISTRIBUTED";
  private static final String CM_PARCEL_STAGE_ACTIVATED = "ACTIVATED";

  private static int API_POLL_PERIOD_MS = 500;

  private CmServerApiLog logger;
  final private RootResourceV3 apiResourceRoot;

  public CmServerApi(String ip, int port, String user, String password) {
    this(ip, port, user, password, new CmServerApiLog.CmServerApiLogSlf4j());
  }

  public CmServerApi(String ip, int port, String user, String password, CmServerApiLog logger) {
    this.logger = logger;
    this.apiResourceRoot = new ClouderaManagerClientBuilder().withHost(ip).withPort(port)
        .withUsernamePassword(user, password).build().getRootV3();
  }

  public String getName(CmServerCluster cluster) {
    try {
      return cluster.getServiceName(CmServerServiceType.CLUSTER);
    } catch (IOException e) {
      throw new RuntimeException("Could not resolve cluster name", e);
    }
  }

  public boolean isProvisioned(CmServerCluster cluster) throws CmServerApiException {

    boolean isProvisioned = false;
    try {

      logger.logOperationStartedSync("ClusterIsProvisioned");

      for (ApiCluster apiCluster : apiResourceRoot.getClustersResource().readClusters(DataView.SUMMARY)) {
        if (apiCluster.getName().equals(getName(cluster))) {
          isProvisioned = true;
          break;
        }
      }

      logger.logOperationFinishedSync("ClusterIsProvisioned");

    } catch (Exception e) {
      throw new CmServerApiException("Failed to detrermine if cluster is provisioned", e);
    }
    return isProvisioned;

  }

  public void isProvisionedAssert(CmServerCluster cluster, String task) throws CmServerApiException {

    if (!isProvisioned(cluster)) {
      throw new CmServerApiException("Cluster is not provisioned, cannot " + task);
    }

  }

  public Map<String, String> initialise(Map<String, String> config) throws CmServerApiException {
    Map<String, String> configPostUpdate = null;
    try {

      logger.logOperationStartedSync("ClusterInitialise");

      configPostUpdate = provisionCmSettings(config);

      logger.logOperationFinishedSync("ClusterInitialise");

    } catch (Exception e) {
      throw new CmServerApiException("Failed to initialise cluster", e);
    }
    return configPostUpdate;
  }

  public Set<ApiHost> hosts() throws CmServerApiException {

    final Set<ApiHost> hosts = new HashSet<ApiHost>();
    try {

      logger.logOperation("GetHosts", new CmServerApiLogSyncCommand() {
        @Override
        public void execute() {
          for (ApiHost host : apiResourceRoot.getHostsResource().readHosts(DataView.SUMMARY).getHosts()) {
            hosts.add(host);
          }
        }
      });

    } catch (Exception e) {
      throw new CmServerApiException("Failed to list cluster hosts", e);
    }

    return hosts;
  }

  public void provision(CmServerCluster cluster) throws CmServerApiException {

    try {

      logger.logOperationStartedSync("ClusterProvision");

      if (!isProvisioned(cluster)) {
        provsionCluster(cluster);
        provisionParcels(cluster);
      }

      logger.logOperationFinishedSync("ClusterProvision");

    } catch (Exception e) {
      throw new CmServerApiException("Failed to provision cluster", e);
    }

  }

  public void configure(CmServerCluster cluster) throws CmServerApiException {

    try {

      logger.logOperationStartedSync("ClusterConfigure");

      isProvisionedAssert(cluster, "configure");
      configureServices(cluster);

      logger.logOperationFinishedSync("ClusterConfigure");

    } catch (Exception e) {
      throw new CmServerApiException("Failed to configure cluster", e);
    }

  }

  public void startFirst(CmServerCluster cluster) throws CmServerApiException {

    try {

      logger.logOperationStartedSync("ClusterFirstStart");

      isProvisionedAssert(cluster, "first start");
      for (CmServerServiceType type : cluster.getServiceTypes()) {
        initPreStartServices(cluster, type);
        startService(cluster, type);
        initPostStartServices(cluster, type);
      }

      logger.logOperationFinishedSync("ClusterFirstStart");

    } catch (Exception e) {
      throw new CmServerApiException("Failed to first start cluster", e);
    }

  }

  public void start(final CmServerCluster cluster) throws CmServerApiException {

    try {

      logger.logOperationStartedSync("ClusterStart");

      isProvisionedAssert(cluster, "start");
      logger.logOperation("StartCluster", new CmServerApiLogSyncCommand() {
        @Override
        public void execute() throws IOException {
          apiResourceRoot.getClustersResource().startCommand(getName(cluster));
        }
      });

      logger.logOperationFinishedSync("ClusterStart");

    } catch (Exception e) {
      throw new CmServerApiException("Failed to start cluster", e);
    }

  }

  public void stop(final CmServerCluster cluster) throws CmServerApiException {

    try {

      logger.logOperationStartedSync("ClusterStop");

      isProvisionedAssert(cluster, "stop");
      logger.logOperation("StopCluster", new CmServerApiLogSyncCommand() {
        @Override
        public void execute() throws IOException {
          apiResourceRoot.getClustersResource().startCommand(getName(cluster));
        }
      });

      logger.logOperationFinishedSync("ClusterStop");

    } catch (Exception e) {
      throw new CmServerApiException("Failed to stop cluster", e);
    }

  }

  public void unconfigure(final CmServerCluster cluster) throws CmServerApiException {

    try {

      logger.logOperationStartedSync("ClusterUnConfigure");

      if (isProvisioned(cluster)) {
        unconfigureServices(cluster);
      }

      logger.logOperationFinishedSync("ClusterUnConfigure");

    } catch (Exception e) {
      throw new CmServerApiException("Failed to unconfigure cluster", e);
    }

  }

  public void unprovision(final CmServerCluster cluster) throws CmServerApiException {

    try {

      logger.logOperationStartedSync("ClusterUnProvision");

      if (isProvisioned(cluster)) {
        logger.logOperation("UnProvisionCluster", new CmServerApiLogSyncCommand() {
          @Override
          public void execute() throws IOException {
            apiResourceRoot.getClustersResource().deleteCluster(getName(cluster));
          }
        });
      }

      logger.logOperationFinishedSync("ClusterUnProvision");

    } catch (Exception e) {
      throw new CmServerApiException("Failed to unprovision cluster", e);
    }

  }

  private Map<String, String> provisionCmSettings(Map<String, String> config) throws InterruptedException {

    Map<String, String> configPostUpdate = new HashMap<String, String>();
    ApiConfigList apiConfigList = new ApiConfigList();
    if (!config.isEmpty()) {
      for (String key : config.keySet()) {
        apiConfigList.add(new ApiConfig(key, config.get(key)));
      }
      apiResourceRoot.getClouderaManagerResource().updateConfig(apiConfigList);
    }
    apiConfigList = apiResourceRoot.getClouderaManagerResource().getConfig(DataView.SUMMARY);
    for (ApiConfig apiConfig : apiConfigList) {
      configPostUpdate.put(apiConfig.getName(), apiConfig.getValue());
    }

    return configPostUpdate;

  }

  private void provsionCluster(final CmServerCluster cluster) throws IOException, InterruptedException,
      CmServerApiException {

    execute(apiResourceRoot.getClouderaManagerResource().inspectHostsCommand());

    final ApiClusterList clusterList = new ApiClusterList();
    ApiCluster apiCluster = new ApiCluster();
    apiCluster.setName(getName(cluster));
    apiCluster.setVersion(ApiClusterVersion.CDH4);
    clusterList.add(apiCluster);

    logger.logOperation("CreateCluster", new CmServerApiLogSyncCommand() {
      @Override
      public void execute() throws IOException {
        apiResourceRoot.getClustersResource().createClusters(clusterList);
      }
    });

    List<ApiHostRef> apiHostRefs = Lists.newArrayList();
    for (ApiHost host : hosts()) {
      apiHostRefs.add(new ApiHostRef(host.getHostId()));
    }
    apiResourceRoot.getClustersResource().addHosts(getName(cluster), new ApiHostRefList(apiHostRefs));

  }

  private void provisionParcels(final CmServerCluster cluster) throws InterruptedException, IOException {

    apiResourceRoot.getClouderaManagerResource().updateConfig(
        new ApiConfigList(Arrays.asList(new ApiConfig[] { new ApiConfig("PARCEL_UPDATE_FREQ", "1") })));
    final Set<CmServerServiceTypeRepository> repositoriesRequired = new HashSet<CmServerServiceTypeRepository>();
    for (CmServerServiceType type : cluster.getServiceTypes()) {
      repositoriesRequired.add(type.getRepository());
    }
    execute("Wait For Parcels Availability", new Callback() {
      @Override
      public boolean poll() {
        Set<CmServerServiceTypeRepository> repositoriesNotLoaded = new HashSet<CmServerServiceTypeRepository>(
            repositoriesRequired);
        for (ApiParcel parcel : apiResourceRoot.getClustersResource().getParcelsResource(getName(cluster))
            .readParcels(DataView.FULL).getParcels()) {
          try {
            repositoriesNotLoaded.remove(CmServerServiceTypeRepository.valueOf(parcel.getProduct()));
          } catch (IllegalArgumentException e) {
            // ignore
          }
        }
        return repositoriesNotLoaded.isEmpty();
      }
    });
    apiResourceRoot.getClouderaManagerResource().updateConfig(
        new ApiConfigList(Arrays.asList(new ApiConfig[] { new ApiConfig("PARCEL_UPDATE_FREQ", "60") })));

    for (CmServerServiceTypeRepository repository : repositoriesRequired) {
      DefaultArtifactVersion parcelVersion = null;
      for (ApiParcel apiParcel : apiResourceRoot.getClustersResource().getParcelsResource(getName(cluster))
          .readParcels(DataView.FULL).getParcels()) {
        if (apiParcel.getProduct().equals(repository.toString())
            && (parcelVersion == null || parcelVersion.compareTo(new DefaultArtifactVersion(apiParcel.getVersion())) < 0)) {
          parcelVersion = new DefaultArtifactVersion(apiParcel.getVersion());
        }
      }
      final ParcelResource apiParcelResource = apiResourceRoot.getClustersResource()
          .getParcelsResource(getName(cluster)).getParcelResource(repository.toString(), parcelVersion.toString());
      execute(apiParcelResource.startDownloadCommand(), new Callback() {
        @Override
        public boolean poll() {
          return apiParcelResource.readParcel().getStage().equals(CM_PARCEL_STAGE_DOWNLOADED);
        }
      }, false);
      execute(apiParcelResource.startDistributionCommand(), new Callback() {
        @Override
        public boolean poll() {
          return apiParcelResource.readParcel().getStage().equals(CM_PARCEL_STAGE_DISTRIBUTED);
        }
      }, false);
      execute(apiParcelResource.activateCommand(), new Callback() {
        @Override
        public boolean poll() {
          return apiParcelResource.readParcel().getStage().equals(CM_PARCEL_STAGE_ACTIVATED);
        }
      }, false);
    }

  }

  private void configureServices(final CmServerCluster cluster) throws IOException, InterruptedException, CmServerApiException {

    final ApiServiceList serviceList = new ApiServiceList();
    for (CmServerServiceType type : cluster.getServiceTypes()) {
      serviceList.add(buildServices(cluster, type));
    }

    logger.logOperation("CreateClusterServices", new CmServerApiLogSyncCommand() {
      @Override
      public void execute() throws IOException, InterruptedException {
        apiResourceRoot.getClustersResource().getServicesResource(getName(cluster)).createServices(serviceList);
      }
    });

    // Necessary, since createServices a habit of kicking off async commands (eg ZkAutoInit )
    for (CmServerServiceType type : cluster.getServiceTypes()) {
      for (ApiCommand command : apiResourceRoot.getClustersResource().getServicesResource(getName(cluster))
          .listActiveCommands(cluster.getServiceName(type), DataView.SUMMARY)) {
        CmServerApi.this.execute(command, false);
      }
    }

  }

  private void unconfigureServices(final CmServerCluster cluster) throws IOException, InterruptedException {

    Set<CmServerServiceType> types = new TreeSet<CmServerServiceType>(Collections.reverseOrder());
    types.addAll(cluster.getServiceTypes());
    for (final CmServerServiceType type : types) {
      logger.logOperation("DestroyClusterService", new CmServerApiLogSyncCommand() {
        @Override
        public void execute() throws IOException {
          apiResourceRoot.getClustersResource().getServicesResource(getName(cluster))
              .deleteService(cluster.getServiceName(type));
        }
      });
    }

  }

  private ApiService buildServices(final CmServerCluster cluster, CmServerServiceType type) throws IOException, CmServerApiException {

    ApiService apiService = new ApiService();
    List<ApiRole> apiRoles = new ArrayList<ApiRole>();
    List<ApiRoleConfigGroup> apiRoleConfigGroups = Lists.newArrayList();

    apiService.setType(type.getLabel());
    apiService.setName(cluster.getServiceName(type));

    ApiServiceConfig apiServiceConfig = new ApiServiceConfig();
    // TODO Refactor, dont hardcode, pass through from whirr config
    switch (type) {
    case HDFS:
      apiServiceConfig.add(new ApiConfig("dfs_block_local_path_access_user", "impala"));
      break;
    case MAPREDUCE:
      apiServiceConfig.add(new ApiConfig("hdfs_service", cluster.getServiceName(CmServerServiceType.HDFS)));
      break;
    case HBASE:
      apiServiceConfig.add(new ApiConfig("hdfs_service", cluster.getServiceName(CmServerServiceType.HDFS)));
      apiServiceConfig.add(new ApiConfig("zookeeper_service", cluster.getServiceName(CmServerServiceType.ZOOKEEPER)));
      break;
    case HIVE:
      apiServiceConfig.add(new ApiConfig("mapreduce_yarn_service", cluster
          .getServiceName(CmServerServiceType.MAPREDUCE)));
      apiServiceConfig.add(new ApiConfig("hive_metastore_database_type", "mysql"));
      apiServiceConfig.add(new ApiConfig("hive_metastore_database_host", "localhost"));
      apiServiceConfig.add(new ApiConfig("hive_metastore_database_password", "hive"));
      apiServiceConfig.add(new ApiConfig("hive_metastore_database_port", "3306"));
      break;
    case IMPALA:
      apiServiceConfig.add(new ApiConfig("hdfs_service", cluster.getServiceName(CmServerServiceType.HDFS)));
      apiServiceConfig.add(new ApiConfig("hbase_service", cluster.getServiceName(CmServerServiceType.HBASE)));
      apiServiceConfig.add(new ApiConfig("hive_service", cluster.getServiceName(CmServerServiceType.HIVE)));
      break;
    default:
      break;
    }
    apiService.setConfig(apiServiceConfig);

    for (CmServerServiceType subType : cluster.getServiceTypes(type)) {
      ApiConfigList apiConfigList = new ApiConfigList();
      // TODO Refactor, dont hardcode, pass through from whirr config
      switch (subType) {
      case HDFS_NAMENODE:
        apiConfigList.add(new ApiConfig("dfs_name_dir_list", cluster.getDataDirsForSuffix("/dfs/nn")));
        break;
      case HDFS_SECONDARY_NAMENODE:
        apiConfigList.add(new ApiConfig("fs_checkpoint_dir_list", cluster.getDataDirsForSuffix("/dfs/snn")));
        break;
      case HDFS_DATANODE:
        apiConfigList.add(new ApiConfig("dfs_data_dir_list", cluster.getDataDirsForSuffix("/dfs/dn")));
        break;
      case MAPREDUCE_JOB_TRACKER:
        apiConfigList.add(new ApiConfig("jobtracker_mapred_local_dir_list", cluster.getDataDirsForSuffix("/mapred/jt")));
        break;
      case MAPREDUCE_TASK_TRACKER:
        apiConfigList.add(new ApiConfig("tasktracker_mapred_local_dir_list", cluster.getDataDirsForSuffix("/mapred/local")));
        break;
      default:
        break;
      }
      ApiRoleConfigGroup apiRoleConfigGroup = new ApiRoleConfigGroup();
      apiRoleConfigGroup.setRoleType(subType.getLabel());
      apiRoleConfigGroup.setConfig(apiConfigList);
      apiRoleConfigGroup.setName(cluster.getService(subType).getGroup());
      apiRoleConfigGroup.setBase(false);
      apiRoleConfigGroups.add(apiRoleConfigGroup);
    }

    for (CmServerService subService : cluster.getServices(type)) {
      String hostId = getHostIdForInstance(subService.getHost());
      ApiRole apiRole = new ApiRole();
      apiRole.setName(subService.getName());
      apiRole.setType(subService.getType().getLabel());
      apiRole.setHostRef(new ApiHostRef(hostId));
      apiRole.setRoleConfigGroupRef(new ApiRoleConfigGroupRef(subService.getGroup()));
      apiRoles.add(apiRole);
    }

    apiService.setRoleConfigGroups(apiRoleConfigGroups);
    apiService.setRoles(apiRoles);

    return apiService;
  }

  private String getHostIdForInstance(Instance instance) throws IOException, CmServerApiException {
    String hostId;
    Set<ApiHost> hosts = hosts();

    final String hostName = instance.getPublicHostName();
    final String privateIp = instance.getPrivateIp();
    final String publicIp = instance.getPublicIp();
    ApiHost actualHost = Iterables.getOnlyElement(Sets.filter(hosts, new Predicate<ApiHost>() {
          @Override
          public boolean apply(ApiHost host) {
            return host.getHostname().equals(hostName)
            || host.getIpAddress().equals(privateIp)
            || host.getIpAddress().equals(publicIp);
          }
        }), null);

    if (actualHost != null) {
      hostId = actualHost.getHostId();
    } else {
      hostId = "";
    }

    return hostId;
  }
  
  private void initPreStartServices(final CmServerCluster cluster, CmServerServiceType type) throws IOException,
      InterruptedException {

    switch (type) {
    case HDFS:
      CmServerService service = cluster.getService(CmServerServiceType.HDFS_NAMENODE);
      if (service != null) {
        ApiRoleNameList formatList = new ApiRoleNameList();
        formatList.add(service.getName());
        execute(
            apiResourceRoot.getClustersResource().getServicesResource(getName(cluster))
                .getRoleCommandsResource(cluster.getServiceName(CmServerServiceType.HDFS)).formatCommand(formatList),
            false);
      }
      break;
    case HIVE:
      execute(apiResourceRoot.getClustersResource().getServicesResource(getName(cluster))
          .hiveCreateMetastoreDatabaseTablesCommand(cluster.getServiceName(CmServerServiceType.HIVE)));
      break;
    case HBASE:
      execute(apiResourceRoot.getClustersResource().getServicesResource(getName(cluster))
          .createHBaseRootCommand(cluster.getServiceName(CmServerServiceType.HBASE)));
      break;
    default:
      break;
    }

  }

  private void initPostStartServices(final CmServerCluster cluster, CmServerServiceType type) throws IOException,
      InterruptedException {

    switch (type) {
    case HDFS:
      CmServerService service = cluster.getService(CmServerServiceType.HDFS_NAMENODE);
      if (service != null) {
        ApiRoleNameList formatList = new ApiRoleNameList();
        formatList.add(service.getName());
        execute(apiResourceRoot.getClustersResource().getServicesResource(getName(cluster))
            .hdfsCreateTmpDir(cluster.getServiceName(CmServerServiceType.HDFS)));
      }
      break;
    default:
      break;
    }

  }

  private void startService(CmServerCluster cluster, CmServerServiceType type) throws InterruptedException, IOException {
    execute(apiResourceRoot.getClustersResource().getServicesResource(getName(cluster))
        .startCommand(cluster.getServiceName(type)));
  }

  @SuppressWarnings("unused")
  private void stopService(CmServerCluster cluster, CmServerServiceType type) throws InterruptedException, IOException {
    execute(apiResourceRoot.getClustersResource().getServicesResource(getName(cluster))
        .stopCommand(cluster.getServiceName(type)));
  }

  @SuppressWarnings("unused")
  private ApiCommand execute(final ApiBulkCommandList bulkCommand) throws InterruptedException {
    return execute(bulkCommand, true);
  }

  private ApiCommand execute(final ApiBulkCommandList bulkCommand, boolean checkReturn) throws InterruptedException {
    ApiCommand lastCommand = null;
    for (ApiCommand command : bulkCommand) {
      lastCommand = execute(command, checkReturn);
    }
    return lastCommand;
  }

  private ApiCommand execute(final ApiCommand command) throws InterruptedException {
    return execute(command, true);
  }

  private ApiCommand execute(final ApiCommand command, boolean checkReturn) throws InterruptedException {
    return execute(command.getName(), command, new Callback() {
      @Override
      public boolean poll() {
        return apiResourceRoot.getCommandsResource().readCommand(command.getId()).getEndTime() != null;
      }
    }, checkReturn);
  }

  private ApiCommand execute(ApiCommand command, Callback callback, boolean checkReturn) throws InterruptedException {
    return execute(command.getName(), command, callback, checkReturn);
  }

  private ApiCommand execute(String label, Callback callback) throws InterruptedException {
    return execute(label, null, callback, false);
  }

  private ApiCommand execute(String label, ApiCommand command, Callback callback, boolean checkReturn)
      throws InterruptedException {

    logger.logOperationStartedAsync(label);

    ApiCommand commandReturn = null;
    while (true) {

      logger.logOperationInProgressAsync(label);

      if (callback.poll()) {
        if (checkReturn && command != null
            && !(commandReturn = apiResourceRoot.getCommandsResource().readCommand(command.getId())).getSuccess()) {
          throw new RuntimeException("Command [" + command + "] failed [" + commandReturn + "]");
        }

        logger.logOperationFinishedAsync(label);

        return commandReturn;
      }
      Thread.sleep(API_POLL_PERIOD_MS);
    }
  }

  private static abstract class Callback {
    public abstract boolean poll();
  }

}
