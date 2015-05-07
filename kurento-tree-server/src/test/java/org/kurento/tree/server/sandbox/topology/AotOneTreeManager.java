package org.kurento.tree.server.sandbox.topology;

import java.io.IOException;

import org.kurento.tree.server.debug.KmsTopologyGrapher;
import org.kurento.tree.server.kmsmanager.FakeFixedNKmsManager;
import org.kurento.tree.server.kmsmanager.KmsManager;
import org.kurento.tree.server.treemanager.LexicalFixedTM;

public class AotOneTreeManager {

	public static void main(String[] args) throws IOException {

		KmsManager kmsManager = new FakeFixedNKmsManager(4);
		LexicalFixedTM aot = new LexicalFixedTM(
				kmsManager);

		KmsTopologyGrapher.showTopologyGraphic(kmsManager);

		String treeId = aot.createTree();
		aot.setTreeSource(null, treeId, "XXX");

		KmsTopologyGrapher.showTopologyGraphic(kmsManager);

		aot.addTreeSink(null, treeId, "JJJ");
		aot.addTreeSink(null, treeId, "FFF");

		KmsTopologyGrapher.showTopologyGraphic(kmsManager);
	}
}
