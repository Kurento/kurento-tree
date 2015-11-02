package org.kurento.tree.server.treemanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.kurento.client.IceCandidate;
import org.kurento.commons.exception.KurentoException;
import org.kurento.jsonrpc.Session;
import org.kurento.tree.client.TreeEndpoint;
import org.kurento.tree.client.TreeException;
import org.kurento.tree.server.app.TreeElementSession;
import org.kurento.tree.server.kms.Element;
import org.kurento.tree.server.kms.Kms;
import org.kurento.tree.server.kms.Pipeline;
import org.kurento.tree.server.kms.Plumber;
import org.kurento.tree.server.kms.WebRtc;
import org.kurento.tree.server.kmsmanager.KmsManager;
import org.kurento.tree.server.kmsmanager.KmsManager.KmsLoad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This TreeManager has the following characteristics:
 * <ul>
 * <li>It allows N trees</li>
 * <li>Creates WebRtcEndpoint for sinks (viewers) in any node (including KMSs
 * with sources)</li>
 * <li>Create source webrtc in less loaded node, but webrtc for sinks in the
 * same kms than source while space available</li>
 * <li>It considers new KMSs after start.</li>
 * </ul>
 *
 * @author micael.gallego@gmail.com
 */
public class LessLoadedOnlySourceTM extends AbstractNTreeTM {

	private static final Logger log = LoggerFactory
			.getLogger(LessLoadedOnlySourceTM.class);

	public class LessLoadedTreeInfo extends TreeInfo {

		// Number of webrtcs reserved in each KMS when the source is connected
		// to it. This allows create new pipelines in another machines to be
		// connected to source pipeline. If value is 0, it is possible that new
		// sinks can not be connected when Kms with source pipeline is full of
		// capacity. If value is a big value, Kms will be under used if a low
		// number of sinks are connected. If value is 1, all new sinks can be
		// connected but their latency can be big if several KMSs are connected
		// from source to sink.
		private static final int RESERVED_WR_TO_CONNECT_KMS = 1;

		private String treeId;

		private Kms sourceKms;

		private Pipeline sourcePipeline;
		private WebRtc source;
		private List<Plumber> sourcePlumbers = new ArrayList<>();

		private List<Pipeline> leafPipelines = new ArrayList<>();
		private List<Plumber> leafPlumbers = new ArrayList<>();
		private Map<String, WebRtc> sinks = new ConcurrentHashMap<>();

		private Map<Kms, Pipeline> ownPipelineByKms = new ConcurrentHashMap<>();
		private Map<String, WebRtc> webRtcsById = new ConcurrentHashMap<>();

		public LessLoadedTreeInfo(String treeId) {

			this.treeId = treeId;

			if (kmsManager.getKmss().isEmpty()) {
				throw new KurentoException(
						"LessLoadedNElasticTM cannot be used without initial kmss");
			}
		}

		@Override
		public void release() {

			remainingHoles.get(sourceKms)
					.addAndGet(1 + RESERVED_WR_TO_CONNECT_KMS);

			source.release();
			for (WebRtc webRtc : sinks.values()) {
				webRtc.release();
			}
		}

		@Override
		public synchronized String setTreeSource(Session session,
				String offerSdp) {

			if (source != null) {
				removeTreeSource();
			}

			if (sourcePipeline == null) {

				for (KmsLoad kmsLoad : kmsManager.getKmssSortedByLoad()) {
					sourceKms = kmsLoad.getKms();
					AtomicInteger numHoles = remainingHoles.computeIfAbsent(
							sourceKms,
							kms -> new AtomicInteger(maxWebRtcsPerKMS));
					if (numHoles
							.addAndGet(-1 - RESERVED_WR_TO_CONNECT_KMS) >= 0) {
						break;
					} else {
						numHoles.addAndGet(+1 + RESERVED_WR_TO_CONNECT_KMS);
					}
				}

				sourcePipeline = sourceKms.createPipeline();
				ownPipelineByKms.put(sourceKms, sourcePipeline);
			}

			source = sourcePipeline.createWebRtc(
					new TreeElementSession(session, treeId, null));

			String sdpAnswer = source.processSdpOffer(offerSdp);

			source.gatherCandidates();

			return sdpAnswer;
		}

		@Override
		public void removeTreeSource() {
			source.release();
			source = null;
		}

		@Override
		public TreeEndpoint addTreeSink(Session session, String sdpOffer) {

			Kms selectedKms = null;

			if (remainingHoles.get(sourceKms).addAndGet(-1) >= 0) {
				selectedKms = sourceKms;
			} else {
				remainingHoles.get(sourceKms).addAndGet(1);

				selectedKms = selectKmsForSink(selectedKms);
			}

			Pipeline pipeline = getOrCreatePipeline(selectedKms);

			if (pipeline.getKms().allowMoreElements()) {

				String id = UUID.randomUUID().toString();
				WebRtc webRtc = pipeline.createWebRtc(
						new TreeElementSession(session, treeId, id));

				if (pipeline != sourcePipeline) {
					pipeline.getPlumbers().get(0).connect(webRtc);
				} else {
					source.connect(webRtc);
				}

				String sdpAnswer = webRtc.processSdpOffer(sdpOffer);
				webRtc.gatherCandidates();

				webRtcsById.put(id, webRtc);
				webRtc.setLabel("Sink " + id + ")");
				return new TreeEndpoint(sdpAnswer, id);

			} else {
				throw new TreeException("Max number of viewers reached");
			}
		}

		private Kms selectKmsForSink(Kms selectedKms) {

			for (KmsLoad kmsLoad : kmsManager.getKmssSortedByLoad()) {

				Kms kms = kmsLoad.getKms();

				AtomicInteger numHoles = remainingHoles.computeIfAbsent(kms,
						k -> new AtomicInteger(maxWebRtcsPerKMS));

				// If there is a pipeline for this tree in this KMS then
				// load is 1, else 1+RESERVED
				int load = 1 + (ownPipelineByKms.get(kms) != null ? 0
						: RESERVED_WR_TO_CONNECT_KMS);

				if (numHoles.addAndGet(-load) >= 0) {
					selectedKms = kmsLoad.getKms();
				} else {
					numHoles.addAndGet(load);
				}
			}

			if (selectedKms == null) {
				log.warn("remainingHoles: " + remainingHoles);
				throw new TreeException("No kms allows more WebRtcEndpoints");
			}
			return selectedKms;
		}

		private Pipeline getOrCreatePipeline(Kms kms) {

			Pipeline pipeline = ownPipelineByKms.get(kms);

			if (pipeline == null) {

				pipeline = kms.createPipeline();

				ownPipelineByKms.put(kms, pipeline);

				pipeline.setLabel(UUID.randomUUID().toString());
				leafPipelines.add(pipeline);
				Plumber[] plumbers = sourcePipeline.link(pipeline);
				source.connect(plumbers[0]);
				this.sourcePlumbers.add(plumbers[0]);
				this.leafPlumbers.add(plumbers[1]);
			}

			return pipeline;
		}

		@Override
		public void removeTreeSink(String sinkId) {
			WebRtc webRtc = webRtcsById.get(sinkId);

			Element elem = webRtc.getSource();

			remainingHoles.get(webRtc.getPipeline().getKms()).addAndGet(1);

			webRtc.release();

			if (elem instanceof Plumber) {

				Plumber plumber = (Plumber) elem;

				if (plumber.getSinks().isEmpty()) {
					Plumber remotePlumber = plumber.getLinkedTo();
					Pipeline pipeline = plumber.getPipeline();
					ownPipelineByKms.remove(pipeline.getKms());

					remainingHoles.get(pipeline.getKms())
							.addAndGet(RESERVED_WR_TO_CONNECT_KMS);

					pipeline.release();

					remotePlumber.release();
				}
			}
		}

		@Override
		public void addSinkIceCandidate(String sinkId,
				IceCandidate iceCandidate) {
			webRtcsById.get(sinkId).addIceCandidate(iceCandidate);
		}

		@Override
		public void addTreeIceCandidate(IceCandidate iceCandidate) {
			source.addIceCandidate(iceCandidate);
		}
	}

	private KmsManager kmsManager;
	private int maxWebRtcsPerKMS;
	private ConcurrentMap<Kms, AtomicInteger> remainingHoles = new ConcurrentHashMap<>();

	public LessLoadedOnlySourceTM(KmsManager kmsManager, int maxWebRtcsPerKMS) {
		this.kmsManager = kmsManager;
		this.maxWebRtcsPerKMS = maxWebRtcsPerKMS;
	}

	@Override
	public KmsManager getKmsManager() {
		return kmsManager;
	}

	@Override
	protected TreeInfo createTreeInfo(String treeId) {
		return new LessLoadedTreeInfo(treeId);
	}

}
