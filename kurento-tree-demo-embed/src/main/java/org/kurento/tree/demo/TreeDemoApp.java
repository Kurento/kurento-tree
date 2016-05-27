/*
 * (C) Copyright 2014-2016 Kurento (http://kurento.org/)
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

import org.kurento.jsonrpc.client.JsonRpcClientLocal;
import org.kurento.tree.client.KurentoTreeClient;
import org.kurento.tree.server.app.ClientsJsonRpcHandler;
import org.kurento.tree.server.app.KurentoTreeServerApp;
import org.kurento.tree.server.treemanager.TreeManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebRTC broadcasting (tree topology). The kurento-tree-server is embedded (notice that this class
 * extends KurentoTreeServerApp)
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.4.1
 */
public class TreeDemoApp extends KurentoTreeServerApp implements WebSocketConfigurer {

  @Autowired
  private ApplicationContext app;

  @Bean(destroyMethod = "cleanup", initMethod = "init")
  public TreeDemoHandler callHandler() {
    return new TreeDemoHandler();
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(callHandler(), "/tree");
  }

  @Bean
  public KurentoTreeClient kurentoTreeClient() {
    TreeManager treeMgr = app.getBean(TreeManager.class);
    JsonRpcClientLocal localClient = new JsonRpcClientLocal(new ClientsJsonRpcHandler(treeMgr));
    return new KurentoTreeClient(localClient);
  }

  public static void main(String[] args) throws Exception {
    new SpringApplication(TreeDemoApp.class).run(args);
  }

}
