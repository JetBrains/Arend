package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.DocFactory;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class CycleError extends GeneralError {
  public final List<Concrete.Definition> cycle;

  public CycleError(List<Concrete.Definition> cycle) {
    super(Level.ERROR, "Dependency cycle");
    this.cycle = cycle;
  }

  @Override
  public Object getCause() {
    return cycle.get(0).getData();
  }

  @Override
  public Doc getCauseDoc(PrettyPrinterInfoProvider src) {
    return DocFactory.ppDoc(cycle.get(0), src);
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterInfoProvider src) {
    List<LineDoc> docs = new ArrayList<>(cycle.size() + 1);
    docs.add(refDoc(cycle.get(cycle.size() - 1).getReferable()));
    for (Concrete.Definition definition : cycle) {
      docs.add(refDoc(definition.getReferable()));
    }
    return hSep(text(" - "), docs);
  }
}
