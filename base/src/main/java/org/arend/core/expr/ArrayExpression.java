package org.arend.core.expr;

import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.core.sort.Sort;
import org.arend.core.subst.LevelSubstitution;
import org.arend.ext.core.expr.CoreArrayExpression;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ArrayExpression extends Expression implements CoreArrayExpression {
  private Sort mySortArg;
  private final Expression myElementsType;
  private final List<Expression> myElements;
  private final Expression myTail;

  private ArrayExpression(Sort sortArg, Expression elementsType, List<Expression> elements, Expression tail) {
    mySortArg = sortArg;
    myElementsType = elementsType;
    myElements = elements;
    myTail = tail;
  }

  public static Expression make(Sort sortArg, Expression elementsType, List<Expression> elements, Expression tail) {
    if (tail instanceof ArrayExpression) {
      List<Expression> newElements = new ArrayList<>(elements.size() + ((ArrayExpression) tail).myElements.size());
      newElements.addAll(elements);
      newElements.addAll(((ArrayExpression) tail).myElements);
      return new ArrayExpression(sortArg, elementsType, newElements, ((ArrayExpression) tail).myTail);
    } else {
      return tail != null && elements.isEmpty() ? tail : new ArrayExpression(sortArg, elementsType, elements, tail);
    }
  }

  public void substSort(LevelSubstitution substitution) {
    mySortArg = mySortArg.subst(substitution);
  }

  public Expression drop(int n) {
    assert n <= myElements.size();
    if (n >= myElements.size()) {
      return myTail == null ? new ArrayExpression(mySortArg, myElementsType, Collections.emptyList(), null) : myTail;
    } else {
      return new ArrayExpression(mySortArg, myElementsType, myElements.subList(n, myElements.size()), myTail);
    }
  }

  public List<Expression> getConstructorArguments(boolean withElementsType) {
    return myElements.isEmpty() ? (withElementsType ? Collections.singletonList(myElementsType) : Collections.emptyList()) : (withElementsType ? Arrays.asList(myElementsType, myElements.get(0), drop(1)) : Arrays.asList(myElements.get(0), drop(1)));
  }

  @Override
  public @NotNull Expression getElementsType() {
    return myElementsType;
  }

  @Override
  public @NotNull Sort getSortArgument() {
    return mySortArg;
  }

  @Override
  public @NotNull List<Expression> getElements() {
    return myElements;
  }

  @Override
  public @Nullable Expression getTail() {
    return myTail;
  }

  @Override
  public <P, R> R accept(@NotNull CoreExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitArray(this, params);
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitArray(this, params);
  }

  @Override
  public <P1, P2, R> R accept(ExpressionVisitor2<? super P1, ? super P2, ? extends R> visitor, P1 param1, P2 param2) {
    return visitor.visitArray(this, param1, param2);
  }

  @Override
  public Decision isWHNF() {
    return Decision.YES;
  }

  @Override
  public Expression getStuckExpression() {
    return null;
  }
}
