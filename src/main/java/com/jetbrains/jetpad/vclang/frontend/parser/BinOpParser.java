package com.jetbrains.jetpad.vclang.frontend.parser;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.frontend.resolving.ResolveListener;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

public class BinOpParser {
  private final Abstract.BinOpSequenceExpression myBinOpExpression;
  private final ResolveListener myResolveListener;
  private final ErrorReporter myErrorReporter;

  public BinOpParser(Abstract.BinOpSequenceExpression binOpExpression, ResolveListener resolveListener, ErrorReporter errorReporter) {
    myBinOpExpression = binOpExpression;
    myResolveListener = resolveListener;
    myErrorReporter = errorReporter;
  }

  public class StackElem {
    public final Abstract.Expression argument;
    public final Abstract.Definition binOp;
    public final Abstract.Precedence prec;
    public final Abstract.ReferenceExpression var;

    public StackElem(Abstract.Expression argument, Abstract.Definition binOp, Abstract.Precedence prec, Abstract.ReferenceExpression var) {
      this.argument = argument;
      this.binOp = binOp;
      this.prec = prec;
      this.var = var;
    }
  }

  public void pushOnStack(List<StackElem> stack, Abstract.Expression argument, Abstract.Definition binOp, Abstract.Precedence prec, Abstract.ReferenceExpression var) {
    StackElem elem = new StackElem(argument, binOp, prec, var);
    if (stack.isEmpty()) {
      stack.add(elem);
      return;
    }

    StackElem topElem = stack.get(stack.size() - 1);

    if (topElem.prec.priority < elem.prec.priority || (topElem.prec.priority == elem.prec.priority && topElem.prec.associativity == Abstract.Precedence.Associativity.RIGHT_ASSOC && elem.prec.associativity == Abstract.Precedence.Associativity.RIGHT_ASSOC)) {
      stack.add(elem);
      return;
    }

    if (!(topElem.prec.priority > elem.prec.priority || (topElem.prec.priority == elem.prec.priority && topElem.prec.associativity == Abstract.Precedence.Associativity.LEFT_ASSOC && elem.prec.associativity == Abstract.Precedence.Associativity.LEFT_ASSOC))) {
      String msg = "Precedence parsing error: cannot mix (" + topElem.binOp.getName() + ") [" + topElem.prec + "] and (" + elem.binOp.getName() + ") [" + elem.prec + "] in the same infix expression";
      myErrorReporter.report(new GeneralError(msg, elem.var));
    }
    stack.remove(stack.size() - 1);
    pushOnStack(stack, myResolveListener.makeBinOp(myBinOpExpression, topElem.argument, topElem.binOp, topElem.var, elem.argument), elem.binOp, elem.prec, elem.var);
  }

  public Abstract.Expression rollUpStack(List<StackElem> stack, Abstract.Expression expr) {
    for (int i = stack.size() - 1; i >= 0; --i) {
      expr = myResolveListener.makeBinOp(myBinOpExpression, stack.get(i).argument, stack.get(i).binOp, stack.get(i).var, expr);
    }
    return expr;
  }
}
