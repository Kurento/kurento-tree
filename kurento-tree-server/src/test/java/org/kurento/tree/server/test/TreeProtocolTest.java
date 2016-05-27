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
package org.kurento.tree.server.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.kurento.jsonrpc.client.JsonRpcClientLocal;
import org.kurento.tree.client.KurentoTreeClient;
import org.kurento.tree.client.TreeEndpoint;
import org.kurento.tree.client.TreeException;
import org.kurento.tree.server.app.ClientsJsonRpcHandler;
import org.kurento.tree.server.treemanager.TreeManager;

public class TreeProtocolTest {

  private TreeManager treeMgr;
  private JsonRpcClientLocal localClient;
  private KurentoTreeClient client;

  @Before
  public void init() {
    treeMgr = mock(TreeManager.class);
    localClient = new JsonRpcClientLocal(new ClientsJsonRpcHandler(treeMgr));
    client = new KurentoTreeClient(localClient);
  }

  @Test
  public void testCreateTree() throws IOException, TreeException {

    when(treeMgr.createTree()).thenReturn("TreeId");
    assertThat(client.createTree(), is("TreeId"));
  }

  @Test
  public void testCreateTreeWithId() throws IOException, TreeException {
    client.createTree("TreeId");
    verify(treeMgr).createTree("TreeId");
  }

  @Test
  public void testCreateTreeWithCollision() throws IOException, TreeException {

    doThrow(new TreeException("message")).when(treeMgr).createTree("TreeId");

    try {
      client.createTree("TreeId");
      fail("TreeException should be thrown");
    } catch (TreeException e) {
      assertThat(e.getMessage(), containsString("message"));
    }
  }

  @Test
  public void testSetTreeSource() throws IOException, TreeException {

    when(treeMgr.setTreeSource(localClient.getSession(), "TreeId", "sdpOffer"))
        .thenReturn("sdpAnswer");

    assertThat(client.setTreeSource("TreeId", "sdpOffer"), is("sdpAnswer"));
  }

  @Test
  public void testAddTreeSink() throws IOException, TreeException {
    when(treeMgr.addTreeSink(localClient.getSession(), "TreeId", "sdpOffer"))
        .thenReturn(new TreeEndpoint("SinkId", "sdpAnswer"));

    assertThat(client.addTreeSink("TreeId", "sdpOffer"),
        is(new TreeEndpoint("SinkId", "sdpAnswer")));
  }

  @Test
  public void testReleaseTree() throws IOException, TreeException {

    client.releaseTree("TreeId");
    verify(treeMgr).releaseTree("TreeId");
  }

  @Test
  public void testRemoveSink() throws IOException, TreeException {

    client.removeTreeSink("TreeId", "SinkId");
    verify(treeMgr).removeTreeSink("TreeId", "SinkId");
  }

  @Test
  public void testRemoveSource() throws IOException, TreeException {

    client.removeTreeSource("TreeId");
    verify(treeMgr).removeTreeSource("TreeId");
  }
}
