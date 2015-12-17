package org.kurento.tree.server.kmsmanager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

  private int countWebRtcsAndPlumbers(Kms kms) {
    int count = 0;
    for (Pipeline pipeline : kms.getPipelines()) {
      count += pipeline.getWebRtcs().size();
      count += pipeline.getPlumbers().size();
    }
    return count;
  }

  private void checkLoadAndUpdateKmss() {

    Iterator<Kms> it = kmss.iterator();

    boolean minSpaceKmsFound = false;

    List<Kms> removedKmss = new ArrayList<Kms>();

    while (it.hasNext()) {
      Kms kms = it.next();

      int count = countWebRtcsAndPlumbers(kms);

      if (count > 0 && KMS_MAX_WEBRTC - count >= KMS_MIN_FREE_SPACE) {
        minSpaceKmsFound = true;
      }

      if (count == 0 && kmss.size() > 1) {
        it.remove();
        removedKmss.add(kms);
      }
    }

    if (!minSpaceKmsFound) {
      if (removedKmss.size() > 0) {
        Kms kms = removedKmss.remove(0);
        kmss.add(kms);
      } else {
        log.info("Creating new Kms");
        addKms();
      }
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
