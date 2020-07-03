package org.arend.typechecking.error.local;

import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.DocFactory;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.naming.reference.FieldReferable;
import org.arend.naming.reference.TCReferable;
import org.arend.term.concrete.Concrete;

import java.util.Collection;
import java.util.stream.Collectors;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class FieldsImplementationError extends TypecheckingError {
  public boolean alreadyImplemented;
  public TCReferable classReferable;
  public Collection<? extends FieldReferable> fields;

  public FieldsImplementationError(boolean alreadyImplemented, TCReferable classReferable, Collection<? extends FieldReferable> fields, Concrete.SourceNode cause) {
    super("The following fields are " + (alreadyImplemented ? "already" : "not") + " implemented: ", cause);
    this.alreadyImplemented = alreadyImplemented;
    this.classReferable = classReferable;
    this.fields = fields;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig src) {
    return hList(text(message), hSep(text(", "), fields.stream().map(DocFactory::refDoc).collect(Collectors.toList())));
  }
}
