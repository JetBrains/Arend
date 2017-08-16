package com.jetbrains.jetpad.vclang.term.provider;

import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.DefinitionLocator;

public interface SourceInfoProvider<SourceIdT extends SourceId> extends DefinitionLocator<SourceIdT>, PrettyPrinterInfoProvider {
  String fullNameFor(Abstract.GlobalReferableSourceNode definition);

  default String positionOf(Abstract.SourceNode sourceNode) {
    return null;
  }

  default String moduleOf(Abstract.SourceNode sourceNode) {
    return null;
  }

  class Trivial implements SourceInfoProvider {
    @Override
    public String fullNameFor(Abstract.GlobalReferableSourceNode definition) {
      return definition.getName();
    }

    @Override
    public SourceId sourceOf(Abstract.GlobalReferableSourceNode definition) {
      return null;
    }

    @Override
    public Abstract.Precedence precedenceOf(Abstract.GlobalReferableSourceNode referable) {
      return Abstract.Precedence.DEFAULT;
    }

    @Override
    public String nameFor(Abstract.ReferableSourceNode referable) {
      return referable.getName();
    }
  }
  SourceInfoProvider TRIVIAL = new Trivial();
}
