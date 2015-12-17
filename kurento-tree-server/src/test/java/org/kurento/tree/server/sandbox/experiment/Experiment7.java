package org.kurento.tree.server.sandbox.experiment;

import org.kurento.tree.server.kms.loadmanager.MaxWebRtcLoadManager;
import org.kurento.tree.server.kmsmanager.FakeElasticKmsManager;
import org.kurento.tree.server.kmsmanager.KmsManager;
import org.kurento.tree.server.sandbox.experiment.framework.Experiment;
import org.kurento.tree.server.sandbox.experiment.framework.TreeManagerCreator;
import org.kurento.tree.server.sandbox.experiment.usage.CyclicAddRemoveSinksUsage;
import org.kurento.tree.server.treemanager.LessLoadedOnlySourceTM;
import org.kurento.tree.server.treemanager.TreeManager;

public class Experiment7 extends Experiment {

  public void configureExperiment() {

    setKmsManager(new FakeElasticKmsManager(0.3, 2, 10, new MaxWebRtcLoadManager(12), true));

    addUsageSimulation(new CyclicAddRemoveSinksUsage(3, 5, 2, -1, 0));

    addTreeManagerCreator(new TreeManagerCreator() {
      @Override
      public TreeManager createTreeManager(KmsManager kmsManager) {
        return new LessLoadedOnlySourceTM(kmsManager, 8);
      }
    });
  }

  public static void main(String[] args) {
    new Experiment7().run();
  }
}
