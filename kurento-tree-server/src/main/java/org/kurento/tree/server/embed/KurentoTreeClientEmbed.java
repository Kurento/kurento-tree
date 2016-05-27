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
package org.kurento.tree.server.embed;

import java.io.IOException;

import org.kurento.jsonrpc.client.JsonRpcClientLocal;
import org.kurento.tree.client.KurentoTreeClient;
import org.kurento.tree.server.app.ClientsJsonRpcHandler;
import org.kurento.tree.server.app.KurentoTreeServerApp;
import org.kurento.tree.server.treemanager.TreeManager;
import org.springframework.context.ConfigurableApplicationContext;

public class KurentoTreeClientEmbed {

  public static KurentoTreeClient create(int port) {

    final ConfigurableApplicationContext server = KurentoTreeServerApp.start(port);

    TreeManager treeMgr = server.getBean(TreeManager.class);

    JsonRpcClientLocal localClient = new JsonRpcClientLocal(new ClientsJsonRpcHandler(treeMgr));

    return new KurentoTreeClient(localClient) {
      @Override
      public void close() throws IOException {
        super.close();
        server.close();
      }
    };
  }

}
