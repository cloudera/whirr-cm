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
  if which dpkg &> /dev/null; then
    export DEBIAN_FRONTEND=noninteractive
    retry_apt_get update
    retry_apt_get -q -y install lsb-release
  elif which rpm &> /dev/null; then
    retry_yum install -y redhat-lsb
  fi
  REPOCM=${REPOCM:-cm4}
  CM_REPO_HOST=${CM_REPO_HOST:-archive.cloudera.com}
  CM_MAJOR_VERSION=$(echo $REPOCM | sed -e 's/cm\([0-9]\).*/\1/')
  CM_VERSION=$(echo $REPOCM | sed -e 's/cm\([0-9][0-9]*\)/\1/')
  OS_CODENAME=$(lsb_release -sc)
  OS_DISTID=$(lsb_release -si | tr '[A-Z]' '[a-z]')
  
  CM_REPO_ROOT=${CM_REPO_ROOT:-http://$CM_REPO_HOST/cm$CM_MAJOR_VERSION}

  if [ $CM_MAJOR_VERSION -ge 4 ]; then
      if which dpkg &> /dev/null; then
          cat > /etc/apt/sources.list.d/cloudera-$REPOCM.list <<EOF
deb [arch=amd64] $CM_REPO_ROOT/$OS_DISTID/$OS_CODENAME/amd64/cm $OS_CODENAME-$REPOCM contrib
deb-src $CM_REPO_ROOT/$OS_DISTID/$OS_CODENAME/amd64/cm $OS_CODENAME-$REPOCM contrib
EOF
          curl -s $CM_REPO_ROOT/$OS_DISTID/$OS_CODENAME/amd64/cm/archive.key | apt-key add -
      elif which rpm &> /dev/null; then
          RHEL_VERSION=$(lsb_release -sr | sed -e 's/\([0-9]*\)\..*/\1/')
          cat > /etc/yum.repos.d/cloudera-$REPOCM.repo <<EOF
[cloudera-manager-$REPOCM]
name=Cloudera Manager, Version $CM_VERSION
baseurl=$CM_REPO_ROOT/redhat/$RHEL_VERSION/\$basearch/cm/$CM_VERSION/
gpgkey=$CM_REPO_ROOT/redhat/$RHEL_VERSION/\$basearch/cm/RPM-GPG-KEY-cloudera
gpgcheck=1
EOF
          rpm --import $CM_REPO_ROOT/redhat/${RHEL_VERSION}/$(rpm -q --qf "%{ARCH}" $(rpm -q --whatprovides redhat-release))/cm/RPM-GPG-KEY-cloudera
      fi
  fi
}
