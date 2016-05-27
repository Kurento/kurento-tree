/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.kurento.tree.server.treemanager;

import org.kurento.tree.client.TreeException;

public abstract class AbstractOneTreeTM implements TreeManager {

  private static final String DEFAULT_TREE_ID = "TreeId";

  private String treeId;

  protected boolean createdTree = false;

  public AbstractOneTreeTM() {
    // TODO Auto-generated constructor stub
  }

  @Override
  public synchronized String createTree() throws TreeException {
    if (createdTree) {
      throw new TreeException(
          "AotOneTreeManager " + " can only create one tree and this tree was previously created");
    }
    treeId = DEFAULT_TREE_ID;
    createdTree = true;
    return treeId;
  }

  @Override
  public void createTree(String treeId) throws TreeException {
    if (createdTree) {
      throw new TreeException(
          "AotOneTreeManager " + " can only create one tree and this tree was previously created");
    }
    this.treeId = treeId;
    createdTree = true;
  }

  protected void checkTreeId(String treeId) throws TreeException {
    if (!this.treeId.equals(treeId)) {
      throw new TreeException("Unknown tree '" + treeId + "'");
    }
  }
}
