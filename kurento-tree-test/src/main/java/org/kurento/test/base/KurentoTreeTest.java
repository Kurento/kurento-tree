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
import org.kurento.test.config.TestScenario;
import org.kurento.tree.client.KurentoTreeClient;

/**
 * Base for kurento-tree tests.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class KurentoTreeTest extends BrowserKurentoClientTest {

	public final static String KTS_WS_URI_PROP = "kts.ws.uri";
	public final static String KTS_WS_URI_DEFAULT = "ws://localhost:8890/kurento-tree";

	protected static KurentoTreeClient kurentoTreeClient;

	public KurentoTreeTest() {
		super();
	}

	public KurentoTreeTest(TestScenario testScenario) {
		super(testScenario);
		this.setClient(new KurentoTreeBrowser());
	}

	@Before
	public void setupKurentoClient() throws IOException {
		kurentoTreeClient = new KurentoTreeClient(System.getProperty(KTS_WS_URI_PROP, KTS_WS_URI_DEFAULT));
	}

	@After
	public void teardownKurentoClient() throws Exception {
		if (kurentoTreeClient != null) {
			kurentoTreeClient.close();
		}
	}

	@Override
	public KurentoTreeBrowser getBrowser() {
		return (KurentoTreeBrowser) super.getBrowser();
	}

	@Override
	public KurentoTreeBrowser getBrowser(int index) {
		return (KurentoTreeBrowser) super.getBrowser(index);
	}

}
