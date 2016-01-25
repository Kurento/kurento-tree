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

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.kurento.client.KurentoClient;
import org.kurento.test.services.FakeKmsService;
import org.kurento.test.services.KmsService;
import org.kurento.test.services.Service;
import org.kurento.test.services.WebServerService;
import org.kurento.tree.client.KurentoTreeClient;
import org.kurento.tree.server.app.KurentoTreeServerApp;

/**
 * Base for kurento-tree tests.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class KurentoTreeTestBase extends BrowserTest<TreeTestPage> {

  public final static String KTS_WS_URI_PROP = "kts.ws.uri";
  public final static String KTS_WS_URI_DEFAULT =
      "wss://localhost:" + WebServerService.getAppHttpPort() + "/kurento-tree";

  public static @Service(1) KmsService kms = new KmsService();
  public static @Service(2) KmsService fakeKms = new FakeKmsService();
  public static @Service(3) WebServerService webServer =
      new WebServerService(KurentoTreeServerApp.class);

  protected static KurentoTreeClient kurentoTreeClient;

  private KurentoClient fakeKurentoClient;

  @BeforeClass
  public static void setupTreeClient() throws IOException {

    SslContextFactory ctxFactory = new SslContextFactory(true);
    ctxFactory.setValidateCerts(true);
    kurentoTreeClient =
        new KurentoTreeClient(System.getProperty(KTS_WS_URI_PROP, KTS_WS_URI_DEFAULT), ctxFactory);
  }

  @AfterClass
  public static void teardownTreeClient() throws Exception {
    if (kurentoTreeClient != null) {
      kurentoTreeClient.close();
    }
  }

  protected synchronized KurentoClient fakeKurentoClient() {

    if (fakeKurentoClient == null) {

      if (fakeKms.isKmsStarted()) {
        fakeKurentoClient = fakeKms.getKurentoClient();
      } else {
        fakeKurentoClient = kms.getKurentoClient();
      }
    }

    return fakeKurentoClient;
  }

}
