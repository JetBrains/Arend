package org.arend.module.serialization;

import org.arend.core.definition.Definition;

public interface CallTargetIndexProvider {
  int getDefIndex(Definition definition);
}
