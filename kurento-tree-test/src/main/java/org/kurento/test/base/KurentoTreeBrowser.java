/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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
package org.kurento.test.base;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.kurento.jsonrpc.JsonUtils;
import org.kurento.test.client.KurentoTestClient;
import org.kurento.test.client.SdpOfferProcessor;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.tree.client.KurentoTreeClient;
import org.kurento.tree.client.TreeEndpoint;
import org.kurento.tree.client.internal.IceCandidateInfo;

import com.google.gson.JsonObject;

/**
 * Specific browser page for kurento-tree tests.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class KurentoTreeBrowser extends KurentoTestClient {

	public KurentoTreeBrowser() {
	}

	public KurentoTreeBrowser(KurentoTreeBrowser client) {
		super(client);
	}

	public String setTreeSource(KurentoTreeClient kurentoTree, String treeId, WebRtcChannel channel, WebRtcMode mode)
			throws InterruptedException {
		return internalTreeManagement(kurentoTree, treeId, channel, mode);
	}

	public String addTreeSink(KurentoTreeClient kurentoTree, String treeId, WebRtcChannel channel, WebRtcMode mode)
			throws InterruptedException {
		return internalTreeManagement(kurentoTree, treeId, channel, mode);
	}

	@SuppressWarnings("deprecation")
	public String internalTreeManagement(final KurentoTreeClient kurentoTree, final String treeId,
			final WebRtcChannel channel, final WebRtcMode mode) throws InterruptedException {

		final String out[] = new String[1];
		Thread notif = new Thread("notif") {
			public void run() {
				log.info("Starting gathering candidates from server by polling blocking queue");
				while (true) {
					try {
						IceCandidateInfo candidateInfo = kurentoTree.getServerCandidate();

						if (candidateInfo == null) {
							log.info("Finished gathering candidates from server (notif thread exiting)");
							return;
						}

						JsonObject candidate = JsonUtils.toJsonObject(candidateInfo.getIceCandidate());
						log.debug("Sending candidate {}", candidate);

						browserClient.executeScript("addIceCandidate('" + candidate + "');");
					} catch (Exception e) {
						log.warn("Exception while processing ICE candidate and sending notification", e);
					}
				}

			}
		};
		notif.start();

		final CountDownLatch latch = new CountDownLatch(1);
		Thread t = new Thread() {
			public void run() {
				initWebRtcSdpProcessor(new SdpOfferProcessor() {
					@Override
					public String processSdpOffer(String sdpOffer) {
						String sdpAnswer = null;
						try {
							if (mode == WebRtcMode.SEND_ONLY) {
								sdpAnswer = kurentoTree.setTreeSource(treeId, sdpOffer);
							} else if (mode == WebRtcMode.RCV_ONLY) {
								TreeEndpoint treeEndpoint = kurentoTree.addTreeSink(treeId, sdpOffer);
								sdpAnswer = treeEndpoint.getSdp();
								out[0] = treeEndpoint.getId();
							}

						} catch (Exception e) {
							log.error("Exception processing sdp offer", e);
						}
						return sdpAnswer;
					}
				}, channel, mode);
				latch.countDown();
			}
		};
		t.start();
		if (!latch.await(browserClient.getTimeout(), TimeUnit.SECONDS)) {
			t.interrupt();
			t.stop();
		}

		return out[0];
	}

}
