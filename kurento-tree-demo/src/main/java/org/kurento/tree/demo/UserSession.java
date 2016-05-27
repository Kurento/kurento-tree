/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
package org.kurento.tree.demo;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.google.gson.JsonObject;

/**
 * User session.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.0
 */
public class UserSession {

  private static final Logger log = LoggerFactory.getLogger(UserSession.class);

  private WebSocketSession session;
  private String sinkId;

  public UserSession(WebSocketSession session) {
    this.session = session;
  }

  public WebSocketSession getSession() {
    return session;
  }

  public void sendMessage(JsonObject message) throws IOException {
    log.debug("Sending message from user with session Id '{}': {}", session.getId(), message);
    session.sendMessage(new TextMessage(message.toString()));
  }

  public void setSinkId(String sinkId) {
    this.sinkId = sinkId;
  }

  public String getSinkId() {
    return sinkId;
  }
}
