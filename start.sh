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
#
# Starts runclient.sh in background and log stdout and stderr to file.

if [ $UID != 0 ] ; then
 echo You should run this script as root or via sudo
else
 nohup /opt/google/secure-data-connector/1.1/bin/runclient.sh $* >>/var/log/google/secure-data-connector/1.1/agent 2>&1 &
 echo Please review log /var/log/google/secure-data-connector/1.1/agent for details
fi


