package org.kurento.tree.server.sandbox.experiment;

import org.kurento.tree.server.kmsmanager.KmsManager;
import org.kurento.tree.server.kmsmanager.ReserveKmsManager;
import org.kurento.tree.server.sandbox.experiment.framework.Experiment;
import org.kurento.tree.server.sandbox.experiment.framework.TreeManagerCreator;
import org.kurento.tree.server.sandbox.experiment.usage.CyclicAddRemoveSinksUsage;
import org.kurento.tree.server.treemanager.LessLoadedOnlySource2TM;
import org.kurento.tree.server.treemanager.TreeManager;

public class Experiment9 extends Experiment {

	public void configureExperiment() {

		System.setProperty("kms.maxWebrtc", "8");
		System.setProperty("kms.real", "false");
		System.setProperty("kms.avgLoadToNewKms", "0.9");

		ReserveKmsManager reserveKmsManager = new ReserveKmsManager();

		setKmsManager(reserveKmsManager);

		addUsageSimulation(new CyclicAddRemoveSinksUsage(4, 10, 2, -1, 3));

		addTreeManagerCreator(new TreeManagerCreator() {
			@Override
			public TreeManager createTreeManager(KmsManager kmsManager) {
				LessLoadedOnlySource2TM treeManager = new LessLoadedOnlySource2TM(
						kmsManager);
				reserveKmsManager.setKmsListener(treeManager);
				return treeManager;
			}
		});
	}

	public static void main(String[] args) {
		new Experiment9().run();
	}
}
