package org.arend.typechecking.error.local;

import org.arend.core.expr.Expression;
import org.arend.error.doc.Doc;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrinterConfig;

import static org.arend.error.doc.DocFactory.*;

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
}
