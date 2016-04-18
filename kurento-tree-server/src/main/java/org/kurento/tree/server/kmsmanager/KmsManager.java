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
