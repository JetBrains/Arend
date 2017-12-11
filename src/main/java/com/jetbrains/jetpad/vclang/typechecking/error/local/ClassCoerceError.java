package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import java.util.Set;
import java.util.stream.Collectors;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class ClassCoerceError extends TypecheckingError {
  public Set<ClassField> coercingFields;

  public ClassCoerceError(Set<ClassField> coercingFields, Concrete.SourceNode cause) {
    super(Level.WARNING, "A class can have at most one coercing field, but this class has the following: ", cause);
    this.coercingFields = coercingFields;
  }

  @Override
  public LineDoc getHeaderDoc(PrettyPrinterConfig src) {
    return hList(super.getHeaderDoc(src), hSep(text(", "), coercingFields.stream().map(field -> refDoc(field.getReferable())).collect(Collectors.toList())));
  }
}
