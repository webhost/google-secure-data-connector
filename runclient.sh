#!/bin/bash
#
# Copyright 2008 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Runs agent with appropriate class path.
#
# $Id$

WOODSTOCK_HOME=/opt/google/secure-data-connector/1.1
WOODSTOCK_LIB=/opt/google/secure-data-connector/1.1/lib
CONFIGFILE=/etc/google/secure-data-connector/1.1/localConfig.xml
RULESFILE=/etc/google/secure-data-connector/1.1/resourceRules.xml
LOG4JPROPERTIESFILE=/etc/google/secure-data-connector/1.1/log4j.properties
JVM_ARGS="-Djava.net.preferIPv4Stack=true"
GROUP=securedataconnector
USER=securedataconnector
DEBUG=""
NOVERIFY="false"
# Dynamically generate classpath from lib directory.
jarfiles=$(find ${WOODSTOCK_LIB} -type f -a -name \*.jar)
for jar in ${jarfiles}; do
  CLASSPATH=${CLASSPATH}:${jar}
done
export CLASSPATH

# Create flags for local config file.
OPTS=$(getopt -o dnf:r:j: --long debug,noverify,localConfigFile::,rulesFile::,jvm_args:: -n 'runagent' -- "$@")
if [ $? != 0 ]; then
  echo -e "\nUsage:
    -d|--debug) Place into debug mode
    -n|--noverify) Skip file permission checks.
    -f|--localConfigFile) Local configuration file
    -r|--rulesFile) Resource rules file.
    -j|--jvm-args) Override default jvmargs with these.
  " >&2
  exit 1
fi

# Parse command line
eval set -- "${OPTS}"
while true; do
  case "$1" in
    -d|--debug) DEBUG="--debug"; shift 1 ;;
    -n|--noverify) NOVERIFY="true"; shift 1 ;;
    -f|--localConfigFile) CONFIGFILE=$2; shift 2 ;;
    -r|--rulesFile) RULESFILE=$2; shift 2 ;;
    -j|--jvm-args) JVM_ARGS=$2; shift 2 ;;
    -l|--log4jPropertiesFile) LOG4JPROPERTIESFILE=$2; shift 2 ;;
    --) shift ; break ;;
    *) echo "Error!" ; exit 1 ;;
  esac
done

# Make sure we are the only one who can read any generated files.
umask 0077

# Check file permissions

# Checks to see if the file has the supplied permissions and ownership.
# exits the script if its incorrect.
#
# $1 filename
# $2 owner
# $3 group
# $4 mode

function checkfile {

if [ ${NOVERIFY} = "false" ]; then

  file=$1
  if [ -e ${file} ]; then
    filegroup=$(stat -c "%G" ${file})
    fileowner=$(stat -c "%U" ${file})
    filemode=$(stat -c "%a" ${file})

    if [ ${fileowner} != "$2" -o  \
        ${filegroup} != "$3" -o \
        ${filemode} != "$4" ]; then
      echo $file has incorrect permissions should be $2:$3 $4
      exit
    fi
  else
    echo $file does not exist!
    exit
  fi
fi
}

function migrate {

  local_config_file=$1
  resource_rules_file=$2

  # Local Config
  egrep -q '<clientId' ${local_config_file}

  if [ $? = 0 ]; then
    echo "Migrating ${local_config_file}"
    sed -i ${local_config_file} -e 's^clientId>^agentId>^g'
  fi

  # Resource Rules
  egrep -q '<feed|<entity|<allowedEntities|<clientId|<pattern|<patternType' \
      ${resource_rules_file}

  if [ $? = 0 ]; then
    echo "Migrating ${resource_rules_file}"
    sed -i ${resource_rules_file} -e 's^feed>^resourceRules>^g'
    # repeatable
    sed -i ${resource_rules_file} -e 's^<entity^<rule^g'
    sed -i ${resource_rules_file} -e 's^entity>^rule>^g'
    # repeatable
    sed -i ${resource_rules_file} -e 's^<allowedEntities^<viewerEmail^g'
    sed -i ${resource_rules_file} -e 's^allowedEntities>^viewerEmail>^g'
    sed -i ${resource_rules_file} -e 's^clientId>^agentId>^g'
    sed -i ${resource_rules_file} -e 's^pattern>^url>^g'
    sed -i ${resource_rules_file} -e 's^patternType>^urlMatch>^g'
  fi
}

if [ ${NOVERIFY} = "false" ]; then
  # Check current file permissions and refuse to run if they arent correct.
  checkfile ${CONFIGFILE} root ${GROUP} 640
  checkfile ${RULESFILE} root ${GROUP} 640
  checkfile ${ETCPREFIX}/localConfig.xml root ${GROUP} 640
  checkfile ${ETCPREFIX}/resourceRules.xml root ${GROUP} 640
fi

migrate ${CONFIGFILE} ${RULESFILE}

while /bin/true; do
  su ${USER} -c "__JAVABIN__ ${JVM_ARGS} com.google.dataconnector.client.Client \
    ${DEBUG} \
    -localConfigFile \"${CONFIGFILE}\" \
    -rulesFile \"${RULESFILE}\" \
    -log4jPropertiesFile \"${LOG4JPROPERTIESFILE}\" \
    $*"
  sleep 5
  echo "RECONNECTING..."
done
