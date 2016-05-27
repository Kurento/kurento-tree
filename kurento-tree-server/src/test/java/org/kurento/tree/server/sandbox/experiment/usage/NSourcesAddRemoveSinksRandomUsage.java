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
package org.kurento.tree.server.sandbox.experiment.usage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.kurento.tree.client.TreeException;
import org.kurento.tree.server.treemanager.TreeManager;

public class NSourcesAddRemoveSinksRandomUsage extends UsageSimulation {

  private int numTrees;
  private double addProb;
  private long randomSeed;

  public NSourcesAddRemoveSinksRandomUsage(int numTrees, double addProb, long randomSeed) {
    this.numTrees = numTrees;
    this.addProb = addProb;
    this.randomSeed = randomSeed;
  }

  public NSourcesAddRemoveSinksRandomUsage() {
    this(4, 0.8, 0);
  }

  @Override
  public void useTreeManager(TreeManager treeManager) {

    for (int numTree = 0; numTree < numTrees; numTree++) {
      String treeId = getTreeId(numTree);
      treeManager.createTree(treeId);
      treeManager.setTreeSource(null, treeId, "XXX");
    }

    Random r = new Random(randomSeed);

    List<String> sinks = new ArrayList<String>();
    try {
      while (true) {

        if (r.nextDouble() < addProb) {
          int numTree = r.nextInt(numTrees);
          String treeId = getTreeId(numTree);
          String sinkId = treeManager.addTreeSink(null, treeId, "fakeSdp").getId();
          sinks.add(treeId + "|" + sinkId);
        } else {
          if (!sinks.isEmpty()) {
            int sinkNumber = (int) (r.nextDouble() * sinks.size());
            String sink = sinks.remove(sinkNumber);
            String[] parts = sink.split("\\|");
            treeManager.removeTreeSink(parts[0], parts[1]);
          }
        }
      }

    } catch (TreeException e) {
      System.out.println("Reached maximum tree capacity");
    } catch (Exception e) {
      throw e;
    }
  }

  private static String getTreeId(int numTree) {
    return "Tree" + numTree;
  }
}
