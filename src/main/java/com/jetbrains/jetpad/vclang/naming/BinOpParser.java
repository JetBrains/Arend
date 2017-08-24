package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.error.NamingError;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Precedence;

import java.util.List;

public class BinOpParser<T> {
  private final Concrete.BinOpSequenceExpression<T> myBinOpExpression;
  private final ErrorReporter<T> myErrorReporter;

  public BinOpParser(Concrete.BinOpSequenceExpression<T> binOpExpression, ErrorReporter<T> errorReporter) {
    myBinOpExpression = binOpExpression;
    myErrorReporter = errorReporter;
  }

  public static class StackElem<T> {
    public final Concrete.Expression<T> argument;
    public final Referable binOp;
    public final Precedence prec;
    public final Concrete.ReferenceExpression<T> var;

    public StackElem(Concrete.Expression<T> argument, Referable binOp, Precedence prec, Concrete.ReferenceExpression<T> var) {
      this.argument = argument;
      this.binOp = binOp;
      this.prec = prec;
      this.var = var;
    }
  }

  public void pushOnStack(List<StackElem<T>> stack, Concrete.Expression<T> argument, Referable binOp, Precedence prec, Concrete.ReferenceExpression<T> var, boolean ignoreAssoc) {
    if (stack.isEmpty()) {
      stack.add(new StackElem<>(argument, binOp, prec, var));
      return;
    }

    StackElem<T> topElem = stack.get(stack.size() - 1);

    if (argument != null) {
      if (topElem.prec.priority < prec.priority || (topElem.prec.priority == prec.priority && topElem.prec.associativity == Precedence.Associativity.RIGHT_ASSOC && (ignoreAssoc || prec.associativity == Precedence.Associativity.RIGHT_ASSOC))) {
        stack.add(new StackElem<>(argument, binOp, prec, var));
        return;
      }

      if (!(topElem.prec.priority > prec.priority || (topElem.prec.priority == prec.priority && topElem.prec.associativity == Precedence.Associativity.LEFT_ASSOC && (ignoreAssoc || prec.associativity == Precedence.Associativity.LEFT_ASSOC)))) {
        String msg = "Precedence parsing error: cannot mix (" + topElem.binOp.textRepresentation() + ") [" + topElem.prec + "] and (" + binOp.textRepresentation() + ") [" + prec + "] in the same infix expression";
        myErrorReporter.report(new NamingError<>(msg, var));
      }
    }

    stack.remove(stack.size() - 1);
    pushOnStack(stack, myBinOpExpression.makeBinOp(topElem.argument, topElem.binOp, topElem.var, argument), binOp, prec, var, ignoreAssoc);
  }

  public Concrete.Expression<T> rollUpStack(List<StackElem<T>> stack, Concrete.Expression<T> expr) {
    for (int i = stack.size() - 1; i >= 0; --i) {
      expr = myBinOpExpression.makeBinOp(stack.get(i).argument, stack.get(i).binOp, stack.get(i).var, expr);
    }
    return expr;
  }
}
