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
package org.kurento.tree.server.kmsmanager;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.kurento.client.KurentoClient;
import org.kurento.tree.server.app.KmsRegistrar;
import org.kurento.tree.server.kms.Kms;
import org.kurento.tree.server.kms.real.RealKms;

public class RealElasticKmsManager extends KmsManager implements KmsRegistrar {

  public List<Kms> kmss = new CopyOnWriteArrayList<>();

  public RealElasticKmsManager(List<String> kmsWsUris) {
    for (String kmsWsUri : kmsWsUris) {
      addKms(kmsWsUri);
    }
  }

  private void addKms(String kmsWsUri) {
    this.kmss.add(new RealKms(KurentoClient.create(kmsWsUri)));
  }

  public List<Kms> getKmss() {
    return kmss;
  }

  @Override
  public void register(String kmsWsUri) {
    addKms(kmsWsUri);
  }
}
