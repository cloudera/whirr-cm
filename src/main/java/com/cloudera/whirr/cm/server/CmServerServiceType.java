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
  CLUSTER(null, "CLUSTER", CmServerServiceTypeRepo.CDH),

  // Gateway
  GATEWAY(null, "GATEWAY", CmServerServiceTypeRepo.CDH),

  // HDFS
  HDFS(CLUSTER, "HDFS", CmServerServiceTypeRepo.CDH), HDFS_GATEWAY(HDFS, "HDFS_GATEWAY", CmServerServiceTypeRepo.CDH), HDFS_NAMENODE(
      HDFS, "NAMENODE", CmServerServiceTypeRepo.CDH), HDFS_SECONDARY_NAMENODE(HDFS, "SECONDARYNAMENODE",
      CmServerServiceTypeRepo.CDH), HDFS_BALANCER(HDFS, "BALANCER", CmServerServiceTypeRepo.CDH), HDFS_FAILOVER_CONTROLLER(
      HDFS, "FAILOVERCONTROLLER", CmServerServiceTypeRepo.CDH), HDFS_JOURNALNODE(HDFS, "JOURNALNODE",
      CmServerServiceTypeRepo.CDH), HDFS_HTTP_FS(HDFS, "HTTPFS", CmServerServiceTypeRepo.CDH), HDFS_DATANODE(HDFS,
      "DATANODE", CmServerServiceTypeRepo.CDH),

  // MapReduce
  MAPREDUCE(CLUSTER, "MAPREDUCE", CmServerServiceTypeRepo.CDH), MAPREDUCE_GATEWAY(MAPREDUCE, "MAPREDUCE_GATEWAY",
      CmServerServiceTypeRepo.CDH), MAPREDUCE_JOB_TRACKER(MAPREDUCE, "JOBTRACKER", CmServerServiceTypeRepo.CDH), MAPREDUCE_TASK_TRACKER(
      MAPREDUCE, "TASKTRACKER", CmServerServiceTypeRepo.CDH),

  // Zookeeper
  ZOOKEEPER(CLUSTER, "ZOOKEEPER", CmServerServiceTypeRepo.CDH), ZOOKEEPER_SERVER(ZOOKEEPER, "SERVER",
      CmServerServiceTypeRepo.CDH),

  // HBase
  HBASE(CLUSTER, "HBASE", CmServerServiceTypeRepo.CDH), HBASE_GATEWAY(HBASE, "HBASE_GATEWAY",
      CmServerServiceTypeRepo.CDH), HBASE_MASTER(HBASE, "MASTER", CmServerServiceTypeRepo.CDH), HBASE_THRIFT_SERVER(
      HBASE, "HBASETHRIFTSERVER", CmServerServiceTypeRepo.CDH), HBASE_REST_SERVER(HBASE, "HBASERESTSERVER",
      CmServerServiceTypeRepo.CDH), HBASE_REGIONSERVER(HBASE, "REGIONSERVER", CmServerServiceTypeRepo.CDH),

  // Hive
  HIVE(CLUSTER, "HIVE", CmServerServiceTypeRepo.CDH), HIVE_GATEWAY(HIVE, "HIVE_GATEWAY", CmServerServiceTypeRepo.CDH), HIVE_METASTORE(
      HIVE, "HIVEMETASTORE", CmServerServiceTypeRepo.CDH), HIVE_SERVER2(HIVE, "HIVESERVER2",
      CmServerServiceTypeRepo.CDH), HIVE_HCATALOG(HIVE, "WEBHCAT", CmServerServiceTypeRepo.CDH),

  // Solr
  SOLR(CLUSTER, "SOLR", CmServerServiceTypeRepo.SOLR), SOLR_SERVER(SOLR, "SOLR_SERVER",
      CmServerServiceTypeRepo.SOLR),

  // Sqoop
  SQOOP(CLUSTER, "SQOOP", CmServerServiceTypeRepo.CDH), SQOOP_SERVER(SQOOP, "SQOOP_SERVER", CmServerServiceTypeRepo.CDH),

  // Hue
  HUE(CLUSTER, "HUE", CmServerServiceTypeRepo.CDH), HUE_SERVER(HUE, "HUE_SERVER", CmServerServiceTypeRepo.CDH), HUE_BEESWAX_SERVER(
      HUE, "BEESWAX_SERVER", CmServerServiceTypeRepo.CDH),

  // Oozie
  OOZIE(CLUSTER, "OOZIE", CmServerServiceTypeRepo.CDH), OOZIE_SERVER(OOZIE, "OOZIE_SERVER", CmServerServiceTypeRepo.CDH),

  // Impala
  IMPALA(CLUSTER, "IMPALA", CmServerServiceTypeRepo.IMPALA), IMPALA_STATE_STORE(IMPALA, "STATESTORE",
      CmServerServiceTypeRepo.IMPALA), IMPALA_DAEMON(IMPALA, "IMPALAD", CmServerServiceTypeRepo.IMPALA),

  // Flume
  FLUME(CLUSTER, "FLUME", CmServerServiceTypeRepo.CDH), FLUME_AGENT(FLUME, "AGENT", CmServerServiceTypeRepo.CDH),

  // Client
  CLIENT(CLUSTER, "GATEWAY", CmServerServiceTypeRepo.CDH);

  private CmServerServiceType parent;
  private String id;
  private CmServerServiceTypeRepo respository;

  private CmServerServiceType(CmServerServiceType parent, String id, CmServerServiceTypeRepo respository) {
    this.parent = parent;
    this.id = id;
    this.respository = respository;
  }

  public CmServerServiceType getParent() {
    return parent;
  }

  public String getId() {
    return id;
  }

  public CmServerServiceTypeRepo getRepository() {
    return respository;
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
