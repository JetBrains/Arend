package org.arend.typechecking.error.local;

import org.arend.core.expr.Expression;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.doc.Doc;
import org.arend.term.concrete.Concrete;

import static org.arend.ext.prettyprinting.doc.DocFactory.*;

public class NotPiType extends TypecheckingError {
  public final Expression argument;
  public final Expression type;

  public NotPiType(Expression argument, Expression type, Concrete.SourceNode cause) {
    super("Expression is applied to an argument, but does not have a function type", cause);
    this.argument = argument;
    this.type = type;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterConfig ppConfig) {
    return vList(
      hang(text("Argument:"), termDoc(argument, ppConfig)),
      hang(text("Type:"), termDoc(type, ppConfig))
    );
  }

  @Override
  public boolean hasExpressions() {
    return true;
  }
}
