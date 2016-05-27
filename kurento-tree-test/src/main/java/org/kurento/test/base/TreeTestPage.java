/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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
package org.kurento.test.base;

import java.util.concurrent.CountDownLatch;

import org.kurento.client.IceCandidate;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.browser.WebRtcTestPage;
import org.kurento.tree.client.IceCandidateInfo;
import org.kurento.tree.client.KurentoTreeClient;
import org.kurento.tree.client.TreeEndpoint;

import com.google.gson.JsonObject;

/**
 * Specific browser page for kurento-tree tests.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class TreeTestPage extends WebRtcTestPage {

	public TreeTestPage() {
		super();
	}

	public String setTreeSource(KurentoTreeClient kurentoTree, String treeId,
			WebRtcChannel channel, WebRtcMode mode)
					throws InterruptedException {
		return internalTreeManagement(kurentoTree, treeId, channel, mode);
	}

	public String addTreeSink(KurentoTreeClient kurentoTree, String treeId,
			WebRtcChannel channel, WebRtcMode mode)
					throws InterruptedException {
		return internalTreeManagement(kurentoTree, treeId, channel, mode);
	}

	private void retrieveIceCandidates(KurentoTreeClient kurentoTree) {

		log.info(
				"Starting gathering candidates from server by polling blocking queue");

		while (true) {
			try {
				IceCandidateInfo candidateInfo = kurentoTree
						.getServerCandidate();

				if (candidateInfo == null) {
					log.info(
							"Finished gathering candidates from server (notif thread exiting)");
					return;
				}

				JsonObject candidate = JsonUtils
						.toJsonObject(candidateInfo.getIceCandidate());
				log.debug("Sending candidate {}", candidate);

				TreeTestPage.super.addIceCandidate(candidate);

			} catch (Exception e) {
				log.warn(
						"Exception while processing ICE candidate and sending notification",
						e);
			}
		}
	}

	public String internalTreeManagement(final KurentoTreeClient kurentoTree,
			final String treeId, final WebRtcChannel channel,
			final WebRtcMode mode) throws InterruptedException {

		new Thread(() -> retrieveIceCandidates(kurentoTree)).start();

		String[] id = new String[1];
		CountDownLatch sinkIdReceived = new CountDownLatch(1);

		WebRtcConfigurer webRtcConfigurer = new WebRtcConfigurer() {

			@Override
			public void addIceCandidate(IceCandidate candidate) {
				try {
					sinkIdReceived.await();
					kurentoTree.addIceCandidate(treeId, id[0], candidate);
				} catch (Exception e) {
					log.error("Exception processing iceCandidate");
				}
			}

			public String processOffer(String sdpOffer) {

				String sdpAnswer = null;
				try {

					if (mode == WebRtcMode.SEND_ONLY) {
						sdpAnswer = kurentoTree.setTreeSource(treeId, sdpOffer);

					} else if (mode == WebRtcMode.RCV_ONLY) {
						TreeEndpoint treeEndpoint = kurentoTree
								.addTreeSink(treeId, sdpOffer);
						sdpAnswer = treeEndpoint.getSdp();
						id[0] = treeEndpoint.getId();
					}

				} catch (Exception e) {
					log.error("Exception processing sdp offer", e);
				}

				sinkIdReceived.countDown();
				return sdpAnswer;
			}
		};

		initWebRtc(webRtcConfigurer, channel, mode);

		sinkIdReceived.await();

		return id[0];
	}

}
