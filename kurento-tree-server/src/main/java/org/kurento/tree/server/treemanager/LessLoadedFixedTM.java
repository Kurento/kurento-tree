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

import org.kurento.client.IceCandidate;
import org.kurento.commons.exception.KurentoException;
import org.kurento.jsonrpc.Session;
import org.kurento.tree.client.TreeEndpoint;
import org.kurento.tree.client.TreeException;
import org.kurento.tree.server.app.TreeElementSession;
import org.kurento.tree.server.kms.Kms;
import org.kurento.tree.server.kms.Pipeline;
import org.kurento.tree.server.kms.Plumber;
import org.kurento.tree.server.kms.WebRtc;
import org.kurento.tree.server.kmsmanager.KmsLoad;
import org.kurento.tree.server.kmsmanager.KmsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This TreeManager has the following characteristics:
 * <ul>
 * <li>It allows N trees</li>
 * <li>Creates WebRtcEndpoint for sinks (viewers) only in non-source KMSs</li>
 * <li>Fills less loaded node.</li>
 * <li>It doesn't consider new kmss after start.</li>
 * </ul>
 *
 * @author micael.gallego@gmail.com
 */
public class LessLoadedFixedTM extends AbstractNTreeTM {

  private static final Logger log = LoggerFactory.getLogger(LessLoadedFixedTM.class);

  public class LessLoadedTreeInfo extends TreeInfo {

    private KmsManager kmsManager;

    private boolean oneKms = true;

    private String treeId;

    private Pipeline sourcePipeline;
    private List<Plumber> sourcePlumbers = new ArrayList<>();
    private WebRtc source;

    private List<Pipeline> leafPipelines = new ArrayList<>();
    private List<Plumber> leafPlumbers = new ArrayList<>();
    private Map<String, WebRtc> sinks = new ConcurrentHashMap<>();

    private Map<Kms, Pipeline> ownPipelineByKms = new ConcurrentHashMap<>();
    private Map<String, WebRtc> webRtcsById = new ConcurrentHashMap<>();

    private int numSinks = 0;

    public LessLoadedTreeInfo(String treeId, KmsManager kmsManager) {

      this.treeId = treeId;
      this.kmsManager = kmsManager;

      if (kmsManager.getKmss().isEmpty()) {
        throw new KurentoException(
            "LessLoadedNElasticTreeManager cannot be used without initial kmss");

      } else if (kmsManager.getKmss().size() == 1) {

        oneKms = true;

        sourcePipeline = kmsManager.getKmss().get(0).createPipeline();

      } else {

        oneKms = false;

        int numPipeline = 0;
        for (Kms kms : kmsManager.getKmss()) {
          Pipeline pipeline = kms.createPipeline();

          ownPipelineByKms.put(kms, pipeline);

          if (sourcePipeline == null) {
            sourcePipeline = pipeline;
          } else {
            pipeline.setLabel(Integer.toString(numPipeline));
            leafPipelines.add(pipeline);
            Plumber[] plumbers = sourcePipeline.link(pipeline);
            this.sourcePlumbers.add(plumbers[0]);
            this.leafPlumbers.add(plumbers[1]);
            numPipeline++;
          }
        }
      }
    }

    @Override
    public void release() {
      source.release();
      for (WebRtc webRtc : sinks.values()) {
        webRtc.release();
      }
    }

    @Override
    public String setTreeSource(Session session, String offerSdp) {

      if (source != null) {
        removeTreeSource();
      }
      source = sourcePipeline.createWebRtc(new TreeElementSession(session, treeId, null));

      if (!oneKms) {
        for (Plumber plumber : this.sourcePlumbers) {
          source.connect(plumber);
        }
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

      TreeEndpoint result = null;

      if (oneKms) {

        if (sourcePipeline.getKms().allowMoreElements()) {
          String id = UUID.randomUUID().toString();
          WebRtc webRtc = sourcePipeline.createWebRtc(new TreeElementSession(session, treeId, id));
          source.connect(webRtc);
          String sdpAnswer = webRtc.processSdpOffer(sdpOffer);
          webRtc.gatherCandidates();

          webRtcsById.put(id, webRtc);
          webRtc.setLabel("Sink " + id + ")");
          result = new TreeEndpoint(sdpAnswer, id);
        }

      } else {

        List<KmsLoad> kmss = kmsManager.getKmssSortedByLoad();

        Pipeline pipeline = ownPipelineByKms.get(kmss.get(0).getKms());

        if (pipeline == sourcePipeline) {
          pipeline = ownPipelineByKms.get(kmss.get(1).getKms());
        }

        if (pipeline.getKms().allowMoreElements()) {
          String id = UUID.randomUUID().toString();
          WebRtc webRtc = pipeline.createWebRtc(new TreeElementSession(session, treeId, id));
          pipeline.getPlumbers().get(0).connect(webRtc);
          String sdpAnswer = webRtc.processSdpOffer(sdpOffer);
          webRtc.gatherCandidates();

          webRtcsById.put(id, webRtc);
          webRtc.setLabel("Sink " + id + ")");
          result = new TreeEndpoint(sdpAnswer, id);
        }
      }

      if (result != null) {
        numSinks++;
        return result;
      } else {
        throw new TreeException("Max number of viewers reached");
      }
    }

    @Override
    public void removeTreeSink(String sinkId) {
      WebRtc webRtc = webRtcsById.get(sinkId);
      webRtc.release();
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

  public LessLoadedFixedTM(KmsManager kmsManager) {
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
