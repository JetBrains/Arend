package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class ConstructorUniverseError extends LocalTypeCheckingError {
  public final Sort conSort;
  public final Concrete.Constructor constructor;
  public final Sort userSort;

  public ConstructorUniverseError(Sort conSort, Concrete.Constructor constructor, Sort userSort) {
    super("", constructor);
    this.conSort = conSort;
    this.constructor = constructor;
    this.userSort = userSort;
  }

  @Override
  public LineDoc getHeaderDoc(PrettyPrinterInfoProvider src) {
    return hList(super.getHeaderDoc(src), text(" The universe " + conSort + " of constructor '"), refDoc(constructor.getReferable()), text("' is not compatible with expected universe " + userSort));
  }
}
