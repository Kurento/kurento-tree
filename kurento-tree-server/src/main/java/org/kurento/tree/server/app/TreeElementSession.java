/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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
			builder.append("sessionId=").append(session.getSessionId())
			.append(", ");
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
			sessResult = prime
					* sessResult
					+ ((session.getRegisterInfo() == null) ? 0 : session
							.getRegisterInfo().hashCode());
			sessResult = prime
					* sessResult
					+ ((session.getSessionId() == null) ? 0 : session
							.getSessionId().hashCode());
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
			} else if (!session.getRegisterInfo().equals(
					other.session.getRegisterInfo()))
				return false;
			if (session.getSessionId() == null) {
				if (other.session.getSessionId() != null)
					return false;
			} else if (!session.getSessionId().equals(
					other.session.getSessionId()))
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
