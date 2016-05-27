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
package org.kurento.tree.server.kms.loadmanager;

import org.kurento.tree.server.kms.Kms;
import org.kurento.tree.server.kms.Pipeline;

public class MaxWebRtcLoadManager implements LoadManager {

  private int maxWebRtcPerKms;

  public MaxWebRtcLoadManager(int maxWebRtcPerKms) {
    this.maxWebRtcPerKms = maxWebRtcPerKms;
  }

  @Override
  public double calculateLoad(Kms kms) {
    int numWebRtcs = countWebRtcEndpoints(kms);
    if (numWebRtcs > maxWebRtcPerKms) {
      return 1;
    } else {
      return numWebRtcs / ((double) maxWebRtcPerKms);
    }
  }

  private int countWebRtcEndpoints(Kms kms) {
    int numWebRtcs = 0;
    for (Pipeline pipeline : kms.getPipelines()) {
      numWebRtcs += pipeline.getWebRtcs().size();
      numWebRtcs += pipeline.getPlumbers().size();
    }
    return numWebRtcs;
  }

  @Override
  public boolean allowMoreElements(Kms kms) {
    return countWebRtcEndpoints(kms) < maxWebRtcPerKms;
  }
}
