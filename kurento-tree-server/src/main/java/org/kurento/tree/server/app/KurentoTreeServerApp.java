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

package org.kurento.tree.server.app;

import static org.kurento.commons.PropertiesManager.getProperty;

import java.util.Arrays;
import java.util.Properties;

import org.kurento.commons.ConfigFileManager;
import org.kurento.jsonrpc.JsonRpcHandler;
import org.kurento.jsonrpc.internal.server.config.JsonRpcConfiguration;
import org.kurento.jsonrpc.server.JsonRpcConfigurer;
import org.kurento.jsonrpc.server.JsonRpcHandlerRegistry;
import org.kurento.tree.client.TreeException;
import org.kurento.tree.server.kmsmanager.KmsManager;
import org.kurento.tree.server.kmsmanager.MinWebRtcEpsKmsManager;
import org.kurento.tree.server.kmsmanager.RealElasticKmsManager;
import org.kurento.tree.server.treemanager.LessLoadedElasticTM;
import org.kurento.tree.server.treemanager.LessLoadedOnlySource2TM;
import org.kurento.tree.server.treemanager.TreeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

import com.google.gson.JsonObject;

@SpringBootApplication
@Import(JsonRpcConfiguration.class)
@EnableWebSocket
public class KurentoTreeServerApp implements JsonRpcConfigurer {

  private static final Logger log = LoggerFactory.getLogger(KurentoTreeServerApp.class);

  private static final String UNSECURE_RANDOM_PROPERTY = "unsecureRandom";
  private static final boolean UNSECURE_RANDOM_DEFAULT = true;

  public static final String WEBSOCKET_PORT_PROPERTY = "ws.port";
  public static final String WEBSOCKET_PORT_DEFAULT = "8890";

  public static final String WEBSOCKET_PATH_PROPERTY = "ws.path";
  public static final String WEBSOCKET_PATH_DEFAULT = "kurento-tree";

  public static final String KMS_URI_PROPERTY = "kms.url";
  public static final String KMS_URI_DEFAULT = "ws://localhost:8888/kurento";

  public static final String KMS_MODE_PROPERTY = "kms.mode";

  private KmsMode kmsMode = getProperty(KMS_MODE_PROPERTY, KmsMode.NUBOMEDIA);

  private enum KmsMode {
    REGISTRAR, NUBOMEDIA
  }

  private static ConfigurableApplicationContext app;

  @Bean
  public KmsManager kmsManager() {

    switch (kmsMode) {
      case REGISTRAR:
        return new RealElasticKmsManager(Arrays.asList(loadKmsUrl()));
      case NUBOMEDIA:
        return new MinWebRtcEpsKmsManager();
      default:
        throw new TreeException("Unsupported kmsMode " + kmsMode);
    }
  }

  private String loadKmsUrl() {
    String kmsUrl = getProperty(KMS_URI_PROPERTY, KMS_URI_DEFAULT);

    log.info("Configuring Kurento Tree Server to use kms: " + kmsUrl);

    return kmsUrl;
  }

  @Bean
  public TreeManager treeManager() {

    KmsManager kmsManager = kmsManager();

    switch (kmsMode) {
      case REGISTRAR:
        return new LessLoadedElasticTM(kmsManager);
      case NUBOMEDIA:
        LessLoadedOnlySource2TM treeManager = new LessLoadedOnlySource2TM(kmsManager);
        ((MinWebRtcEpsKmsManager) kmsManager).setKmsListener(treeManager);

        return treeManager;
      default:
        throw new TreeException("Unsupported kmsMode " + kmsMode);
    }
  }

  @Override
  public void registerJsonRpcHandlers(JsonRpcHandlerRegistry registry) {
    registry.addHandler(clientsJsonRpcHandler().withSockJS(),
        getProperty(WEBSOCKET_PATH_PROPERTY, WEBSOCKET_PATH_DEFAULT));

    if (kmsMode == KmsMode.REGISTRAR) {
      JsonRpcHandler<JsonObject> registrar =
          new RegistrarJsonRpcHandler((KmsRegistrar) kmsManager());

      registry.addHandler(registrar, "/registrar");
    }
  }

  @Bean
  public ClientsJsonRpcHandler clientsJsonRpcHandler() {
    return new ClientsJsonRpcHandler(treeManager());
  }

  public static ConfigurableApplicationContext start() {
    return start(-1);
  }

  public static ConfigurableApplicationContext start(int port) {

    ConfigFileManager.loadConfigFile("kurento-tree.conf.json");

    if (getProperty(UNSECURE_RANDOM_PROPERTY, UNSECURE_RANDOM_DEFAULT)) {
      log.info("Using /dev/urandom for secure random generation");
      System.setProperty("java.security.egd", "file:/dev/./urandom");
    }

    if (port == -1) {
      port = Integer.parseInt(getProperty(WEBSOCKET_PORT_PROPERTY, WEBSOCKET_PORT_DEFAULT));
    }

    SpringApplication application = new SpringApplication(KurentoTreeServerApp.class);

    Properties properties = new Properties();
    properties.put("server.port", port);
    application.setDefaultProperties(properties);

    app = application.run();

    return app;
  }

  public static void stop() {
    if (app != null) {
      app.close();
    }
  }

  public static void main(String[] args) throws Exception {
    start();
  }

}
