package org.kurento.tree.server.sandbox.experiment;

import org.kurento.tree.server.kmsmanager.KmsManager;
import org.kurento.tree.server.kmsmanager.ReserveKmsManager;
import org.kurento.tree.server.sandbox.experiment.framework.Experiment;
import org.kurento.tree.server.sandbox.experiment.framework.TreeManagerCreator;
import org.kurento.tree.server.sandbox.experiment.usage.CyclicAddRemoveSinksUsage;
import org.kurento.tree.server.treemanager.LessLoadedOnlySourceTM;
import org.kurento.tree.server.treemanager.TreeManager;

public class Experiment8 extends Experiment {

  public void configureExperiment() {

    System.setProperty("kms.maxWebrtc", "8");
    System.setProperty("kms.real", "false");
    System.setProperty("kms.avgLoadToNewKms", "0.9");

    setKmsManager(new ReserveKmsManager());

    addUsageSimulation(new CyclicAddRemoveSinksUsage(3, 5, 2, -1, 3));

    addTreeManagerCreator(new TreeManagerCreator() {
      @Override
      public TreeManager createTreeManager(KmsManager kmsManager) {
        return new LessLoadedOnlySourceTM(kmsManager);
      }
    });
  }

  public static void main(String[] args) {
    new Experiment8().run();
  }
}
