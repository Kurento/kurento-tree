package org.kurento.tree.server.kms.real;

import org.kurento.client.MediaElement;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.commons.PropertiesManager;
import org.kurento.module.plumberendpoint.PlumberEndpoint;
import org.kurento.tree.server.kms.Element;
import org.kurento.tree.server.kms.Plumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealPlumber extends Plumber implements RealElement {

  private static final boolean USE_WEBRTC_AS_PLUMBER = PropertiesManager
      .getProperty("useWebRtcAsPlumber", true);

  private static Logger log = LoggerFactory.getLogger(RealPlumber.class);

  private MediaElement endpoint;

  public RealPlumber(RealPipeline pipeline) {
    super(pipeline);
    if (USE_WEBRTC_AS_PLUMBER) {
      endpoint = new WebRtcEndpoint.Builder(pipeline.getMediaPipeline()).build();
    } else {
      endpoint = new PlumberEndpoint.Builder(pipeline.getMediaPipeline()).build();
    }

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

    if (USE_WEBRTC_AS_PLUMBER) {

      WebRtcEndpoint thisWebRtc = (WebRtcEndpoint) endpoint;
      WebRtcEndpoint otherWebRtc = (WebRtcEndpoint) ((RealPlumber) plumber).endpoint;

      log.debug("Connecting webRtcs from Kms {} to Kms {}", this.getPipeline().getKms(),
          plumber.getPipeline().getKms());

      thisWebRtc.addOnIceCandidateListener((e) -> otherWebRtc.addIceCandidate(e.getCandidate()));
      otherWebRtc.addOnIceCandidateListener((e) -> thisWebRtc.addIceCandidate(e.getCandidate()));

      thisWebRtc.processAnswer(otherWebRtc.processOffer(thisWebRtc.generateOffer()));

      thisWebRtc.gatherCandidates();
      otherWebRtc.gatherCandidates();

    } else {

      PlumberEndpoint thisPlumber = (PlumberEndpoint) endpoint;
      PlumberEndpoint otherPlumber = (PlumberEndpoint) ((RealPlumber) plumber).endpoint;

      String address = otherPlumber.getAddress();
      int port = otherPlumber.getPort();
      log.debug("Connecting plumber to adress:" + address + " port:" + port);
      thisPlumber.link(address, port);
    }
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
