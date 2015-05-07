package org.kurento.tree.server.kms;

import org.kurento.client.IceCandidate;

public class WebRtc extends Element {

	protected WebRtc(Pipeline pipeline) {
		super(pipeline);
	}

	public String processSdpOffer(String sdpOffer) {
		return "fakeSdpResponse";
	}

	public void gatherCandidates() {
	}

	public void addIceCandidate(IceCandidate candidate) {
	}
}
