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
public abstract class KurentoTreeTestBase extends BrowserTest<TreeTestPage> {

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

    kurentoTreeClient = new KurentoTreeClient(
        System.getProperty(KTS_WS_URI_PROP, KTS_WS_URI_DEFAULT), new SslContextFactory(true));
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
