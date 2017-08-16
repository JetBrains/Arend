package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.provider.SourceInfoProvider;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class ConstructorUniverseError extends LocalTypeCheckingError {
  public final Sort conSort;
  public final Abstract.Constructor constructor;
  public final Sort userSort;

  public ConstructorUniverseError(Sort conSort, Abstract.Constructor constructor, Sort userSort) {
    super("", constructor);
    this.conSort = conSort;
    this.constructor = constructor;
    this.userSort = userSort;
  }

  @Override
  public LineDoc getHeaderDoc(SourceInfoProvider src) {
    return hList(super.getHeaderDoc(src), text(" The universe " + conSort + " of constructor '"), refDoc(constructor), text("' is not compatible with expected universe " + userSort));
  }
}
