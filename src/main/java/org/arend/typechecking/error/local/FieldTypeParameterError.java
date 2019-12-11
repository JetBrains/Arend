package org.arend.typechecking.error.local;

import org.arend.core.expr.Expression;
import org.arend.error.doc.Doc;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import javax.annotation.Nonnull;

import static org.arend.error.doc.DocFactory.*;

public class FieldTypeParameterError extends TypecheckingError {
  public final Expression fieldType;

  public FieldTypeParameterError(Expression type, @Nonnull Concrete.SourceNode cause) {
    super("Field type does not have enough parameters", cause);
    fieldType = type;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return hang(text("Field type:"), termDoc(fieldType, ppConfig));
  }

  @Override
  public boolean isShort() {
    return true;
  }
}
