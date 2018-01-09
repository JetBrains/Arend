package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class SuperClassError extends TypecheckingError {
  public Referable superClass;
  public Referable subClass;

  public SuperClassError(Referable superClass, Referable subClass, Concrete.SourceNode cause) {
    super("", cause);
    this.superClass = superClass;
    this.subClass = subClass;
  }

  @Override
  public LineDoc getHeaderDoc(PrettyPrinterConfig src) {
    return hList(super.getHeaderDoc(src), text(" "), refDoc(subClass), text(" does not extend "), refDoc(superClass));
  }
}
