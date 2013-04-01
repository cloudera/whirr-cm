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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang.WordUtils;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

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
import com.cloudera.api.model.ApiRoleState;
import com.cloudera.api.model.ApiService;
import com.cloudera.api.model.ApiServiceConfig;
import com.cloudera.api.model.ApiServiceList;
import com.cloudera.api.v3.ParcelResource;
import com.cloudera.api.v3.RootResourceV3;
import com.cloudera.whirr.cm.api.CmServerApiLog.CmServerApiLogSyncCommand;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class CmServerApiImpl implements CmServerApi {

  private static final String CM_PARCEL_STAGE_DOWNLOADED = "DOWNLOADED";
  private static final String CM_PARCEL_STAGE_DISTRIBUTED = "DISTRIBUTED";
  private static final String CM_PARCEL_STAGE_ACTIVATED = "ACTIVATED";

  private static int API_POLL_PERIOD_MS = 500;

  private CmServerApiLog logger;
  final private RootResourceV3 apiResourceRoot;
  private boolean isFirstStartRequired = true;

  protected CmServerApiImpl(String ip, int port, String user, String password, CmServerApiLog logger) {
    this.logger = logger;
    this.apiResourceRoot = new ClouderaManagerClientBuilder().withHost(ip).withPort(port)
        .withUsernamePassword(user, password).build().getRootV3();
  }

  @Override
  public String getName(CmServerCluster cluster) {
    try {
      return cluster.getServiceName(CmServerServiceType.CLUSTER);
    } catch (IOException e) {
      throw new RuntimeException("Could not resolve cluster name", e);
    }
  }

  @Override
  public boolean getServiceConfigs(final CmServerCluster cluster, final File directory) throws CmServerApiException {

    final AtomicBoolean executed = new AtomicBoolean(false);
    try {

      logger.logOperation("GetConfig", new CmServerApiLogSyncCommand() {
        @Override
        public void execute() throws IOException {
          for (ApiService apiService : apiResourceRoot.getClustersResource().getServicesResource(getName(cluster))
              .readServices(DataView.SUMMARY)) {
            switch (CmServerServiceType.valueOfId(apiService.getType())) {
            case HDFS:
            case MAPREDUCE:
            case HIVE:
              ZipInputStream configInput = null;
              try {
                configInput = new ZipInputStream(apiResourceRoot.getClustersResource()
                    .getServicesResource(getName(cluster)).getClientConfig(apiService.getName()).getInputStream());
                ZipEntry configInputZipEntry = null;
                while ((configInputZipEntry = configInput.getNextEntry()) != null) {
                  String configFile = configInputZipEntry.getName();
                  if (configFile.contains(File.separator)) {
                    configFile = configFile.substring(configFile.lastIndexOf(File.separator), configFile.length());
                  }
                  directory.mkdirs();
                  BufferedWriter configOutput = null;
                  try {
                    configOutput = new BufferedWriter(new FileWriter(new File(directory, configFile)));
                    while (configInput.available() > 0) {
                      configOutput.write(configInput.read());
                    }
                  } finally {
                    configOutput.close();
                  }
                }
              } finally {
                if (configInput != null) {
                  configInput.close();
                }
              }
              executed.set(true);
              break;
            default:
              break;
            }
          }
        }
      });

    } catch (Exception e) {
      throw new CmServerApiException("Failed to list cluster hosts", e);
    }

    return executed.get();

  }

  @Override
  public List<CmServerService> getServiceHosts() throws CmServerApiException {

    final List<CmServerService> services = new ArrayList<CmServerService>();
    try {

      logger.logOperation("GetHosts", new CmServerApiLogSyncCommand() {
        @Override
        public void execute() {
          for (ApiHost host : apiResourceRoot.getHostsResource().readHosts(DataView.SUMMARY).getHosts()) {
            services.add(new CmServerService(host.getHostId(), host.getIpAddress()));
          }
        }
      });

    } catch (Exception e) {
      throw new CmServerApiException("Failed to list cluster hosts", e);
    }

    return services;
  }

  @Override
  public CmServerService getServiceHost(CmServerService service) throws CmServerApiException {

    return getServiceHost(service, getServiceHosts());

  }

  @Override
  public CmServerService getServiceHost(CmServerService service, List<CmServerService> services)
      throws CmServerApiException {

    CmServerService serviceFound = null;
    try {

      for (CmServerService serviceTmp : services) {
        if ((service.getHost() != null && service.getHost().equals(serviceTmp.getHost()))
            || (service.getIp() != null && service.getIp().equals(serviceTmp.getIp()))
            || (service.geIpInternal() != null && service.geIpInternal().equals(serviceTmp.getIp()))) {
          serviceFound = serviceTmp;
          break;
        }
      }

    } catch (Exception e) {
      throw new CmServerApiException("Failed to find service", e);
    }

    if (serviceFound == null) {
      throw new CmServerApiException("Failed to find service matching " + service);
    } else {
      return serviceFound;
    }

  }

  @Override
  public List<CmServerService> getServices(final CmServerCluster cluster) throws CmServerApiException {

    final List<CmServerService> services = new ArrayList<CmServerService>();
    try {

      logger.logOperation("GetServices", new CmServerApiLogSyncCommand() {
        @Override
        public void execute() throws IOException, CmServerApiException {
          for (ApiService apiService : apiResourceRoot.getClustersResource().getServicesResource(getName(cluster))
              .readServices(DataView.SUMMARY)) {
            for (ApiRole apiRole : apiResourceRoot.getClustersResource().getServicesResource(getName(cluster))
                .getRolesResource(apiService.getName()).readRoles()) {
              services.add(new CmServerService(apiRole.getName(), apiRole.getHostRef().getHostId(), null, null));
            }
          }
        }
      });

    } catch (Exception e) {
      throw new CmServerApiException("Failed to find services", e);
    }

    return services;

  }

  @Override
  public CmServerService getService(final CmServerCluster cluster, final CmServerServiceType type)
      throws CmServerApiException {

    List<CmServerService> services = getServices(cluster, type);
    if (services.isEmpty()) {
      throw new CmServerApiException("Failed to find service matching type [" + type + "]");
    }

    return services.get(0);

  }

  @Override
  public List<CmServerService> getServices(final CmServerCluster cluster, final CmServerServiceType type)
      throws CmServerApiException {

    List<CmServerService> services = new ArrayList<CmServerService>();
    try {

      for (CmServerService service : getServices(cluster)) {
        if (type.equals(CmServerServiceType.CLUSTER) || type.equals(service.getType().getParent())
            || type.equals(service.getType())) {
          services.add(service);
        }
      }

    } catch (Exception e) {
      throw new CmServerApiException("Failed to find services", e);
    }

    return services;

  }

  @Override
  public boolean isProvisioned(final CmServerCluster cluster) throws CmServerApiException {

    boolean executed = false;
    try {

      for (ApiCluster apiCluster : apiResourceRoot.getClustersResource().readClusters(DataView.SUMMARY)) {
        if (apiCluster.getName().equals(getName(cluster))) {
          executed = true;
          break;
        }
      }

    } catch (Exception e) {
      throw new CmServerApiException("Failed to detrermine if cluster is provisioned", e);
    }

    return executed;

  }

  @Override
  public boolean isConfigured(final CmServerCluster cluster) throws CmServerApiException {

    boolean executed = false;
    final Set<String> servicesNotConfigured = new HashSet<String>();
    try {

      if (isProvisioned(cluster)) {
        for (CmServerService service : cluster.getServices(CmServerServiceType.CLUSTER)) {
          servicesNotConfigured.add(service.getName());
        }
        for (ApiService apiService : apiResourceRoot.getClustersResource().getServicesResource(getName(cluster))
            .readServices(DataView.SUMMARY)) {
          for (ApiRole apiRole : apiResourceRoot.getClustersResource().getServicesResource(getName(cluster))
              .getRolesResource(apiService.getName()).readRoles()) {
            servicesNotConfigured.remove(apiRole.getName());
          }
        }
        executed = true;
      }

    } catch (Exception e) {
      throw new CmServerApiException("Failed to detrermine if cluster is configured", e);
    }

    return executed && servicesNotConfigured.size() == 0;

  }

  @Override
  public boolean isStarted(final CmServerCluster cluster) throws CmServerApiException {

    boolean executed = true;
    final Set<String> servicesNotStarted = new HashSet<String>();
    try {

      if (isConfigured(cluster)) {
        for (CmServerService service : cluster.getServices(CmServerServiceType.CLUSTER)) {
          servicesNotStarted.add(service.getName());
        }
        for (ApiService apiService : apiResourceRoot.getClustersResource().getServicesResource(getName(cluster))
            .readServices(DataView.SUMMARY)) {
          for (ApiRole apiRole : apiResourceRoot.getClustersResource().getServicesResource(getName(cluster))
              .getRolesResource(apiService.getName()).readRoles()) {
            if (apiRole.getRoleState().equals(ApiRoleState.STARTED)) {
              servicesNotStarted.remove(apiRole.getName());
            }
          }
        }
      } else {
        executed = false;
      }

    } catch (Exception e) {
      throw new CmServerApiException("Failed to detrermine if cluster is started", e);
    }

    return executed ? servicesNotStarted.size() == 0 : false;

  }

  @Override
  public boolean isStopped(final CmServerCluster cluster) throws CmServerApiException {

    final Set<String> servicesNotStopped = new HashSet<String>();
    try {

      if (isConfigured(cluster)) {
        for (CmServerService service : cluster.getServices(CmServerServiceType.CLUSTER)) {
          servicesNotStopped.add(service.getName());
        }
        for (ApiService apiService : apiResourceRoot.getClustersResource().getServicesResource(getName(cluster))
            .readServices(DataView.SUMMARY)) {
          for (ApiRole apiRole : apiResourceRoot.getClustersResource().getServicesResource(getName(cluster))
              .getRolesResource(apiService.getName()).readRoles()) {
            if (apiRole.getRoleState().equals(ApiRoleState.STOPPED)) {
              servicesNotStopped.remove(apiRole.getName());
            }
          }
        }
      }

    } catch (Exception e) {
      throw new CmServerApiException("Failed to detrermine if cluster is stopped", e);
    }

    return servicesNotStopped.size() == 0;

  }

  @Override
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

  @Override
  public boolean provision(CmServerCluster cluster) throws CmServerApiException {

    boolean executed = false;
    try {

      logger.logOperationStartedSync("ClusterProvision");

      if (!isProvisioned(cluster)) {
        provsionCluster(cluster);
        provisionParcels(cluster);
        executed = true;
      }

      logger.logOperationFinishedSync("ClusterProvision");

    } catch (Exception e) {
      throw new CmServerApiException("Failed to provision cluster", e);
    }

    return executed;
  }

  @Override
  public boolean configure(CmServerCluster cluster) throws CmServerApiException {

    boolean executed = false;
    try {

      logger.logOperationStartedSync("ClusterConfigure");

      if (!isProvisioned(cluster)) {
        provision(cluster);
      }
      if (!isConfigured(cluster)) {
        configureServices(cluster);
        isFirstStartRequired = true;
        executed = true;
      }

      logger.logOperationFinishedSync("ClusterConfigure");

    } catch (Exception e) {
      throw new CmServerApiException("Failed to configure cluster", e);
    }

    return executed;
  }

  @Override
  public boolean start(final CmServerCluster cluster) throws CmServerApiException {

    boolean executed = true;
    try {

      logger.logOperationStartedSync("ClusterStart");

      if (!isConfigured(cluster)) {
        configure(cluster);
      }
      if (!isStarted(cluster)) {
        for (CmServerServiceType type : cluster.getServiceTypes()) {
          if (isFirstStartRequired) {
            for (CmServerService service : cluster.getServices(type)) {
              initPreStartServices(cluster, service);
            }
          }
          startService(cluster, type);
          if (isFirstStartRequired) {
            for (CmServerService service : cluster.getServices(type)) {
              initPostStartServices(cluster, service);
            }
          }
        }
        isFirstStartRequired = false;
      } else {
        executed = false;
      }

      logger.logOperationFinishedSync("ClusterStart");

    } catch (Exception e) {
      throw new CmServerApiException("Failed to start cluster", e);
    }

    return executed;
  }

  @Override
  public boolean stop(final CmServerCluster cluster) throws CmServerApiException {

    boolean executed = true;
    try {

      logger.logOperationStartedSync("ClusterStop");

      if (isConfigured(cluster) && !isStopped(cluster)) {
        final Set<CmServerServiceType> types = new TreeSet<CmServerServiceType>(Collections.reverseOrder());
        types.addAll(cluster.getServiceTypes());
        for (CmServerServiceType type : types) {
          stopService(cluster, type);
        }
      } else {
        executed = false;
      }

      logger.logOperationFinishedSync("ClusterStop");

    } catch (Exception e) {
      throw new CmServerApiException("Failed to stop cluster", e);
    }

    return executed;
  }

  @Override
  public boolean unconfigure(final CmServerCluster cluster) throws CmServerApiException {

    boolean executed = false;
    try {

      logger.logOperationStartedSync("ClusterUnConfigure");

      if (isConfigured(cluster)) {
        if (!isStopped(cluster)) {
          stop(cluster);
        }
        unconfigureServices(cluster);
        executed = true;
      }

      logger.logOperationFinishedSync("ClusterUnConfigure");

    } catch (Exception e) {
      throw new CmServerApiException("Failed to unconfigure cluster", e);
    }

    return executed;
  }

  @Override
  public boolean unprovision(final CmServerCluster cluster) throws CmServerApiException {

    boolean executed = false;
    try {

      logger.logOperationStartedSync("ClusterUnProvision");

      if (isProvisioned(cluster)) {
        logger.logOperation("UnProvisionCluster", new CmServerApiLogSyncCommand() {
          @Override
          public void execute() throws IOException {
            apiResourceRoot.getClustersResource().deleteCluster(getName(cluster));
          }
        });
        executed = true;
      }

      logger.logOperationFinishedSync("ClusterUnProvision");

    } catch (Exception e) {
      throw new CmServerApiException("Failed to unprovision cluster", e);
    }

    return executed;

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
    for (CmServerService service : getServiceHosts()) {
      apiHostRefs.add(new ApiHostRef(service.getHost()));
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
    execute("WaitForParcelsAvailability", new Callback() {
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

  private void configureServices(final CmServerCluster cluster) throws IOException, InterruptedException,
      CmServerApiException {

    final List<CmServerService> services = getServiceHosts();
    final ApiServiceList serviceList = new ApiServiceList();
    for (CmServerServiceType type : cluster.getServiceTypes()) {
      serviceList.add(buildServices(cluster, type, services));
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
        CmServerApiImpl.this.execute(command, false);
      }
    }

  }

  private void unconfigureServices(final CmServerCluster cluster) throws IOException, InterruptedException {

    final Set<CmServerServiceType> types = new TreeSet<CmServerServiceType>(Collections.reverseOrder());
    types.addAll(cluster.getServiceTypes());
    logger.logOperation("DestroyClusterServices", new CmServerApiLogSyncCommand() {
      @Override
      public void execute() throws IOException {
        for (final CmServerServiceType type : types) {
          apiResourceRoot.getClustersResource().getServicesResource(getName(cluster))
              .deleteService(cluster.getServiceName(type));
        }
      }
    });

  }

  private ApiService buildServices(final CmServerCluster cluster, CmServerServiceType type,
      List<CmServerService> services) throws IOException, CmServerApiException {

    ApiService apiService = new ApiService();
    List<ApiRole> apiRoles = new ArrayList<ApiRole>();
    List<ApiRoleConfigGroup> apiRoleConfigGroups = Lists.newArrayList();

    apiService.setType(type.getId());
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
    case HUE:
      apiServiceConfig.add(new ApiConfig("hue_webhdfs", cluster.getServiceName(CmServerServiceType.HDFS_NAMENODE)));
      apiServiceConfig.add(new ApiConfig("hive_service", cluster.getServiceName(CmServerServiceType.HIVE)));
      apiServiceConfig.add(new ApiConfig("oozie_service", cluster.getServiceName(CmServerServiceType.OOZIE)));
      break;
    case OOZIE:
      apiServiceConfig.add(new ApiConfig("mapreduce_yarn_service", cluster
          .getServiceName(CmServerServiceType.MAPREDUCE)));
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
      Set<String> defaultMountPoints = ImmutableSet.<String> builder().add("/data/1").build();
      switch (subType) {
      case HDFS_NAMENODE:
        apiConfigList.add(new ApiConfig("dfs_name_dir_list", cluster
            .getDataDirsForSuffix(defaultMountPoints, "/dfs/nn")));
        break;
      case HDFS_SECONDARY_NAMENODE:
        apiConfigList.add(new ApiConfig("fs_checkpoint_dir_list", cluster.getDataDirsForSuffix(defaultMountPoints,
            "/dfs/snn")));
        break;
      case HDFS_DATANODE:
        apiConfigList.add(new ApiConfig("dfs_data_dir_list", cluster
            .getDataDirsForSuffix(defaultMountPoints, "/dfs/dn")));
        break;
      case MAPREDUCE_JOB_TRACKER:
        apiConfigList.add(new ApiConfig("jobtracker_mapred_local_dir_list", cluster.getDataDirsForSuffix(
            defaultMountPoints, "/mapred/jt")));
        break;
      case MAPREDUCE_TASK_TRACKER:
        apiConfigList.add(new ApiConfig("tasktracker_mapred_local_dir_list", cluster.getDataDirsForSuffix(
            defaultMountPoints, "/mapred/local")));
        break;
      default:
        break;
      }
      ApiRoleConfigGroup apiRoleConfigGroup = new ApiRoleConfigGroup();
      apiRoleConfigGroup.setRoleType(subType.getId());
      apiRoleConfigGroup.setConfig(apiConfigList);
      apiRoleConfigGroup.setName(cluster.getService(subType).getGroup());
      apiRoleConfigGroup.setBase(false);
      apiRoleConfigGroups.add(apiRoleConfigGroup);
    }
    for (CmServerService subService : cluster.getServices(type)) {
      String hostId = getServiceHost(subService, services).getHost();
      ApiRole apiRole = new ApiRole();
      apiRole.setName(subService.getName());
      apiRole.setType(subService.getType().getId());
      apiRole.setHostRef(new ApiHostRef(hostId));
      apiRole.setRoleConfigGroupRef(new ApiRoleConfigGroupRef(subService.getGroup()));
      apiRoles.add(apiRole);
    }

    apiService.setRoleConfigGroups(apiRoleConfigGroups);
    apiService.setRoles(apiRoles);

    return apiService;
  }

  private void initPreStartServices(final CmServerCluster cluster, CmServerService service) throws IOException,
      InterruptedException {

    switch (service.getType().getParent()) {
    case HIVE:
      execute(apiResourceRoot.getClustersResource().getServicesResource(getName(cluster))
          .hiveCreateMetastoreDatabaseTablesCommand(cluster.getServiceName(CmServerServiceType.HIVE)));
      break;
    case OOZIE:
      execute(apiResourceRoot.getClustersResource().getServicesResource(getName(cluster))
          .installOozieShareLib(cluster.getServiceName(CmServerServiceType.OOZIE)));
      execute(
          apiResourceRoot.getClustersResource().getServicesResource(getName(cluster))
              .createOozieDb(cluster.getServiceName(CmServerServiceType.OOZIE)), false);
      break;
    case HBASE:
      execute(apiResourceRoot.getClustersResource().getServicesResource(getName(cluster))
          .createHBaseRootCommand(cluster.getServiceName(CmServerServiceType.HBASE)));
      break;
    default:
      break;
    }

    switch (service.getType()) {
    case HDFS_NAMENODE:
      execute(
          apiResourceRoot.getClustersResource().getServicesResource(getName(cluster))
              .getRoleCommandsResource(cluster.getServiceName(CmServerServiceType.HDFS))
              .formatCommand(new ApiRoleNameList(ImmutableList.<String> builder().add(service.getName()).build())),
          false);
      break;
    case HUE_SERVER:
      execute(
          apiResourceRoot.getClustersResource().getServicesResource(getName(cluster))
              .getRoleCommandsResource(cluster.getServiceName(CmServerServiceType.HUE))
              .syncHueDbCommand(new ApiRoleNameList(ImmutableList.<String> builder().add(service.getName()).build())),
          false);
      break;
    case HUE_BEESWAX_SERVER:
      execute(apiResourceRoot.getClustersResource().getServicesResource(getName(cluster))
          .createBeeswaxWarehouseCommand(cluster.getServiceName(CmServerServiceType.HUE)));
      break;
    default:
      break;
    }

  }

  private void initPostStartServices(final CmServerCluster cluster, CmServerService service) throws IOException,
      InterruptedException {

    switch (service.getType().getParent()) {
    default:
      break;
    }

    switch (service.getType()) {
    case HDFS_NAMENODE:
      ApiRoleNameList formatList = new ApiRoleNameList();
      formatList.add(service.getName());
      execute(apiResourceRoot.getClustersResource().getServicesResource(getName(cluster))
          .hdfsCreateTmpDir(cluster.getServiceName(CmServerServiceType.HDFS)));
      break;
    default:
      break;
    }

  }

  private void startService(CmServerCluster cluster, CmServerServiceType type) throws InterruptedException, IOException {
    execute(
        "Start " + type.getId().toLowerCase(),
        apiResourceRoot.getClustersResource().getServicesResource(getName(cluster))
            .startCommand(cluster.getServiceName(type)));
  }

  private void stopService(CmServerCluster cluster, CmServerServiceType type) throws InterruptedException, IOException {
    execute(
        "Stop " + type.getId().toLowerCase(),
        apiResourceRoot.getClustersResource().getServicesResource(getName(cluster))
            .stopCommand(cluster.getServiceName(type)), false);
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

  private ApiCommand execute(String label, final ApiCommand command) throws InterruptedException {
    return execute(label, command, true);
  }

  private ApiCommand execute(final ApiCommand command, boolean checkReturn) throws InterruptedException {
    return execute(command.getName(), command, checkReturn);
  }

  private ApiCommand execute(String label, final ApiCommand command, boolean checkReturn) throws InterruptedException {
    return execute(label, command, new Callback() {
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

    logger.logOperationStartedAsync(WordUtils.capitalize(label.replace("-", " ").replace("_", " ")).replace(" ", ""));

    ApiCommand commandReturn = null;
    while (true) {

      logger.logOperationInProgressAsync(label);

      if (callback.poll()) {
        if (checkReturn && command != null
            && !(commandReturn = apiResourceRoot.getCommandsResource().readCommand(command.getId())).getSuccess()) {

          logger.logOperationFailedAsync(label);

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
