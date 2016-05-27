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

public class Plumber extends Element {

  private Plumber linkedTo;

  protected Plumber(Pipeline pipeline) {
    super(pipeline);
  }

  public void link(Plumber plumber) {
    if (plumber.getPipeline().getKms() == this.getPipeline().getKms()) {
      throw new RuntimeException("Two plumbers of the same Kms can not be linked");
    }

    if (this.linkedTo != null || plumber.linkedTo != null) {
      throw new RuntimeException("A plumber only can be connected once");
    }

    this.linkedTo = plumber;
    plumber.linkedTo = this;
  }

  public Plumber getLinkedTo() {
    return linkedTo;
  }

  @Override
  public void release() {
    this.releaseLink();
    super.release();
  }

  private void releaseLink() {
    if (linkedTo != null) {
      linkedTo.linkedTo = null;
    }
  }
}
