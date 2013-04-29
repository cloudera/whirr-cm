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

function configure_cm_agent() {
  local OPTIND
  local OPTARG
  CM_SERVER_HOST=localhost
  CM_SERVER_PORT=7180
  CM_AGENT_LOG_FILE=/var/log/cloudera-scm-agent
  while getopts "h:p:l:" OPTION; do
    case $OPTION in
      h)
        CM_SERVER_HOST="$OPTARG"
        ;;
	  p)
	    CM_SERVER_PORT="$OPTARG"
	    ;;
	  l)
	    CM_AGENT_LOG_FILE="$OPTARG"
	    ;;
	  esac
  done
  sed -i -e "s|server_host=.*|server_host=$CM_SERVER_HOST|" /etc/cloudera-scm-agent/config.ini
  sed -i -e "s|server_port=.*|server_port=$CM_SERVER_PORT|" /etc/cloudera-scm-agent/config.ini
  sed -i -e "s|# log_file=.*|log_file=$CM_AGENT_LOG_FILE|" /etc/cloudera-scm-agent/config.ini
  service cloudera-scm-agent start
}
