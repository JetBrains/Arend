package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.module.ModuleID;

public interface SourceInfoProvider {
  String nameFor(Abstract.Definition definition);
  ModuleID moduleOf(Abstract.Definition definition);

  class Null implements SourceInfoProvider {
    @Override
    public String nameFor(Abstract.Definition definition) {
      return null;
    }

    @Override
    public ModuleID moduleOf(Abstract.Definition definition) {
      return null;
    }
  }
}
