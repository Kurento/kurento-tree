package org.kurento.tree.server.sandbox.experiment;

import org.kurento.tree.server.kms.loadmanager.MaxWebRtcLoadManager;
import org.kurento.tree.server.kmsmanager.FakeFixedNKmsManager;
import org.kurento.tree.server.kmsmanager.KmsManager;
import org.kurento.tree.server.sandbox.experiment.framework.Experiment;
import org.kurento.tree.server.sandbox.experiment.framework.TreeManagerCreator;
import org.kurento.tree.server.sandbox.experiment.usage.OneSourceAddRemoveSinksUsage;
import org.kurento.tree.server.treemanager.LessLoadedOneTreeFixedTM;
import org.kurento.tree.server.treemanager.TreeManager;

public class Experiment1 extends Experiment {

  public void configureExperiment() {

    setKmsManager(new FakeFixedNKmsManager(4, new MaxWebRtcLoadManager(5)));

    addUsageSimulation(new OneSourceAddRemoveSinksUsage());

    addTreeManagerCreator(new TreeManagerCreator() {
      @Override
      public TreeManager createTreeManager(KmsManager kmsManager) {
        return new LessLoadedOneTreeFixedTM(kmsManager);
      }
    });
  }

  public static void main(String[] args) {
    new Experiment1().run();
  }
}
