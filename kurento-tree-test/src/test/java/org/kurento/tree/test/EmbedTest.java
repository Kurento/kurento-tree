/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
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
