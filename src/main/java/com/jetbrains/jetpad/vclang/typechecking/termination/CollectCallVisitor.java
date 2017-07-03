package com.jetbrains.jetpad.vclang.typechecking.termination;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.patternmatching.Util;
import com.jetbrains.jetpad.vclang.typechecking.visitor.ProcessDefCallsVisitor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollectCallVisitor extends ProcessDefCallsVisitor<List<Expression>> {
  private final Set<BaseCallMatrix<Definition>> myCollectedCalls = new HashSet<>();
  private final FunctionDefinition myDefinition;
  private final Set<? extends Definition> myCycle;

  CollectCallVisitor(FunctionDefinition def, Set<? extends Definition> cycle) {
    assert cycle != null;
    myDefinition = def;
    myCycle = cycle;
    if (def.getElimTree() != null)  {
      Util.ElimTreeWalker walker = new Util.ElimTreeWalker((list, expr) -> expr.accept(this, list));
      walker.walk(def.getElimTree());
    }
  }

  public Set<BaseCallMatrix<Definition>> getResult() {
    return myCollectedCalls;
  }

  private static BaseCallMatrix.R isLess(Expression expr1, Expression expr2) {
    if (expr2.toConCall() == null) {
      return expr1.equals(expr2) ? BaseCallMatrix.R.Equal : BaseCallMatrix.R.Unknown;
    }
    if (expr1.toConCall() != null && expr1.toConCall().getDefinition() == expr2.toConCall().getDefinition()) {
      BaseCallMatrix.R ord = isLess(expr1.toConCall().getDefCallArguments(), expr2.toConCall().getDefCallArguments());
      if (ord != BaseCallMatrix.R.Unknown) return ord;
    }
    for (Expression arg : expr2.toConCall().getDefCallArguments()) {
      if (isLess(expr1, arg) != BaseCallMatrix.R.Unknown) return BaseCallMatrix.R.LessThan;
    }
    return BaseCallMatrix.R.Unknown;
  }

  private static BaseCallMatrix.R isLess(List<? extends Expression> exprs1, List<? extends Expression> exprs2) {
    for (int i = 0; i < Math.min(exprs1.size(), exprs2.size()); i++) {
      BaseCallMatrix.R ord = isLess(exprs1.get(i), exprs2.get(i));
      if (ord != BaseCallMatrix.R.Equal) return ord;
    }
    return exprs1.size() >= exprs2.size() ? BaseCallMatrix.R.Equal : BaseCallMatrix.R.Unknown;
  }

  private BaseCallMatrix.R compare(Expression argument, Expression sample) {
    // strip currentExpression of App & Proj calls
    while (true) {
      if (argument instanceof AppExpression) {
        argument = ((AppExpression) argument).getFunction();
      } else if (argument instanceof ProjExpression) {
        argument = ((ProjExpression) argument).getExpression();
      } else if (argument instanceof FunCallExpression && ((FunCallExpression) argument).getDefinition() == Prelude.AT) {
        argument = ((FunCallExpression) argument).getDefCallArguments().get(3);
      } else {
        break;
      }
    }
    return isLess(argument, sample);
  }

  @Override
  protected boolean processDefCall(DefCallExpression expression, List<Expression> vector) {
    if (!myCycle.contains(expression.getDefinition())) {
      return false;
    }

    BaseCallMatrix<Definition> cm = new CallMatrix(myDefinition, expression);
    assert cm.getHeight() == vector.size();
    for (int i = 0; i < vector.size(); i++) {
      for (int j = 0; j < expression.getDefCallArguments().size(); j++) {
        cm.set(i, j, compare(expression.getDefCallArguments().get(j), vector.get(i)));
      }
    }

    myCollectedCalls.add(cm);
    return false;
  }
}
