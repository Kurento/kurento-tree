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
import java.util.concurrent.ConcurrentHashMap;

import org.kurento.client.IceCandidate;
import org.kurento.jsonrpc.Session;
import org.kurento.tree.client.TreeEndpoint;
import org.kurento.tree.client.TreeException;
import org.kurento.tree.server.app.TreeElementSession;
import org.kurento.tree.server.kms.Kms;
import org.kurento.tree.server.kms.Pipeline;
import org.kurento.tree.server.kms.Plumber;
import org.kurento.tree.server.kms.WebRtc;
import org.kurento.tree.server.kmsmanager.KmsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This TreeManager has the following characteristics:
 * <ul>
 * <li>It allows only one tree</li>
 * <li>Creates WebRtcEndpoint for sinks (viewers) only in non-root kmss.</li>
 * <li>Fills KMSs lexicographically until reach the configured maxViewersPerPipeline.</li>
 * <li>It doesn't consider new kmss after start.</li>
 * </ul>
 *
 * @author micael.gallego@gmail.com
 */
public class LexicalFixedTM extends AbstractOneTreeTM {

  private static final Logger log = LoggerFactory.getLogger(LexicalFixedTM.class);

  private KmsManager kmsManager;
  private int maxViewersPerPipeline = 2;

  private boolean oneKms = true;

  private Pipeline rootPipeline;
  private List<Plumber> rootPlumbers = new ArrayList<>();
  private WebRtc sourceWebRtc;

  private List<Pipeline> leafPipelines = new ArrayList<>();
  private List<Plumber> leafPlumbers = new ArrayList<>();
  private Map<String, WebRtc> sinks = new ConcurrentHashMap<>();

  public LexicalFixedTM(KmsManager kmsManager) {
    this(kmsManager, 5);
  }

  public LexicalFixedTM(KmsManager kmsManager, int maxViewersPerPipeline) {

    this.maxViewersPerPipeline = maxViewersPerPipeline;
    this.kmsManager = kmsManager;

    if (kmsManager.getKmss().isEmpty()) {
      log.error("LexicalFixedNoRootTM cannot be used without initial kmss");

    } else if (kmsManager.getKmss().size() == 1) {

      oneKms = true;

      rootPipeline = kmsManager.getKmss().get(0).createPipeline();

    } else {

      oneKms = false;

      for (Kms kms : kmsManager.getKmss()) {
        Pipeline pipeline = kms.createPipeline();
        if (rootPipeline == null) {
          rootPipeline = pipeline;
        } else {
          leafPipelines.add(pipeline);
          Plumber[] plumbers = rootPipeline.link(pipeline);
          this.rootPlumbers.add(plumbers[0]);
          this.leafPlumbers.add(plumbers[1]);
        }
      }
    }
  }

  @Override
  public KmsManager getKmsManager() {
    return kmsManager;
  }

  @Override
  public synchronized void releaseTree(String treeId) throws TreeException {

    checkTreeId(treeId);

    createdTree = false;
    sourceWebRtc.release();
    sourceWebRtc = null;

    for (WebRtc webRtc : sinks.values()) {
      webRtc.release();
    }
  }

  @Override
  public synchronized String setTreeSource(Session session, String treeId, String offerSdp)
      throws TreeException {

    checkTreeId(treeId);

    if (sourceWebRtc != null) {
      removeTreeSource(treeId);
    }

    sourceWebRtc = rootPipeline.createWebRtc(new TreeElementSession(session, treeId, null));

    if (!oneKms) {
      for (Plumber plumber : this.rootPlumbers) {
        sourceWebRtc.connect(plumber);
      }
    }
    String sdpAnswer = sourceWebRtc.processSdpOffer(offerSdp);
    sourceWebRtc.gatherCandidates();
    return sdpAnswer;
  }

  @Override
  public synchronized void removeTreeSource(String treeId) throws TreeException {

    checkTreeId(treeId);

    sourceWebRtc.release();
    sourceWebRtc = null;
  }

  @Override
  public synchronized TreeEndpoint addTreeSink(Session session, String treeId, String sdpOffer)
      throws TreeException {

    checkTreeId(treeId);

    TreeEndpoint result = null;

    if (oneKms) {
      if (rootPipeline.getWebRtcs().size() < maxViewersPerPipeline) {
        String id = "r_" + (rootPipeline.getWebRtcs().size() - 1);
        WebRtc webRtc = rootPipeline.createWebRtc(new TreeElementSession(session, treeId, id));
        sourceWebRtc.connect(webRtc);
        String sdpAnswer = webRtc.processSdpOffer(sdpOffer);
        webRtc.gatherCandidates();
        result = new TreeEndpoint(sdpAnswer, id);
      } else {
        throw new TreeException("Max number of viewers reached");
      }
    } else {
      int numPipeline = 0;
      for (Pipeline pipeline : this.leafPipelines) {
        if (pipeline.getWebRtcs().size() < maxViewersPerPipeline) {
          String id = numPipeline + "_" + (pipeline.getWebRtcs().size() - 1);
          WebRtc webRtc = pipeline.createWebRtc(new TreeElementSession(session, treeId, id));
          pipeline.getPlumbers().get(0).connect(webRtc);
          String sdpAnswer = webRtc.processSdpOffer(sdpOffer);
          webRtc.gatherCandidates();
          result = new TreeEndpoint(sdpAnswer, id);
          break;
        }
        numPipeline++;
      }
    }

    if (result != null) {
      return result;
    } else {
      throw new TreeException("Max number of viewers reached");
    }
  }

  @Override
  public synchronized void removeTreeSink(String treeId, String sinkId) throws TreeException {
    checkTreeId(treeId);
    getSink(sinkId).disconnect();
  }

  @Override
  public void addSinkIceCandidate(String treeId, String sinkId, IceCandidate iceCandidate) {
    checkTreeId(treeId);
    getSink(sinkId).addIceCandidate(iceCandidate);
  }

  @Override
  public void addTreeIceCandidate(String treeId, IceCandidate iceCandidate) {
    checkTreeId(treeId);
    if (sourceWebRtc != null)
      sourceWebRtc.addIceCandidate(iceCandidate);
  }

  private WebRtc getSink(String sinkId) {
    WebRtc sink;
    String[] sinkIdTokens = sinkId.split("_");
    if (sinkIdTokens[0].equals("r")) {
      int numWebRtc = Integer.parseInt(sinkIdTokens[1]);
      sink = this.rootPipeline.getWebRtcs().get(numWebRtc);
    } else {
      int numPipeline = Integer.parseInt(sinkIdTokens[0]);
      int numWebRtc = Integer.parseInt(sinkIdTokens[1]);
      sink = this.leafPipelines.get(numPipeline).getWebRtcs().get(numWebRtc);
    }
    return sink;
  }
}
