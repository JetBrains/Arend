package com.jetbrains.jetpad.vclang.module.caching.serialization;

import com.jetbrains.jetpad.vclang.term.definition.Definition;

public interface CalltargetIndexProvider {
  int getDefIndex(Definition definition);
}
