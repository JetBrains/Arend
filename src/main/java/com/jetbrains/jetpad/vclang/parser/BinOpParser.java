package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;
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
    public ResolvedName name;
    public Definition.Precedence prec;
    public Abstract.DefCallExpression var;

    public StackElem(Abstract.Expression argument, ResolvedName name, Definition.Precedence prec,  Abstract.DefCallExpression var) {
      this.argument = argument;
      this.name = name;
      this.prec = prec;
      this.var = var;
    }
  }

  public void pushOnStack(List<StackElem> stack, Abstract.Expression argument, ResolvedName name, Definition.Precedence prec,  Abstract.DefCallExpression var) {
    StackElem elem = new StackElem(argument, name, prec, var);
    if (stack.isEmpty()) {
      stack.add(elem);
      return;
    }

    StackElem topElem = stack.get(stack.size() - 1);

    if (topElem.prec.priority < elem.prec.priority || (topElem.prec.priority == elem.prec.priority && topElem.prec.associativity == Definition.Associativity.RIGHT_ASSOC && elem.prec.associativity == Definition.Associativity.RIGHT_ASSOC)) {
      stack.add(elem);
      return;
    }

    if (!(topElem.prec.priority > elem.prec.priority || (topElem.prec.priority == elem.prec.priority && topElem.prec.associativity == Definition.Associativity.LEFT_ASSOC && elem.prec.associativity == Definition.Associativity.LEFT_ASSOC))) {
      String msg = "Precedence parsing error: cannot mix (" + topElem.name.name + ") [" + topElem.prec + "] and (" + elem.name.name + ") [" + elem.prec + "] in the same infix expression";
      myErrorReporter.report(new TypeCheckingError(msg, elem.var, null));
    }
    stack.remove(stack.size() - 1);
    pushOnStack(stack, myResolveListener.makeBinOp(myBinOpExpression, topElem.argument, topElem.name, topElem.var, elem.argument), elem.name, elem.prec, elem.var);
  }

  public Abstract.Expression rollUpStack(List<StackElem> stack, Abstract.Expression expr) {
    for (int i = stack.size() - 1; i >= 0; --i) {
      expr = myResolveListener.makeBinOp(myBinOpExpression, stack.get(i).argument, stack.get(i).name, stack.get(i).var, expr);
    }
    return expr;
  }
}
