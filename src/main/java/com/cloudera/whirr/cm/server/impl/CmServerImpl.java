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
package com.cloudera.whirr.cm.server.impl;

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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.cxf.jaxrs.client.ServerWebApplicationException;
import org.apache.cxf.jaxrs.ext.multipart.InputStreamDataSource;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import com.cloudera.api.ApiRootResource;
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
import com.cloudera.api.model.ApiRoleNameList;
import com.cloudera.api.model.ApiRoleState;
import com.cloudera.api.model.ApiService;
import com.cloudera.api.model.ApiServiceConfig;
import com.cloudera.api.model.ApiServiceList;
import com.cloudera.api.model.ApiServiceState;
import com.cloudera.api.v3.ParcelResource;
import com.cloudera.api.v3.RootResourceV3;
import com.cloudera.api.v4.RootResourceV4;
import com.cloudera.api.v5.RootResourceV5;
import com.cloudera.whirr.cm.server.CmServer;
import com.cloudera.whirr.cm.server.CmServerBuilder.CmServerCommandMethod;
import com.cloudera.whirr.cm.server.CmServerCluster;
import com.cloudera.whirr.cm.server.CmServerException;
import com.cloudera.whirr.cm.server.CmServerService;
import com.cloudera.whirr.cm.server.CmServerService.CmServerServiceStatus;
import com.cloudera.whirr.cm.server.CmServerServiceBuilder;
import com.cloudera.whirr.cm.server.CmServerServiceType;
import com.cloudera.whirr.cm.server.CmServerServiceTypeCms;
import com.cloudera.whirr.cm.server.CmServerServiceTypeRepo;
import com.cloudera.whirr.cm.server.impl.CmServerLog.CmServerLogSyncCommand;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class CmServerImpl implements CmServer {

  // CM Version Matrix, of form {CM_VERSION=CM_API_VERSION}
  // Entry for the latest CM minor version for each API upgrade, from baseline 4.5.0
  public static Map<String, Integer> CM_VERSION_API_MATRIX = ImmutableMap.of("4.5.0", 3, "4.5.3", 3, "4.6.3", 4,
      "4.7.0", 5);
  public static String CM_VERSION_EARLIEST = CM_VERSION_API_MATRIX.keySet().toArray(
      new String[CM_VERSION_API_MATRIX.size()])[0];
  public static String CM_VERSION_LATEST = CM_VERSION_API_MATRIX.keySet().toArray(
      new String[CM_VERSION_API_MATRIX.size()])[CM_VERSION_API_MATRIX.size() - 1];
  public static int CM_VERSION_API_EARLIEST = CM_VERSION_API_MATRIX.values().toArray(
      new Integer[CM_VERSION_API_MATRIX.size()])[0];
  public static int CM_VERSION_API_LATEST = CM_VERSION_API_MATRIX.values().toArray(
      new Integer[CM_VERSION_API_MATRIX.size()])[CM_VERSION_API_MATRIX.size() - 1];

  private static final String CM_PARCEL_STAGE_DOWNLOADED = "DOWNLOADED";
  private static final String CM_PARCEL_STAGE_DISTRIBUTED = "DISTRIBUTED";
  private static final String CM_PARCEL_STAGE_ACTIVATED = "ACTIVATED";

  private static final String CM_CONFIG_UPDATE_MESSAGE = "Update base config group with defaults";

  private static int API_POLL_PERIOD_MS = 500;
  private static int API_POLL_PERIOD_BACKOFF_NUMBER = 3;
  private static int API_POLL_PERIOD_BACKOFF_INCRAMENT = 2;

  private CmServerLog logger;

  private String version;
  private int versionApi;
  private ApiClusterVersion versionCdh;
  private CmServerService host;

  final private RootResourceV3 apiResourceRootV3;
  final private RootResourceV4 apiResourceRootV4;
  @SuppressWarnings("unused")
  final private RootResourceV5 apiResourceRootV5;

  private boolean isFirstStartRequired = true;

  protected CmServerImpl(String version, String vesionApi, String versionCdh, String ip, String ipInternal, int port,
      String user, String password, CmServerLog logger) throws CmServerException {
    this.version = getVersion(version);
    this.versionApi = getVersionApi(this.version, vesionApi);
    this.versionCdh = getVersionCdh(versionCdh);
    this.host = new CmServerServiceBuilder().ip(ip).ipInternal(ipInternal).build();
    this.logger = logger;
    ApiRootResource apiResource = new ClouderaManagerClientBuilder().withHost(ip).withPort(port)
        .withUsernamePassword(user, password).build();
    this.apiResourceRootV3 = apiResource.getRootV3();
    this.apiResourceRootV4 = this.versionApi >= 4 ? apiResource.getRootV4() : null;
    this.apiResourceRootV5 = this.versionApi >= 5 ? apiResource.getRootV5() : null;
  }

  protected String getVersion(String version) throws CmServerException {
    String versionValidated = null;
    if (version != null && !version.equals("")) {
      String versionFullyQualified = version.contains(".") ? version : version + "." + Integer.MAX_VALUE + "."
          + Integer.MAX_VALUE;
      if (new DefaultArtifactVersion(versionFullyQualified).compareTo(new DefaultArtifactVersion(CM_VERSION_EARLIEST)) < 0
          || new DefaultArtifactVersion(versionFullyQualified).compareTo(new DefaultArtifactVersion(
              new DefaultArtifactVersion(CM_VERSION_LATEST).getMajorVersion() + "." + Integer.MAX_VALUE + "."
                  + Integer.MAX_VALUE)) > 0) {
        throw new CmServerException("Requested CM version [" + version
            + "] is invalid and cannot be reconciled with CM versions " + CM_VERSION_API_MATRIX.keySet());
      } else {
        versionValidated = version;
      }
    }
    return versionValidated;
  }

  protected int getVersionApi(String version, String versionApi) throws CmServerException {
    Integer versionApiValidated = null;
    if (version == null || version.equals("")) {
      version = CM_VERSION_LATEST;
    }
    if (!version.contains(".")) {
      version = version + "." + Integer.MAX_VALUE + "." + Integer.MAX_VALUE;
    }
    ArtifactVersion versionArtifact = new DefaultArtifactVersion(version);
    for (String versionUpperBound : CM_VERSION_API_MATRIX.keySet()) {
      if (versionArtifact.compareTo(new DefaultArtifactVersion(versionUpperBound)) <= 0) {
        versionApiValidated = CM_VERSION_API_MATRIX.get(versionUpperBound);
        break;
      }
      if (versionApiValidated == null) {
        versionApiValidated = CM_VERSION_API_LATEST;
      }
    }
    if (versionApi != null
        && !versionApi.equals("")
        && (new DefaultArtifactVersion(versionApi)
            .compareTo(new DefaultArtifactVersion(versionApiValidated.toString())) > 0 || new DefaultArtifactVersion(
            versionApi).compareTo(new DefaultArtifactVersion("" + CM_VERSION_API_EARLIEST)) < 0)) {
      throw new CmServerException("Requested CM API version [" + versionApi + "] of CM version [" + version
          + "] could not be reconciled with CM API version matrix " + CM_VERSION_API_MATRIX);
    }
    if (versionApi != null && !StringUtils.isNumeric(versionApi)) {
      throw new CmServerException("CM API version requested is non-numeric [" + versionApi + "]");
    }
    return versionApi == null ? versionApiValidated : Integer.parseInt(versionApi);
  }

  protected ApiClusterVersion getVersionCdh(String versionCdh) throws CmServerException {
    ApiClusterVersion versionCdhValidated = null;
    if (versionCdh == null || versionCdh.equals("")) {
      versionCdhValidated = ApiClusterVersion.CDH4;
    } else {
      try {
        versionCdhValidated = ApiClusterVersion.valueOf("CDH" + versionCdh);
        if (new DefaultArtifactVersion(versionCdh).getMajorVersion() < 4) {
          throw new CmServerException("CDH version requested [" + versionApi + "] is below mininum supported [4]");
        }
      } catch (IllegalArgumentException e) {
        throw new CmServerException("CDH version requested [" + versionApi + "] cannot be corelated with CDH versions "
            + Arrays.asList(ApiClusterVersion.values()));
      }
    }
    return versionCdhValidated;
  }

  @Override
  public String getVersion() {
    return version;
  }

  @Override
  public int getVersionApi() {
    return versionApi;
  }

  @Override
  public String getVersionCdh() {
    return versionCdh.toString();
  }

  @Override
  @CmServerCommandMethod(name = "client")
  public boolean getServiceConfigs(final CmServerCluster cluster, final File directory) throws CmServerException {

    final AtomicBoolean executed = new AtomicBoolean(false);
    try {

      if (isProvisioned(cluster)) {
        logger.logOperation("GetConfig", new CmServerLogSyncCommand() {
          @Override
          public void execute() throws IOException {
            for (ApiService apiService : apiResourceRootV3.getClustersResource().getServicesResource(getName(cluster))
                .readServices(DataView.SUMMARY)) {
              CmServerServiceType type = CmServerServiceType.valueOfId(apiService.getType());
              if (type.equals(CmServerServiceType.HDFS) || type.equals(CmServerServiceType.MAPREDUCE)
                  || type.equals(CmServerServiceType.HBASE) || versionApi >= 4 && type.equals(CmServerServiceType.HIVE)
                  || versionApi >= 5 && type.equals(CmServerServiceType.SOLR)) {
                ZipInputStream configInputZip = null;
                try {
                  InputStreamDataSource configInput = apiResourceRootV3.getClustersResource()
                      .getServicesResource(getName(cluster)).getClientConfig(apiService.getName());
                  if (configInput != null) {
                    configInputZip = new ZipInputStream(configInput.getInputStream());
                    ZipEntry configInputZipEntry = null;
                    while ((configInputZipEntry = configInputZip.getNextEntry()) != null) {
                      String configFile = configInputZipEntry.getName();
                      if (configFile.contains(File.separator)) {
                        configFile = configFile.substring(configFile.lastIndexOf(File.separator), configFile.length());
                      }
                      directory.mkdirs();
                      BufferedWriter configOutput = null;
                      try {
                        int read;
                        configOutput = new BufferedWriter(new FileWriter(new File(directory, configFile)));
                        while (configInputZip.available() > 0) {
                          if ((read = configInputZip.read()) != -1) {
                            configOutput.write(read);
                          }
                        }
                      } finally {
                        configOutput.close();
                      }
                    }
                  }
                } finally {
                  if (configInputZip != null) {
                    configInputZip.close();
                  }
                }
                executed.set(true);
              }
            }
          }
        });
      }

    } catch (Exception e) {
      throw new CmServerException("Failed to get cluster config", e);
    }

    return executed.get();

  }

  @Override
  public List<CmServerService> getServiceHosts() throws CmServerException {

    final List<CmServerService> services = new ArrayList<CmServerService>();
    try {

      logger.logOperation("GetHosts", new CmServerLogSyncCommand() {
        @Override
        public void execute() {
          for (ApiHost host : apiResourceRootV3.getHostsResource().readHosts(DataView.SUMMARY).getHosts()) {
            services.add(new CmServerServiceBuilder().host(host.getHostId()).ip(host.getIpAddress())
                .ipInternal(host.getIpAddress()).status(CmServerServiceStatus.STARTED).build());
          }
        }
      });

    } catch (Exception e) {
      throw new CmServerException("Failed to list cluster hosts", e);
    }

    return services;
  }

  @Override
  public CmServerService getServiceHost(CmServerService service) throws CmServerException {

    return getServiceHost(service, getServiceHosts());

  }

  @Override
  public CmServerService getServiceHost(CmServerService service, List<CmServerService> services)
      throws CmServerException {

    CmServerService serviceFound = null;
    try {
      for (CmServerService serviceTmp : services) {
        if (service.getHost() != null && service.getHost().equals(serviceTmp.getHost()) || service.getHost() != null
            && service.getHost().equals(serviceTmp.getIp()) || service.getHost() != null
            && service.getHost().equals(serviceTmp.getIpInternal()) || service.getIp() != null
            && service.getIp().equals(serviceTmp.getIp()) || service.getIp() != null
            && service.getIp().equals(serviceTmp.getIpInternal()) || service.getIpInternal() != null
            && service.getIpInternal().equals(serviceTmp.getIp()) || service.getIpInternal() != null
            && service.getIpInternal().equals(serviceTmp.getIpInternal())) {
          serviceFound = serviceTmp;
          break;
        }
      }

    } catch (Exception e) {
      throw new CmServerException("Failed to find service", e);
    }

    return serviceFound;
  }

  @Override
  @CmServerCommandMethod(name = "services")
  public CmServerCluster getServices(final CmServerCluster cluster) throws CmServerException {

    final CmServerCluster clusterView = new CmServerCluster();
    try {
      clusterView.setServer(cluster.getServer());
      List<CmServerService> services = getServiceHosts();
      for (CmServerService server : cluster.getAgents()) {
        if (getServiceHost(server, services) != null) {
          clusterView.addAgent(getServiceHost(server, services));
        }
      }
      if (!cluster.isEmpty() && isProvisioned(cluster)) {
        logger.logOperation("GetServices", new CmServerLogSyncCommand() {
          @Override
          public void execute() throws IOException, CmServerException {
            Map<String, String> ips = new HashMap<String, String>();
            for (ApiService apiService : apiResourceRootV3.getClustersResource().getServicesResource(getName(cluster))
                .readServices(DataView.SUMMARY)) {
              for (ApiRole apiRole : apiResourceRootV3.getClustersResource().getServicesResource(getName(cluster))
                  .getRolesResource(apiService.getName()).readRoles()) {
                if (!ips.containsKey(apiRole.getHostRef().getHostId())) {
                  ips.put(apiRole.getHostRef().getHostId(),
                      apiResourceRootV3.getHostsResource().readHost(apiRole.getHostRef().getHostId()).getIpAddress());
                }
                CmServerServiceStatus status = null;
                try {
                  status = CmServerServiceStatus.valueOf(apiRole.getRoleState().toString());
                } catch (IllegalArgumentException exception) {
                  status = CmServerServiceStatus.UNKNOWN;
                }
                try {
                  CmServerService service = new CmServerServiceBuilder().name(apiRole.getName())
                      .host(apiRole.getHostRef().getHostId()).ip(ips.get(apiRole.getHostRef().getHostId()))
                      .ipInternal(ips.get(apiRole.getHostRef().getHostId())).status(status).build();
                  if (service.getType().isConcrete()) {
                    clusterView.addService(service);
                  }
                } catch (IllegalArgumentException exception) {
                  // ignore
                }
              }
            }
          }
        });

      }

    } catch (Exception e) {
      throw new CmServerException("Failed to find services", e);
    }

    return clusterView;

  }

  @Override
  public CmServerService getService(final CmServerCluster cluster, final CmServerServiceType type)
      throws CmServerException {

    return getServices(cluster, type).getService(type, versionApi);

  }

  @Override
  public CmServerCluster getServices(final CmServerCluster cluster, final CmServerServiceType type)
      throws CmServerException {

    final CmServerCluster clusterView = new CmServerCluster();
    try {

      for (CmServerService service : getServices(cluster).getServices(CmServerServiceType.CLUSTER, versionApi)) {
        if (type.equals(CmServerServiceType.CLUSTER) || type.equals(service.getType().getParent())
            || type.equals(service.getType())) {
          clusterView.addService(service);
        }
      }

    } catch (Exception e) {
      throw new CmServerException("Failed to find services", e);
    }

    return clusterView;

  }

  @Override
  public boolean isProvisioned(final CmServerCluster cluster) throws CmServerException {

    boolean executed = false;
    try {

      for (ApiCluster apiCluster : apiResourceRootV3.getClustersResource().readClusters(DataView.SUMMARY)) {
        if (apiCluster.getName().equals(getName(cluster))) {
          executed = true;
          break;
        }
      }

    } catch (Exception e) {
      throw new CmServerException("Failed to detrermine if cluster is provisioned", e);
    }

    return executed;

  }

  @Override
  public boolean isConfigured(final CmServerCluster cluster) throws CmServerException {

    boolean executed = false;
    final Set<String> servicesNotConfigured = new HashSet<String>();
    try {

      if (isProvisioned(cluster)) {
        for (CmServerService service : cluster.getServices(CmServerServiceType.CLUSTER, versionApi)) {
          servicesNotConfigured.add(service.getName());
        }
        for (ApiService apiService : apiResourceRootV3.getClustersResource().getServicesResource(getName(cluster))
            .readServices(DataView.SUMMARY)) {
          for (ApiRole apiRole : apiResourceRootV3.getClustersResource().getServicesResource(getName(cluster))
              .getRolesResource(apiService.getName()).readRoles()) {
            servicesNotConfigured.remove(apiRole.getName());
          }
        }
        executed = true;
      }

    } catch (Exception e) {
      throw new CmServerException("Failed to detrermine if cluster is configured", e);
    }

    return executed && servicesNotConfigured.size() == 0;

  }

  @Override
  public boolean isStarted(final CmServerCluster cluster) throws CmServerException {

    boolean executed = true;
    final Set<String> servicesNotStarted = new HashSet<String>();
    try {

      if (isConfigured(cluster)) {
        for (CmServerService service : cluster.getServices(CmServerServiceType.CLUSTER, versionApi)) {
          servicesNotStarted.add(service.getName());
        }
        for (ApiService apiService : apiResourceRootV3.getClustersResource().getServicesResource(getName(cluster))
            .readServices(DataView.SUMMARY)) {
          for (ApiRole apiRole : apiResourceRootV3.getClustersResource().getServicesResource(getName(cluster))
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
      throw new CmServerException("Failed to detrermine if cluster is started", e);
    }

    return executed ? servicesNotStarted.size() == 0 : false;

  }

  @Override
  public boolean isStopped(final CmServerCluster cluster) throws CmServerException {

    final Set<String> servicesNotStopped = new HashSet<String>();
    try {

      if (isConfigured(cluster)) {
        for (CmServerService service : cluster.getServices(CmServerServiceType.CLUSTER, versionApi)) {
          servicesNotStopped.add(service.getName());
        }
        for (ApiService apiService : apiResourceRootV3.getClustersResource().getServicesResource(getName(cluster))
            .readServices(DataView.SUMMARY)) {
          for (ApiRole apiRole : apiResourceRootV3.getClustersResource().getServicesResource(getName(cluster))
              .getRolesResource(apiService.getName()).readRoles()) {
            if (apiRole.getRoleState().equals(ApiRoleState.STOPPED)) {
              servicesNotStopped.remove(apiRole.getName());
            }
          }
        }
      }

    } catch (Exception e) {
      throw new CmServerException("Failed to detrermine if cluster is stopped", e);
    }

    return servicesNotStopped.size() == 0;

  }

  @Override
  @CmServerCommandMethod(name = "initialise")
  public boolean initialise(final CmServerCluster cluster) throws CmServerException {

    boolean executed = false;
    try {

      logger.logOperationStartedSync("ClusterInitialise");

      Map<String, String> configuration = cluster.getServiceConfiguration(versionApi).get(
          CmServerServiceTypeCms.CM.getId());
      configuration.remove("cm_database_name");
      configuration.remove("cm_database_type");
      executed = CmServerServiceTypeCms.CM.getId() != null
          && provisionCmSettings(configuration).size() >= configuration.size();

      logger.logOperationFinishedSync("ClusterInitialise");

    } catch (Exception e) {
      logger.logOperationFailedSync("ClusterInitialise");
      throw new CmServerException("Failed to initialise cluster", e);
    }

    return executed;
  }

  @Override
  public boolean provision(CmServerCluster cluster) throws CmServerException {

    boolean executed = false;
    try {

      logger.logOperationStartedSync("ClusterProvision");

      provisionManagement(cluster);
      if (!cluster.isEmpty() && !isProvisioned(cluster)) {
        provsionCluster(cluster);
        if (cluster.getIsParcel()) {
          provisionParcels(cluster);
        }
        executed = true;
      }

      logger.logOperationFinishedSync("ClusterProvision");

    } catch (Exception e) {
      logger.logOperationFailedSync("ClusterProvision");
      throw new CmServerException("Failed to provision cluster", e);
    }

    return executed;
  }

  @Override
  @CmServerCommandMethod(name = "configure")
  public boolean configure(CmServerCluster cluster) throws CmServerException {

    boolean executed = false;
    try {

      logger.logOperationStartedSync("ClusterConfigure");

      if (!cluster.isEmpty()) {
        if (!isProvisioned(cluster)) {
          provision(cluster);
        }
        if (!isConfigured(cluster)) {
          configureServices(cluster);
          isFirstStartRequired = true;
          executed = true;
        }
      }

      logger.logOperationFinishedSync("ClusterConfigure");

    } catch (Exception e) {
      logger.logOperationFailedSync("ClusterConfigure");
      throw new CmServerException("Failed to configure cluster", e);
    }

    return executed;
  }

  @Override
  public boolean start(final CmServerCluster cluster) throws CmServerException {

    boolean executed = true;
    try {

      logger.logOperationStartedSync("ClusterStart");

      if (!cluster.isEmpty()) {
        if (!isConfigured(cluster)) {
          configure(cluster);
        }
        if (!isStarted(cluster)) {
          for (CmServerServiceType type : cluster.getServiceTypes(versionApi)) {
            if (isFirstStartRequired) {
              for (CmServerService service : cluster.getServices(type, versionApi)) {
                initPreStartServices(cluster, service);
              }
            }
            startService(cluster, type);
            if (isFirstStartRequired) {
              for (CmServerService service : cluster.getServices(type, versionApi)) {
                initPostStartServices(cluster, service);
              }
            }
          }
          isFirstStartRequired = false;
        } else {
          executed = false;
        }

        // push into provision phase once OPSAPS-13194/OPSAPS-12870 is addressed
        startManagement(cluster);

      }

      logger.logOperationFinishedSync("ClusterStart");

    } catch (Exception e) {
      logger.logOperationFailedSync("ClusterStart");
      throw new CmServerException("Failed to start cluster", e);
    }

    return executed;
  }

  @Override
  public boolean stop(final CmServerCluster cluster) throws CmServerException {

    boolean executed = true;
    try {

      logger.logOperationStartedSync("ClusterStop");

      if (!cluster.isEmpty()) {
        if (isConfigured(cluster) && !isStopped(cluster)) {
          final Set<CmServerServiceType> types = new TreeSet<CmServerServiceType>(Collections.reverseOrder());
          types.addAll(cluster.getServiceTypes(versionApi));
          for (CmServerServiceType type : types) {
            stopService(cluster, type);
          }
        } else {
          executed = false;
        }
      }

      logger.logOperationFinishedSync("ClusterStop");

    } catch (Exception e) {
      logger.logOperationFailedSync("ClusterStop");
      throw new CmServerException("Failed to stop cluster", e);
    }

    return executed;
  }

  @Override
  @CmServerCommandMethod(name = "unconfigure")
  public boolean unconfigure(final CmServerCluster cluster) throws CmServerException {

    boolean executed = false;
    try {

      logger.logOperationStartedSync("ClusterUnConfigure");

      if (!cluster.isEmpty()) {
        if (isConfigured(cluster)) {
          if (!isStopped(cluster)) {
            stop(cluster);
          }
          unconfigureServices(cluster);
          executed = true;
        }
      }

      logger.logOperationFinishedSync("ClusterUnConfigure");

    } catch (Exception e) {
      logger.logOperationFailedSync("ClusterUnConfigure");
      throw new CmServerException("Failed to unconfigure cluster", e);
    }

    return executed;
  }

  @Override
  @CmServerCommandMethod(name = "unprovision")
  public boolean unprovision(final CmServerCluster cluster) throws CmServerException {

    boolean executed = false;
    try {

      logger.logOperationStartedSync("ClusterUnProvision");

      if (!cluster.isEmpty()) {
        if (isProvisioned(cluster)) {
          logger.logOperation("UnProvisionCluster", new CmServerLogSyncCommand() {
            @Override
            public void execute() throws IOException {
              apiResourceRootV3.getClustersResource().deleteCluster(getName(cluster));
            }
          });
          executed = true;
        }
      }

      logger.logOperationFinishedSync("ClusterUnProvision");

    } catch (Exception e) {
      logger.logOperationFailedSync("ClusterUnProvision");
      throw new CmServerException("Failed to unprovision cluster", e);
    }

    return executed;

  }

  private String getName(CmServerCluster cluster) {
    try {
      return cluster.getServiceName(CmServerServiceType.CLUSTER);
    } catch (IOException e) {
      throw new RuntimeException("Could not resolve cluster name", e);
    }
  }

  private Map<String, String> provisionCmSettings(Map<String, String> config) throws InterruptedException {

    Map<String, String> configPostUpdate = new HashMap<String, String>();
    ApiConfigList apiConfigList = new ApiConfigList();
    if (config != null && !config.isEmpty()) {
      for (String key : config.keySet()) {
        apiConfigList.add(new ApiConfig(key, config.get(key)));
      }
      apiResourceRootV3.getClouderaManagerResource().updateConfig(apiConfigList);
    }
    apiConfigList = apiResourceRootV3.getClouderaManagerResource().getConfig(DataView.SUMMARY);
    for (ApiConfig apiConfig : apiConfigList) {
      configPostUpdate.put(apiConfig.getName(), apiConfig.getValue());
    }

    return configPostUpdate;

  }

  private void provisionManagement(final CmServerCluster cluster) throws CmServerException, InterruptedException {

    boolean cmsProvisionRequired = false;
    try {
      try {
        cmsProvisionRequired = apiResourceRootV3.getClouderaManagerResource().getMgmtServiceResource()
            .readService(DataView.SUMMARY) == null;
      } catch (ServerWebApplicationException exception) {
        cmsProvisionRequired = true;
      }
    } catch (ServerWebApplicationException exception) {
      // ignore
    }

    if (cmsProvisionRequired) {

      final ApiHostRef cmServerHostRefApi = new ApiHostRef(getServiceHost(host).getHost());

      boolean licenseDeployed = false;
      try {
        licenseDeployed = apiResourceRootV3.getClouderaManagerResource().readLicense() != null;
      } catch (Exception e) {
        // ignore
      }
      final boolean enterpriseDeployed = licenseDeployed;

      if (versionApi >= 4 || licenseDeployed) {
        logger.logOperation("CreateManagementServices", new CmServerLogSyncCommand() {
          @Override
          public void execute() throws IOException, CmServerException, InterruptedException {
            ApiService cmsServiceApi = new ApiService();
            List<ApiRole> cmsRoleApis = new ArrayList<ApiRole>();
            cmsServiceApi.setName(CmServerServiceTypeCms.MANAGEMENT.getName());
            cmsServiceApi.setType(CmServerServiceTypeCms.MANAGEMENT.getId());
            for (CmServerServiceTypeCms type : CmServerServiceTypeCms.values()) {
              if (type.getParent() != null && (!type.getEnterprise() || enterpriseDeployed)) {
                ApiRole cmsRoleApi = new ApiRole();
                cmsRoleApi.setName(type.getName());
                cmsRoleApi.setType(type.getId());
                cmsRoleApi.setHostRef(cmServerHostRefApi);
                cmsRoleApis.add(cmsRoleApi);
              }
            }
            cmsServiceApi.setRoles(cmsRoleApis);

            apiResourceRootV3.getClouderaManagerResource().getMgmtServiceResource().setupCMS(cmsServiceApi);

            for (ApiRoleConfigGroup cmsRoleConfigGroupApi : apiResourceRootV3.getClouderaManagerResource()
                .getMgmtServiceResource().getRoleConfigGroupsResource().readRoleConfigGroups()) {
              try {

                CmServerServiceTypeCms type = CmServerServiceTypeCms.valueOf(cmsRoleConfigGroupApi.getRoleType());
                if (!type.getEnterprise() || enterpriseDeployed) {
                  ApiRoleConfigGroup cmsRoleConfigGroupApiNew = new ApiRoleConfigGroup();
                  ApiServiceConfig cmsServiceConfigApi = new ApiServiceConfig();
                  if (cluster.getServiceConfiguration(versionApi).get(type.getId()) != null) {
                    for (String setting : cluster.getServiceConfiguration(versionApi).get(type.getId()).keySet()) {
                      cmsServiceConfigApi.add(new ApiConfig(setting, cluster.getServiceConfiguration(versionApi)
                          .get(type.getId()).get(setting)));
                    }
                  }
                  cmsRoleConfigGroupApiNew.setConfig(cmsServiceConfigApi);

                  apiResourceRootV3
                      .getClouderaManagerResource()
                      .getMgmtServiceResource()
                      .getRoleConfigGroupsResource()
                      .updateRoleConfigGroup(cmsRoleConfigGroupApi.getName(), cmsRoleConfigGroupApiNew,
                          CM_CONFIG_UPDATE_MESSAGE);

                }
              } catch (IllegalArgumentException e) {
                // ignore
              }
            }
          }
        });
      }
    }

  }

  private void provsionCluster(final CmServerCluster cluster) throws IOException, InterruptedException,
      CmServerException {

    execute(apiResourceRootV3.getClouderaManagerResource().inspectHostsCommand());

    final ApiClusterList clusterList = new ApiClusterList();
    ApiCluster apiCluster = new ApiCluster();
    apiCluster.setName(getName(cluster));
    apiCluster.setVersion(versionCdh);
    clusterList.add(apiCluster);

    logger.logOperation("CreateCluster", new CmServerLogSyncCommand() {
      @Override
      public void execute() throws IOException {
        apiResourceRootV3.getClustersResource().createClusters(clusterList);
      }
    });

    List<ApiHostRef> apiHostRefs = Lists.newArrayList();
    for (CmServerService service : getServiceHosts()) {
      apiHostRefs.add(new ApiHostRef(service.getHost()));
    }
    apiResourceRootV3.getClustersResource().addHosts(getName(cluster), new ApiHostRefList(apiHostRefs));

  }

  private void provisionParcels(final CmServerCluster cluster) throws InterruptedException, IOException {

    apiResourceRootV3.getClouderaManagerResource().updateConfig(
        new ApiConfigList(Arrays.asList(new ApiConfig[] { new ApiConfig("PARCEL_UPDATE_FREQ", "1") })));
    final Set<CmServerServiceTypeRepo> repositoriesRequired = new HashSet<CmServerServiceTypeRepo>();
    for (CmServerServiceType type : cluster.getServiceTypes(versionApi)) {
      repositoriesRequired.add(type.getRepository());
    }
    execute("WaitForParcelsAvailability", new Callback() {
      @Override
      public boolean poll() {
        Set<CmServerServiceTypeRepo> repositoriesNotLoaded = new HashSet<CmServerServiceTypeRepo>(repositoriesRequired);
        for (ApiParcel parcel : apiResourceRootV3.getClustersResource().getParcelsResource(getName(cluster))
            .readParcels(DataView.FULL).getParcels()) {
          try {
            repositoriesNotLoaded.remove(CmServerServiceTypeRepo.valueOf(parcel.getProduct()));
          } catch (IllegalArgumentException e) {
            // ignore
          }
        }
        return repositoriesNotLoaded.isEmpty();
      }
    });
    apiResourceRootV3.getClouderaManagerResource().updateConfig(
        new ApiConfigList(Arrays.asList(new ApiConfig[] { new ApiConfig("PARCEL_UPDATE_FREQ", "60") })));

    for (CmServerServiceTypeRepo repository : repositoriesRequired) {
      DefaultArtifactVersion parcelVersion = null;
      for (ApiParcel apiParcel : apiResourceRootV3.getClustersResource().getParcelsResource(getName(cluster))
          .readParcels(DataView.FULL).getParcels()) {
        if (apiParcel.getProduct().equals(repository.toString())
            && (parcelVersion == null || parcelVersion.compareTo(new DefaultArtifactVersion(apiParcel.getVersion())) < 0)) {
          parcelVersion = new DefaultArtifactVersion(apiParcel.getVersion());
        }
      }
      final ParcelResource apiParcelResource = apiResourceRootV3.getClustersResource()
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
      CmServerException {

    final List<CmServerService> services = getServiceHosts();

    logger.logOperation("CreateClusterServices", new CmServerLogSyncCommand() {
      @Override
      public void execute() throws IOException, InterruptedException, CmServerException {

        ApiServiceList serviceList = new ApiServiceList();
        for (CmServerServiceType type : cluster.getServiceTypes(versionApi)) {

          ApiService apiService = new ApiService();
          List<ApiRole> apiRoles = new ArrayList<ApiRole>();
          apiService.setType(type.getId());
          apiService.setName(cluster.getServiceName(type));

          ApiServiceConfig apiServiceConfig = new ApiServiceConfig();
          if (cluster.getServiceConfiguration(versionApi).get(type.getId()) != null) {
            for (String setting : cluster.getServiceConfiguration(versionApi).get(type.getId()).keySet()) {
              apiServiceConfig.add(new ApiConfig(setting, cluster.getServiceConfiguration(versionApi).get(type.getId())
                  .get(setting)));
            }
          }
          switch (type) {
          case MAPREDUCE:
            apiServiceConfig.add(new ApiConfig("hdfs_service", cluster.getServiceName(CmServerServiceType.HDFS)));
            break;
          case HBASE:
            apiServiceConfig.add(new ApiConfig("hdfs_service", cluster.getServiceName(CmServerServiceType.HDFS)));
            apiServiceConfig.add(new ApiConfig("zookeeper_service", cluster
                .getServiceName(CmServerServiceType.ZOOKEEPER)));
            break;
          case SOLR:
            apiServiceConfig.add(new ApiConfig("hdfs_service", cluster.getServiceName(CmServerServiceType.HDFS)));
            apiServiceConfig.add(new ApiConfig("zookeeper_service", cluster
                .getServiceName(CmServerServiceType.ZOOKEEPER)));
            break;
          case SOLR_INDEXER:
            apiServiceConfig.add(new ApiConfig("hbase_service", cluster.getServiceName(CmServerServiceType.HBASE)));
            apiServiceConfig.add(new ApiConfig("solr_service", cluster.getServiceName(CmServerServiceType.HBASE)));
            break;
          case HUE:
            apiServiceConfig.add(new ApiConfig("hue_webhdfs", cluster.getServiceName(CmServerServiceType.HDFS_NAMENODE)));
            apiServiceConfig.add(new ApiConfig("hive_service", cluster.getServiceName(CmServerServiceType.HIVE)));
            apiServiceConfig.add(new ApiConfig("oozie_service", cluster.getServiceName(CmServerServiceType.OOZIE)));
            break;
          case SQOOP:
            apiServiceConfig.add(new ApiConfig("mapreduce_yarn_service", cluster
                .getServiceName(CmServerServiceType.MAPREDUCE)));
            break;
          case OOZIE:
            apiServiceConfig.add(new ApiConfig("mapreduce_yarn_service", cluster
                .getServiceName(CmServerServiceType.MAPREDUCE)));
            break;
          case HIVE:
            apiServiceConfig.add(new ApiConfig("mapreduce_yarn_service", cluster
                .getServiceName(CmServerServiceType.MAPREDUCE)));
            if (versionApi >= 4) {
              apiServiceConfig.add(new ApiConfig("zookeeper_service", cluster
                  .getServiceName(CmServerServiceType.ZOOKEEPER)));
            }
            break;
          case IMPALA:
            apiServiceConfig.add(new ApiConfig("hdfs_service", cluster.getServiceName(CmServerServiceType.HDFS)));
            apiServiceConfig.add(new ApiConfig("hbase_service", cluster.getServiceName(CmServerServiceType.HBASE)));
            apiServiceConfig.add(new ApiConfig("hive_service", cluster.getServiceName(CmServerServiceType.HIVE)));
            break;
          case FLUME:
            apiServiceConfig.add(new ApiConfig("hdfs_service", cluster.getServiceName(CmServerServiceType.HDFS)));
            apiServiceConfig.add(new ApiConfig("hbase_service", cluster.getServiceName(CmServerServiceType.HBASE)));
          default:
            break;
          }
          apiService.setConfig(apiServiceConfig);

          for (CmServerService subService : cluster.getServices(type, versionApi)) {
            if (versionApi >= subService.getType().getVersion()) {
              CmServerService subServiceHost = getServiceHost(subService, services);
              if (subServiceHost == null || subServiceHost.getHost() == null) {
                throw new CmServerException("Could not find CM agent host to match [" + subService + "]");
              }
              ApiRole apiRole = new ApiRole();
              apiRole.setName(subService.getName());
              apiRole.setType(subService.getType().getId());
              apiRole.setHostRef(new ApiHostRef(subServiceHost.getHost()));
              apiRoles.add(apiRole);
            }
          }

          apiService.setRoles(apiRoles);
          serviceList.add(apiService);

        }

        apiResourceRootV3.getClustersResource().getServicesResource(getName(cluster)).createServices(serviceList);

        for (CmServerServiceType type : cluster.getServiceTypes(versionApi)) {
          for (ApiRoleConfigGroup roleConfigGroup : apiResourceRootV3.getClustersResource()
              .getServicesResource(getName(cluster)).getRoleConfigGroupsResource(cluster.getServiceName(type))
              .readRoleConfigGroups()) {

            ApiConfigList apiConfigList = new ApiConfigList();
            CmServerServiceType roleConfigGroupType = null;
            try {
              roleConfigGroupType = CmServerServiceType.valueOfId(roleConfigGroup.getRoleType());
            } catch (IllegalArgumentException e) {
              // ignore
            }
            if (roleConfigGroupType != null && roleConfigGroupType.equals(CmServerServiceType.GATEWAY)) {
              try {
                roleConfigGroupType = CmServerServiceType.valueOfId(new CmServerServiceBuilder()
                    .name(roleConfigGroup.getServiceRef().getServiceName()).build().getType()
                    + "_" + roleConfigGroup.getRoleType());
              } catch (IllegalArgumentException e) {
                // ignore
              }
            }
            if (roleConfigGroupType != null) {
              Map<String, String> config = cluster.getServiceConfiguration(versionApi).get(roleConfigGroupType.getId());
              if (config != null) {
                for (String setting : config.keySet()) {
                  apiConfigList.add(new ApiConfig(setting, config.get(setting)));
                }
              }
              ApiRoleConfigGroup apiRoleConfigGroup = new ApiRoleConfigGroup();
              apiRoleConfigGroup.setConfig(apiConfigList);
              apiResourceRootV3.getClustersResource().getServicesResource(getName(cluster))
                  .getRoleConfigGroupsResource(cluster.getServiceName(type))
                  .updateRoleConfigGroup(roleConfigGroup.getName(), apiRoleConfigGroup, CM_CONFIG_UPDATE_MESSAGE);
            }

          }
        }
      }
    });

    // Necessary, since createServices a habit of kicking off async commands (eg ZkAutoInit )
    for (CmServerServiceType type : cluster.getServiceTypes(versionApi)) {
      for (ApiCommand command : apiResourceRootV3.getClustersResource().getServicesResource(getName(cluster))
          .listActiveCommands(cluster.getServiceName(type), DataView.SUMMARY)) {
        CmServerImpl.this.execute(command, false);
      }
    }

  }

  private void unconfigureServices(final CmServerCluster cluster) throws IOException, InterruptedException {

    final Set<CmServerServiceType> types = new TreeSet<CmServerServiceType>(Collections.reverseOrder());
    types.addAll(cluster.getServiceTypes(versionApi));
    logger.logOperation("DestroyClusterServices", new CmServerLogSyncCommand() {
      @Override
      public void execute() throws IOException {
        for (final CmServerServiceType type : types) {
          apiResourceRootV3.getClustersResource().getServicesResource(getName(cluster))
              .deleteService(cluster.getServiceName(type));
        }
      }
    });

  }

  private void initPreStartServices(final CmServerCluster cluster, CmServerService service) throws IOException,
      InterruptedException {

    switch (service.getType().getParent()) {
    case HIVE:
      execute(apiResourceRootV3.getClustersResource().getServicesResource(getName(cluster))
          .createHiveWarehouseCommand(cluster.getServiceName(CmServerServiceType.HIVE)));
      execute(apiResourceRootV3.getClustersResource().getServicesResource(getName(cluster))
          .hiveCreateMetastoreDatabaseTablesCommand(cluster.getServiceName(CmServerServiceType.HIVE)));
      break;
    case OOZIE:
      execute(
          apiResourceRootV3.getClustersResource().getServicesResource(getName(cluster))
              .installOozieShareLib(cluster.getServiceName(CmServerServiceType.OOZIE)), false);
      execute(
          apiResourceRootV3.getClustersResource().getServicesResource(getName(cluster))
              .createOozieDb(cluster.getServiceName(CmServerServiceType.OOZIE)), false);
      break;
    case HBASE:
      execute(apiResourceRootV3.getClustersResource().getServicesResource(getName(cluster))
          .createHBaseRootCommand(cluster.getServiceName(CmServerServiceType.HBASE)));
    case ZOOKEEPER:
      execute(
          apiResourceRootV3.getClustersResource().getServicesResource(getName(cluster))
              .zooKeeperInitCommand(cluster.getServiceName(CmServerServiceType.ZOOKEEPER)), false);
      break;
    case SOLR:
      if (versionApi >= 4) {
        execute(apiResourceRootV4.getClustersResource().getServicesResource(getName(cluster))
            .initSolrCommand(cluster.getServiceName(CmServerServiceType.SOLR)));
        execute(apiResourceRootV4.getClustersResource().getServicesResource(getName(cluster))
            .createSolrHdfsHomeDirCommand(cluster.getServiceName(CmServerServiceType.SOLR)));
      }
      break;
    case SQOOP:
      if (versionApi >= 4) {
        execute(apiResourceRootV4.getClustersResource().getServicesResource(getName(cluster))
            .createSqoopUserDirCommand(cluster.getServiceName(CmServerServiceType.SQOOP)));
      }
      break;
    default:
      break;
    }

    switch (service.getType()) {
    case HDFS_NAMENODE:
      execute(
          apiResourceRootV3.getClustersResource().getServicesResource(getName(cluster))
              .getRoleCommandsResource(cluster.getServiceName(CmServerServiceType.HDFS))
              .formatCommand(new ApiRoleNameList(ImmutableList.<String> builder().add(service.getName()).build())),
          false);
      break;
    case HUE_SERVER:
      execute(
          apiResourceRootV3.getClustersResource().getServicesResource(getName(cluster))
              .getRoleCommandsResource(cluster.getServiceName(CmServerServiceType.HUE))
              .syncHueDbCommand(new ApiRoleNameList(ImmutableList.<String> builder().add(service.getName()).build())),
          false);
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
      execute(apiResourceRootV3.getClustersResource().getServicesResource(getName(cluster))
          .hdfsCreateTmpDir(cluster.getServiceName(CmServerServiceType.HDFS)));
      break;
    default:
      break;
    }

  }

  private void startManagement(final CmServerCluster cluster) throws InterruptedException {

    try {
      if (apiResourceRootV3.getClouderaManagerResource().getMgmtServiceResource().readService(DataView.SUMMARY)
          .getServiceState().equals(ApiServiceState.STOPPED)) {
        CmServerImpl.this.execute("Start " + CmServerServiceTypeCms.MANAGEMENT.getId().toLowerCase(), apiResourceRootV3
            .getClouderaManagerResource().getMgmtServiceResource().startCommand());
      }
    } catch (ServerWebApplicationException exception) {
      // ignore
    }

  }

  private void startService(CmServerCluster cluster, CmServerServiceType type) throws InterruptedException, IOException {
    execute(
        "Start " + type.getId().toLowerCase(),
        apiResourceRootV3.getClustersResource().getServicesResource(getName(cluster))
            .startCommand(cluster.getServiceName(type)));
  }

  private void stopService(CmServerCluster cluster, CmServerServiceType type) throws InterruptedException, IOException {
    execute(
        "Stop " + type.getId().toLowerCase(),
        apiResourceRootV3.getClustersResource().getServicesResource(getName(cluster))
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
        return apiResourceRootV3.getCommandsResource().readCommand(command.getId()).getEndTime() != null;
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
    label = WordUtils.capitalize(label.replace("-", " ").replace("_", " ")).replace(" ", "");
    logger.logOperationStartedAsync(label);
    ApiCommand commandReturn = null;
    int apiPollPeriods = 1;
    int apiPollPeriodLog = 1;
    int apiPollPeriodBackoffNumber = API_POLL_PERIOD_BACKOFF_NUMBER;
    while (true) {
      if (apiPollPeriods++ % apiPollPeriodLog == 0) {
        logger.logOperationInProgressAsync(label);
        if (apiPollPeriodBackoffNumber-- == 0) {
          apiPollPeriodLog += API_POLL_PERIOD_BACKOFF_INCRAMENT;
          apiPollPeriodBackoffNumber = API_POLL_PERIOD_BACKOFF_NUMBER;
        }
      }
      if (callback.poll()) {
        if (checkReturn && command != null
            && !(commandReturn = apiResourceRootV3.getCommandsResource().readCommand(command.getId())).getSuccess()) {
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
