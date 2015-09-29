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

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.kurento.client.EventListener;
import org.kurento.client.MediaPipeline;
import org.kurento.client.OnIceCandidateEvent;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.config.TestScenario;
import org.kurento.tree.client.KurentoTreeClient;
import org.kurento.tree.client.TreeEndpoint;
import org.kurento.tree.client.TreeException;

/**
 * Base for kurento-tree tests.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class KurentoTreeTest extends KurentoClientWebPageTest<TreeTestPage> {

	public final static String KTS_WS_URI_PROP = "kts.ws.uri";
	public final static String KTS_WS_URI_DEFAULT = "ws://localhost:8890/kurento-tree";

	protected static KurentoTreeClient kurentoTreeClient;

	public KurentoTreeTest(TestScenario testScenario) {
		super(testScenario);
	}

	@Before
	public void setupTreeClient() throws IOException {
		kurentoTreeClient = new KurentoTreeClient(System.getProperty(KTS_WS_URI_PROP, KTS_WS_URI_DEFAULT));
	}

	@After
	public void teardownTreeClient() throws Exception {
		if (kurentoTreeClient != null) {
			kurentoTreeClient.close();
		}
	}

	public void addFakeClients(int numMockClients, final KurentoTreeClient kurentoTree, final String treeId,
			final String sinkId) throws TreeException, IOException {

		MediaPipeline mockPipeline = fakeKurentoClient.createMediaPipeline();

		for (int i = 0; i < numMockClients; i++) {
			WebRtcEndpoint mockReceiver = new WebRtcEndpoint.Builder(mockPipeline).build();

			mockReceiver.addOnIceCandidateListener(new EventListener<OnIceCandidateEvent>() {
				@Override
				public void onEvent(OnIceCandidateEvent event) {
					try {
						kurentoTree.addIceCandidate(treeId, sinkId, event.getCandidate());

					} catch (Exception e) {
						log.error("Exception adding candidate", e);
					}
				}
			});

			String sdpOffer = mockReceiver.generateOffer();
			TreeEndpoint treeEndpoint = kurentoTree.addTreeSink(treeId, sdpOffer);
			String sdpAnswer = treeEndpoint.getSdp();
			mockReceiver.processAnswer(sdpAnswer);
			mockReceiver.gatherCandidates();
		}
	}

}
