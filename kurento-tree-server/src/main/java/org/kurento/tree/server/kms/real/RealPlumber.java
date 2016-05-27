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

import org.kurento.client.MediaElement;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.tree.server.kms.Element;
import org.kurento.tree.server.kms.Plumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealPlumber extends Plumber implements RealElement {

  private static Logger log = LoggerFactory.getLogger(RealPlumber.class);

  private MediaElement endpoint;

  public RealPlumber(RealPipeline pipeline) {
    super(pipeline);
    endpoint = new WebRtcEndpoint.Builder(pipeline.getMediaPipeline()).build();

    if (getLabel() != null) {
      endpoint.setName(getLabel());
    }
  }

  @Override
  public void connect(Element element) {
    if (!(element instanceof RealElement)) {
      throw new RuntimeException("A real element can not be connected to non real one");
    }
    super.connect(element);
    endpoint.connect(((RealElement) element).getMediaElement());
  }

  @Override
  public MediaElement getMediaElement() {
    return endpoint;
  }

  @Override
  public void link(Plumber plumber) {
    if (!(plumber instanceof RealPlumber)) {
      throw new RuntimeException("A real plumber can not be linked to non real one");
    }

    super.link(plumber);

    WebRtcEndpoint thisWebRtc = (WebRtcEndpoint) endpoint;
    WebRtcEndpoint otherWebRtc = (WebRtcEndpoint) ((RealPlumber) plumber).endpoint;

    log.debug("Connecting webRtcs from Kms {} to Kms {}", this.getPipeline().getKms(),
        plumber.getPipeline().getKms());

    thisWebRtc.addOnIceCandidateListener((e) -> otherWebRtc.addIceCandidate(e.getCandidate()));
    otherWebRtc.addOnIceCandidateListener((e) -> thisWebRtc.addIceCandidate(e.getCandidate()));

    thisWebRtc.processAnswer(otherWebRtc.processOffer(thisWebRtc.generateOffer()));

    thisWebRtc.gatherCandidates();
    otherWebRtc.gatherCandidates();

  }

  @Override
  public void release() {
    super.release();
    endpoint.release();
  }

  @Override
  public void setLabel(String label) {
    super.setLabel(label);
    endpoint.setName(label);
  }
}
