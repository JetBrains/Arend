package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Referable;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.listener.ResolveListener;

import java.util.List;

public class BinOpParser {
  private final ErrorReporter myErrorReporter;
  private final Abstract.BinOpSequenceExpression myBinOpExpression;
  private final ResolveListener myResolveListener;

  public BinOpParser(ErrorReporter errorReporter, Abstract.BinOpSequenceExpression binOpExpression, ResolveListener resolveListener) {
    myErrorReporter = errorReporter;
    myBinOpExpression = binOpExpression;
    myResolveListener = resolveListener;
  }

  public class StackElem {
    public Abstract.Expression argument;
    public Referable binOp;
    public Abstract.Definition.Precedence prec;
    public Abstract.DefCallExpression var;

    public StackElem(Abstract.Expression argument, Referable binOp, Abstract.Definition.Precedence prec, Abstract.DefCallExpression var) {
      this.argument = argument;
      this.binOp = binOp;
      this.prec = prec;
      this.var = var;
    }
  }

  public void pushOnStack(List<StackElem> stack, Abstract.Expression argument, Referable binOp, Abstract.Definition.Precedence prec, Abstract.DefCallExpression var) {
    StackElem elem = new StackElem(argument, binOp, prec, var);
    if (stack.isEmpty()) {
      stack.add(elem);
      return;
    }

    StackElem topElem = stack.get(stack.size() - 1);

    if (topElem.prec.priority < elem.prec.priority || (topElem.prec.priority == elem.prec.priority && topElem.prec.associativity == Abstract.Binding.Associativity.RIGHT_ASSOC && elem.prec.associativity == Abstract.Binding.Associativity.RIGHT_ASSOC)) {
      stack.add(elem);
      return;
    }

    if (!(topElem.prec.priority > elem.prec.priority || (topElem.prec.priority == elem.prec.priority && topElem.prec.associativity == Abstract.Binding.Associativity.LEFT_ASSOC && elem.prec.associativity == Abstract.Binding.Associativity.LEFT_ASSOC))) {
      String msg = "Precedence parsing error: cannot mix (" + topElem.binOp.getName() + ") [" + topElem.prec + "] and (" + elem.binOp.getName() + ") [" + elem.prec + "] in the same infix expression";
      myErrorReporter.report(new TypeCheckingError(msg, elem.var));
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
