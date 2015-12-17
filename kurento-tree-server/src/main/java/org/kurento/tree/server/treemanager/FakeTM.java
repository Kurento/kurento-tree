package org.kurento.tree.server.treemanager;

import org.kurento.client.IceCandidate;
import org.kurento.jsonrpc.Session;
import org.kurento.tree.client.TreeEndpoint;
import org.kurento.tree.client.TreeException;
import org.kurento.tree.server.kmsmanager.KmsManager;

public class FakeTM implements TreeManager {

  @Override
  public String createTree() {
    return "1";
  }

  @Override
  public void releaseTree(String treeId) throws TreeException {
  }

  @Override
  public String setTreeSource(Session session, String treeId, String offerSdp)
      throws TreeException {
    return "sdp";
  }

  @Override
  public void removeTreeSource(String treeId) throws TreeException {
  }

  @Override
  public TreeEndpoint addTreeSink(Session session, String treeId, String offerSdp)
      throws TreeException {
    return new TreeEndpoint("sdp", "id");
  }

  @Override
  public void removeTreeSink(String treeId, String sinkId) throws TreeException {
  }

  @Override
  public KmsManager getKmsManager() {
    return null;
  }

  @Override
  public void createTree(String treeId) throws TreeException {
  }

  @Override
  public void addSinkIceCandidate(String treeId, String sinkId, IceCandidate iceCandidate) {
  }

  @Override
  public void addTreeIceCandidate(String treeId, IceCandidate iceCandidate) {
  }
}
