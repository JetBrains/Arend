package org.arend.typechecking.termination;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.Definition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.elimtree.Body;
import org.arend.core.elimtree.Clause;
import org.arend.core.elimtree.IntervalElim;
import org.arend.core.expr.*;
import org.arend.core.pattern.BindingPattern;
import org.arend.core.pattern.ConstructorPattern;
import org.arend.core.pattern.Pattern;
import org.arend.core.pattern.Patterns;
import org.arend.prelude.Prelude;
import org.arend.typechecking.visitor.ProcessDefCallsVisitor;
import org.arend.util.Pair;

import java.util.*;

public class CollectCallVisitor extends ProcessDefCallsVisitor<Void> {
  private final Set<BaseCallMatrix<Definition>> myCollectedCalls;
  private final FunctionDefinition myDefinition;
  private final Set<? extends Definition> myCycle;
  private List<Pattern> myVector;

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
      myVector = new ArrayList<>();
      for (DependentLink link = myDefinition.getParameters(); link.hasNext(); link = link.getNext()) {
        myVector.add(new BindingPattern(link));
      }

      int i = myVector.size() - elim.getCases().size();
      for (Pair<Expression, Expression> pair : elim.getCases()) {
        Pattern old = myVector.get(i);
        myVector.set(i, new ConstructorPattern(ExpressionFactory.Left(), new Patterns(Collections.emptyList())));
        pair.proj1.accept(this, null);
        myVector.set(i, new ConstructorPattern(ExpressionFactory.Right(), new Patterns(Collections.emptyList())));
        pair.proj2.accept(this, null);
        myVector.set(i, old);
      }
    }

    Expression resultType = myDefinition.getResultType();
    if (resultType != null) {
      myVector = new ArrayList<>();

      for (DependentLink p = myDefinition.getParameters(); p.hasNext(); p = p.getNext()) {
        p = p.getNextTyped(null);
        myVector.add(new BindingPattern(p));
      }

      resultType.accept(this, null);
    }
  }

  public void collect(Clause clause) {
    if (clause.expression != null) {
      myVector = clause.patterns;
      clause.expression.accept(this, null);
    }
  }

  public Set<BaseCallMatrix<Definition>> getResult() {
    return myCollectedCalls;
  }

  private static BaseCallMatrix.R isLess(Expression expr1, Pattern pattern2) {
    if (!(pattern2 instanceof ConstructorPattern)) {
      return pattern2 instanceof BindingPattern && expr1 instanceof ReferenceExpression && ((ReferenceExpression) expr1).getBinding() == ((BindingPattern) pattern2).getBinding() ? BaseCallMatrix.R.Equal : BaseCallMatrix.R.Unknown;
    }
    ConstructorPattern conPattern = (ConstructorPattern) pattern2;

    List<? extends Expression> exprArguments = conPattern.getMatchingExpressionArguments(expr1, false);
    if (exprArguments != null) {
      BaseCallMatrix.R ord = isLess(exprArguments, conPattern.getArguments());
      if (ord != BaseCallMatrix.R.Unknown) return ord;
    }

    for (Pattern arg : conPattern.getPatterns().getPatternList()) {
      if (isLess(expr1, arg) != BaseCallMatrix.R.Unknown) return BaseCallMatrix.R.LessThan;
    }
    return BaseCallMatrix.R.Unknown;
  }

  private static BaseCallMatrix.R isLess(List<? extends Expression> exprs1, List<Pattern> patterns2) {
    for (int i = 0; i < Math.min(exprs1.size(), patterns2.size()); i++) {
      BaseCallMatrix.R ord = isLess(exprs1.get(i), patterns2.get(i));
      if (ord != BaseCallMatrix.R.Equal) return ord;
    }
    return exprs1.size() >= patterns2.size() ? BaseCallMatrix.R.Equal : BaseCallMatrix.R.Unknown;
  }

  private BaseCallMatrix.R compare(Expression argument, Pattern pattern) {
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
