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

import org.kurento.client.IceCandidate;
import org.kurento.jsonrpc.Session;
import org.kurento.tree.client.TreeEndpoint;
import org.kurento.tree.client.TreeException;
import org.kurento.tree.server.kmsmanager.KmsManager;

public interface TreeManager {

  public String createTree() throws TreeException;

  public void createTree(String treeId) throws TreeException;

  public void releaseTree(String treeId) throws TreeException;

  public String setTreeSource(Session session, String treeId, String sdpOffer) throws TreeException;

  public void removeTreeSource(String treeId) throws TreeException;

  public TreeEndpoint addTreeSink(Session session, String treeId, String sdpOffer)
      throws TreeException;

  public void removeTreeSink(String treeId, String sinkId) throws TreeException;

  public KmsManager getKmsManager();

  public void addSinkIceCandidate(String treeId, String sinkId, IceCandidate iceCandidate);

  public void addTreeIceCandidate(String treeId, IceCandidate iceCandidate);

}
