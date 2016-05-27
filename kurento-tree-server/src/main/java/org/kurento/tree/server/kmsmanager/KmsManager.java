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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.kurento.tree.server.kms.Kms;

public abstract class KmsManager {

  public abstract List<Kms> getKmss();

  public Kms getLessLoadedKms() {
    ArrayList<KmsLoad> kmsLoads = new ArrayList<>();
    for (Kms kms : getKmss()) {
      kmsLoads.add(new KmsLoad(kms, kms.getLoad()));
    }
    return Collections.min(kmsLoads).getKms();
  }

  public List<KmsLoad> getKmssSortedByLoad() {
    ArrayList<KmsLoad> kmsLoads = new ArrayList<>();
    for (Kms kms : getKmss()) {
      kmsLoads.add(new KmsLoad(kms, kms.getLoad()));
    }
    Collections.sort(kmsLoads);
    return kmsLoads;
  }

}
