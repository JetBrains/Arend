package org.arend.typechecking.error.local;

import org.arend.core.definition.ClassField;
import org.arend.error.doc.LineDoc;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import javax.annotation.Nonnull;

import static org.arend.error.doc.DocFactory.*;

public class FieldDependencyError extends TypecheckingError {
  public final ClassField field1;
  public final ClassField field2;

  public FieldDependencyError(ClassField field1, ClassField field2, @Nonnull Concrete.SourceNode cause) {
    super("", cause);
    this.field1 = field1;
    this.field2 = field2;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    return hList(text("Field '"), refDoc(field1.getReferable()), text("' depends on '"), refDoc(field2.getReferable()), text(" but is not implemented"));
  }
}
