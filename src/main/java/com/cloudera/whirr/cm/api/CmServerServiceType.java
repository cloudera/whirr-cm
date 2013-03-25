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

enum CmServerServiceTypeRepository {
  CDH, IMPALA
};

public enum CmServerServiceType {

  // Cluster
  CLUSTER(null, "CLUSTER", CmServerServiceTypeRepository.CDH),

  // HDFS
  HDFS(CLUSTER, "HDFS", CmServerServiceTypeRepository.CDH), HDFS_NAMENODE(HDFS, "NAMENODE",
      CmServerServiceTypeRepository.CDH), HDFS_SECONDARY_NAMENODE(HDFS, "SECONDARYNAMENODE",
      CmServerServiceTypeRepository.CDH), HDFS_DATANODE(HDFS, "DATANODE", CmServerServiceTypeRepository.CDH),

  // MapReduce
  MAPREDUCE(CLUSTER, "MAPREDUCE", CmServerServiceTypeRepository.CDH), MAPREDUCE_JOB_TRACKER(MAPREDUCE, "JOBTRACKER",
      CmServerServiceTypeRepository.CDH), MAPREDUCE_TASK_TRACKER(MAPREDUCE, "TASKTRACKER",
      CmServerServiceTypeRepository.CDH),

  // Zookeeper
  ZOOKEEPER(CLUSTER, "ZOOKEEPER", CmServerServiceTypeRepository.CDH), ZOOKEEPER_SERVER(ZOOKEEPER, "SERVER",
      CmServerServiceTypeRepository.CDH),

  // HBase
  HBASE(CLUSTER, "HBASE", CmServerServiceTypeRepository.CDH), HBASE_MASTER(HBASE, "MASTER",
      CmServerServiceTypeRepository.CDH), HBASE_REGIONSERVER(HBASE, "REGIONSERVER", CmServerServiceTypeRepository.CDH),

  // Hive
  HIVE(CLUSTER, "HIVE", CmServerServiceTypeRepository.CDH), HIVE_METASTORE(HIVE, "HIVEMETASTORE",
      CmServerServiceTypeRepository.CDH),

  // Hue
  HUE(CLUSTER, "HUE", CmServerServiceTypeRepository.CDH), HUE_SERVER(HUE, "HUE_SERVER",
      CmServerServiceTypeRepository.CDH), HUE_BEESWAX_SERVER(HUE, "BEESWAX_SERVER", CmServerServiceTypeRepository.CDH),

  // Oozie
  OOZIE(CLUSTER, "OOZIE", CmServerServiceTypeRepository.CDH), OOZIE_SERVER(OOZIE, "OOZIE_SERVER",
                                                                           CmServerServiceTypeRepository.CDH),

  // Impala
  IMPALA(CLUSTER, "IMPALA", CmServerServiceTypeRepository.IMPALA), IMPALA_STATE_STORE(IMPALA, "STATESTORE",
      CmServerServiceTypeRepository.IMPALA), IMPALA_DAEMON(IMPALA, "IMPALAD", CmServerServiceTypeRepository.IMPALA),

  // Client
  CLIENT(CLUSTER, "GATEWAY", CmServerServiceTypeRepository.CDH);

  private CmServerServiceType parent;
  private String label;
  private CmServerServiceTypeRepository respository;

  private CmServerServiceType(CmServerServiceType parent, String label, CmServerServiceTypeRepository respository) {
    this.parent = parent;
    this.label = label;
    this.respository = respository;
  }

  public CmServerServiceType getParent() {
    return parent;
  }

  public String getLabel() {
    return label;
  }

  public CmServerServiceTypeRepository getRepository() {
    return respository;
  }

}
