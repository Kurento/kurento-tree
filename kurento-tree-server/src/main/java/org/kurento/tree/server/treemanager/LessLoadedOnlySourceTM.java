/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
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
import org.kurento.tree.server.kmsmanager.KmsLoad;
import org.kurento.tree.server.kmsmanager.KmsManager;
import org.kurento.tree.server.kmsmanager.ReserveKmsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This TreeManager has the following characteristics:
 * <ul>
 * <li>It allows N trees</li>
 * <li>Creates WebRtcEndpoint for sinks (viewers) in any node (including KMSs with sources)</li>
 * <li>Create source webrtc in less loaded node, but webrtc for sinks in the same kms than source
 * while space available</li>
 * <li>It considers new KMSs after start.</li>
 * </ul>
 *
 * @author micael.gallego@gmail.com
 */
public class LessLoadedOnlySourceTM extends AbstractNTreeTM {

  private static final Logger log = LoggerFactory.getLogger(LessLoadedOnlySourceTM.class);

  public class LessLoadedTreeInfo extends TreeInfo {

    // Number of webrtcs reserved in each KMS when the source is connected
    // to it. This allows create new pipelines in another machines to be
    // connected to source pipeline.
    private static final int NUM_WEBRTC_FOR_TREE = 1;

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
        throw new KurentoException("LessLoadedNElasticTM cannot be used without initial kmss");
      }
    }

    @Override
    public void release() {

      remainingHoles.get(sourceKms).addAndGet(1 + NUM_WEBRTC_FOR_TREE);

      for (Plumber p : sourcePlumbers) {
        if (p.getLinkedTo() != null) {
          throw new TreeException("Exception removing TreeSource. Some plumbers are connected");
        } else {
          p.release();
        }
      }

      source.release();
      for (WebRtc webRtc : sinks.values()) {
        webRtc.release();
      }
    }

    @Override
    public synchronized String setTreeSource(Session session, String offerSdp) {

      if (source != null) {
        removeTreeSource();
      }

      if (sourcePipeline == null) {

        for (KmsLoad kmsLoad : kmsManager.getKmssSortedByLoad()) {
          sourceKms = kmsLoad.getKms();
          AtomicInteger numHoles = remainingHoles.computeIfAbsent(sourceKms,
              kms -> new AtomicInteger(maxWebRtcsPerKMS));
          if (numHoles.addAndGet(-1 - NUM_WEBRTC_FOR_TREE) >= 0) {
            break;
          } else {
            numHoles.addAndGet(+1 + NUM_WEBRTC_FOR_TREE);
          }
        }

        sourcePipeline = sourceKms.createPipeline();
        ownPipelineByKms.put(sourceKms, sourcePipeline);
      }

      source = sourcePipeline.createWebRtc(new TreeElementSession(session, treeId, null));

      for (int i = 0; i < NUM_WEBRTC_FOR_TREE; i++) {
        Plumber sourcePipelinePlumber = sourcePipeline.createPlumber();
        source.connect(sourcePipelinePlumber);
        this.sourcePlumbers.add(sourcePipelinePlumber);
      }

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
      Pipeline pipeline = null;

      if (remainingHoles.get(sourceKms).addAndGet(-1) >= 0) {
        selectedKms = sourceKms;
        pipeline = sourcePipeline;

      } else {

        remainingHoles.get(sourceKms).addAndGet(1);

        Plumber freeSourcePlumber = null;
        for (Plumber p : this.sourcePlumbers) {
          if (p.getLinkedTo() == null) {
            freeSourcePlumber = p;
            break;
          }
        }

        selectedKms = selectKmsForSink();

        pipeline = ownPipelineByKms.get(selectedKms);

        if (pipeline == null) {

          if (freeSourcePlumber == null) {
            throw new TreeException("Source KMS doesn't allow new webRtc and "
                + "hasn't free plumbers to connect to another KMS");
          }

          pipeline = selectedKms.createPipeline();

          ownPipelineByKms.put(selectedKms, pipeline);

          pipeline.setLabel(UUID.randomUUID().toString());
          leafPipelines.add(pipeline);

          Plumber sinkPipelinePlumber = pipeline.createPlumber();

          freeSourcePlumber.link(sinkPipelinePlumber);
          this.leafPlumbers.add(sinkPipelinePlumber);
        }
      }

      String id = UUID.randomUUID().toString();
      WebRtc webRtc = pipeline.createWebRtc(new TreeElementSession(session, treeId, id));

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
    }

    private Kms selectKmsForSink() {

      Kms selectedKms = null;

      for (KmsLoad kmsLoad : kmsManager.getKmssSortedByLoad()) {

        Kms kms = kmsLoad.getKms();

        AtomicInteger numHoles = remainingHoles.computeIfAbsent(kms,
            k -> new AtomicInteger(maxWebRtcsPerKMS));

        // If there is a pipeline for this tree in this KMS then
        // load is 1, else 1+RESERVED
        int load = 1 + (ownPipelineByKms.get(kms) != null ? 0 : NUM_WEBRTC_FOR_TREE);

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

    @Override
    public void removeTreeSink(String sinkId) {
      WebRtc webRtc = webRtcsById.get(sinkId);

      Element elem = webRtc.getSource();

      remainingHoles.get(webRtc.getPipeline().getKms()).addAndGet(1);

      webRtc.release();

      if (elem instanceof Plumber) {

        Plumber plumber = (Plumber) elem;

        if (plumber.getSinks().isEmpty()) {
          // Plumber remotePlumber = plumber.getLinkedTo();
          Pipeline pipeline = plumber.getPipeline();
          ownPipelineByKms.remove(pipeline.getKms());

          remainingHoles.get(pipeline.getKms()).addAndGet(NUM_WEBRTC_FOR_TREE);

          pipeline.release();

          // remotePlumber.release();
        }
      }
    }

    @Override
    public void addSinkIceCandidate(String sinkId, IceCandidate iceCandidate) {
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

  public LessLoadedOnlySourceTM(KmsManager kmsManager) {
    this.kmsManager = kmsManager;
    this.maxWebRtcsPerKMS = ReserveKmsManager.KMS_MAX_WEBRTC;
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
