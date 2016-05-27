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

package org.kurento.tree.client;

import static org.kurento.tree.client.internal.ProtocolElements.ADD_ICE_CANDIDATE_METHOD;
import static org.kurento.tree.client.internal.ProtocolElements.ADD_TREE_SINK_METHOD;
import static org.kurento.tree.client.internal.ProtocolElements.ANSWER_SDP;
import static org.kurento.tree.client.internal.ProtocolElements.CREATE_TREE_METHOD;
import static org.kurento.tree.client.internal.ProtocolElements.ICE_CANDIDATE;
import static org.kurento.tree.client.internal.ProtocolElements.ICE_SDP_MID;
import static org.kurento.tree.client.internal.ProtocolElements.ICE_SDP_M_LINE_INDEX;
import static org.kurento.tree.client.internal.ProtocolElements.OFFER_SDP;
import static org.kurento.tree.client.internal.ProtocolElements.RELEASE_TREE_METHOD;
import static org.kurento.tree.client.internal.ProtocolElements.REMOVE_TREE_SINK_METHOD;
import static org.kurento.tree.client.internal.ProtocolElements.REMOVE_TREE_SOURCE_METHOD;
import static org.kurento.tree.client.internal.ProtocolElements.SET_TREE_SOURCE_METHOD;
import static org.kurento.tree.client.internal.ProtocolElements.SINK_ID;
import static org.kurento.tree.client.internal.ProtocolElements.TREE_ID;

import java.io.IOException;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.kurento.client.IceCandidate;
import org.kurento.jsonrpc.JsonRpcErrorException;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.client.JsonRpcClientWebSocket;
import org.kurento.tree.client.internal.JsonTreeUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class KurentoTreeClient {

  private JsonRpcClient client;
  private ServerJsonRpcHandler handler;

  public KurentoTreeClient(String wsUri) {
    this(new JsonRpcClientWebSocket(wsUri + "/websocket"));
  }

  public KurentoTreeClient(String wsUri, SslContextFactory sslContextFactory) {
    this(new JsonRpcClientWebSocket(wsUri + "/websocket", sslContextFactory));
  }

  public KurentoTreeClient(JsonRpcClient client) {
    this.client = client;
    this.handler = new ServerJsonRpcHandler();
    this.client.setServerRequestHandler(this.handler);
  }

  public KurentoTreeClient(JsonRpcClient client, ServerJsonRpcHandler handler) {
    this.client = client;
    this.handler = handler;
    this.client.setServerRequestHandler(this.handler);
  }

  public String createTree() throws IOException {
    JsonElement response = client.sendRequest(CREATE_TREE_METHOD);
    return JsonUtils.extractJavaValueFromResult(response, String.class);
  }

  public void createTree(String treeId) throws IOException {
    JsonObject params = new JsonObject();
    params.addProperty(TREE_ID, treeId);
    try {
      client.sendRequest(CREATE_TREE_METHOD, params);
    } catch (JsonRpcErrorException e) {
      processException(e);
    }
  }

  public void releaseTree(String treeId) throws TreeException, IOException {
    JsonObject params = new JsonObject();
    params.addProperty(TREE_ID, treeId);
    try {
      client.sendRequest(RELEASE_TREE_METHOD, params);
    } catch (JsonRpcErrorException e) {
      processException(e);
    }
  }

  public String setTreeSource(String treeId, String offerSdp) throws TreeException, IOException {

    JsonObject params = new JsonObject();
    params.addProperty(TREE_ID, treeId);
    params.addProperty(OFFER_SDP, offerSdp);

    try {

      JsonElement result = client.sendRequest(SET_TREE_SOURCE_METHOD, params);
      return JsonTreeUtils.getResponseProperty(result, ANSWER_SDP, String.class);
    } catch (JsonRpcErrorException e) {
      processException(e);
      return null;
    }
  }

  public void removeTreeSource(String treeId) throws TreeException, IOException {

    JsonObject params = new JsonObject();
    params.addProperty(TREE_ID, treeId);

    try {

      client.sendRequest(REMOVE_TREE_SOURCE_METHOD, params);

    } catch (JsonRpcErrorException e) {
      processException(e);
    }
  }

  public TreeEndpoint addTreeSink(String treeId, String offerSdp)
      throws IOException, TreeException {

    JsonObject params = new JsonObject();
    params.addProperty(TREE_ID, treeId);
    params.addProperty(OFFER_SDP, offerSdp);

    try {

      JsonElement result = client.sendRequest(ADD_TREE_SINK_METHOD, params);

      return new TreeEndpoint(JsonTreeUtils.getResponseProperty(result, ANSWER_SDP, String.class),
          JsonTreeUtils.getResponseProperty(result, SINK_ID, String.class));

    } catch (JsonRpcErrorException e) {
      processException(e);
      return null;
    }
  }

  public void removeTreeSink(String treeId, String sinkId) throws TreeException, IOException {

    JsonObject params = new JsonObject();
    params.addProperty(TREE_ID, treeId);
    params.addProperty(SINK_ID, sinkId);

    try {

      client.sendRequest(REMOVE_TREE_SINK_METHOD, params);

    } catch (JsonRpcErrorException e) {
      processException(e);
    }
  }

  /**
   * Polls the candidates list maintained by this client to obtain a candidate gathered on the
   * server side. This method blocks until there is a candidate to return. This is a one-time
   * operation for the returned element.
   *
   * @return the gathered candidate, null when interrupted while waiting
   */
  public IceCandidateInfo getServerCandidate() {
    return this.handler.getCandidateInfo();
  }

  /**
   * Notifies the server of a gathered ICE candidate on the client side.
   *
   * @param treeId
   *          the tree identifier
   * @param sinkId
   *          optional (nullable) identifier
   * @param candidate
   *          the gathered candidate
   * @throws TreeException
   * @throws IOException
   */
  public void addIceCandidate(String treeId, String sinkId, IceCandidate candidate)
      throws TreeException, IOException {
    JsonObject params = new JsonObject();
    params.addProperty(TREE_ID, treeId);
    if (sinkId != null && !sinkId.isEmpty()) {
      params.addProperty(SINK_ID, sinkId);
    }
    params.addProperty(ICE_CANDIDATE, candidate.getCandidate());
    params.addProperty(ICE_SDP_M_LINE_INDEX, candidate.getSdpMLineIndex());
    params.addProperty(ICE_SDP_MID, candidate.getSdpMid());
    try {
      client.sendRequest(ADD_ICE_CANDIDATE_METHOD, params);
    } catch (JsonRpcErrorException e) {
      processException(e);
    }
  }

  public void close() throws IOException {
    this.client.close();
  }

  private void processException(JsonRpcErrorException e) throws TreeException {
    if (e.getCode() == 2) {
      throw new TreeException(e.getMessage());
    } else {
      throw e;
    }
  }
}
