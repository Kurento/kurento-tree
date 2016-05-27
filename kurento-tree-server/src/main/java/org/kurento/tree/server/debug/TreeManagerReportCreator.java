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
package org.kurento.tree.server.debug;

import java.awt.Desktop;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.kurento.client.IceCandidate;
import org.kurento.jsonrpc.Session;
import org.kurento.tree.client.TreeEndpoint;
import org.kurento.tree.client.TreeException;
import org.kurento.tree.server.kmsmanager.KmsManager;
import org.kurento.tree.server.treemanager.TreeManager;

public class TreeManagerReportCreator implements TreeManager {

  private KmsManager kmsManager;
  private TreeManager treeManager;
  private String name;

  private Path reportPath;
  private PrintWriter writer;
  private int step = 1;

  public TreeManagerReportCreator(KmsManager manager, String name) {
    this.kmsManager = manager;
    this.name = name;
    initReport();
  }

  public void setTreeManager(TreeManager treeManager) {
    this.treeManager = treeManager;
    writer.println("<h1>TreeManager: " + treeManager.getClass().getName() + "</h1>");
    includeTreeManagerSnapshot();
  }

  private void initReport() {

    try {
      reportPath = Files.createTempFile("TreeReport", ".html");
      writer = new PrintWriter(Files.newBufferedWriter(reportPath, StandardCharsets.UTF_8));
      writer.println("<html>");
      writer.println("<title>" + name + "</title>");
      writer.println("<body>");
      writer.println("<h1>" + name + "</h1>");

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // Public API

  public void addSection(String sectionName) {
    writer.println("<h2>" + sectionName + "</h2>");
  }

  public void createReport(String path) throws IOException {
    createReport();
    Files.move(this.reportPath, Paths.get(path), StandardCopyOption.REPLACE_EXISTING);
    this.reportPath = Paths.get(path);
  }

  public void createReport() {
    writer.println("</body>");
    writer.println("</html>");
    writer.close();
  }

  public void showReport() throws IOException {
    System.out.println("Opening report in " + reportPath);
    Desktop.getDesktop().browse(reportPath.toUri());
  }

  // Report generation operations

  private void includeOperation(String operation) {
    writer.println("<p>Step " + step + ": " + operation + "</p>");
    System.out.println("Step " + step + ": " + operation);
    System.out.println(
        "---------------------------------------------------------------------------------------------------------------------------------------");
    step++;
    includeTreeManagerSnapshot();
  }

  private void includeTreeManagerSnapshot() {
    writer.println(KmsTopologyGrapher.createSvgTopologyGrapher(kmsManager, false));
  }

  // TreeManager API

  @Override
  public String createTree() throws TreeException {
    String treeId = treeManager.createTree();
    includeOperation("createTree() -> " + treeId);
    return treeId;
  }

  @Override
  public void releaseTree(String treeId) throws TreeException {
    treeManager.releaseTree(treeId);
    includeOperation("releaseTree(" + treeId + ")");
  }

  @Override
  public String setTreeSource(Session session, String treeId, String sdpOffer)
      throws TreeException {
    String answerSdp = treeManager.setTreeSource(session, treeId, sdpOffer);
    includeOperation("setTreeSource(" + treeId + "," + sdpOffer + ") -> " + answerSdp);
    return answerSdp;
  }

  @Override
  public void removeTreeSource(String treeId) throws TreeException {
    treeManager.removeTreeSource(treeId);
    includeOperation("removeTreeSource(" + treeId + ")");
  }

  @Override
  public TreeEndpoint addTreeSink(Session session, String treeId, String sdpOffer)
      throws TreeException {
    TreeEndpoint endpoint = treeManager.addTreeSink(session, treeId, sdpOffer);
    includeOperation("addTreeSink(" + treeId + "," + sdpOffer + ") -> " + "sdp = "
        + endpoint.getSdp() + " , sinkId = " + endpoint.getId());
    return endpoint;
  }

  @Override
  public void removeTreeSink(String treeId, String sinkId) throws TreeException {
    treeManager.removeTreeSink(treeId, sinkId);
    includeOperation("removeTreeSink(" + treeId + "," + sinkId + ")");
  }

  @Override
  public KmsManager getKmsManager() {
    return kmsManager;
  }

  public void addText(String text) {
    writer.println("<p>" + text + "</p>");
  }

  @Override
  public void createTree(String treeId) throws TreeException {
    treeManager.createTree(treeId);
    includeOperation("createTree(" + treeId + ")");
  }

  @Override
  public void addSinkIceCandidate(String treeId, String sinkId, IceCandidate iceCandidate) {
    treeManager.addSinkIceCandidate(treeId, sinkId, iceCandidate);
    includeOperation(
        "addSinkIceCandidate(" + treeId + "," + sinkId + "," + iceCandidate.getCandidate() + ")");
  }

  @Override
  public void addTreeIceCandidate(String treeId, IceCandidate iceCandidate) {
    treeManager.addTreeIceCandidate(treeId, iceCandidate);
    includeOperation("addTreeIceCandidate(" + treeId + "," + iceCandidate.getCandidate() + ")");
  }

}
