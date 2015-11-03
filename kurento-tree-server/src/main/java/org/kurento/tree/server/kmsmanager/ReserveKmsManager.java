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

public class ReserveKmsManager extends KmsManager {

	private static final int KMS_MAX_WEBRTC = PropertiesManager
			.getProperty("kms.maxWebrtc", 50);

	private static final boolean REAL_KMS = PropertiesManager
			.getProperty("kms.real", true);

	private static final double AVG_LOAD_TO_NEW_KMS = PropertiesManager
			.getProperty("kms.avgLoadToNewKms", 0.8);

	private List<Kms> kmss = new ArrayList<>();

	public ReserveKmsManager() {
		addKms();
	}

	private void addKms() {
		Kms kms;
		if (REAL_KMS) {
			kms = new RealKms(KurentoClient
					.create(Properties.of("loadPoints", KMS_MAX_WEBRTC)));
		} else {
			kms = new Kms();
		}

		kms.setLoadManager(new MaxWebRtcLoadManager(KMS_MAX_WEBRTC));
		kms.setLabel("Kms" + kmss.size());
		kmss.add(kms);
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

		while (it.hasNext()) {
			Kms kms = it.next();

			double load = kms.getLoad();

			if (load == 0) {
				if (kmss.size() > 1) {
					it.remove();
					if (kms instanceof RealKms) {
						KurentoClient client = ((RealKms) kms)
								.getKurentoClient();
						client.destroy();
					}
				}
			} else {
				loadSum += load;
				numKms++;
			}
		}

		double avgLoad = loadSum / numKms;

		if (avgLoad >= AVG_LOAD_TO_NEW_KMS) {
			addKms();
		}
	}

}
