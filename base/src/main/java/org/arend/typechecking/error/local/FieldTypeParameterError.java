package org.arend.typechecking.error.local;

import org.arend.core.expr.Expression;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class FieldTypeParameterError extends TypecheckingError {
  public final Expression fieldType;

  public FieldTypeParameterError(Expression type, @NotNull Concrete.SourceNode cause) {
    super("Field type does not have enough parameters", cause);
    fieldType = type;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return hang(text("Field type:"), termDoc(fieldType, ppConfig));
  }

  @Override
  public boolean hasExpressions() {
    return true;
  }
}
