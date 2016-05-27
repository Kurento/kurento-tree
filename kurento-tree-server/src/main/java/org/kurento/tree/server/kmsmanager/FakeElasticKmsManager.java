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
import java.util.List;

import org.kurento.tree.server.kms.Kms;
import org.kurento.tree.server.kms.loadmanager.LoadManager;

public class FakeElasticKmsManager extends KmsManager {

  private double meanLoadToGrow;
  public List<Kms> kmss = new ArrayList<>();
  private LoadManager loadManager;
  private int maxKmss;
  private boolean ignoreFirstKmsInLoadMeasure;

  public FakeElasticKmsManager(double meanLoadToGrow, int minKmss, int maxKmss) {
    this(meanLoadToGrow, minKmss, maxKmss, null, true);
  }

  public FakeElasticKmsManager(double meanLoadToGrow, int minKmss, int maxKmss,
      LoadManager loadManager, boolean ignoreFirstKmsInLoadMeasure) {
    this.meanLoadToGrow = meanLoadToGrow;
    this.loadManager = loadManager;
    this.maxKmss = maxKmss;
    this.ignoreFirstKmsInLoadMeasure = ignoreFirstKmsInLoadMeasure;
    for (int i = 0; i < minKmss; i++) {
      kmss.add(newKms(i));
    }
  }

  private Kms newKms(int num) {
    Kms kms = new Kms("Kms " + num);
    if (loadManager != null) {
      kms.setLoadManager(loadManager);
    }
    return kms;
  }

  public List<Kms> getKmss() {
    checkLoadAndUpdateKmss();
    return kmss;
  }

  private void checkLoadAndUpdateKmss() {

    if (kmss.size() < maxKmss) {
      double totalLoad = 0;

      int numKms = 0;
      for (Kms kms : kmss) {
        if (!ignoreFirstKmsInLoadMeasure || numKms > 0) {
          totalLoad += kms.getLoad();
        }
        numKms++;
      }

      int kmssToMean = ignoreFirstKmsInLoadMeasure ? this.kmss.size() - 1 : this.kmss.size();

      double meanLoad = totalLoad / kmssToMean;
      System.out.println("Mean load: " + meanLoad);
      if (meanLoad > meanLoadToGrow) {
        this.kmss.add(newKms(kmss.size()));
      }
    }
  }
}
