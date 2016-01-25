
package org.kurento.tree.test;

import java.io.IOException;

import org.junit.Test;
import org.kurento.test.base.KurentoTest;
import org.kurento.test.services.KmsService;
import org.kurento.test.services.Service;
import org.kurento.tree.client.KurentoTreeClient;
import org.kurento.tree.server.embed.KurentoTreeClientEmbed;

public class EmbedTest extends KurentoTest {

  @Service
  public KmsService kms = new KmsService();

  @Test
  public void test() throws IOException {

    KurentoTreeClient client = KurentoTreeClientEmbed.create(6666);

    client.createTree("XXXXX");

    client.close();
  }
}
