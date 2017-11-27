package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

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
