package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.provider.PrettyPrinterInfoProvider;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class SolveEquationError extends LocalTypeCheckingError {
  public final Expression expr1;
  public final Expression expr2;

  public SolveEquationError(Expression expr1, Expression expr2, Concrete.SourceNode expression) {
    super("Cannot solve equation", expression);
    this.expr1 = expr1;
    this.expr2 = expr2;
  }

  @Override
  public Doc getBodyDoc(PrettyPrinterInfoProvider src) {
    return vList(
      hang(text("1st expression:"), termDoc(expr1)),
      hang(text("2st expression:"), termDoc(expr2)));
  }
}
