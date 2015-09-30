package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.module.DefinitionPair;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

import java.util.List;

public class BinOpParser {
  private final ErrorReporter myErrorReporter;
  private final Abstract.BinOpSequenceExpression myBinOpExpression;

  public BinOpParser(ErrorReporter errorReporter, Abstract.BinOpSequenceExpression binOpExpression) {
    myErrorReporter = errorReporter;
    myBinOpExpression = binOpExpression;
  }

  public class StackElem {
    public Abstract.Expression argument;
    public DefinitionPair binOp;
    public Abstract.DefCallExpression var;

    public StackElem(Abstract.Expression argument, DefinitionPair binOp, Abstract.DefCallExpression var) {
      this.argument = argument;
      this.binOp = binOp;
      this.var = var;
    }
  }

  public void pushOnStack(List<StackElem> stack, Abstract.Expression argument, DefinitionPair binOp, Abstract.DefCallExpression var) {
    StackElem elem = new StackElem(argument, binOp, var);
    if (stack.isEmpty()) {
      stack.add(elem);
      return;
    }

    StackElem topElem = stack.get(stack.size() - 1);
    Definition.Precedence prec = topElem.binOp.getPrecedence();
    Definition.Precedence prec2 = elem.binOp.getPrecedence();

    if (prec.priority < prec2.priority || (prec.priority == prec2.priority && prec.associativity == Definition.Associativity.RIGHT_ASSOC && prec2.associativity == Definition.Associativity.RIGHT_ASSOC)) {
      stack.add(elem);
      return;
    }

    if (!(prec.priority > prec2.priority || (prec.priority == prec2.priority && prec.associativity == Definition.Associativity.LEFT_ASSOC && prec2.associativity == Definition.Associativity.LEFT_ASSOC))) {
      String msg = "Precedence parsing error: cannot mix (" + topElem.binOp.namespace.getName().name + ") [" + prec + "] and (" + elem.binOp.namespace.getName().name + ") [" + prec2 + "] in the same infix expression";
      myErrorReporter.report(new TypeCheckingError(null, msg, elem.var, null));
    }
    stack.remove(stack.size() - 1);
    pushOnStack(stack, myBinOpExpression.makeBinOp(topElem.argument, topElem.binOp, topElem.var, elem.argument), elem.binOp, elem.var);
  }

  public Abstract.Expression rollUpStack(List<StackElem> stack, Abstract.Expression expr) {
    for (int i = stack.size() - 1; i >= 0; --i) {
      expr = myBinOpExpression.makeBinOp(stack.get(i).argument, stack.get(i).binOp, stack.get(i).var, expr);
    }
    return expr;
  }
}
