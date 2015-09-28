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
package org.kurento.test.scalability.tree;

import static org.kurento.commons.PropertiesManager.getProperty;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.test.base.KurentoClientTest;
import org.kurento.test.base.KurentoTreeTest;
import org.kurento.test.client.BrowserClient;
import org.kurento.test.client.BrowserType;
import org.kurento.test.client.Client;
import org.kurento.test.client.WebRtcChannel;
import org.kurento.test.client.WebRtcMode;
import org.kurento.test.config.BrowserConfig;
import org.kurento.test.config.BrowserScope;
import org.kurento.test.config.TestScenario;
import org.kurento.test.latency.ChartWriter;
import org.kurento.test.latency.LatencyController;
import org.kurento.test.latency.LatencyRegistry;
import org.kurento.tree.client.KurentoTreeClient;

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * <strong>Description</strong>: WebRTC one to one (plus N fake clients) with
 * tree.<br/>
 * <strong>Pipeline</strong>:
 * <ul>
 * <li>TreeSource -> TreeSink</li>
 * </ul>
 * <strong>Pass criteria</strong>:
 * <ul>
 * <li>Media should be received in the video tag</li>
 * <li>Color of the video should be as expected</li>
 * </ul>
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class TreeScalabilityTest extends KurentoTreeTest {

	private static final int WIDTH = 500;
	private static final int HEIGHT = 270;

	private static int playTime = getProperty("test.scalability.latency.playtime", 30); // seconds
	private static String[] fakeClientsArray = getProperty("test.scalability.latency.fakeclients", "0,50,100,150")
			.split(",");

	private static Map<Long, LatencyRegistry> latencyResult = new HashMap<>();

	private int fakeClients;

	public TreeScalabilityTest(TestScenario testScenario, int fakeClients) {
		super(testScenario);
		this.fakeClients = fakeClients;
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		String videoPath = KurentoClientTest.getPathTestFiles() + "/video/15sec/rgbHD.y4m";
		TestScenario test = new TestScenario();
		test.addBrowser(BrowserConfig.BROWSER + 0, new BrowserClient.Builder().client(Client.WEBRTC)
				.browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL).video(videoPath).build());
		test.addBrowser(BrowserConfig.BROWSER + 1, new BrowserClient.Builder().client(Client.WEBRTC)
				.browserType(BrowserType.CHROME).scope(BrowserScope.LOCAL).build());

		Collection<Object[]> out = new ArrayList<>();
		for (String s : fakeClientsArray) {
			out.add(new Object[] { test, Integer.parseInt(s) });
		}

		return out;
	}

	@Ignore
	@Test
	public void testTreeScalability() throws Exception {
		KurentoTreeClient kurentoTreeClientSink = null;

		try {
			// Creating tree
			String treeId = "myTree";
			kurentoTreeClient.createTree(treeId);
			kurentoTreeClientSink = new KurentoTreeClient(System.getProperty(KTS_WS_URI_PROP, KTS_WS_URI_DEFAULT));

			// Starting tree source
			getBrowser(0).setTreeSource(kurentoTreeClient, treeId, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.SEND_ONLY);

			// Starting tree sink
			getBrowser(1).subscribeEvents("playing");
			String sinkId = getBrowser(1).addTreeSink(kurentoTreeClientSink, treeId, WebRtcChannel.AUDIO_AND_VIDEO,
					WebRtcMode.RCV_ONLY);

			// Fake clients
			addFakeClients(fakeClients, kurentoTreeClient, treeId, sinkId);

			// Latency assessment
			final LatencyController cs = new LatencyController();
			cs.checkRemoteLatency(playTime, TimeUnit.SECONDS, getBrowser(0), getBrowser(1));
			cs.drawChart(getDefaultOutputFile("-fakeClients" + fakeClients + ".png"), WIDTH, HEIGHT);
			cs.writeCsv(getDefaultOutputFile("-fakeClients" + fakeClients + ".csv"));
			cs.logLatencyErrorrs();

			// Latency average
			long avgLatency = 0;
			Map<Long, LatencyRegistry> latencyMap = cs.getLatencyMap();
			for (LatencyRegistry lr : latencyMap.values()) {
				avgLatency += lr.getLatency();
			}

			int latencyMapSize = latencyMap.size();
			if (latencyMapSize > 0) {
				avgLatency /= latencyMap.size();
			}
			latencyResult.put((long) fakeClients, new LatencyRegistry(avgLatency));

		} finally {
			// Close tree client
			if (kurentoTreeClientSink != null) {
				kurentoTreeClientSink.close();
			}

			// Write csv
			PrintWriter pw = new PrintWriter(new FileWriter(getDefaultOutputFile("-latency.csv")));
			for (long time : latencyResult.keySet()) {
				pw.println(time + "," + latencyResult.get(time).getLatency());
			}
			pw.close();

			// Draw chart
			ChartWriter chartWriter = new ChartWriter(latencyResult, "Latency avg",
					"Latency of fake clients: " + Arrays.toString(fakeClientsArray), "Number of client(s)",
					"Latency (ms)");
			chartWriter.drawChart(getDefaultOutputFile("-latency-evolution.png"), WIDTH, HEIGHT);
		}
	}

}