package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.DocFactory;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.SourceInfoProvider;

import java.util.List;

public class CycleError extends GeneralError {
  public final List<Abstract.Definition> cycle;

  public CycleError(List<Abstract.Definition> cycle) {
    super("Dependency cycle", cycle.get(0));
    this.cycle = cycle;
  }

  @Override
  public Doc getBodyDoc(SourceInfoProvider src) {
    StringBuilder builder = new StringBuilder();
    builder.append(cycle.get(cycle.size() - 1));
    for (Abstract.Definition definition : cycle) {
      builder.append(" - ");
      builder.append(definition.getName());
    }
    return DocFactory.text(builder.toString());
  }
}
