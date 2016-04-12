/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
 * Video call 1 to N demo (main).
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.0
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
    registry.addHandler(callHandler(), "/call");
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
