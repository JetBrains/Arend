package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class FieldsDependentImplementationError extends TypecheckingError {
  public GlobalReferable implementedField;
  public GlobalReferable referredField;

  public FieldsDependentImplementationError(GlobalReferable implementedField, GlobalReferable referredField, Concrete.SourceNode cause) {
    super("", cause);
    this.implementedField = implementedField;
    this.referredField = referredField;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig src) {
    return hList(text("Field "), refDoc(implementedField), text(" cannot be implemented since it depends on field "), refDoc(referredField), text(" which is not implemented"));
  }
}
