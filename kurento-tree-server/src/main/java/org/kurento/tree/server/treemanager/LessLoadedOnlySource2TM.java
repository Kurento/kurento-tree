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
 * <li>It is possible to connect more than 2 KMSs with plumbers</li>
 * </ul>
 *
 * @author micael.gallego@gmail.com
 */
public class LessLoadedOnlySource2TM extends AbstractNTreeTM implements KmsListener {

  private static final Logger log = LoggerFactory.getLogger(LessLoadedOnlySource2TM.class);

  public class LessLoadedTreeInfo extends TreeInfo {

    // Number of webrtcs reserved in each KMS when the source is connected
    // to it. This allows create new pipelines in another machines to be
    // connected to source pipeline.
    private static final int NUM_WEBRTC_FOR_TREE = 2;

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

      freeWebRtc(sourceKms, 1 + NUM_WEBRTC_FOR_TREE);

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

      log.info("setTreeSource treeId={}", treeId);

      if (source != null) {
        removeTreeSource();
      }

      if (sourcePipeline == null) {

        for (KmsLoad kmsLoad : kmsManager.getKmssSortedByLoad()) {
          sourceKms = kmsLoad.getKms();
          if (reserveWebRtc(sourceKms, 1 + NUM_WEBRTC_FOR_TREE)) {
            break;
          }
        }

        sourcePipeline = sourceKms.createPipeline();
        sourcePipeline.setLabel(treeId);

        ownPipelineByKms.put(sourceKms, sourcePipeline);
      }

      source = sourcePipeline.createWebRtc(new TreeElementSession(session, treeId, null));
      source.setLabel(treeId + "_source");

      for (int i = 0; i < NUM_WEBRTC_FOR_TREE; i++) {
        Plumber sourcePipelinePlumber = sourcePipeline.createPlumber();
        sourcePipelinePlumber.setLabel(treeId + "_plumber" + i);

        source.connect(sourcePipelinePlumber);
        this.sourcePlumbers.add(sourcePipelinePlumber);
      }

      String sdpAnswer = source.processSdpOffer(offerSdp);

      source.gatherCandidates();

      System.out.println("Holes: " + remainingHoles);

      return sdpAnswer;
    }

    @Override
    public void removeTreeSource() {
      if (source != null) {

        if (source.getSinks().size() > NUM_WEBRTC_FOR_TREE) {
          throw new TreeException(
              "Removing or changing TreeSource with sinks is not currently supported");
        }

        freeWebRtc(sourceKms, 1 + NUM_WEBRTC_FOR_TREE);
        source.release();
        source = null;
      }
    }

    @Override
    public TreeEndpoint addTreeSink(Session session, String sdpOffer) {

      log.info("addTreeSink treeId={}", treeId);

      List<KmsLoad> kmssSortedByLoad = kmsManager.getKmssSortedByLoad();

      Kms selectedKms = null;
      Pipeline pipeline = null;

      if (reserveWebRtc(sourceKms, 1)) {

        selectedKms = sourceKms;
        pipeline = sourcePipeline;

      } else {

        Plumber freeSourcePlumber = null;
        for (Plumber p : this.sourcePlumbers) {
          if (p.getLinkedTo() == null) {
            freeSourcePlumber = p;
            break;
          }
        }

        selectedKms = selectKmsForSink(kmssSortedByLoad);

        pipeline = ownPipelineByKms.get(selectedKms);

        if (pipeline == null) {

          if (freeSourcePlumber == null) {

            if (!allowMoreThan2KmsPerTree) {
              throw new TreeException("Source KMS doesn't allow new webRtc and hasn't free "
                  + "plumbers to connect to another KMS");
            } else {
              freeSourcePlumber = selectKmsForSinkWithFreePlumber(kmssSortedByLoad);

              if (freeSourcePlumber == null) {
                throw new TreeException(
                    "There are no plumbers to connect a new " + "pipeline for tree. This is a bug");
              }
            }
          }

          pipeline = selectedKms.createPipeline();
          pipeline.setLabel(treeId);

          ownPipelineByKms.put(selectedKms, pipeline);

          leafPipelines.add(pipeline);

          Plumber sinkPipelinePlumber = pipeline.createPlumber();
          sinkPipelinePlumber.setLabel(treeId + "_sinkPlumber_" + pipeline.getKms().getLabel());
          freeSourcePlumber.link(sinkPipelinePlumber);

          for (int i = 0; i < NUM_WEBRTC_FOR_TREE; i++) {
            Plumber leafOutputPlumber = pipeline.createPlumber();
            leafOutputPlumber
                .setLabel(treeId + "_plumber" + i + "_" + pipeline.getKms().getLabel());

            sinkPipelinePlumber.connect(leafOutputPlumber);
            this.leafPlumbers.add(sinkPipelinePlumber);
          }

        }
      }

      String id = UUID.randomUUID().toString();
      WebRtc webRtc = pipeline.createWebRtc(new TreeElementSession(session, treeId, id));
      webRtc.setLabel(treeId + "_sink_" + id.substring(0, id.indexOf('-')));

      if (pipeline != sourcePipeline) {
        pipeline.getPlumbers().get(0).connect(webRtc);
      } else {
        source.connect(webRtc);
      }

      String sdpAnswer = webRtc.processSdpOffer(sdpOffer);
      webRtc.gatherCandidates();

      webRtcsById.put(id, webRtc);

      System.out.println("Holes: " + remainingHoles);

      return new TreeEndpoint(sdpAnswer, id);
    }

    private Plumber selectKmsForSinkWithFreePlumber(List<KmsLoad> kmssSortedByLoad) {

      for (KmsLoad kmsLoad : kmssSortedByLoad) {

        Kms kms = kmsLoad.getKms();

        Pipeline pipeline = ownPipelineByKms.get(kms);
        if (pipeline != null) {
          for (Plumber p : pipeline.getPlumbers()) {
            if (p.getLinkedTo() == null) {
              return p;
            }
          }
        }
      }

      throw new TreeException("There are no pipeline with free plumber");
    }

    private Kms selectKmsForSink(List<KmsLoad> kmssSortedByLoad) {

      Kms selectedKms = null;

      for (KmsLoad kmsLoad : kmssSortedByLoad) {

        Kms kms = kmsLoad.getKms();

        int numWebRtcs = 1;

        boolean pipelineTreeInKms = ownPipelineByKms.get(kms) != null;
        if (!pipelineTreeInKms) {
          numWebRtcs += 1 + NUM_WEBRTC_FOR_TREE;
        }

        if (reserveWebRtc(kms, numWebRtcs)) {
          selectedKms = kmsLoad.getKms();
          break;
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

      log.info("removeTreeSink treeId={}", treeId);

      WebRtc webRtc = webRtcsById.get(sinkId);

      Element elem = webRtc.getSource();

      freeWebRtc(webRtc.getPipeline().getKms(), 1);

      webRtc.release();

      if (elem instanceof Plumber) {
        removePlumberIfNotConnected((Plumber) elem);
      }

      System.out.println("Holes: " + remainingHoles);
    }

    private void removePlumberIfNotConnected(Plumber plumber) {

      if (plumber.getSinks().size() == NUM_WEBRTC_FOR_TREE) {

        boolean remove = true;
        for (Element e : plumber.getSinks()) {
          if (((Plumber) e).getLinkedTo() != null) {
            remove = false;
            break;
          }
        }

        if (remove) {

          Pipeline pipeline = plumber.getPipeline();
          ownPipelineByKms.remove(pipeline.getKms());

          freeWebRtc(pipeline.getKms(), 1 + NUM_WEBRTC_FOR_TREE);

          pipeline.release();

          Element elem = plumber.getLinkedTo().getSource();
          if (elem instanceof Plumber) {
            removePlumberIfNotConnected((Plumber) elem);
          }
        }
      }
    }

    @Override
    public void addSinkIceCandidate(String sinkId, IceCandidate iceCandidate) {
      WebRtc webRtc = webRtcsById.get(sinkId);
      if (webRtc != null) {
        webRtc.addIceCandidate(iceCandidate);
      } else {
        log.warn("Reciving iceCandidate, but sink {} has no WebRtc", sinkId);
      }
    }

    @Override
    public void addTreeIceCandidate(IceCandidate iceCandidate) {
      source.addIceCandidate(iceCandidate);
    }
  }

  private KmsManager kmsManager;
  private int maxWebRtcsPerKMS;
  private ConcurrentMap<Kms, AtomicInteger> remainingHoles = new ConcurrentHashMap<>();
  private boolean allowMoreThan2KmsPerTree = true;

  public LessLoadedOnlySource2TM(KmsManager kmsManager, int maxWebRtcsPerKMS) {
    this.kmsManager = kmsManager;
    this.maxWebRtcsPerKMS = maxWebRtcsPerKMS;
  }

  public LessLoadedOnlySource2TM(KmsManager kmsManager) {
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

  private boolean reserveWebRtc(Kms kms, int numWebRtc) {

    AtomicInteger space = remainingHoles.computeIfAbsent(kms,
        k -> new AtomicInteger(maxWebRtcsPerKMS));

    checkLoad(kms, space);

    int holes = space.addAndGet(-numWebRtc);
    if (holes >= 0) {
      log.debug("Reserve {} #webRtc {} => {}", kms, numWebRtc, holes);
      return true;
    } else {
      space.addAndGet(numWebRtc);
      return false;
    }
  }

  private void checkLoad(Kms kms, AtomicInteger space) {
    int count = countWebRtcsAndPlumbers(kms);
    if (maxWebRtcsPerKMS - count != space.get()) {
      throw new TreeException("Incongruent count in kms " + kms.getLabel() + ". There are "
          + space.get() + " holes but should be " + (maxWebRtcsPerKMS - count));
    }
  }

  private int countWebRtcsAndPlumbers(Kms kms) {
    int count = 0;
    for (Pipeline pipeline : kms.getPipelines()) {
      count += pipeline.getWebRtcs().size();
      count += pipeline.getPlumbers().size();
    }
    return count;
  }

  public void freeWebRtc(Kms kms, int numWebRtc) {
    AtomicInteger space = remainingHoles.get(kms);
    checkLoad(kms, space);
    int holes = space.addAndGet(numWebRtc);
    log.debug("Free {} #webRtc {} => {}", kms, numWebRtc, holes);
  }

  @Override
  public void kmsRemoved(Kms kms) {
    this.remainingHoles.remove(kms);
  }

  @Override
  public void kmsAdded(Kms kms) {

  }

}
