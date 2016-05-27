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

package org.kurento.tree.test.functional;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.test.base.KurentoTreeTestBase;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.TestScenario;
import org.kurento.tree.client.KurentoTreeClient;

/**
 * <strong>Description</strong>: WebRTC one to one with tree.<br/>
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
public class TreeOne2OneTest extends KurentoTreeTestBase {

  private static final int PLAYTIME = 10; // seconds

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromes(2);
  }

  @Test
  public void testTreeOne2One() throws Exception {
    KurentoTreeClient kurentoTreeClientSink = null;

    String treeId = "myTree";

    try {
      // Creating tree
      kurentoTreeClient.createTree(treeId);

      SslContextFactory ctxFactory = new SslContextFactory(true);
      ctxFactory.setValidateCerts(true);
      kurentoTreeClientSink = new KurentoTreeClient(
          System.getProperty(KTS_WS_URI_PROP, KTS_WS_URI_DEFAULT), ctxFactory);

      // Starting tree source
      getPage(0).setTreeSource(kurentoTreeClient, treeId, WebRtcChannel.AUDIO_AND_VIDEO,
          WebRtcMode.SEND_ONLY);

      // Starting tree sink
      getPage(1).subscribeEvents("playing");
      getPage(1).addTreeSink(kurentoTreeClientSink, treeId, WebRtcChannel.AUDIO_AND_VIDEO,
          WebRtcMode.RCV_ONLY);

      // Play the video
      Thread.sleep(TimeUnit.SECONDS.toMillis(PLAYTIME));

      // Assertions
      Assert.assertTrue("Not received media (timeout waiting playing event)",
          getPage(1).waitForEvent("playing"));

      Assert.assertTrue("The color of the video should be green (RGB #008700)",
          getPage(1).similarColor(CHROME_VIDEOTEST_COLOR));

    } finally {

      if (kurentoTreeClientSink != null) {
        kurentoTreeClientSink.close();
      }

      kurentoTreeClient.releaseTree(treeId);
    }
  }

}
