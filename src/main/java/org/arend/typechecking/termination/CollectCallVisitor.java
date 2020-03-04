package org.arend.typechecking.termination;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.Definition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.elimtree.Body;
import org.arend.core.elimtree.ElimClause;
import org.arend.core.elimtree.IntervalElim;
import org.arend.core.expr.*;
import org.arend.core.pattern.*;
import org.arend.prelude.Prelude;
import org.arend.typechecking.visitor.ProcessDefCallsVisitor;
import org.arend.util.Pair;

import java.util.*;

public class CollectCallVisitor extends ProcessDefCallsVisitor<Void> {
  private final Set<BaseCallMatrix<Definition>> myCollectedCalls;
  private final FunctionDefinition myDefinition;
  private final Set<? extends Definition> myCycle;
  private List<? extends ExpressionPattern> myVector;

  CollectCallVisitor(FunctionDefinition def, Set<? extends Definition> cycle) {
    assert cycle != null;
    myDefinition = def;
    myCycle = cycle;
    myCollectedCalls = new HashSet<>();

    collectIntervals();
  }

  private void collectIntervals() {
    Body body = myDefinition.getActualBody();
    if (body instanceof IntervalElim) {
      IntervalElim elim = (IntervalElim) body;
      List<ExpressionPattern> vector = new ArrayList<>();
      for (DependentLink link = myDefinition.getParameters(); link.hasNext(); link = link.getNext()) {
        vector.add(new BindingPattern(link));
      }

      int i = vector.size() - elim.getCases().size();
      for (Pair<Expression, Expression> pair : elim.getCases()) {
        ExpressionPattern old = vector.get(i);
        vector.set(i, new ConstructorExpressionPattern(ExpressionFactory.Left(), Collections.emptyList()));
        pair.proj1.accept(this, null);
        vector.set(i, new ConstructorExpressionPattern(ExpressionFactory.Right(), Collections.emptyList()));
        pair.proj2.accept(this, null);
        vector.set(i, old);
      }

      myVector = vector;
    }

    Expression resultType = myDefinition.getResultType();
    if (resultType != null) {
      List<ExpressionPattern> vector = new ArrayList<>();

      for (DependentLink p = myDefinition.getParameters(); p.hasNext(); p = p.getNext()) {
        p = p.getNextTyped(null);
        vector.add(new BindingPattern(p));
      }

      myVector = vector;
      resultType.accept(this, null);
    }
  }

  public void collect(ElimClause<ExpressionPattern> clause) {
    if (clause.getExpression() != null) {
      myVector = clause.getPatterns();
      clause.getExpression().accept(this, null);
    }
  }

  public Set<BaseCallMatrix<Definition>> getResult() {
    return myCollectedCalls;
  }

  private static BaseCallMatrix.R isLess(Expression expr1, ExpressionPattern pattern2) {
    if (!(pattern2 instanceof ConstructorExpressionPattern)) {
      if (pattern2 instanceof BindingPattern) {
        DependentLink binding2 = ((BindingPattern) pattern2).getBinding();
        if (expr1 instanceof ReferenceExpression) {
          if (((ReferenceExpression) expr1).getBinding() == binding2) return BaseCallMatrix.R.Equal;
        } else if (expr1 instanceof AppExpression) {
          Expression function = expr1.getFunction();
          if (function instanceof ReferenceExpression && ((ReferenceExpression) function).getBinding() == binding2) return BaseCallMatrix.R.LessThan; // ensures that "e x < e"
        }
      }
      return BaseCallMatrix.R.Unknown;
    }
    ConstructorExpressionPattern conPattern = (ConstructorExpressionPattern) pattern2;

    List<? extends Expression> exprArguments = conPattern.getMatchingExpressionArguments(expr1, false);
    if (exprArguments != null) {
      BaseCallMatrix.R ord = isLess(exprArguments, conPattern.getSubPatterns());
      if (ord != BaseCallMatrix.R.Unknown) return ord;
    }

    for (ExpressionPattern arg : conPattern.getSubPatterns()) {
      if (isLess(expr1, arg) != BaseCallMatrix.R.Unknown) return BaseCallMatrix.R.LessThan;
    }
    return BaseCallMatrix.R.Unknown;
  }

  private static BaseCallMatrix.R isLess(List<? extends Expression> exprs1, List<? extends ExpressionPattern> patterns2) {
    for (int i = 0; i < Math.min(exprs1.size(), patterns2.size()); i++) {
      BaseCallMatrix.R ord = isLess(exprs1.get(i), patterns2.get(i));
      if (ord != BaseCallMatrix.R.Equal) return ord;
    }
    return exprs1.size() >= patterns2.size() ? BaseCallMatrix.R.Equal : BaseCallMatrix.R.Unknown;
  }

  private BaseCallMatrix.R compare(Expression argument, ExpressionPattern pattern) {
    // strip currentExpression of App & Proj calls
    while (true) {
      if (argument instanceof AppExpression) {
        argument = argument.getFunction();
      } else if (argument instanceof ProjExpression) {
        argument = ((ProjExpression) argument).getExpression();
      } else if (argument instanceof FunCallExpression && ((FunCallExpression) argument).getDefinition() == Prelude.AT) {
        argument = ((FunCallExpression) argument).getDefCallArguments().get(3);
      } else {
        break;
      }
    }

    return isLess(argument, pattern);
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
