package org.kurento.tree.server.test;

import java.io.IOException;

import org.junit.Test;
import org.kurento.tree.client.KurentoTreeClient;
import org.kurento.tree.server.embed.KurentoTreeClientEmbed;

public class EmbedTest {

	@Test
	public void test() throws IOException {
		
		KurentoTreeClient client = KurentoTreeClientEmbed.create(6666);
		
		client.createTree("XXXXX");
		
		client.close();
	}	
}
