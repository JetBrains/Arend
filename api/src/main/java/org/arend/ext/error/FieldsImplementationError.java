package org.arend.ext.error;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.ext.reference.ArendRef;

import java.util.Collection;
import java.util.stream.Collectors;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class FieldsImplementationError extends TypecheckingError {
  public final boolean alreadyImplemented;
  public final ArendRef classRef;
  public final Collection<? extends ArendRef> fields;

  public FieldsImplementationError(boolean alreadyImplemented, ArendRef classRef, Collection<? extends ArendRef> fields, ConcreteSourceNode cause) {
    super("The following fields are " + (alreadyImplemented ? "already" : "not") + " implemented: ", cause);
    this.alreadyImplemented = alreadyImplemented;
    this.classRef = classRef;
    this.fields = fields;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig src) {
    return hList(text(message), hSep(text(", "), fields.stream().map(DocFactory::refDoc).collect(Collectors.toList())));
  }
}
