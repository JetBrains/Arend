package org.arend.typechecking.error.local;

import org.arend.core.definition.ClassField;
import org.arend.error.doc.LineDoc;
import org.arend.naming.reference.TCFieldReferable;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import javax.annotation.Nonnull;

import static org.arend.error.doc.DocFactory.*;

public class AnotherClassifyingFieldError extends TypecheckingError {
  public final TCFieldReferable candidate;
  public final ClassField actual;

  public AnotherClassifyingFieldError(TCFieldReferable candidate, ClassField actual, @Nonnull Concrete.SourceNode cause) {
    super("", cause);
    this.candidate = candidate;
    this.actual = actual;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    return hList(text("Field '"), refDoc(candidate), text("' cannot be a classifying field since the class already has one: '"), refDoc(actual.getReferable()), text("'"));
  }
}
