package com.jetbrains.jetpad.vclang.module.caching.serialization;

import com.jetbrains.jetpad.vclang.core.definition.Definition;

public interface CalltargetIndexProvider {
  int getDefIndex(Definition definition);
}
