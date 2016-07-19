package com.jetbrains.jetpad.vclang.term.expr.sort;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.expr.factory.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.term.expr.type.PiUniverseType;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.term.expr.visitor.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ToAbstractVisitor;

import java.util.Collections;

public class SortMax {
  private LevelMax myPLevel;
  private LevelMax myHLevel;

  public SortMax() {
    myPLevel = new LevelMax();
    myHLevel = new LevelMax();
  }

  public SortMax(Sort sort) {
    myPLevel = new LevelMax(sort.getPLevel());
    myHLevel = new LevelMax(sort.getHLevel());
  }

  public SortMax(LevelMax pLevel, LevelMax hLevel) {
    myPLevel = pLevel;
    myHLevel = hLevel;
  }

  public LevelMax getPLevel() {
    return myPLevel;
  }

  public LevelMax getHLevel() {
    return myHLevel;
  }

  // TODO [sorts]
  @Deprecated
  public Sort toSort() {
    if (myHLevel.isMinimum()) {
      return Sort.PROP;
    }
    return new Sort(myPLevel.toLevel(), myHLevel.toLevel());
  }

  public void add(SortMax sorts) {
    myPLevel = myPLevel.max(sorts.myPLevel);
    myHLevel = myHLevel.max(sorts.myHLevel);
  }

  public Type toType() {
    return new PiUniverseType(EmptyDependentLink.getInstance(), this);
  }

  public boolean isLessOrEquals(SortMax sorts) {
    if (myHLevel.isMinimum()) {
      return true;
    }
    if (sorts.getHLevel().isMinimum()) {
      return myHLevel.isMinimum();
    }
    return myPLevel.isLessOrEquals(sorts.getPLevel()) && myHLevel.isLessOrEquals(sorts.getHLevel());
  }

  public boolean isLessOrEquals(Sort sort) {
    if (myHLevel.isMinimum()) {
      return true;
    }
    if (sort.getHLevel().isClosed() && sort.getHLevel().getConstant() == 0) {
      return myHLevel.isMinimum();
    }
    return myPLevel.isLessOrEquals(sort.getPLevel()) && myHLevel.isLessOrEquals(sort.getHLevel());
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    new ToAbstractVisitor(new ConcreteExpressionFactory(), Collections.<String>emptyList()).visitSortMax(this).accept(new PrettyPrintVisitor(builder, 0), Abstract.Expression.PREC);
    return builder.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SortMax sortMax = (SortMax) o;

    return myPLevel.equals(sortMax.myPLevel) && myHLevel.equals(sortMax.myHLevel);

  }

  @Override
  public int hashCode() {
    int result = myPLevel.hashCode();
    result = 31 * result + myHLevel.hashCode();
    return result;
  }
}
