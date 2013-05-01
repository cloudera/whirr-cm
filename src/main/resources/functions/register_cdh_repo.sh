#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
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
function register_cdh_repo() {
    REPOCDH=${REPOCDH:-cdh4}
    CDH_REPO_HOST=${CDH_REPO_HOST:-archive.cloudera.com}
    CDH_MAJOR_VERSION=$(echo $REPOCDH | sed -e 's/cdh\([0-9]\).*/\1/')
    CDH_VERSION=$(echo $REPOCDH | sed -e 's/cdh\([0-9][0-9]*\)/\1/')
    IMPALA_VERSION=${IMPALA_VERSION:-1}
    CDH_REPO_ROOT=${CDH_REPO_ROOT:-http://$CDH_REPO_HOST/cdh$CDH_MAJOR_VERSION}
    IMPALA_REPO_ROOT=${CDH_REPO_ROOT:-http://$CDH_REPO_HOST/impala}
    if which dpkg &> /dev/null; then
	retry_apt-get -y install lsb-release
	OS_CODENAME=$(lsb_release -sc)
	OS_DISTID=$(lsb_release -si | tr '[A-Z]' '[a-z]')
        cat > /etc/apt/sources.list.d/cloudera-$REPOCDH.list <<EOF
deb [arch=amd64] $CDH_REPO_ROOT/$OS_DISTID/$OS_CODENAME/amd64/cdh $OS_CODENAME-$REPOCDH contrib
deb-src $CDH_REPO_ROOT/$OS_DISTID/$OS_CODENAME/amd64/cdh $OS_CODENAME-$REPOCDH contrib
EOF
        curl -s $CDH_REPO_ROOT/$OS_DISTID/$OS_CODENAME/amd64/cdh/archive.key | apt-key add -
        cat > /etc/apt/sources.list.d/cloudera-impala.list <<EOF
deb [arch=amd64] $IMPALA_REPO_ROOT/$OS_DISTID/$OS_CODENAME/amd64/impala $OS_CODENAME-impala contrib
deb-src $IMPALA_REPO_ROOT/$OS_DISTID/$OS_CODENAME/amd64/impala $OS_CODENAME-impala contrib
EOF
        curl -s $IMPALA_REPO_ROOT/$OS_DISTID/$OS_CODENAME/amd64/impala/archive.key | apt-key add -
        retry_apt_get -y update
    elif which rpm &> /dev/null; then
        cat > /etc/yum.repos.d/cloudera-$REPOCDH.repo <<EOF
[cloudera-$REPOCDH]
name=Cloudera's Distribution for Hadoop, Version $CDH_VERSION
baseurl=$CDH_REPO_ROOT/redhat/\$releasever/\$basearch/cdh/$CDH_VERSION/
gpgkey=$CDH_REPO_ROOT/redhat/\$releasever/\$basearch/cdh/RPM-GPG-KEY-cloudera
gpgcheck=1
EOF
        rpm --import $CDH_REPO_ROOT/redhat/$(rpm -q --qf "%{VERSION}" $(rpm -q --whatprovides redhat-release))/$(rpm -q --qf "%{ARCH}" $(rpm -q --whatprovides redhat-release))/cdh/RPM-GPG-KEY-cloudera
        cat > /etc/yum.repos.d/cloudera-impala.repo <<EOF
[cloudera-impala]
name=Cloudera Impala, version $IMPALA_VERSION
baseurl=$IMPALA_REPO_ROOT/redhat/\$releasever/\$basearch/impala/$IMPALA_VERSION/
gpgkey=$IMPALA_REPO_ROOT/redhat/\$releasever/\$basearch/impala/RPM-GPG-KEY-cloudera
gpgcheck=1
EOF
        rpm --import $IMPALA_REPO_ROOT/redhat/$(rpm -q --qf "%{VERSION}" $(rpm -q --whatprovides redhat-release))/$(rpm -q --qf "%{ARCH}" $(rpm -q --whatprovides redhat-release))/impala/RPM-GPG-KEY-cloudera
        
        retry_yum update -y retry_yum
    fi
}
