package org.arend.core.expr;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.param.TypedSingleDependentLink;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.ExpressionVisitor2;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.LevelPair;
import org.arend.core.subst.ListLevels;
import org.arend.ext.core.level.LevelSubstitution;
import org.arend.ext.core.expr.CoreArrayExpression;
import org.arend.ext.core.expr.CoreExpressionVisitor;
import org.arend.prelude.Prelude;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.arend.core.expr.ExpressionFactory.Fin;
import static org.arend.core.expr.ExpressionFactory.Suc;

public class ArrayExpression extends Expression implements CoreArrayExpression {
  private LevelPair myLevels;
  private final Expression myElementsType;
  private final List<Expression> myElements;
  private final Expression myTail;
  private Boolean myValue;

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

  @Override
  public boolean isValue() {
    if (myValue == null) {
      boolean isValue = myTail == null || myTail.isValue();
      if (isValue) {
        for (Expression element : myElements) {
          if (!element.isValue()) {
            isValue = false;
            break;
          }
        }
      }
      myValue = isValue;
    }
    return myValue;
  }

  public void substLevels(LevelSubstitution substitution) {
    myLevels = myLevels.subst(substitution);
  }

  public void setLevels(LevelPair levels) {
    myLevels = levels;
  }

  public Expression drop(int n) {
    assert n <= myElements.size();
    if (n == 0) return this;
    if (n >= myElements.size() && myTail != null) {
      return myTail;
    }

    Expression newElementsLength = new SmallIntegerExpression(getElements().size() - n);
    TypedSingleDependentLink param = new TypedSingleDependentLink(true, "j", Fin(myTail == null ? newElementsLength : FunCallExpression.make(Prelude.PLUS, new ListLevels(Collections.emptyList()), Arrays.asList(FieldCallExpression.make(Prelude.ARRAY_LENGTH, myTail), newElementsLength))));
    Expression index = new ReferenceExpression(param);
    for (int i = 0; i < n; i++) {
      index = Suc(index);
    }
    return new ArrayExpression(myLevels, new LamExpression(myLevels.toSort().max(Sort.SET0), param, AppExpression.make(myElementsType, index, true)), myElements.subList(n, myElements.size()), myTail);
  }

  public Expression getLength() {
    if (myTail == null) {
      return new SmallIntegerExpression(myElements.size());
    }

    Expression result = FieldCallExpression.make(Prelude.ARRAY_LENGTH, myTail);
    for (Expression ignored : myElements) {
      result = ExpressionFactory.Suc(result);
    }
    return result;
  }

  public List<Expression> getConstructorArguments(boolean withElementsType, boolean withLength) {
    if (myElements.isEmpty()) {
      return withElementsType ? Collections.singletonList(myElementsType) : Collections.emptyList();
    }

    List<Expression> result = new ArrayList<>(4);
    if (withLength) result.add(getLength());
    if (withElementsType) result.add(myElementsType);
    result.add(myElements.get(0));
    result.add(drop(1));
    return result;
  }

  public boolean isEmpty() {
    return myTail == null && myElements.isEmpty();
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
    return myTail == null ? Decision.YES : myTail.isWHNF();
  }

  @Override
  public Expression getStuckExpression() {
    return myTail == null ? null : myTail.getStuckExpression();
  }
}
