package org.kurento.tree.server.treemanager;

import org.kurento.tree.server.kms.Kms;

public interface KmsListener {

  public void kmsAdded(Kms kms);

  public void kmsRemoved(Kms kms);

}
