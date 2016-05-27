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
package org.kurento.tree.server.sandbox.experiment;

import org.kurento.tree.server.kms.loadmanager.MaxWebRtcLoadManager;
import org.kurento.tree.server.kmsmanager.FakeFixedNKmsManager;
import org.kurento.tree.server.kmsmanager.KmsManager;
import org.kurento.tree.server.sandbox.experiment.framework.Experiment;
import org.kurento.tree.server.sandbox.experiment.framework.TreeManagerCreator;
import org.kurento.tree.server.sandbox.experiment.usage.OneSourceAddRemoveSinksUsage;
import org.kurento.tree.server.treemanager.LessLoadedOneTreeFixedTM;
import org.kurento.tree.server.treemanager.TreeManager;

public class Experiment1 extends Experiment {

  public void configureExperiment() {

    setKmsManager(new FakeFixedNKmsManager(4, new MaxWebRtcLoadManager(5)));

    addUsageSimulation(new OneSourceAddRemoveSinksUsage());

    addTreeManagerCreator(new TreeManagerCreator() {
      @Override
      public TreeManager createTreeManager(KmsManager kmsManager) {
        return new LessLoadedOneTreeFixedTM(kmsManager);
      }
    });
  }

  public static void main(String[] args) {
    new Experiment1().run();
  }
}
