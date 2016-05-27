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
package org.kurento.tree.server.app;

import static org.kurento.tree.client.internal.ProtocolElements.ANSWER_SDP;
import static org.kurento.tree.client.internal.ProtocolElements.ICE_CANDIDATE;
import static org.kurento.tree.client.internal.ProtocolElements.ICE_SDP_MID;
import static org.kurento.tree.client.internal.ProtocolElements.ICE_SDP_M_LINE_INDEX;
import static org.kurento.tree.client.internal.ProtocolElements.OFFER_SDP;
import static org.kurento.tree.client.internal.ProtocolElements.SINK_ID;
import static org.kurento.tree.client.internal.ProtocolElements.TREE_ID;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.kurento.client.IceCandidate;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.JsonRpcErrorException;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
import org.kurento.tree.client.TreeEndpoint;
import org.kurento.tree.client.TreeException;
import org.kurento.tree.client.internal.JsonTreeUtils;
import org.kurento.tree.server.treemanager.TreeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class ClientsJsonRpcHandler extends DefaultJsonRpcHandler<JsonObject> {

  private static final Logger log = LoggerFactory.getLogger(ClientsJsonRpcHandler.class);

  private TreeManager treeManager;

  public ClientsJsonRpcHandler(TreeManager treeManager) {
    this.treeManager = treeManager;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void handleRequest(Transaction transaction, Request<JsonObject> request) throws Exception {

    Response<JsonElement> response = null;

    try {
      Method method = this.getClass().getMethod(request.getMethod(), Session.class, Request.class);
      Session session = transaction.getSession();

      response = (Response<JsonElement>) method.invoke(this, session, request);

      if (response != null) {
        response.setId(request.getId());
        transaction.sendResponseObject(response);
      } else {
        transaction.sendVoidResponse();
      }

    } catch (InvocationTargetException e) {
      log.error("Exception executing request {}", request, e);

      transaction.sendError(e.getCause());

    } catch (NoSuchMethodException e) {
      log.error("Requesting unrecognized method '{}'", request.getMethod());
      transaction.sendError(1, "Unrecognized method '" + request.getMethod() + "'", null);

    } catch (Exception e) {
      log.error("Exception processing request {}", request, e);
      transaction.sendError(e);
    }
  }

  public Response<JsonElement> createTree(Session session, Request<JsonObject> request)
      throws TreeException {

    String treeId = JsonTreeUtils.getRequestParam(request, TREE_ID, String.class, true);
    try {
      if (treeId == null) {
        String newTreeId = treeManager.createTree();
        return new Response<JsonElement>(null, new JsonPrimitive(newTreeId));
      } else {
        treeManager.createTree(treeId);
        return null;
      }
    } catch (TreeException e) {
      e.printStackTrace();
      throw new JsonRpcErrorException(2, e.getMessage());
    }
  }

  public void releaseTree(Session session, Request<JsonObject> request) {
    try {
      treeManager.releaseTree(JsonTreeUtils.getRequestParam(request, TREE_ID, String.class));
    } catch (TreeException e) {
      throw new JsonRpcErrorException(2, e.getMessage());
    }
  }

  public Response<JsonElement> setTreeSource(Session session, Request<JsonObject> request) {
    try {
      String sdp = treeManager.setTreeSource(session,
          JsonTreeUtils.getRequestParam(request, TREE_ID, String.class),
          JsonTreeUtils.getRequestParam(request, OFFER_SDP, String.class));

      JsonObject result = new JsonObject();
      result.addProperty(ANSWER_SDP, sdp);

      return new Response<JsonElement>(null, result);

    } catch (TreeException e) {
      throw new JsonRpcErrorException(2, e.getMessage());
    }
  }

  public Response<JsonElement> addTreeSink(Session session, Request<JsonObject> request) {
    try {
      log.info("Session: id {} , regInfo {} , class {}", session.getSessionId(),
          session.getRegisterInfo(), session.getClass().getName());
      TreeEndpoint endpoint = treeManager.addTreeSink(session,
          JsonTreeUtils.getRequestParam(request, TREE_ID, String.class),
          JsonTreeUtils.getRequestParam(request, OFFER_SDP, String.class));

      JsonObject result = new JsonObject();
      result.addProperty(SINK_ID, endpoint.getId());
      result.addProperty(ANSWER_SDP, endpoint.getSdp());

      return new Response<JsonElement>(null, result);

    } catch (TreeException e) {
      throw new JsonRpcErrorException(2, e.getMessage());
    }
  }

  public void removeTreeSource(Session session, Request<JsonObject> request) {
    try {
      treeManager.removeTreeSource(JsonTreeUtils.getRequestParam(request, TREE_ID, String.class));

    } catch (TreeException e) {
      throw new JsonRpcErrorException(2, e.getMessage());
    }
  }

  public void removeTreeSink(Session session, Request<JsonObject> request) {
    try {
      treeManager.removeTreeSink(JsonTreeUtils.getRequestParam(request, TREE_ID, String.class),
          JsonTreeUtils.getRequestParam(request, SINK_ID, String.class));

    } catch (TreeException e) {
      throw new JsonRpcErrorException(2, e.getMessage());
    }
  }

  public void addIceCandidate(Session session, Request<JsonObject> request) {
    try {
      String candidate = JsonTreeUtils.getRequestParam(request, ICE_CANDIDATE, String.class);
      String sdpMid = JsonTreeUtils.getRequestParam(request, ICE_SDP_MID, String.class);
      int sdpMLineIndex = JsonTreeUtils.getRequestParam(request, ICE_SDP_M_LINE_INDEX,
          Integer.class);
      IceCandidate iceCandidate = new IceCandidate(candidate, sdpMid, sdpMLineIndex);
      String treeId = JsonTreeUtils.getRequestParam(request, TREE_ID, String.class);
      String sinkId = JsonTreeUtils.getRequestParam(request, SINK_ID, String.class, true);

      if (sinkId != null) {
        treeManager.addSinkIceCandidate(treeId, sinkId, iceCandidate);
      } else {
        treeManager.addTreeIceCandidate(treeId, iceCandidate);
      }

    } catch (TreeException e) {
      throw new JsonRpcErrorException(2, e.getMessage());
    }
  }
}
