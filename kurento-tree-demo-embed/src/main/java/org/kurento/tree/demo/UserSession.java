/*
 * (C) Copyright 2014-2016 Kurento (http://kurento.org/)
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
package org.kurento.tree.demo;

import org.springframework.web.socket.WebSocketSession;

/**
 * User session.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.4.1
 */
public class UserSession {

  private WebSocketSession session;
  private String sinkId;

  public UserSession(WebSocketSession session) {
    this.session = session;
  }

  public WebSocketSession getSession() {
    return session;
  }

  public void setSinkId(String sinkId) {
    this.sinkId = sinkId;
  }

  public String getSinkId() {
    return sinkId;
  }
}
