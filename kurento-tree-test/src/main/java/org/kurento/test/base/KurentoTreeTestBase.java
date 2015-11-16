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
import static org.kurento.test.TestConfiguration.FAKE_KMS_WS_URI_PROP;

import java.io.File;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.kurento.client.EventListener;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.OnIceCandidateEvent;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.commons.exception.KurentoException;
import org.kurento.test.TestConfiguration;
import org.kurento.test.config.TestScenario;
import org.kurento.test.services.KurentoMediaServerManager;
import org.kurento.test.services.KurentoServicesTestHelper;
import org.kurento.tree.client.KurentoTreeClient;
import org.kurento.tree.client.TreeEndpoint;
import org.kurento.tree.client.TreeException;
import org.kurento.tree.server.app.KurentoTreeServerApp;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Base for kurento-tree tests.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class KurentoTreeTestBase extends WebPageTest<TreeTestPage> {

	public final static String KTS_WS_URI_PROP = "kts.ws.uri";
	public final static String KTS_WS_URI_DEFAULT = "ws://localhost:8890/kurento-tree";

	static {
		System.setProperty("test.port",
				KurentoTreeServerApp.WEBSOCKET_PORT_DEFAULT);
	}

	protected static KurentoTreeClient kurentoTreeClient;
	private static String mainKmsWsUri;
	private static KurentoMediaServerManager kms;
	private static KurentoMediaServerManager fakeKms;
	private static KurentoClient fakeKurentoClient;

	private static ConfigurableApplicationContext app;

	public KurentoTreeTestBase(TestScenario testScenario) {
		super(testScenario);
	}

	@BeforeClass
	public static void setupTreeClient() throws IOException {

		String appAutostart = getProperty(
				TestConfiguration.TEST_APP_AUTOSTART_PROPERTY,
				TestConfiguration.TEST_APP_AUTOSTART_DEFAULT);

		if (appAutostart.equals(TestConfiguration.AUTOSTART_TESTSUITE_VALUE)
				&& app == null) {

			String kmsAutostart = getProperty(
					TestConfiguration.KMS_AUTOSTART_PROP,
					TestConfiguration.KMS_AUTOSTART_DEFAULT);

			if (!kmsAutostart.equals(TestConfiguration.AUTOSTART_FALSE_VALUE)) {

				kms = KurentoServicesTestHelper.startKurentoMediaServer(false);

				mainKmsWsUri = kms.getWsUri();

				System.setProperty("kms.url", mainKmsWsUri);
			}

			app = KurentoTreeServerApp.start();
		}

		kurentoTreeClient = new KurentoTreeClient(
				System.getProperty(KTS_WS_URI_PROP, KTS_WS_URI_DEFAULT));
	}

	@AfterClass
	public static void teardownTreeClient() throws Exception {

		// TEST_SUITE Tests not should close app every time
		// String appAutostart = getProperty(
		// TestConfiguration.TEST_APP_AUTOSTART_PROPERTY,
		// TestConfiguration.TEST_APP_AUTOSTART_DEFAULT);
		//
		// if (appAutostart.equals(TestConfiguration.AUTOSTART_TESTSUITE_VALUE))
		// {
		//
		// KurentoTreeServerApp.stop();
		//
		// String kmsAutostart = getProperty(
		// TestConfiguration.KMS_AUTOSTART_PROP,
		// TestConfiguration.KMS_AUTOSTART_DEFAULT);
		//
		// if (!kmsAutostart.equals(TestConfiguration.AUTOSTART_FALSE_VALUE)) {
		// kms.destroy();
		// }
		// }
		//
		// if (fakeKms != null) {
		// fakeKms.destroy();
		// }
		//
		if (kurentoTreeClient != null) {
			kurentoTreeClient.close();
		}
	}

	public void addFakeClients(int numMockClients,
			final KurentoTreeClient kurentoTree, final String treeId,
			final String sinkId) throws TreeException, IOException {

		MediaPipeline mockPipeline = fakeKurentoClient().createMediaPipeline();

		for (int i = 0; i < numMockClients; i++) {
			WebRtcEndpoint mockReceiver = new WebRtcEndpoint.Builder(
					mockPipeline).build();

			mockReceiver.addOnIceCandidateListener(
					new EventListener<OnIceCandidateEvent>() {
						@Override
						public void onEvent(OnIceCandidateEvent event) {
							try {
								kurentoTree.addIceCandidate(treeId, sinkId,
										event.getCandidate());

							} catch (Exception e) {
								log.error("Exception adding candidate", e);
							}
						}
					});

			String sdpOffer = mockReceiver.generateOffer();
			TreeEndpoint treeEndpoint = kurentoTree.addTreeSink(treeId,
					sdpOffer);
			String sdpAnswer = treeEndpoint.getSdp();
			mockReceiver.processAnswer(sdpAnswer);
			mockReceiver.gatherCandidates();
		}
	}

	protected String getDefaultOutputFile(String suffix) {

		KurentoServicesTestHelper.setTestName(testName.getMethodName());
		KurentoServicesTestHelper.setTestCaseName(this.getClass().getName());

		File testResultsFolder = new File(KurentoServicesTestHelper.getTestDir()
				+ "/" + KurentoServicesTestHelper.getTestCaseName());

		if (!testResultsFolder.exists()) {
			if (!testResultsFolder.mkdirs()) {
				throw new KurentoException(
						"Exception creating folders " + testResultsFolder);
			}
		}

		String testName = KurentoServicesTestHelper.getSimpleTestName();
		return testResultsFolder.getAbsolutePath() + "/" + testName + suffix;
	}

	protected synchronized KurentoClient fakeKurentoClient() {

		if (fakeKurentoClient == null) {

			String fakeWsUri = getProperty(FAKE_KMS_WS_URI_PROP);

			if (fakeWsUri == null) {

				String kmsAutostart = getProperty(
						TestConfiguration.FAKE_KMS_AUTOSTART_PROP,
						TestConfiguration.FAKE_KMS_AUTOSTART_DEFAULT);

				String baseWarningMessage = "Requested KurentoClient to simulate fake users but property "
						+ FAKE_KMS_WS_URI_PROP + " is not set.";
				if (!kmsAutostart
						.equals(TestConfiguration.AUTOSTART_FALSE_VALUE)) {
					log.warn(
							baseWarningMessage
									+ " Creating a new KMS for fake clients according to property {}",
							TestConfiguration.FAKE_KMS_AUTOSTART_PROP);

					try {
						fakeKms = KurentoServicesTestHelper
								.startKurentoMediaServer(true);
						fakeWsUri = fakeKms.getWsUri();
					} catch (IOException e) {
						log.warn("Exception creating Kms for fake clients", e);
					}

				} else {
					log.warn(
							baseWarningMessage
									+ " Using the main KMS for fake clients according to property {}",
							TestConfiguration.FAKE_KMS_AUTOSTART_PROP);

					fakeWsUri = mainKmsWsUri;
				}
			}

			fakeKurentoClient = KurentoClient.create(fakeWsUri);
		}

		return fakeKurentoClient;
	}

}
