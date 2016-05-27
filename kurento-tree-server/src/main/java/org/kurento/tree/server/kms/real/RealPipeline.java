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
package org.kurento.tree.server.kms.real;

import org.kurento.client.MediaPipeline;
import org.kurento.tree.server.app.TreeElementSession;
import org.kurento.tree.server.kms.Pipeline;
import org.kurento.tree.server.kms.Plumber;
import org.kurento.tree.server.kms.WebRtc;

public class RealPipeline extends Pipeline {

  private MediaPipeline mediaPipeline;

  public RealPipeline(RealKms realKms) {
    super(realKms);
    mediaPipeline = realKms.getKurentoClient().createMediaPipeline();
  }

  public MediaPipeline getMediaPipeline() {
    return mediaPipeline;
  }

  @Override
  protected WebRtc newWebRtc(TreeElementSession session) {
    return new RealWebRtc(this, session);
  }

  @Override
  protected Plumber newPlumber() {
    return new RealPlumber(this);
  }

  @Override
  public void release() {
    super.release();
    mediaPipeline.release();
  }

  @Override
  public void setLabel(String label) {
    super.setLabel(label);
    mediaPipeline.setName(label);
  }

}
