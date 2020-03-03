package org.arend.typechecking.error.local;

import org.arend.core.definition.ClassField;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.naming.reference.TCFieldReferable;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class AnotherClassifyingFieldError extends TypecheckingError {
  public final TCFieldReferable candidate;
  public final ClassField actual;

  public AnotherClassifyingFieldError(TCFieldReferable candidate, ClassField actual, @NotNull Concrete.SourceNode cause) {
    super("", cause);
    this.candidate = candidate;
    this.actual = actual;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    return hList(text("Field '"), refDoc(candidate), text("' cannot be a classifying field since the class already has one: '"), refDoc(actual.getReferable()), text("'"));
  }
}
