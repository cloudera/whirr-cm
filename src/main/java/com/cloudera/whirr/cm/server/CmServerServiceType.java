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

public enum CmServerServiceType {

  // Cluster
  CLUSTER(null, "CLUSTER", 3, CmServerServiceTypeRepo.CDH),

  // Gateway
  GATEWAY(null, "GATEWAY", 3, CmServerServiceTypeRepo.CDH),

  // HDFS
  HDFS(CLUSTER, "HDFS", 3, CmServerServiceTypeRepo.CDH), HDFS_GATEWAY(HDFS, "HDFS_GATEWAY", 3,
      CmServerServiceTypeRepo.CDH), HDFS_NAMENODE(HDFS, "NAMENODE", 3, CmServerServiceTypeRepo.CDH), HDFS_SECONDARY_NAMENODE(
      HDFS, "SECONDARYNAMENODE", 3, CmServerServiceTypeRepo.CDH), HDFS_BALANCER(HDFS, "BALANCER", 3,
      CmServerServiceTypeRepo.CDH), HDFS_FAILOVER_CONTROLLER(HDFS, "FAILOVERCONTROLLER", 3, CmServerServiceTypeRepo.CDH), HDFS_JOURNALNODE(
      HDFS, "JOURNALNODE", 3, CmServerServiceTypeRepo.CDH), HDFS_HTTP_FS(HDFS, "HTTPFS", 3, CmServerServiceTypeRepo.CDH), HDFS_DATANODE(
      HDFS, "DATANODE", 3, CmServerServiceTypeRepo.CDH),

  // MapReduce
  MAPREDUCE(CLUSTER, "MAPREDUCE", 3, CmServerServiceTypeRepo.CDH), MAPREDUCE_GATEWAY(MAPREDUCE, "MAPREDUCE_GATEWAY", 3,
      CmServerServiceTypeRepo.CDH), MAPREDUCE_JOB_TRACKER(MAPREDUCE, "JOBTRACKER", 3, CmServerServiceTypeRepo.CDH), MAPREDUCE_TASK_TRACKER(
      MAPREDUCE, "TASKTRACKER", 3, CmServerServiceTypeRepo.CDH),

  // Zookeeper
  ZOOKEEPER(CLUSTER, "ZOOKEEPER", 3, CmServerServiceTypeRepo.CDH), ZOOKEEPER_SERVER(ZOOKEEPER, "SERVER", 3,
      CmServerServiceTypeRepo.CDH),

  // HBase
  HBASE(CLUSTER, "HBASE", 3, CmServerServiceTypeRepo.CDH), HBASE_GATEWAY(HBASE, "HBASE_GATEWAY", 3,
      CmServerServiceTypeRepo.CDH), HBASE_MASTER(HBASE, "MASTER", 3, CmServerServiceTypeRepo.CDH), HBASE_THRIFT_SERVER(
      HBASE, "HBASETHRIFTSERVER", 3, CmServerServiceTypeRepo.CDH), HBASE_REST_SERVER(HBASE, "HBASERESTSERVER", 3,
      CmServerServiceTypeRepo.CDH), HBASE_REGIONSERVER(HBASE, "REGIONSERVER", 3, CmServerServiceTypeRepo.CDH),

  // Hive
  HIVE(CLUSTER, "HIVE", 3, CmServerServiceTypeRepo.CDH), HIVE_GATEWAY(HIVE, "HIVE_GATEWAY", 3,
      CmServerServiceTypeRepo.CDH), HIVE_METASTORE(HIVE, "HIVEMETASTORE", 3, CmServerServiceTypeRepo.CDH), HIVE_SERVER2(
      HIVE, "HIVESERVER2", 4, CmServerServiceTypeRepo.CDH), HIVE_HCATALOG(HIVE, "WEBHCAT", 4,
      CmServerServiceTypeRepo.CDH),

  // Solr
  SOLR(CLUSTER, "SOLR", 4, CmServerServiceTypeRepo.SOLR), SOLR_SERVER(SOLR, "SOLR_SERVER", 4,
      CmServerServiceTypeRepo.SOLR),

  // Sqoop
  SQOOP(CLUSTER, "SQOOP", 4, CmServerServiceTypeRepo.CDH), SQOOP_SERVER(SQOOP, "SQOOP_SERVER", 4,
      CmServerServiceTypeRepo.CDH),

  // Hue
  HUE(CLUSTER, "HUE", 3, CmServerServiceTypeRepo.CDH), HUE_SERVER(HUE, "HUE_SERVER", 3, CmServerServiceTypeRepo.CDH), HUE_BEESWAX_SERVER(
      HUE, "BEESWAX_SERVER", 3, CmServerServiceTypeRepo.CDH),

  // Oozie
  OOZIE(CLUSTER, "OOZIE", 3, CmServerServiceTypeRepo.CDH), OOZIE_SERVER(OOZIE, "OOZIE_SERVER", 3,
      CmServerServiceTypeRepo.CDH),

  // Impala
  IMPALA(CLUSTER, "IMPALA", 3, CmServerServiceTypeRepo.IMPALA), IMPALA_STATE_STORE(IMPALA, "STATESTORE", 3,
      CmServerServiceTypeRepo.IMPALA), IMPALA_DAEMON(IMPALA, "IMPALAD", 3, CmServerServiceTypeRepo.IMPALA),

  // Flume
  FLUME(CLUSTER, "FLUME", 3, CmServerServiceTypeRepo.CDH), FLUME_AGENT(FLUME, "AGENT", 3, CmServerServiceTypeRepo.CDH),

  // Client
  CLIENT(CLUSTER, "GATEWAY", 3, CmServerServiceTypeRepo.CDH);

  private CmServerServiceType parent;
  private String id;
  private int version;
  private CmServerServiceTypeRepo respository;

  private CmServerServiceType(CmServerServiceType parent, String id, int version, CmServerServiceTypeRepo respository) {
    this.parent = parent;
    this.id = id;
    this.version = version;
    this.respository = respository;
  }

  public CmServerServiceType getParent() {
    return parent;
  }

  public String getId() {
    return id;
  }

  public int getVersion() {
    return version;
  }

  public CmServerServiceTypeRepo getRepository() {
    return respository;
  }

  public boolean isConcrete() {
    return getParent() != null && !getParent().equals(CLUSTER);
  }

  public static CmServerServiceType valueOfId(String id) {
    for (CmServerServiceType type : values()) {
      if (type.getId().equals(id)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown " + CmServerServiceType.class.getName() + " id [" + id + "]");
  }

}
