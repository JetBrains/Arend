package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.DocFactory;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

import java.util.List;

public class CycleError<T> extends GeneralError<T> {
  public final List<Concrete.Definition<T>> cycle;

  public CycleError(List<Concrete.Definition<T>> cycle) {
    super(Level.ERROR, "Dependency cycle");
    this.cycle = cycle;
  }

  @Override
  public T getCause() {
    return cycle.get(0).getData();
  }

  @Override
  public PrettyPrintable getCausePP() {
    return cycle.get(0);
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterInfoProvider src) {
    StringBuilder builder = new StringBuilder();
    builder.append(cycle.get(cycle.size() - 1));
    for (Concrete.Definition<T> definition : cycle) {
      builder.append(" - ");
      builder.append(definition.textRepresentation());
    }
    return DocFactory.text(builder.toString());
  }
}
