/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
