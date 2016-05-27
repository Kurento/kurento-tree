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
package org.kurento.tree.server.kmsmanager;

import org.kurento.tree.server.kms.Kms;

public class KmsLoad implements Comparable<KmsLoad> {

  private Kms kms;
  private double load;

  public KmsLoad(Kms kms, double load) {
    this.kms = kms;
    this.load = load;
  }

  public Kms getKms() {
    return kms;
  }

  public double getLoad() {
    return load;
  }

  @Override
  public int compareTo(KmsLoad o) {
    return Double.compare(this.load, o.load);
  }

  @Override
  public String toString() {
    return "KmsLoad [kms=" + kms + ", load=" + load + "]";
  }

}