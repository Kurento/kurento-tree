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