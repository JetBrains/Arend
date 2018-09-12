package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class ImplementationReferenceError extends TypecheckingError {
  public ClassField classField;

  public ImplementationReferenceError(ClassField classField, Concrete.SourceNode cause) {
    super("The implementation refers to a non-implemented field ", cause);
    this.classField = classField;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig src) {
    return hList(text(message), refDoc(classField.getReferable()));
  }
}
