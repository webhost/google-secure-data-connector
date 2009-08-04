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
# Stops secure-data-connector running in the background.


if [ $UID != 0 ] ; then
 echo You should run this script as root or via sudo
else
 # Kill the wrapper runclient.sh before stopping java process so it is not restarted
 ps -ef |grep runclient.sh |grep -v grep | awk '{ print $2 }' | xargs kill
 ps -ef |grep .*java.*resourceRules.xml | awk '{ print $2 }' | xargs kill
fi

