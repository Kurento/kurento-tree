/*
 * (C) Copyright 2014-2016 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package org.kurento.tree.demo;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.kurento.client.IceCandidate;
import org.kurento.tree.client.IceCandidateInfo;
import org.kurento.tree.client.KurentoTreeClient;
import org.kurento.tree.client.TreeEndpoint;
import org.kurento.tree.client.TreeException;
import org.kurento.tree.client.internal.ProtocolElements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Protocol handler for 1 to N video call communication.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.4.1
 */
public class TreeDemoHandler extends TextWebSocketHandler {

  private final Logger log = LoggerFactory.getLogger(TreeDemoHandler.class);

  private Map<String, UserSession> viewers = new ConcurrentHashMap<String, UserSession>();

  @Autowired
  private KurentoTreeClient kurentoTree;

  private UserSession presenterUserSession;

  private String treeId;

  private Thread notifThread;

  public TreeDemoHandler() {
    this.notifThread = new Thread("notif:") {
      @Override
      public void run() {
        try {
          internalSendNotification();
        } catch (InterruptedException e) {
          return;
        }
      }
    };
  }

  /**
   * This bean's 'initMethod'.
   */
  public void init() {
    this.notifThread.start();
  }

  /**
   * This bean's 'destroyMethod'.
   */
  public void cleanup() {
    this.notifThread.interrupt();
  }

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message)
      throws TreeException, IOException {
    JsonObject jsonMessage = new GsonBuilder().create().fromJson(message.getPayload(),
        JsonObject.class);
    log.info("Incoming message from session {}: {}", session.getId(), jsonMessage);

    switch (jsonMessage.get("id").getAsString()) {
    case "presenter":
      try {
        presenter(session, jsonMessage);
      } catch (Throwable t) {
        stop(session);
        log.error(t.getMessage(), t);
        JsonObject response = new JsonObject();
        response.addProperty("id", "presenterResponse");
        response.addProperty("response", "rejected");
        response.addProperty("message", t.getMessage());
        sendMessage(session, response);
      }
      break;
    case "viewer":
      try {
        viewer(session, jsonMessage);
      } catch (Throwable t) {
        stop(session);
        log.error(t.getMessage(), t);
        JsonObject response = new JsonObject();
        response.addProperty("id", "viewerResponse");
        response.addProperty("response", "rejected");
        response.addProperty("message", t.getMessage());
        sendMessage(session, response);
      }
      break;
    case "stop":
      stop(session);
      break;
    case "onIceCandidate":
      onIceCandidate(session, jsonMessage);
      break;
    default:
      break;
    }
  }

  private synchronized void presenter(WebSocketSession session, JsonObject jsonMessage)
      throws IOException {

    if (presenterUserSession == null) {
      presenterUserSession = new UserSession(session);

      treeId = kurentoTree.createTree();

      String sdpOffer = jsonMessage.getAsJsonPrimitive("sdpOffer").getAsString();

      String sdpAnswer = kurentoTree.setTreeSource(treeId, sdpOffer);

      JsonObject response = new JsonObject();
      response.addProperty("id", "presenterResponse");
      response.addProperty("response", "accepted");
      response.addProperty("sdpAnswer", sdpAnswer);
      sendMessage(presenterUserSession.getSession(), response);

    } else {

      JsonObject response = new JsonObject();
      response.addProperty("id", "presenterResponse");
      response.addProperty("response", "rejected");
      response.addProperty("message",
          "Another user is currently acting as sender. Try again later ...");
      sendMessage(session, response);
    }
  }

  private synchronized void viewer(WebSocketSession session, JsonObject jsonMessage)
      throws IOException {

    if (presenterUserSession == null) {

      JsonObject response = new JsonObject();
      response.addProperty("id", "viewerResponse");
      response.addProperty("response", "rejected");
      response.addProperty("message",
          "No active sender now. Become sender or . Try again later ...");
      sendMessage(session, response);

    } else {

      if (viewers.containsKey(session.getId())) {
        JsonObject response = new JsonObject();
        response.addProperty("id", "viewerResponse");
        response.addProperty("response", "rejected");
        response.addProperty("message",
            "You are already viewing in this session. Use a different browser to add additional viewers.");
        sendMessage(session, response);
        return;
      }

      UserSession viewer = new UserSession(session);
      viewers.put(session.getId(), viewer);

      String sdpOffer = jsonMessage.getAsJsonPrimitive("sdpOffer").getAsString();

      TreeEndpoint treeEndpoint = kurentoTree.addTreeSink(treeId, sdpOffer);
      viewer.setSinkId(treeEndpoint.getId());

      String sdpAnswer = treeEndpoint.getSdp();

      JsonObject response = new JsonObject();
      response.addProperty("id", "viewerResponse");
      response.addProperty("response", "accepted");
      response.addProperty("sdpAnswer", sdpAnswer);

      sendMessage(viewer.getSession(), response);
    }
  }

  private synchronized void stop(WebSocketSession session) throws TreeException, IOException {
    String sessionId = session.getId();
    if (presenterUserSession != null
        && presenterUserSession.getSession().getId().equals(sessionId)) {

      for (UserSession viewer : viewers.values()) {
        JsonObject response = new JsonObject();
        response.addProperty("id", "stopCommunication");

        sendMessage(viewer.getSession(), response);

      }

      log.info("Releasing media pipeline");
      kurentoTree.releaseTree(treeId);
      presenterUserSession = null;

    } else if (viewers.containsKey(sessionId)) {

      String sinkId = viewers.get(sessionId).getSinkId();
      if (sinkId != null) {
        kurentoTree.removeTreeSink(treeId, sinkId);
      }
      viewers.remove(sessionId);
    }
  }

  private synchronized void onIceCandidate(WebSocketSession session, JsonObject jsonMessage)
      throws IOException {
    String sessionId = session.getId();
    String sinkId = null;
    if (viewers.containsKey(sessionId)) {
      sinkId = viewers.get(sessionId).getSinkId();
    } else if (presenterUserSession == null
        || !presenterUserSession.getSession().getId().equals(sessionId)) {
      log.warn("No active user session found for id " + sessionId + ". Ice candidate discarded: "
          + jsonMessage);
      return;
    }

    String candidate = jsonMessage.get(ProtocolElements.ICE_CANDIDATE).getAsString();
    int sdpMLineIndex = jsonMessage.get(ProtocolElements.ICE_SDP_M_LINE_INDEX).getAsInt();
    String sdpMid = jsonMessage.get(ProtocolElements.ICE_SDP_MID).getAsString();
    kurentoTree.addIceCandidate(treeId, sinkId, new IceCandidate(candidate, sdpMid, sdpMLineIndex));
  }

  private void internalSendNotification() throws InterruptedException {
    log.info("Starting gathering candidates from server by polling blocking queue");
    while (true) {
      try {
        IceCandidateInfo candidateInfo = kurentoTree.getServerCandidate();
        if (candidateInfo == null) {
          log.info("Finished gathering candidates from server (notif thread exiting)");
          return;
        }
        log.debug("Sending notification {}", candidateInfo);
        WebSocketSession session = null;
        if (!candidateInfo.getTreeId().equals(treeId)) {
          throw new TreeException(
              "Unrecognized ice candidate info for current tree " + treeId + " : " + candidateInfo);
        }
        if (candidateInfo.getSinkId() == null) {
          if (presenterUserSession == null) {
            throw new TreeException(
                "No sender session, so candidate info will be discarded: " + candidateInfo);
          }
          session = presenterUserSession.getSession();
        } else {

          do {
            for (UserSession userSession : viewers.values()) {
              if (candidateInfo.getSinkId() != null && userSession.getSinkId() != null
                  && userSession.getSinkId().equals(candidateInfo.getSinkId())) {
                session = userSession.getSession();
                break;
              }
            }
            if (session == null) {
              // Wait to establish WebSocket connection
              Thread.sleep(500);

            } else {
              break;
            }
          } while (true);

        }
        if (session == null) {
          throw new TreeException(
              "No viewer session for the 'sinkId' from candidate info, will be discarded: "
                  + candidateInfo);
        }
        JsonObject notification = new JsonObject();
        notification.addProperty("id", ProtocolElements.ICE_CANDIDATE_EVENT);
        notification.addProperty(ProtocolElements.ICE_CANDIDATE,
            candidateInfo.getIceCandidate().getCandidate());
        notification.addProperty(ProtocolElements.ICE_SDP_M_LINE_INDEX,
            candidateInfo.getIceCandidate().getSdpMLineIndex());
        notification.addProperty(ProtocolElements.ICE_SDP_MID,
            candidateInfo.getIceCandidate().getSdpMid());

        sendMessage(session, notification);

      } catch (Exception e) {
        log.warn("Exception while processing ICE candidate and sending notification", e);
      }
    }

  }

  public synchronized void sendMessage(WebSocketSession session, JsonObject jsonObject) {
    try {
      TextMessage message = new TextMessage(jsonObject.toString());
      log.info("Sending message {} in session {}", message.getPayload(), session.getId());

      if (session.isOpen()) {
        session.sendMessage(message);
      }

    } catch (IOException e) {
      log.error("Exception sending message {}", jsonObject, e);
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    log.info("Closed websocket connection of session {}", session.getId());

    stop(session);
  }

}
