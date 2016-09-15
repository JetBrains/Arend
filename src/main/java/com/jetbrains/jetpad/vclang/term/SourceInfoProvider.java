package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.module.source.ModuleSourceId;

public interface SourceInfoProvider {
  String nameFor(Abstract.Definition definition);
  ModuleSourceId sourceOf(Abstract.Definition definition);

  class Trivial implements SourceInfoProvider {
    @Override
    public String nameFor(Abstract.Definition definition) {
      return definition.getName();
    }

    @Override
    public ModuleSourceId sourceOf(Abstract.Definition definition) {
      return null;
    }
  }
  SourceInfoProvider TRIVIAL = new Trivial();
}
