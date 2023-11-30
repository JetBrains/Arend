package org.arend.typechecking.error.local;

import org.arend.core.definition.ClassField;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.term.concrete.Concrete;

import java.util.ArrayList;
import java.util.List;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class FieldCycleError extends TypecheckingError {
  public final List<? extends ClassField> cycle;
  public final Concrete.SourceNode cause;

  public FieldCycleError(List<? extends ClassField> cycle, Concrete.SourceNode cause) {
    super("Field implementation cycle", cause);
    this.cycle = cycle;
    this.cause = cause;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig src) {
    List<LineDoc> docs = new ArrayList<>(cycle.size());
    for (ClassField field : cycle) {
      docs.add(refDoc(field.getRef()));
    }
    return hSep(text(" - "), docs);
  }
}
