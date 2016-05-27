/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

package org.kurento.tree.client;

import static org.kurento.tree.client.internal.ProtocolElements.ICE_CANDIDATE;
import static org.kurento.tree.client.internal.ProtocolElements.ICE_CANDIDATE_EVENT;
import static org.kurento.tree.client.internal.ProtocolElements.ICE_SDP_MID;
import static org.kurento.tree.client.internal.ProtocolElements.ICE_SDP_M_LINE_INDEX;
import static org.kurento.tree.client.internal.ProtocolElements.SINK_ID;
import static org.kurento.tree.client.internal.ProtocolElements.TREE_ID;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.kurento.client.IceCandidate;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.kurento.tree.client.internal.JsonTreeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

public class ServerJsonRpcHandler extends DefaultJsonRpcHandler<JsonObject> {

  private static final Logger log = LoggerFactory.getLogger(ServerJsonRpcHandler.class);

  private static BlockingQueue<IceCandidateInfo> candidates = new ArrayBlockingQueue<IceCandidateInfo>(
      100);

  @Override
  public void handleRequest(Transaction transaction, Request<JsonObject> request) throws Exception {
    try {
      switch (request.getMethod()) {
      case ICE_CANDIDATE_EVENT:
        iceCandidateEvent(transaction, request);
        break;
      default:
        log.error("Unrecognized request {}", request);
        break;
      }
    } catch (Exception e) {
      log.error("Exception processing request {}", request, e);
      transaction.sendError(e);
    }
  }

  private void iceCandidateEvent(Transaction transaction, Request<JsonObject> request) {

    String candidate = JsonTreeUtils.getRequestParam(request, ICE_CANDIDATE, String.class);
    String sdpMid = JsonTreeUtils.getRequestParam(request, ICE_SDP_MID, String.class);
    int sdpMLineIndex = JsonTreeUtils.getRequestParam(request, ICE_SDP_M_LINE_INDEX, Integer.class);

    IceCandidate iceCandidate = new IceCandidate(candidate, sdpMid, sdpMLineIndex);

    String treeId = JsonTreeUtils.getRequestParam(request, TREE_ID, String.class);
    String sinkId = JsonTreeUtils.getRequestParam(request, SINK_ID, String.class, true);

    IceCandidateInfo eventInfo = new IceCandidateInfo(iceCandidate, treeId, sinkId);

    log.debug("Enqueueing ICE candidate info {}", eventInfo);
    try {
      candidates.put(eventInfo);
      log.debug("Enqueued ICE candidate info {}", eventInfo);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * Blocks until an element is available and then returns it by removing it from the queue.
   * 
   * @return an {@link IceCandidateInfo} from the queue, null when interrupted
   * @see BlockingQueue#take()
   */
  public IceCandidateInfo getCandidateInfo() {
    try {
      IceCandidateInfo candidateInfo = candidates.take();
      log.debug("Dequeued ICE candidate info {}", candidateInfo);
      return candidateInfo;
    } catch (InterruptedException e) {
      e.printStackTrace();
      return null;
    }
  }
}
