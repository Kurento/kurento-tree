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

import org.kurento.client.IceCandidate;

public class IceCandidateInfo {

  private IceCandidate iceCandidate;
  private String treeId;
  private String sinkId;

  public IceCandidateInfo(IceCandidate iceCandidate, String treeId, String sinkId) {
    super();
    this.iceCandidate = iceCandidate;
    this.treeId = treeId;
    this.sinkId = sinkId;
  }

  public IceCandidate getIceCandidate() {
    return iceCandidate;
  }

  public void setIceCandidate(IceCandidate iceCandidate) {
    this.iceCandidate = iceCandidate;
  }

  public String getTreeId() {
    return treeId;
  }

  public void setTreeId(String treeId) {
    this.treeId = treeId;
  }

  public String getSinkId() {
    return sinkId;
  }

  public void setSinkId(String sinkId) {
    this.sinkId = sinkId;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    if (treeId != null)
      builder.append("treeId=").append(treeId).append(", ");
    if (sinkId != null)
      builder.append("sinkId=").append(sinkId).append(", ");
    if (iceCandidate != null)
      builder.append("iceCandidate=[sdpMLineIndex= ").append(iceCandidate.getSdpMLineIndex())
          .append(", sdpMid=").append(iceCandidate.getSdpMid()).append(", candidate=")
          .append(iceCandidate.getCandidate()).append("]");
    builder.append("]");
    return builder.toString();
  }

}
