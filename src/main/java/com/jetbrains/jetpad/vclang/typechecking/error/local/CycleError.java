package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.DocFactory;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

import java.util.List;

public class CycleError<T> extends GeneralError<T> {
  public final List<Abstract.Definition> cycle;

  // TODO[abstract]: override getCause and getCausePP
  public CycleError(List<Abstract.Definition> cycle) {
    super(Level.ERROR, "Dependency cycle");
    this.cycle = cycle;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterInfoProvider src) {
    StringBuilder builder = new StringBuilder();
    builder.append(cycle.get(cycle.size() - 1));
    for (Abstract.Definition definition : cycle) {
      builder.append(" - ");
      builder.append(definition.getName());
    }
    return DocFactory.text(builder.toString());
  }
}
