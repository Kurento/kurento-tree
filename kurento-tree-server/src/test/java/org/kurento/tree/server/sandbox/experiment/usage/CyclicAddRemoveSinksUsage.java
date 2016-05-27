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

import org.kurento.tree.server.treemanager.TreeManager;

public class CyclicAddRemoveSinksUsage extends UsageSimulation {

  public static class TreeUsage {

    private int iterations;
    private int maxSinksPerTree;
    private TreeManager treeManager;
    private String treeId;

    private boolean created = false;
    private boolean growing = true;
    private int iteration = 0;
    private int createIteration = 0;
    private List<String> sinks = new ArrayList<String>();
    private int iterationsToCreate;

    public TreeUsage(int numTree, TreeManager treeManager, int iterations, int maxSinksPerTree,
        int iterationsToCreate) {
      this.treeManager = treeManager;
      this.iterations = iterations;
      this.iterationsToCreate = iterationsToCreate;
      this.maxSinksPerTree = maxSinksPerTree;
      this.treeId = getTreeId(numTree);
    }

    public void evolve() {

      try {

        if (!created) {

          if (createIteration < iterationsToCreate) {

            createIteration++;

          } else {

            treeManager.createTree(treeId);
            treeManager.setTreeSource(null, treeId, "XXX");
            created = true;
          }

        } else if (growing) {

          String sinkId = treeManager.addTreeSink(null, treeId, "fakeSdp").getId();
          sinks.add(treeId + "|" + sinkId);

          if (sinks.size() == maxSinksPerTree) {
            growing = false;
            System.out.println("Shrinking Tree " + treeId);
          }

        } else {
          String sink = sinks.remove(0);
          String[] parts = sink.split("\\|");
          treeManager.removeTreeSink(parts[0], parts[1]);

          if (sinks.isEmpty()) {
            System.out.println("Restarting Tree " + treeId + " in iteration " + iteration);
            treeManager.releaseTree(treeId);
            created = false;
            growing = true;
            iteration++;
          }
        }
      } catch (Exception e) {
        throw e;
      }
    }

    public boolean finished() {
      return iteration >= iterations;
    }
  }

  private int numTrees;
  private int maxSinksPerTree;
  private int iterations;
  private long randomSeed;
  private int iterationsToCreate;

  public CyclicAddRemoveSinksUsage(int numTrees, int maxSinksPerTree, int iterations,
      long randomSeed, int iterationsToCreate) {
    this.numTrees = numTrees;
    this.maxSinksPerTree = maxSinksPerTree;
    this.iterations = iterations;
    this.randomSeed = randomSeed;
    this.iterationsToCreate = iterationsToCreate;
  }

  public CyclicAddRemoveSinksUsage() {
    this(4, 5, 3, 0, 2);
  }

  @Override
  public void useTreeManager(TreeManager treeManager) {

    List<TreeUsage> trees = new ArrayList<>();

    for (int i = 0; i < numTrees; i++) {
      trees.add(new TreeUsage(i, treeManager, this.iterations, this.maxSinksPerTree,
          i * this.iterationsToCreate));
    }

    Random r = null;
    if (randomSeed != -1) {
      r = new Random(randomSeed);
    }

    int numTree = -1;
    while (!trees.isEmpty()) {

      TreeUsage tree;
      do {

        if (r != null) {
          numTree = r.nextInt(trees.size());
        } else {
          numTree = (numTree + 1) % trees.size();
        }

        tree = trees.get(numTree);

        if (tree.finished()) {
          trees.remove(tree);
          if (trees.isEmpty()) {
            return;
          } else {
            tree = null;
          }
        }
      } while (tree == null);

      tree.evolve();
    }
  }

  private static String getTreeId(int numTree) {
    return "Tree" + numTree;
  }
}
