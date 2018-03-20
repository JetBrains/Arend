package com.jetbrains.jetpad.vclang.module.serialization;

import com.jetbrains.jetpad.vclang.core.definition.Definition;

public interface CallTargetIndexProvider {
  int getDefIndex(Definition definition);
}
