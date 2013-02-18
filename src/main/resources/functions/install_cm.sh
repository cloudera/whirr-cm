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
  OS_CODENAME=$(lsb_release -sc)
  OS_DISTID=$(lsb_release -si | tr '[A-Z]' '[a-z]')
  if [ $CM_MAJOR_VERSION -ge 4 ]; then
	  if which dpkg &> /dev/null; then
        cat > /etc/apt/sources.list.d/cloudera-$REPO.list <<EOF
deb http://$REPO_HOST/cm$CM_MAJOR_VERSION/$OS_DISTID/$OS_CODENAME/amd64/cm $OS_CODENAME-$REPO contrib
deb-src http://$REPO_HOST/cm$CM_MAJOR_VERSION/$OS_DISTID/$OS_CODENAME/amd64/cm $OS_CODENAME-$REPO contrib
EOF
        curl -s http://$REPO_HOST/cm$CM_MAJOR_VERSION/$OS_DISTID/$OS_CODENAME/amd64/cm/archive.key | apt-key add -
	  elif which rpm &> /dev/null; then
        cat > /etc/yum.repos.d/cloudera-$REPO.repo <<EOF
[cloudera-manager-$REPO]
name=Cloudera Manager, Version $CM_VERSION
baseurl=http://$REPO_HOST/cm$CM_MAJOR_VERSION/redhat/\$releasever/\$basearch/cm/$CM_VERSION/
gpgkey=http://$REPO_HOST/cm$CM_MAJOR_VERSION/redhat/\$releasever/\$basearch/cm/RPM-GPG-KEY-cloudera
gpgcheck=1
EOF
        rpm --import http://$REPO_HOST/cm$CM_MAJOR_VERSION/redhat/$(rpm -q --qf "%{VERSION}" $(rpm -q --whatprovides redhat-release))/$(rpm -q --qf "%{ARCH}" $(rpm -q --whatprovides redhat-release))/cm/RPM-GPG-KEY-cloudera
	  fi
  fi
}
