package org.kurento.tree.server.embed;

import java.io.IOException;

import org.kurento.jsonrpc.client.JsonRpcClientLocal;
import org.kurento.tree.client.KurentoTreeClient;
import org.kurento.tree.server.app.ClientsJsonRpcHandler;
import org.kurento.tree.server.app.KurentoTreeServerApp;
import org.kurento.tree.server.treemanager.TreeManager;
import org.springframework.context.ConfigurableApplicationContext;

public class KurentoTreeClientEmbed {

	public static KurentoTreeClient create(int port){
		
		final ConfigurableApplicationContext server = KurentoTreeServerApp.start(port);
		
		TreeManager treeMgr = server.getBean(TreeManager.class);
		
		JsonRpcClientLocal localClient = new JsonRpcClientLocal(new ClientsJsonRpcHandler(treeMgr));
		
		return new KurentoTreeClient(localClient){
			@Override
			public void close() throws IOException {
				super.close();
				server.close();
			}
		};
	}
	
}
