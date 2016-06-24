package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.module.ModuleID;

public interface SourceInfoProvider {
  String nameFor(Abstract.Definition definition);
  ModuleID moduleOf(Abstract.Definition definition);

  class Trivial implements SourceInfoProvider {
    @Override
    public String nameFor(Abstract.Definition definition) {
      return definition.getName();
    }

    @Override
    public ModuleID moduleOf(Abstract.Definition definition) {
      return null;
    }
  }
  public static final SourceInfoProvider TRIVIAL = new Trivial();
}
