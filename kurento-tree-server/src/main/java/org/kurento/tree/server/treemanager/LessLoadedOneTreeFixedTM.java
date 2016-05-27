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
import org.kurento.tree.server.kmsmanager.KmsLoad;
import org.kurento.tree.server.kmsmanager.KmsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This TreeManager has the following characteristics:
 * <ul>
 * <li>It allows only one tree</li>
 * <li>Creates WebRtcEndpoint for sinks (viewers) only in non-source KMSs</li>
 * <li>Fills less loaded node.</li>
 * <li>It doesn't consider new kmss after start.</li>
 * </ul>
 *
 * @author micael.gallego@gmail.com
 */
public class LessLoadedOneTreeFixedTM extends AbstractOneTreeTM {

  private static final Logger log = LoggerFactory.getLogger(LessLoadedOneTreeFixedTM.class);

  private KmsManager kmsManager;

  private boolean oneKms = true;

  private Pipeline sourcePipeline;
  private List<Plumber> sourcePlumbers = new ArrayList<>();
  private WebRtc source;

  private List<Pipeline> leafPipelines = new ArrayList<>();
  private List<Plumber> leafPlumbers = new ArrayList<>();
  private Map<String, WebRtc> sinks = new ConcurrentHashMap<>();

  private int numSinks = 0;

  public LessLoadedOneTreeFixedTM(KmsManager kmsManager) {

    this.kmsManager = kmsManager;

    if (kmsManager.getKmss().isEmpty()) {
      log.error("AotOneTreeManager cannot be used without initial kmss");

    } else if (kmsManager.getKmss().size() == 1) {

      oneKms = true;

      sourcePipeline = kmsManager.getKmss().get(0).createPipeline();

    } else {

      oneKms = false;

      int numPipeline = 0;
      for (Kms kms : kmsManager.getKmss()) {
        Pipeline pipeline = kms.createPipeline();

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
  public KmsManager getKmsManager() {
    return kmsManager;
  }

  @Override
  public synchronized void releaseTree(String treeId) throws TreeException {

    checkTreeId(treeId);

    createdTree = false;
    source.release();
    source = null;

    for (WebRtc webRtc : sinks.values()) {
      webRtc.release();
    }
  }

  @Override
  public synchronized String setTreeSource(Session session, String treeId, String offerSdp)
      throws TreeException {

    checkTreeId(treeId);

    if (source != null) {
      removeTreeSource(treeId);
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
  public synchronized void removeTreeSource(String treeId) throws TreeException {

    checkTreeId(treeId);
    source.release();
    source = null;
  }

  @Override
  public synchronized TreeEndpoint addTreeSink(Session session, String treeId, String sdpOffer)
      throws TreeException {

    checkTreeId(treeId);

    TreeEndpoint result = null;

    if (oneKms) {
      if (sourcePipeline.getKms().allowMoreElements()) {
        String id = "r_" + (sourcePipeline.getWebRtcs().size() - 1);
        WebRtc webRtc = sourcePipeline.createWebRtc(new TreeElementSession(session, treeId, id));
        source.connect(webRtc);
        String sdpAnswer = webRtc.processSdpOffer(sdpOffer);
        webRtc.gatherCandidates();
        webRtc.setLabel("Sink " + numSinks + " (WR " + id + ")");
        result = new TreeEndpoint(sdpAnswer, id);
      }
    } else {

      List<KmsLoad> kmss = kmsManager.getKmssSortedByLoad();

      Pipeline pipeline;
      if (kmss.get(0).getKms().getPipelines().get(0) != sourcePipeline) {
        pipeline = kmss.get(0).getKms().getPipelines().get(0);
      } else {
        pipeline = kmss.get(1).getKms().getPipelines().get(0);
      }

      if (pipeline.getKms().allowMoreElements()) {
        String id = pipeline.getLabel() + "_" + numSinks;
        WebRtc webRtc = pipeline.createWebRtc(new TreeElementSession(session, treeId, id));
        pipeline.getPlumbers().get(0).connect(webRtc);
        String sdpAnswer = webRtc.processSdpOffer(sdpOffer);
        webRtc.gatherCandidates();
        webRtc.setLabel("Sink " + numSinks + " (WR " + id + ")");
        sinks.put(Integer.toString(numSinks), webRtc);

        result = new TreeEndpoint(sdpAnswer, id);
      } else {
        System.out.println("sss");
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
  public synchronized void removeTreeSink(String treeId, String sinkId) throws TreeException {

    checkTreeId(treeId);

    String[] sinkIdTokens = sinkId.split("_");

    if (sinkIdTokens[0].equals("r")) {

      int numWebRtc = Integer.parseInt(sinkIdTokens[1]);
      this.sourcePipeline.getWebRtcs().get(numWebRtc).disconnect();

    } else {
      sinks.get(sinkIdTokens[1]).release();
    }
  }

  @Override
  public void addSinkIceCandidate(String treeId, String sinkId, IceCandidate iceCandidate) {
    checkTreeId(treeId);
    String[] sinkIdTokens = sinkId.split("_");
    if (sinkIdTokens[0].equals("r")) {
      int numWebRtc = Integer.parseInt(sinkIdTokens[1]);
      this.sourcePipeline.getWebRtcs().get(numWebRtc).addIceCandidate(iceCandidate);
    } else {
      int numPipeline = Integer.parseInt(sinkIdTokens[0]);
      int numWebRtc = Integer.parseInt(sinkIdTokens[1]);
      this.leafPipelines.get(numPipeline).getWebRtcs().get(numWebRtc).addIceCandidate(iceCandidate);
    }
  }

  @Override
  public void addTreeIceCandidate(String treeId, IceCandidate iceCandidate) {
    checkTreeId(treeId);
    if (source != null)
      source.addIceCandidate(iceCandidate);
  }

}
