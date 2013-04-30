#
# Licensed to Cloudera, Inc. under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# Cloudera, Inc. licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -x

function configure_cm_cdh() {
  local OPTIND
  local OPTARG
  CM_CDH_ROLE=
  CM_CDH_DIRS="/data"
  while getopts "r:d:" OPTION; do
    case $OPTION in
      r)
        CM_CDH_ROLE="$OPTARG"
        ;;
      d)
        CM_CDH_DIRS="$OPTARG"
        ;;
    esac
  done
  export IFS=, 
  CM_CDH_DIRS_ARRAY=($CM_CDH_DIRS)
  for i in "${CM_CDH_DIRS_ARRAY[@]}"; do
    mkdir -p $i
    chmod 777 $i
  done
  if [ "$CM_CDH_ROLE" = "cm-cdh-jobtracker" ]; then
  mkdir -p "${CM_CDH_DIRS_ARRAY[0]}/mapreduce/jobtracker/history"
  chmod 777 "${CM_CDH_DIRS_ARRAY[0]}/mapreduce/jobtracker/history"
  elif [ "$CM_CDH_ROLE" = "cm-cdh-oozie-server" ]; then
  if [ -f "/usr/share/java/mysql-connector-java.jar" ]; then
    mkdir -p /var/lib/oozie
    chmod 777 /var/lib/oozie
    ln -s /usr/share/java/mysql-connector-java.jar /var/lib/oozie/mysql-connector-java.jar
    fi
  fi
}
