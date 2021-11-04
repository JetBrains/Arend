package org.arend.typechecking.termination;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.Definition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.elimtree.Body;
import org.arend.core.elimtree.ElimClause;
import org.arend.core.elimtree.IntervalElim;
import org.arend.core.expr.*;
import org.arend.core.pattern.*;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.typechecking.visitor.SearchVisitor;
import org.arend.ext.util.Pair;

import java.util.*;

public class CollectCallVisitor extends SearchVisitor<Void> {
  private final Set<BaseCallMatrix<Definition>> myCollectedCalls;
  private final FunctionDefinition myDefinition;
  private final Set<? extends Definition> myCycle;
  private List<? extends ExpressionPattern> myPatterns;

  CollectCallVisitor(FunctionDefinition def, Set<? extends Definition> cycle) {
    assert cycle != null;
    myDefinition = def;
    myCycle = cycle;
    myCollectedCalls = new HashSet<>();

    Body body = def.getReallyActualBody();
    if (body instanceof IntervalElim) {
      IntervalElim elim = (IntervalElim) body;
      List<ExpressionPattern> patternList = new ArrayList<>();
      for (DependentLink link = myDefinition.getParameters(); link.hasNext(); link = link.getNext()) {
        patternList.add(new BindingPattern(link));
      }

      myPatterns = patternList;
      int i = patternList.size() - elim.getCases().size();

      for (Pair<Expression, Expression> pair : elim.getCases()) {
        ExpressionPattern old = patternList.get(i);
        patternList.set(i, new ConstructorExpressionPattern(ExpressionFactory.Left(), Collections.emptyList()));
        pair.proj1.accept(this, null);
        patternList.set(i, new ConstructorExpressionPattern(ExpressionFactory.Right(), Collections.emptyList()));
        pair.proj2.accept(this, null);
        patternList.set(i, old);
      }

      body = elim.getOtherwise();
    }

    List<ExpressionPattern> patternList = new ArrayList<>();
    for (DependentLink param = myDefinition.getParameters(); param.hasNext(); param = param.getNext()) {
      patternList.add(new BindingPattern(param));
    }

    myPatterns = patternList;
    myDefinition.getResultType().accept(this, null);
    if (body instanceof Expression) {
      ((Expression) body).accept(this, null);
    }
  }

  public void collect(ElimClause<ExpressionPattern> clause) {
    if (clause.getExpression() != null) {
      myPatterns = clause.getPatterns();
      clause.getExpression().accept(this, null);
    }
  }

  public Set<BaseCallMatrix<Definition>> getResult() {
    return myCollectedCalls;
  }

  private static BaseCallMatrix.R isLess(Expression expr1, ExpressionPattern pattern2) {
    if (pattern2 instanceof ConstructorExpressionPattern) {
      ConstructorExpressionPattern conPattern = (ConstructorExpressionPattern) pattern2;

      List<? extends Expression> exprArguments = conPattern.getMatchingExpressionArguments(expr1, false);
      for (ExpressionPattern arg : conPattern.getSubPatterns()) {
        if (isLess(expr1, arg) != BaseCallMatrix.R.Unknown) return BaseCallMatrix.R.LessThan;
      }

      if (exprArguments != null) {
        List<? extends ExpressionPattern> cpSubpatterns = conPattern.getSubPatterns();
        for (int i = 0; i < Math.min(exprArguments.size(), cpSubpatterns.size()); i++) {
          BaseCallMatrix.R ord = isLess(exprArguments.get(i), cpSubpatterns.get(i));
          if (ord != BaseCallMatrix.R.Equal) return ord;
        }

        if (exprArguments.size() >= cpSubpatterns.size()) return BaseCallMatrix.R.Equal;
        return BaseCallMatrix.R.Unknown;
      }

      return BaseCallMatrix.R.Unknown;
    } else if (pattern2 instanceof BindingPattern) {
      DependentLink binding2 = ((BindingPattern) pattern2).getBinding();
      if (expr1 instanceof ReferenceExpression) {
        if (((ReferenceExpression) expr1).getBinding() == binding2) return BaseCallMatrix.R.Equal;
      } else {
        expr1 = removeArgs(expr1);
        if (expr1 instanceof ReferenceExpression && ((ReferenceExpression) expr1).getBinding() == binding2)
          return BaseCallMatrix.R.LessThan; // ensures that "e x < e"
      }
    }
    return BaseCallMatrix.R.Unknown;
  }

  private static void initMatrixBlock(CallMatrix callMatrix,
                                      Expression expr1, DependentLink param1,
                                      ExpressionPattern pattern2, DependentLink param2) {
    DependentLink param1u = CallMatrix.tryUnfoldDependentLink(param1);
    DependentLink param2u = CallMatrix.tryUnfoldDependentLink(param2);
    List<? extends ExpressionPattern> patternList;
    List<? extends Expression> expressionList;
    if (param2u != null && pattern2 instanceof ConstructorExpressionPattern) {
      patternList = pattern2.getSubPatterns();
    } else {
      patternList = Collections.singletonList(pattern2);
    }
    expressionList = CallMatrix.tryUnfoldExpression(expr1);
    if (param1u == null || expressionList == null) {
      expressionList = Collections.singletonList(expr1);
      param1u = null;
    }

    if (param1u != null || param2u != null) {
      if (param1u == null) param1u = param1;
      if (param2u == null) param2u = param2;
      doProcessLists(callMatrix, param2u, patternList, param1u, expressionList);
      return;
    }

    callMatrix.setBlock(param2, param1, isLess(expr1, pattern2));
  }

  private static Expression removeArgs(Expression expr) {
    while (true) {
      if (expr instanceof AppExpression) {
        expr = expr.getFunction();
      } else if (expr instanceof ProjExpression) {
        expr = ((ProjExpression) expr).getExpression();
      } else if (expr instanceof AtExpression) {
        expr = ((AtExpression) expr).getPathArgument();
      } else if (expr instanceof FieldCallExpression) {
        expr = ((FieldCallExpression) expr).getArgument();
      } else {
        break;
      }
    }
    return expr;
  }

  private static void doProcessLists(CallMatrix cm, DependentLink patternIndex, List<? extends ExpressionPattern> patternList, DependentLink argumentIndex, List<? extends Expression> argumentList) {
    for (ExpressionPattern pattern : patternList) {
      DependentLink argIndex = argumentIndex;
      for (Expression argument : argumentList) {
        initMatrixBlock(cm, removeArgs(argument), argIndex, pattern, patternIndex);
        argIndex = argIndex.getNext();
      }
      patternIndex = patternIndex.getNext();
    }
  }

  @Override
  protected CoreExpression.FindAction processDefCall(DefCallExpression expression, Void param) {
    if (!myCycle.contains(expression.getDefinition())) {
      return CoreExpression.FindAction.CONTINUE;
    }

    CallMatrix cm = new CallMatrix(myDefinition, expression);
    doProcessLists(cm, myDefinition.getParameters(), myPatterns,
      expression.getDefinition().getParameters(), expression.getDefCallArguments());

    myCollectedCalls.add(cm);
    return CoreExpression.FindAction.CONTINUE;
  }

  @Override
  public Boolean visitTypeConstructor(TypeConstructorExpression expr, Void params) {
    if (!myCycle.contains(expr.getDefinition())) {
      return false;
    }
    processDefCall(expr.getType(), null);
    return false;
  }
}
