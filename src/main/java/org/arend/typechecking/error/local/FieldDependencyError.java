package org.arend.typechecking.error.local;

import org.arend.core.definition.ClassField;
import org.arend.error.doc.LineDoc;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.arend.error.doc.DocFactory.*;

public class FieldDependencyError extends TypecheckingError {
  public final ClassField field;
  public final Set<ClassField> fields;

  public FieldDependencyError(ClassField field, Set<ClassField> fields, @Nonnull Concrete.SourceNode cause) {
    super("", cause);
    this.field = field;
    this.fields = fields;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    List<LineDoc> fieldDocs = new ArrayList<>();
    for (ClassField field : fields) {
      fieldDocs.add(refDoc(field.getReferable()));
    }
    return hList(text("Field '"), refDoc(field.getReferable()), text("' depends on "), hSep(text(", "), fieldDocs), text(" but is not implemented"));
  }
}
