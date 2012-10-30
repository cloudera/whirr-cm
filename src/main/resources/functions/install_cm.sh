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

function install_cm() {
  REPO=${REPOCM:-cm4}
  REPO_HOST=${REPO_HOST:-archive.cloudera.com}
  CM_MAJOR_VERSION=$(echo $REPO | sed -e 's/cm\([0-9]\).*/\1/')
  CM_VERSION=$(echo $REPO | sed -e 's/cm\([0-9][0-9]*\)/\1/')
  if [ $CM_MAJOR_VERSION -ge "4" ]; then
	  if which dpkg &> /dev/null; then
      cat > /etc/apt/sources.list.d/cloudera-$REPO.list <<EOF
deb http://$REPO_HOST/$REPO/ubuntu/lucid/amd64/cm lucid-$REPO contrib
deb-src http://$REPO_HOST/$REPO/ubuntu/lucid/amd64/cm lucid-$REPO contrib
EOF
      curl -s http://$REPO_HOST/$REPO/ubuntu/lucid/amd64/cm/archive.key | apt-key add -
	    retry_apt_get -y install bigtop-utils bigtop-jsvc bigtop-tomcat hue-plugins
	  elif which rpm &> /dev/null; then
      cat > /etc/yum.repos.d/cloudera-$REPO.repo <<EOF
[cloudera-manager]
# Packages for Cloudera Manager, Version $CM_VERSION, on RedHat or CentOS 6 x86_64
name=Cloudera Manager
baseurl=http://$REPO_HOST/$REPO/redhat/\$releasever/\$basearch/cm/$CM_MAJOR_VERSION/
gpgkey=http://archive.cloudera.com/$REPO/redhat/\$releasever/\$basearch/cm/RPM-GPG-KEY-cloudera
gpgcheck=0
enabled=1
EOF
	    retry_yum install -y bigtop-utils bigtop-jsvc bigtop-tomcat hue-plugins
	  fi
  fi
}
