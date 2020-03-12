package org.arend.typechecking.error.local;

import org.arend.core.expr.Expression;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class TypeFromFieldError extends TypecheckingError {
  public final Expression type;

  public TypeFromFieldError(String message, Expression type, @NotNull Concrete.SourceNode cause) {
    super(message, cause);
    this.type = type;
  }

  public static String parameter() {
    return "Cannot infer the type of the parameter since the candidate has references to \\this parameter";
  }

  public static String resultType() {
    return "Cannot infer the result type since the candidate has references to \\this parameter";
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return hang(text("Candidate:"), termDoc(type, ppConfig));
  }

  @Override
  public boolean hasExpressions() {
    return true;
  }
}
