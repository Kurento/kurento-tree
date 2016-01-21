
package org.kurento.tree.server.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.tree.server.app.KurentoTreeServerApp.KMS_URI_PROPERTY;

import org.junit.Test;
import org.kurento.commons.ConfigFileManager;
import org.kurento.commons.PropertiesManager;

public class TreeApiConfigFileLoaderTest {

  @Test
  public void basicConfigFile() {

    ConfigFileManager.loadConfigFile("basic-kurento-tree.conf.json");

    assertThat(PropertiesManager.getProperty("ws.port"), is("8890"));
    assertThat(PropertiesManager.getProperty("ws.path"), is("kurento-tree"));

    String kmsUri = getProperty(KMS_URI_PROPERTY, "something-else");

    assertThat(kmsUri, is("ws://192.168.0.1:8888/kurento"));

  }

}
