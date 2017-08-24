package com.jetbrains.jetpad.vclang.term.provider;

import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.DefinitionLocator;
import com.jetbrains.jetpad.vclang.term.Precedence;

public interface SourceInfoProvider<SourceIdT extends SourceId> extends DefinitionLocator<SourceIdT>, PrettyPrinterInfoProvider {
  String fullNameFor(GlobalReferable definition);

  class Trivial implements SourceInfoProvider {
    @Override
    public String fullNameFor(GlobalReferable definition) {
      return definition.textRepresentation();
    }

    @Override
    public SourceId sourceOf(GlobalReferable definition) {
      return null;
    }

    @Override
    public Precedence precedenceOf(GlobalReferable referable) {
      return Precedence.DEFAULT;
    }

    @Override
    public String nameFor(Referable referable) {
      return referable.textRepresentation();
    }
  }
  SourceInfoProvider TRIVIAL = new Trivial();
}
