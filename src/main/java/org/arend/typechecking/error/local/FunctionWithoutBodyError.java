package org.arend.typechecking.error.local;

import org.arend.core.definition.FunctionDefinition;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.LineDoc;
import org.arend.term.concrete.Concrete;

import javax.annotation.Nonnull;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class FunctionWithoutBodyError extends TypecheckingError {
  public final FunctionDefinition definition;

  public FunctionWithoutBodyError(FunctionDefinition definition, @Nonnull Concrete.SourceNode cause) {
    super("", cause);
    this.definition = definition;
  }

  @Override
  public LineDoc getShortHeaderDoc(PrettyPrinterConfig ppConfig) {
    return hList(text("Function '"), refDoc(definition.getReferable()), text("' does not have a body"));
  }
}
