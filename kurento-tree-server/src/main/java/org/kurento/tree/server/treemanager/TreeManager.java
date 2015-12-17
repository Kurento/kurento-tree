package org.kurento.tree.server.treemanager;

import org.kurento.client.IceCandidate;
import org.kurento.jsonrpc.Session;
import org.kurento.tree.client.TreeEndpoint;
import org.kurento.tree.client.TreeException;
import org.kurento.tree.server.kmsmanager.KmsManager;

public interface TreeManager {

  public String createTree() throws TreeException;

  public void createTree(String treeId) throws TreeException;

  public void releaseTree(String treeId) throws TreeException;

  public String setTreeSource(Session session, String treeId, String sdpOffer) throws TreeException;

  public void removeTreeSource(String treeId) throws TreeException;

  public TreeEndpoint addTreeSink(Session session, String treeId, String sdpOffer)
      throws TreeException;

  public void removeTreeSink(String treeId, String sinkId) throws TreeException;

  public KmsManager getKmsManager();

  public void addSinkIceCandidate(String treeId, String sinkId, IceCandidate iceCandidate);

  public void addTreeIceCandidate(String treeId, IceCandidate iceCandidate);

}
