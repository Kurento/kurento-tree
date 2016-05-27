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
package org.kurento.tree.server.kms.real;

import java.io.IOException;

import org.kurento.client.EventListener;
import org.kurento.client.IceCandidate;
import org.kurento.client.OnIceCandidateEvent;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.tree.client.internal.ProtocolElements;
import org.kurento.tree.server.app.TreeElementSession;
import org.kurento.tree.server.kms.Element;
import org.kurento.tree.server.kms.WebRtc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

public class RealWebRtc extends WebRtc implements RealElement {
  private static final Logger log = LoggerFactory.getLogger(RealWebRtc.class);

  private WebRtcEndpoint webRtcEndpoint;

  public RealWebRtc(RealPipeline pipeline, final TreeElementSession session) {
    super(pipeline);

    this.webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline.getMediaPipeline()).build();

    if (getLabel() != null) {
      this.webRtcEndpoint.setName(getLabel());
    }

    this.webRtcEndpoint
        .addMediaStateChangedListener(e -> log.info("WebRtcEndpoint {} state changed from {} to {}",
            this.getLabel(), e.getOldState(), e.getNewState()));

    this.webRtcEndpoint.addOnIceCandidateListener(new EventListener<OnIceCandidateEvent>() {
      @Override
      public void onEvent(OnIceCandidateEvent event) {
        try {
          JsonObject params = new JsonObject();
          params.addProperty(ProtocolElements.TREE_ID, session.getTreeId());
          params.addProperty(ProtocolElements.SINK_ID, session.getSinkId());
          params.addProperty(ProtocolElements.ICE_SDP_M_LINE_INDEX,
              event.getCandidate().getSdpMLineIndex());
          params.addProperty(ProtocolElements.ICE_SDP_MID, event.getCandidate().getSdpMid());
          params.addProperty(ProtocolElements.ICE_CANDIDATE, event.getCandidate().getCandidate());
          session.getSession().sendNotification(ProtocolElements.ICE_CANDIDATE_EVENT, params);
          log.debug("Sent ICE candidate notif for {}: {} - {}", session,
              event.getCandidate().getSdpMid(), event.getCandidate().getCandidate());
        } catch (IOException e) {
          log.warn("Exception while sending ice candidate for {}", session, e);
        }
      }
    });
  }

  @Override
  public String processSdpOffer(String sdpOffer) {
    return webRtcEndpoint.processOffer(sdpOffer);
  }

  @Override
  public void gatherCandidates() {
    webRtcEndpoint.gatherCandidates();
  }

  @Override
  public void addIceCandidate(IceCandidate candidate) {
    webRtcEndpoint.addIceCandidate(candidate);
  }

  @Override
  public void release() {
    super.release();
    webRtcEndpoint.release();
  }

  @Override
  public WebRtcEndpoint getMediaElement() {
    return webRtcEndpoint;
  }

  @Override
  public void connect(Element element) {
    if (!(element instanceof RealElement)) {
      throw new RuntimeException("A real element can not be connected to non real one");
    }
    super.connect(element);
    webRtcEndpoint.connect(((RealElement) element).getMediaElement());
  }

  @Override
  public String getId() {
    return webRtcEndpoint.getId();
  }

  @Override
  public void setLabel(String label) {
    super.setLabel(label);
    webRtcEndpoint.setName(label);
  }
}
