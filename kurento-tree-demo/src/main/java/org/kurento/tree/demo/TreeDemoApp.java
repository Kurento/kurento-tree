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

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.kurento.tree.client.KurentoTreeClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Video call 1 to N demo (main).
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.0
 */
@EnableWebSocket
@SpringBootApplication
public class TreeDemoApp implements WebSocketConfigurer {

  final static String DEFAULT_KTS_WS_URI = "wss://localhost:8890/kurento-tree";

  @Bean(destroyMethod = "cleanup", initMethod = "init")
  public TreeDemoHandler callHandler() {
    return new TreeDemoHandler();
  }

  @Bean
  public KurentoTreeClient kurentoTreeClient() {
    return new KurentoTreeClient(System.getProperty("kts.ws.uri", DEFAULT_KTS_WS_URI),
        new SslContextFactory(true));
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(callHandler(), "/call");
  }

  public static void main(String[] args) throws Exception {
    new SpringApplication(TreeDemoApp.class).run(args);
  }

}
