package org.arend.source;

import org.arend.ext.typechecking.DefinitionListener;
import org.arend.extImpl.SerializableKeyRegistryImpl;

/**
 * Represents a module persisted in a binary format.
 */
public interface BinarySource extends Source {
  void setKeyRegistry(SerializableKeyRegistryImpl keyRegistry);

  void setDefinitionListener(DefinitionListener definitionListener);
}
