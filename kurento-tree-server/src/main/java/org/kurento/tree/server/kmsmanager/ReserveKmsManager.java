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
import java.util.Iterator;
import java.util.List;

import org.kurento.client.KurentoClient;
import org.kurento.client.Properties;
import org.kurento.commons.PropertiesManager;
import org.kurento.tree.server.kms.Kms;
import org.kurento.tree.server.kms.loadmanager.MaxWebRtcLoadManager;
import org.kurento.tree.server.kms.real.RealKms;
import org.kurento.tree.server.treemanager.KmsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReserveKmsManager extends KmsManager {

  private static final Logger log = LoggerFactory.getLogger(ReserveKmsManager.class);

  public static final int KMS_MAX_WEBRTC = PropertiesManager.getProperty("kms.maxWebrtc", 50);

  private static final boolean REAL_KMS = PropertiesManager.getProperty("kms.real", true);

  private static final double AVG_LOAD_TO_NEW_KMS = PropertiesManager
      .getProperty("kms.avgLoadToNewKms", 0.8);

  private List<Kms> kmss = new ArrayList<>();

  private KmsListener kmsListener;

  public ReserveKmsManager() {
    addKms();
  }

  private void addKms() {
    Kms kms;
    if (REAL_KMS) {
      kms = new RealKms(KurentoClient.create(Properties.of("loadPoints", KMS_MAX_WEBRTC)));
    } else {
      kms = new Kms();
    }

    kms.setLoadManager(new MaxWebRtcLoadManager(KMS_MAX_WEBRTC));
    kms.setLabel("Kms" + kmss.size());
    kmss.add(kms);

    log.debug("Added new kms " + kms.getLabel());

    if (kmsListener != null) {
      kmsListener.kmsAdded(kms);
    }
  }

  @Override
  public List<Kms> getKmss() {
    checkLoadAndUpdateKmss();
    return kmss;
  }

  private void checkLoadAndUpdateKmss() {

    Iterator<Kms> it = kmss.iterator();

    double loadSum = 0;
    int numKms = 0;

    List<Kms> removedKmss = new ArrayList<Kms>();

    while (it.hasNext()) {
      Kms kms = it.next();

      double load = kms.getLoad();

      if (load == 0) {
        if (kmss.size() > 1) {
          it.remove();
          removedKmss.add(kms);
        }
      } else {
        loadSum += load;
        numKms++;
      }
    }

    while (loadSum / numKms >= AVG_LOAD_TO_NEW_KMS) {

      if (removedKmss.size() > 0) {
        Kms kms = removedKmss.remove(0);
        kmss.add(kms);
      } else {
        log.info("Creating new Kms for avg load {}", loadSum / numKms);
        addKms();
      }
      numKms++;
    }

    for (Kms kms : removedKmss) {
      removeKms(kms);
    }
  }

  private void removeKms(Kms kms) {
    log.info("Removing Kms {}", kms.getLabel());
    if (kms instanceof RealKms) {
      KurentoClient client = ((RealKms) kms).getKurentoClient();
      client.destroy();
    } else {
      kms.release();
    }

    if (kmsListener != null) {
      kmsListener.kmsRemoved(kms);
    }
  }

  public void setKmsListener(KmsListener kmsListener) {
    this.kmsListener = kmsListener;
  }

}
