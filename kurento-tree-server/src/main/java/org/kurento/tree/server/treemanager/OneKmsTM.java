package org.kurento.tree.server.treemanager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.kurento.client.IceCandidate;
import org.kurento.commons.exception.KurentoException;
import org.kurento.jsonrpc.Session;
import org.kurento.tree.client.TreeEndpoint;
import org.kurento.tree.client.TreeException;
import org.kurento.tree.server.app.TreeElementSession;
import org.kurento.tree.server.kms.Pipeline;
import org.kurento.tree.server.kms.WebRtc;
import org.kurento.tree.server.kmsmanager.KmsManager;

/**
 * This TreeManager has the following characteristics:
 * <ul>
 * <li>It uses only one KMS</li>
 * <li>It doesn't use any plumbers</li>
 * <li>It doesn't consider new KMSs after start.</li>
 * </ul>
 *
 * @author micael.gallego@gmail.com
 */
public class OneKmsTM extends AbstractNTreeTM {

	public class LessLoadedTreeInfo extends TreeInfo {

		private Pipeline pipeline;
		private String treeId;
		private WebRtc source;
		private Map<String, WebRtc> sinksById = new ConcurrentHashMap<>();

		public LessLoadedTreeInfo(String treeId, KmsManager kmsManager) {
			this.treeId = treeId;
			if (kmsManager.getKmss().isEmpty()) {
				throw new KurentoException(OneKmsTM.class.getName()
						+ " cannot be used without initial kmss");

			} else if (kmsManager.getKmss().size() == 1) {
				pipeline = kmsManager.getKmss().get(0).createPipeline();
			} else {
				throw new KurentoException(
						OneKmsTM.class.getName()
						+ " is designed to use only one KMS. Please use another TreeManager if you want to use several KMSs");
			}
		}

		@Override
		public void release() {
			if (source != null)
				source.release();
			for (WebRtc webRtc : sinksById.values()) {
				webRtc.release();
			}
		}

		@Override
		public String setTreeSource(Session session, String offerSdp) {
			if (source != null) {
				removeTreeSource();
			}
			source = pipeline.createWebRtc(new TreeElementSession(session,
					treeId, null));
			return source.processSdpOffer(offerSdp);
		}

		@Override
		public void removeTreeSource() {
			source.release();
			source = null;
		}

		@Override
		public TreeEndpoint addTreeSink(Session session, String sdpOffer) {

			if (pipeline.getKms().allowMoreElements()) {
				String id = UUID.randomUUID().toString();
				WebRtc webRtc = pipeline.createWebRtc(new TreeElementSession(
						session, treeId, id));
				source.connect(webRtc);
				String sdpAnswer = webRtc.processSdpOffer(sdpOffer);
				webRtc.gatherCandidates();
				sinksById.put(id, webRtc);
				webRtc.setLabel("Sink " + id + ")");
				return new TreeEndpoint(sdpAnswer, id);

			} else {
				throw new TreeException("Max number of viewers reached");
			}
		}

		@Override
		public void removeTreeSink(String sinkId) {
			WebRtc webRtc = sinksById.get(sinkId);
			webRtc.release();
		}

		@Override
		public void addSinkIceCandidate(String sinkId, IceCandidate iceCandidate) {
			sinksById.get(sinkId).addIceCandidate(iceCandidate);
		}

		@Override
		public void addTreeIceCandidate(IceCandidate iceCandidate) {
			source.addIceCandidate(iceCandidate);
		}
	}

	private KmsManager kmsManager;

	public OneKmsTM(KmsManager kmsManager) {
		this.kmsManager = kmsManager;
	}

	@Override
	public KmsManager getKmsManager() {
		return kmsManager;
	}

	@Override
	protected TreeInfo createTreeInfo(String treeId) {
		return new LessLoadedTreeInfo(treeId, kmsManager);
	}
}
