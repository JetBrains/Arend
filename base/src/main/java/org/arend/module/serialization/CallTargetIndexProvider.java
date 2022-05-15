package org.arend.module.serialization;

import org.arend.core.definition.Definition;
import org.arend.naming.reference.TCReferable;

public interface CallTargetIndexProvider {
  int getDefIndex(Definition definition);
  int getDefIndex(TCReferable definition);
}
