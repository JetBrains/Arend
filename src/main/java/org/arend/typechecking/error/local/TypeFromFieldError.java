package org.arend.typechecking.error.local;

import org.arend.core.expr.Expression;
import org.arend.error.doc.Doc;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import javax.annotation.Nonnull;

import static org.arend.error.doc.DocFactory.*;

public class TypeFromFieldError extends TypecheckingError {
  public final Expression type;

  public TypeFromFieldError(String message, Expression type, @Nonnull Concrete.SourceNode cause) {
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
  public boolean isShort() {
    return true;
  }
}
