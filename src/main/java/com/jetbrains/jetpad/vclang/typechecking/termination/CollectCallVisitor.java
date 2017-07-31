package com.jetbrains.jetpad.vclang.typechecking.termination;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.elimtree.Clause;
import com.jetbrains.jetpad.vclang.core.elimtree.ElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.IntervalElim;
import com.jetbrains.jetpad.vclang.core.elimtree.LeafElimTree;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.pattern.Pattern;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.visitor.ProcessDefCallsVisitor;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollectCallVisitor extends ProcessDefCallsVisitor<Void> {
  private final Set<BaseCallMatrix<Definition>> myCollectedCalls;
  private final FunctionDefinition myDefinition;
  private final Set<? extends Definition> myCycle;
  private List<Expression> myVector;

  CollectCallVisitor(FunctionDefinition def, Set<? extends Definition> cycle) {
    assert cycle != null;
    myDefinition = def;
    myCycle = cycle;
    myCollectedCalls = new HashSet<>();

    collectIntervals();
  }

  private void collectIntervals() {
    ElimTree elimTree;
    if (myDefinition.getBody() instanceof IntervalElim) {
      IntervalElim elim = (IntervalElim) myDefinition.getBody();
      myVector = new ArrayList<>();
      for (DependentLink link = elim.getParameters(); link.hasNext(); link = link.getNext()) {
        myVector.add(new ReferenceExpression(link));
      }

      int i = myVector.size() - elim.getCases().size();
      for (Pair<Expression, Expression> pair : elim.getCases()) {
        Expression old = myVector.get(i);
        myVector.set(i, ExpressionFactory.Left());
        pair.proj1.accept(this, null);
        myVector.set(i, ExpressionFactory.Right());
        pair.proj2.accept(this, null);
        myVector.set(i, old);
      }

      elimTree = elim.getOtherwise();
    } else {
      elimTree = (ElimTree) myDefinition.getBody();
    }

    if (elimTree instanceof LeafElimTree) {
      myVector = new ArrayList<>();
      for (DependentLink link = elimTree.getParameters(); link.hasNext(); link = link.getNext()) {
        myVector.add(new ReferenceExpression(link));
      }
      ((LeafElimTree) elimTree).getExpression().accept(this, null);
    }
  }

  public void collect(Clause clause) {
    if (clause.expression != null) {
      myVector = new ArrayList<>(clause.patterns.size());
      for (Pattern pattern : clause.patterns) {
        myVector.add(pattern.toExpression());
      }
      clause.expression.accept(this, null);
    }
  }

  public Set<BaseCallMatrix<Definition>> getResult() {
    return myCollectedCalls;
  }

  private static BaseCallMatrix.R isLess(Expression expr1, Expression expr2) {
    if (!(expr2 instanceof ConCallExpression)) {
      return expr1.equals(expr2) ? BaseCallMatrix.R.Equal : BaseCallMatrix.R.Unknown;
    }
    ConCallExpression conCall2 = (ConCallExpression) expr2;

    if (expr1 instanceof ConCallExpression) {
      ConCallExpression conCall1 = (ConCallExpression) expr1;
      if (conCall1.getDefinition() == conCall2.getDefinition()) {
        BaseCallMatrix.R ord = isLess(conCall1.getDefCallArguments(), conCall2.getDefCallArguments());
        if (ord != BaseCallMatrix.R.Unknown) return ord;
      }
    }
    for (Expression arg : conCall2.getDefCallArguments()) {
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
  protected boolean processDefCall(DefCallExpression expression, Void param) {
    if (!myCycle.contains(expression.getDefinition())) {
      return false;
    }

    BaseCallMatrix<Definition> cm = new CallMatrix(myDefinition, expression);
    assert cm.getHeight() == myVector.size();
    for (int i = 0; i < myVector.size(); i++) {
      for (int j = 0; j < expression.getDefCallArguments().size(); j++) {
        cm.set(i, j, compare(expression.getDefCallArguments().get(j), myVector.get(i)));
      }
    }

    myCollectedCalls.add(cm);
    return false;
  }
}
