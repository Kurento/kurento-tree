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

import static org.kurento.commons.PropertiesManager.getProperty;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.kurento.test.TestConfiguration;
import org.kurento.test.config.TestScenario;
import org.kurento.test.services.KurentoMediaServerManager;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.kurento.tree.client.KurentoTreeClient;
import org.kurento.tree.server.app.KurentoTreeServerApp;

/**
 * Base for kurento-tree tests.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class KurentoTreeTest extends WebPageTest<TreeTestPage> {

	public final static String KTS_WS_URI_PROP = "kts.ws.uri";
	public final static String KTS_WS_URI_DEFAULT = "ws://localhost:8890/kurento-tree";

	protected static KurentoTreeClient kurentoTreeClient;

	static {
		System.setProperty("test.port",
				KurentoTreeServerApp.WEBSOCKET_PORT_DEFAULT);
	}

	public KurentoTreeTest(TestScenario testScenario) {
		super(testScenario);
	}

	@BeforeClass
	public static void setupTreeClient() throws IOException {

		String appAutostart = getProperty(
				TestConfiguration.TEST_APP_AUTOSTART_PROPERTY,
				TestConfiguration.TEST_APP_AUTOSTART_DEFAULT);

		if (appAutostart.equals(TestConfiguration.AUTOSTART_TESTSUITE_VALUE)) {

			String kmsAutostart = getProperty(
					TestConfiguration.KMS_AUTOSTART_PROP,
					TestConfiguration.KMS_AUTOSTART_DEFAULT);

			if (!kmsAutostart.equals(TestConfiguration.AUTOSTART_FALSE_VALUE)) {

				// Start kms
				KurentoMediaServerManager kms = KurentoServicesTestHelper
						.startKurentoMediaServer(false);

				System.setProperty("kms.url", kms.getWsUri());
			}

			KurentoTreeServerApp.start();
		}

		kurentoTreeClient = new KurentoTreeClient(
				System.getProperty(KTS_WS_URI_PROP, KTS_WS_URI_DEFAULT));
	}

	@AfterClass
	public static void teardownTreeClient() throws Exception {

		String appAutostart = getProperty(
				TestConfiguration.TEST_APP_AUTOSTART_PROPERTY,
				TestConfiguration.TEST_APP_AUTOSTART_DEFAULT);

		if (appAutostart.equals(TestConfiguration.AUTOSTART_TESTSUITE_VALUE)) {

			KurentoTreeServerApp.stop();

			String kmsAutostart = getProperty(
					TestConfiguration.KMS_AUTOSTART_PROP,
					TestConfiguration.KMS_AUTOSTART_DEFAULT);

			if (!kmsAutostart.equals(TestConfiguration.AUTOSTART_FALSE_VALUE)) {
				KurentoServicesTestHelper.teardownKurentoMediaServer();
			}
		}

		if (kurentoTreeClient != null) {
			kurentoTreeClient.close();
		}
	}

	// public void addFakeClients(int numMockClients,
	// final KurentoTreeClient kurentoTree, final String treeId,
	// final String sinkId) throws TreeException, IOException {
	//
	// MediaPipeline mockPipeline = fakeKurentoClient.createMediaPipeline();
	//
	// for (int i = 0; i < numMockClients; i++) {
	// WebRtcEndpoint mockReceiver = new WebRtcEndpoint.Builder(
	// mockPipeline).build();
	//
	// mockReceiver.addOnIceCandidateListener(
	// new EventListener<OnIceCandidateEvent>() {
	// @Override
	// public void onEvent(OnIceCandidateEvent event) {
	// try {
	// kurentoTree.addIceCandidate(treeId, sinkId,
	// event.getCandidate());
	//
	// } catch (Exception e) {
	// log.error("Exception adding candidate", e);
	// }
	// }
	// });
	//
	// String sdpOffer = mockReceiver.generateOffer();
	// TreeEndpoint treeEndpoint = kurentoTree.addTreeSink(treeId,
	// sdpOffer);
	// String sdpAnswer = treeEndpoint.getSdp();
	// mockReceiver.processAnswer(sdpAnswer);
	// mockReceiver.gatherCandidates();
	// }
	// }

}
