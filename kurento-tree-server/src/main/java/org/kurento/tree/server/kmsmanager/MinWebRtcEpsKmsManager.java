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
import java.util.stream.Collectors;

import org.kurento.client.KurentoClient;
import org.kurento.client.Properties;
import org.kurento.commons.PropertiesManager;
import org.kurento.tree.server.kms.Kms;
import org.kurento.tree.server.kms.Pipeline;
import org.kurento.tree.server.kms.loadmanager.MaxWebRtcLoadManager;
import org.kurento.tree.server.kms.real.RealKms;
import org.kurento.tree.server.treemanager.KmsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinWebRtcEpsKmsManager extends KmsManager {

  private static final Logger log = LoggerFactory.getLogger(MinWebRtcEpsKmsManager.class);

  public static final int KMS_MAX_WEBRTC = PropertiesManager.getProperty("kms.maxWebrtc", 50);

  private static final boolean REAL_KMS = PropertiesManager.getProperty("kms.real", true);

  private static final int KMS_MIN_FREE_SPACE = PropertiesManager.getProperty("kms.minFreeSpace",
      3);

  private List<Kms> kmss = new ArrayList<>();

  private KmsListener kmsListener;

  public MinWebRtcEpsKmsManager() {

    log.info("Requesting new Kms because the app is starting");

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

    log.info("Obtained new Kms: " + kms.getLabel());

    if (kmsListener != null) {
      kmsListener.kmsAdded(kms);
    }
  }

  @Override
  public List<Kms> getKmss() {
    checkLoadAndUpdateKmss();
    return kmss;
  }

  private int calculateLoadPoints(Kms kms) {
    int count = 0;
    for (Pipeline pipeline : kms.getPipelines()) {
      count += pipeline.getWebRtcs().size();
      count += pipeline.getPlumbers().size();
    }
    return count;
  }

  private synchronized void checkLoadAndUpdateKmss() {

    List<KmsLoad> kmsLoads = kmss.stream().map(kms -> new KmsLoad(kms, calculateLoadPoints(kms)))
        .collect(Collectors.toList());

    Collections.sort(kmsLoads);

    Kms selectedKms = null;
    int selectedKmsIndex = -1;

    for (int i = kmsLoads.size() - 1; i >= 0; i--) {

      KmsLoad kmsLoad = kmsLoads.get(i);
      double load = kmsLoad.getLoad();
      Kms kms = kmsLoad.getKms();

      log.info("Kms " + kms.getLabel() + " has " + load + " load points used");

      if (KMS_MAX_WEBRTC - kmsLoad.getLoad() >= KMS_MIN_FREE_SPACE) {
        log.info("Kms " + kms.getLabel() + " has enough space");
        selectedKms = kmsLoad.getKms();
        selectedKmsIndex = i;
        break;
      }
    }

    if (selectedKms == null) {

      log.info("Requesting new Kms because there isn't a KMS with enough space available");
      addKms();

    } else {

      log.info("Kms " + selectedKms.getLabel() + "(" + selectedKmsIndex
          + ") is the most loaded KMS with enough space to create a webRtc");

      for (int i = 0; i < selectedKmsIndex; i++) {
        KmsLoad kmsLoad = kmsLoads.get(i);
        Kms kms = kmsLoad.getKms();
        if (kmsLoad.getLoad() == 0) {
          log.info("Removing Kms " + kms.getLabel() + " because its load is 0");
          removeKms(kms);
        }
      }
    }
  }

  private void removeKms(Kms kms) {
    log.info("Removing Kms {}", kms.getLabel());
    kmss.remove(kms);
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
