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
package org.kurento.tree.server.kms;

import java.util.ArrayList;
import java.util.List;

import org.kurento.tree.server.kms.loadmanager.LoadManager;
import org.kurento.tree.server.kms.loadmanager.MaxWebRtcLoadManager;

public class Kms extends KurentoObj {

  protected List<Pipeline> pipelines = new ArrayList<>();
  private LoadManager loadManager = new MaxWebRtcLoadManager(10000);

  public Kms() {

  }

  public Kms(String label) {
    super(label);
  }

  public Pipeline createPipeline() {
    Pipeline pipeline = newPipeline();
    pipelines.add(pipeline);
    return pipeline;
  }

  public List<Pipeline> getPipelines() {
    return pipelines;
  }

  public void setLoadManager(LoadManager loadManager) {
    this.loadManager = loadManager;
  }

  protected Pipeline newPipeline() {
    return new Pipeline(this);
  }

  public double getLoad() {
    return loadManager.calculateLoad(this);
  }

  public boolean allowMoreElements() {
    return loadManager.allowMoreElements(this);
  }

  void removePipeline(Pipeline pipeline) {
    this.pipelines.remove(pipeline);
  }

  @Override
  public String toString() {
    return getLabel();
  }

}
