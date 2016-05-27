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
package org.kurento.tree.client.internal;

public class ProtocolElements {

  public static final String CREATE_TREE_METHOD = "createTree";
  public static final String RELEASE_TREE_METHOD = "releaseTree";
  public static final String SET_TREE_SOURCE_METHOD = "setTreeSource";
  public static final String ADD_TREE_SINK_METHOD = "addTreeSink";
  public static final String REMOVE_TREE_SOURCE_METHOD = "removeTreeSource";
  public static final String REMOVE_TREE_SINK_METHOD = "removeTreeSink";
  public static final String ADD_ICE_CANDIDATE_METHOD = "addIceCandidate";
  public static final String ICE_CANDIDATE_EVENT = "iceCandidate";

  public static final String SINK_ID = "sinkId";
  public static final String ANSWER_SDP = "answerSdp";
  public static final String SOURCE_ID = "sourceId";
  public static final String OFFER_SDP = "offerSdp";
  public static final String TREE_ID = "treeId";
  public static final String ICE_CANDIDATE = "candidate";
  public static final String ICE_SDP_MID = "sdpMid";
  public static final String ICE_SDP_M_LINE_INDEX = "sdpMLineIndex";
}
