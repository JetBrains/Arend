package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.error.NamingError;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.Precedence;

import java.util.List;

public class BinOpParser {
  private final Concrete.BinOpSequenceExpression myBinOpExpression;
  private final ErrorReporter myErrorReporter;

  public BinOpParser(Concrete.BinOpSequenceExpression binOpExpression, ErrorReporter errorReporter) {
    myBinOpExpression = binOpExpression;
    myErrorReporter = errorReporter;
  }

  public static class StackElem {
    public final Concrete.Expression argument;
    public final Referable binOp;
    public final Precedence prec;
    public final Concrete.ReferenceExpression var;

    public StackElem(Concrete.Expression argument, Referable binOp, Precedence prec, Concrete.ReferenceExpression var) {
      this.argument = argument;
      this.binOp = binOp;
      this.prec = prec;
      this.var = var;
    }
  }

  public void pushOnStack(List<StackElem> stack, Concrete.Expression argument, Referable binOp, Precedence prec, Concrete.ReferenceExpression var, boolean ignoreAssoc) {
    if (stack.isEmpty()) {
      stack.add(new StackElem(argument, binOp, prec, var));
      return;
    }

    StackElem topElem = stack.get(stack.size() - 1);

    if (argument != null) {
      if (topElem.prec.priority < prec.priority || (topElem.prec.priority == prec.priority && topElem.prec.associativity == Precedence.Associativity.RIGHT_ASSOC && (ignoreAssoc || prec.associativity == Precedence.Associativity.RIGHT_ASSOC))) {
        stack.add(new StackElem(argument, binOp, prec, var));
        return;
      }

      if (!(topElem.prec.priority > prec.priority || (topElem.prec.priority == prec.priority && topElem.prec.associativity == Precedence.Associativity.LEFT_ASSOC && (ignoreAssoc || prec.associativity == Precedence.Associativity.LEFT_ASSOC)))) {
        String msg = "Precedence parsing error: cannot mix " + topElem.binOp.textRepresentation() + " [" + topElem.prec + "] and " + binOp.textRepresentation() + " [" + prec + "] in the same infix expression";
        myErrorReporter.report(new NamingError(msg, var));
      }
    }

    stack.remove(stack.size() - 1);
    pushOnStack(stack, myBinOpExpression.makeBinOp(topElem.argument, topElem.binOp, topElem.var, argument), binOp, prec, var, ignoreAssoc);
  }

  public Concrete.Expression rollUpStack(List<StackElem> stack, Concrete.Expression expr) {
    for (int i = stack.size() - 1; i >= 0; --i) {
      expr = myBinOpExpression.makeBinOp(stack.get(i).argument, stack.get(i).binOp, stack.get(i).var, expr);
    }
    return expr;
  }
}
