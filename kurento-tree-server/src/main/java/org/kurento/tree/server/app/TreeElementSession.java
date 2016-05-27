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

package org.kurento.tree.server.app;

import org.kurento.jsonrpc.Session;

public class TreeElementSession {
  private Session session;
  private String treeId;
  private String sinkId;

  public TreeElementSession(Session session, String treeId, String sinkId) {
    super();
    this.session = session;
    this.treeId = treeId;
    this.sinkId = sinkId;
  }

  public Session getSession() {
    return session;
  }

  public void setSession(Session session) {
    this.session = session;
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
    if (session != null)
      builder.append("sessionId=").append(session.getSessionId()).append(", ");
    if (treeId != null)
      builder.append("treeId=").append(treeId).append(", ");
    if (sinkId != null)
      builder.append("sinkId=").append(sinkId);
    builder.append("]");
    return builder.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    int sessResult = 0;
    if (session != null) {
      sessResult = prime * sessResult + (session.isNew() ? 1231 : 1237);
      sessResult = prime * sessResult
          + ((session.getRegisterInfo() == null) ? 0 : session.getRegisterInfo().hashCode());
      sessResult = prime * sessResult
          + ((session.getSessionId() == null) ? 0 : session.getSessionId().hashCode());
    }
    result = prime * result + sessResult;
    result = prime * result + ((sinkId == null) ? 0 : sinkId.hashCode());
    result = prime * result + ((treeId == null) ? 0 : treeId.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    TreeElementSession other = (TreeElementSession) obj;
    if (session == null) {
      if (other.session != null)
        return false;
    } else {
      if (other.session == null)
        return false;
      if (session.isNew() != other.session.isNew())
        return false;
      if (session.getRegisterInfo() == null) {
        if (other.session.getRegisterInfo() != null)
          return false;
      } else if (!session.getRegisterInfo().equals(other.session.getRegisterInfo()))
        return false;
      if (session.getSessionId() == null) {
        if (other.session.getSessionId() != null)
          return false;
      } else if (!session.getSessionId().equals(other.session.getSessionId()))
        return false;
    }
    if (sinkId == null) {
      if (other.sinkId != null)
        return false;
    } else if (!sinkId.equals(other.sinkId))
      return false;
    if (treeId == null) {
      if (other.treeId != null)
        return false;
    } else if (!treeId.equals(other.treeId))
      return false;
    return true;
  }

}
