package org.arend.core.expr;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.core.sort.Level;
import org.arend.core.subst.LevelPair;
import org.arend.ext.core.level.LevelSubstitution;
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
  private LevelPair myLevels;
  private final Expression myElementsType;
  private final List<Expression> myElements;
  private final Expression myTail;

  private ArrayExpression(LevelPair levels, Expression elementsType, List<Expression> elements, Expression tail) {
    myLevels = levels;
    myElementsType = elementsType;
    myElements = elements;
    myTail = tail;
  }

  public static Expression make(LevelPair levels, Expression elementsType, List<Expression> elements, Expression tail) {
    if (tail instanceof ArrayExpression) {
      List<Expression> newElements = new ArrayList<>(elements.size() + ((ArrayExpression) tail).myElements.size());
      newElements.addAll(elements);
      newElements.addAll(((ArrayExpression) tail).myElements);
      return new ArrayExpression(levels, elementsType, newElements, ((ArrayExpression) tail).myTail);
    } else {
      return tail != null && elements.isEmpty() ? tail : new ArrayExpression(levels, elementsType, elements, tail);
    }
  }

  public static ArrayExpression makeArray(LevelPair levels, Expression elementsType, List<Expression> elements, Expression tail) {
    return (ArrayExpression) make(levels, elementsType, elements, tail);
  }

  public void substLevels(LevelSubstitution substitution) {
    myLevels = myLevels.subst(substitution);
  }

  public Expression drop(int n) {
    assert n <= myElements.size();
    if (n >= myElements.size()) {
      return myTail == null ? new ArrayExpression(myLevels, myElementsType, Collections.emptyList(), null) : myTail;
    } else {
      return new ArrayExpression(myLevels, myElementsType, myElements.subList(n, myElements.size()), myTail);
    }
  }

  public List<Expression> getConstructorArguments(boolean withElementsType) {
    return myElements.isEmpty() ? (withElementsType ? Collections.singletonList(myElementsType) : Collections.emptyList()) : (withElementsType ? Arrays.asList(myElementsType, myElements.get(0), drop(1)) : Arrays.asList(myElements.get(0), drop(1)));
  }

  @Override
  public @NotNull Expression getElementsType() {
    return myElementsType;
  }

  public @NotNull LevelPair getLevels() {
    return myLevels;
  }

  @Override
  public @NotNull Level getPLevel() {
    return myLevels.get(LevelVariable.PVAR);
  }

  @Override
  public @NotNull Level getHLevel() {
    return myLevels.get(LevelVariable.HVAR);
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
