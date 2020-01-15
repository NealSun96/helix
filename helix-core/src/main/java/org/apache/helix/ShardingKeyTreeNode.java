package org.apache.helix;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.HashMap;
import java.util.Map;

public class ShardingKeyTreeNode {
  private Map<String, ShardingKeyTreeNode> children;
  private Map<String, String> data;
  private String curPath;

  public ShardingKeyTreeNode(String curPath) {
    this.children = new HashMap<String, ShardingKeyTreeNode>();
    this.data = new HashMap<String, String>();
    this.curPath = curPath;
  }

  public ShardingKeyTreeNode(Map<String, ShardingKeyTreeNode> children, Map<String, String> data,
      String curPath) {
    this.children = children;
    this.data = data;
    this.curPath = curPath;
  }

  public Map<String, ShardingKeyTreeNode> getChildren() {
    return children;
  }

  public void setChildren(Map<String, ShardingKeyTreeNode> children) {
    this.children = children;
  }

  public void setChild(String stringKey, ShardingKeyTreeNode node) {
    this.children.put(stringKey, node);
  }

  public Map<String, String> getData() {
    return data;
  }

  public void setData(Map<String, String> data) {
    this.data = data;
  }

  public void setDataEntry(String key, String data) {
    this.data.put(key, data);
  }

  public String getCurPath() {
    return curPath;
  }

  public void setCurPath(String curPath) {
    this.curPath = curPath;
  }
}
