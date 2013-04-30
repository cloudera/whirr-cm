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

function install_mysql() {
  if [ "$INSTALL_DATABASE" != "1" ]; then
    if which dpkg &> /dev/null; then
      export DEBIAN_FRONTEND=noninteractive
      retry_apt_get update
      sleep $[ ( $RANDOM % 30 )  + 1 ]s
      retry_apt_get update
      retry_apt_get -q -y install expect mysql-server-5.5 libmysql-java
      service mysql stop
      MYSQL_CONF="/etc/mysql/my.cnf"
    elif which rpm &> /dev/null; then
      retry_yum install -y expect mysql-server mysql-connector-java
      MYSQL_CONF="/etc/my.cnf"
    fi
    rm -rf /var/lib/mysql/ib_logfile*
    echo '
[mysqld]

datadir=/var/lib/mysql
log_bin=/var/lib/mysql/mysql_binary_log

user=mysql
binlog_format=mixed

default_storage_engine=INNODB

transaction_isolation=READ-COMMITTED

key_buffer=16M
key_buffer_size=32M
max_allowed_packet=16M
thread_stack=256K
thread_cache_size=64
query_cache_limit=8M
query_cache_size=64M
query_cache_type=1
max_connections=750

read_buffer_size=2M
read_rnd_buffer_size=16M
sort_buffer_size=8M
join_buffer_size=8M

innodb_file_per_table=1
innodb_flush_log_at_trx_commit=2
innodb_thread_concurrency=8
innodb_flush_method=O_DIRECT
innodb_log_buffer_size=32M
innodb_buffer_pool_size=2G
innodb_log_file_size=256M

[mysqld_safe]

log_error=/var/log/mysqld.log
pid_file=/var/run/mysqld/mysqld.pid

' > $MYSQL_CONF
    if which dpkg &> /dev/null; then
      service mysql start
    elif which rpm &> /dev/null; then
      service mysqld start
    fi
    cat >> mysql_setup <<END
#!/usr/bin/expect -f
set timeout 5000
spawn mysql_secure_installation 
expect "Enter current password for root (enter for none): "
send "\r"
expect "Set root password?"
send "n\r"
expect "Remove anonymous users?"
send "n\r"
expect "Disallow root login remotely?"
send "n\r"
expect "Remove test database and access to it?"
send "Y\r"
expect "Reload privilege tables now?"
send "Y\r"
expect EOF
END
    chmod +x mysql_setup
    ./mysql_setup
    rm -rf ./mysql_setup
    INSTALL_DATABASE=1
  fi
  mysql -u root -e "CREATE DATABASE $1 DEFAULT CHARACTER SET utf8"
  mysql -u root -e "CREATE USER '$1'@'localhost' IDENTIFIED BY '$1'"
  mysql -u root -e "GRANT ALL PRIVILEGES ON $1.* TO '$1'@'localhost' WITH GRANT OPTION"
  mysql -u root -e "CREATE USER '$1'@'%' IDENTIFIED BY '$1'"
  mysql -u root -e "GRANT ALL PRIVILEGES ON $1.* TO '$1'@'%' WITH GRANT OPTION"
}

function install_database() {
  local OPTIND
  local OPTARG
  TYPE=mysql
  DATABASE=database
  while getopts "t:d:" OPTION; do
    case $OPTION in
      t)
        TYPE="$OPTARG"
        ;;
      d)
        DATABASE="$OPTARG"
        ;;
    esac
  done
  if [ "$TYPE" == "mysql" ]; then
    install_mysql $DATABASE
  else
    echo "Unknown database type [$TYPE]."
  fi 
}