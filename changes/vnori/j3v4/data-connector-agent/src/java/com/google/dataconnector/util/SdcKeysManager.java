/* Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

package com.google.dataconnector.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.dataconnector.protocol.proto.SdcFrame.ResourceKey;
import com.google.inject.Singleton;

import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Manage SDC keys
 * @author vnori@google.com (Your Name Here)
 */
@Singleton
public class SdcKeysManager {
  private static final Logger LOG = Logger.getLogger(SdcKeysManager.class);

  // HashMap that stores all the layer 4 rules associated with their unique key.
  private Multimap<String, Pair<String, Integer>> keysMap = HashMultimap.create();

  /**
   * stores the secretkeys for the given patterns from the input 
   * {@link ResourceKey} list.
   * 
   * "synchronized" on this can be improved upon. 
   * what we really need is a more lightweight semaphore
   * to handle many readers and rare writers.
   *  
   * @param resourceKeysList
   */
  public synchronized void storeSecretKeys(List<ResourceKey> resourceKeysList) {
    // remove existing keys
    LOG.debug("clearing keys and about to store new set of keys received");
    keysMap.clear();
    
    // store the keys 
    for (ResourceKey resourceKey : resourceKeysList) {
      Pair<String, Integer> p = Pair.of(resourceKey.getIp(), resourceKey.getPort());
      LOG.info("Adding rule for " + p);
      keysMap.put(String.valueOf(resourceKey.getKey()), p);
    }
    
    // print the keys
    for (Map.Entry<String, Pair<String, Integer>> entry : keysMap.entries()) {
      LOG.debug("key: " + entry.getKey() + "," + entry.getValue() + "\n");
    }
  }

  synchronized boolean checkKeyIpPort(String key, String ip, int port) {
    LOG.debug("checking key for ip: " + ip + ", port: " + port);
    return keysMap.containsEntry(key, Pair.of(ip, port));  
  }
  
  synchronized boolean containsKey(String key) {
    LOG.debug("checking to see if this key exists " + key);
    return keysMap.containsKey(key);  
  }
  
  @VisibleForTesting
  Multimap<String, Pair<String, Integer>> getKeysMap() {
    return keysMap;
  }
}