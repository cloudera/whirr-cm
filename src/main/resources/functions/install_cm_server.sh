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

function install_cm_server() {
  if which dpkg &> /dev/null; then
    export DEBIAN_FRONTEND=noninteractive
    retry_apt_get update
    retry_apt_get -q -y install oracle-j2sdk1.6 cloudera-manager-server cloudera-manager-daemons
  elif which rpm &> /dev/null; then
    retry_yum install -y jdk cloudera-manager-server cloudera-manager-daemons
  fi
  rm -rvf /etc/cloudera-scm-server/db.mgmt.properties
  mkdir -p /data/manager/parcels/parcel-repository
  chown cloudera-scm.cloudera-scm /data/manager/parcels/parcel-repository
}
