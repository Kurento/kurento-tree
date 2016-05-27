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
package org.kurento.tree.server.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;
import org.kurento.tree.server.kms.Element;
import org.kurento.tree.server.kms.Kms;
import org.kurento.tree.server.kms.Pipeline;
import org.kurento.tree.server.kms.WebRtc;

public class FacadeFakeKmsTest {

  @Test
  public void basicTreeTest() {

    Kms kms = new Kms();
    Pipeline pipeline = kms.createPipeline();
    WebRtc master = pipeline.createWebRtc(null);

    for (int i = 0; i < 3; i++) {
      WebRtc viewer = pipeline.createWebRtc(null);
      master.connect(viewer);
    }

    assertThat(master.getSinks().size(), is(3));

    for (Element sink : master.getSinks()) {
      assertThat(master, is(sink.getSource()));
    }

    for (Element sink : new ArrayList<>(master.getSinks())) {
      sink.disconnect();
      assertThat(sink.getSource(), is(nullValue()));
    }

    assertThat(master.getSinks(), is(Collections.<Element> emptyList()));

  }
}
