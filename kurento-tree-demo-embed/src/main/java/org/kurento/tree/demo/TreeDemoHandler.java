/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Protocol handler for 1 to N video call communication.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.0
 */
public class TreeDemoHandler extends TextWebSocketHandler {

  private static final Logger log = LoggerFactory.getLogger(TreeDemoHandler.class);
  private static final Gson gson = new GsonBuilder().create();

  private ConcurrentHashMap<String, UserSession> viewers = new ConcurrentHashMap<String, UserSession>();

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
  public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);
    log.debug("Incoming message from session '{}': {}", session.getId(), jsonMessage);

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
        session.sendMessage(new TextMessage(response.toString()));
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
        session.sendMessage(new TextMessage(response.toString()));
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
      presenterUserSession.sendMessage(response);

    } else {

      JsonObject response = new JsonObject();
      response.addProperty("id", "presenterResponse");
      response.addProperty("response", "rejected");
      response.addProperty("message",
          "Another user is currently acting as sender. Try again later ...");
      session.sendMessage(new TextMessage(response.toString()));
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
      session.sendMessage(new TextMessage(response.toString()));

    } else {

      if (viewers.containsKey(session.getId())) {
        JsonObject response = new JsonObject();
        response.addProperty("id", "viewerResponse");
        response.addProperty("response", "rejected");
        response.addProperty("message",
            "You are already viewing in this session. Use a different browser to add additional viewers.");
        session.sendMessage(new TextMessage(response.toString()));
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
      viewer.sendMessage(response);
    }
  }

  private synchronized void stop(WebSocketSession session) throws IOException {
    String sessionId = session.getId();
    if (presenterUserSession != null
        && presenterUserSession.getSession().getId().equals(sessionId)) {

      for (UserSession viewer : viewers.values()) {
        JsonObject response = new JsonObject();
        response.addProperty("id", "stopCommunication");
        viewer.sendMessage(response);
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

          while (session == null) {

            // TODO improve, maybe keep sinkIds and sessionIds in a
            // separate map
            for (UserSession userSession : viewers.values()) {
              if (candidateInfo.getSinkId() != null && userSession.getSinkId() != null
                  && userSession.getSinkId().equals(candidateInfo.getSinkId())) {
                session = userSession.getSession();
                break;
              }
            }

            // FIXME Dirty hack to avoid concurrency problem with candidates
            Thread.sleep(500);
          }
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

        synchronized (this) {
          session.sendMessage(new TextMessage(notification.toString()));
        }

      } catch (Exception e) {
        log.warn("Exception while processing ICE candidate and sending notification", e);
      }
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    stop(session);
  }

}
