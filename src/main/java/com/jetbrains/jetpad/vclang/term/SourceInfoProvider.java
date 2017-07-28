package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.module.source.SourceId;

public interface SourceInfoProvider<SourceIdT extends SourceId> extends DefinitionLocator<SourceIdT> {
  String nameFor(Abstract.Definition definition);

  default String positionOf(Abstract.SourceNode sourceNode) {
    return null;
  }

  default String moduleOf(Abstract.SourceNode sourceNode) {
    return null;
  }

  class Trivial implements SourceInfoProvider {
    @Override
    public String nameFor(Abstract.Definition definition) {
      return definition.getName();
    }

    @Override
    public SourceId sourceOf(Abstract.Definition definition) {
      return null;
    }
  }
  SourceInfoProvider TRIVIAL = new Trivial();
}
