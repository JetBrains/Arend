package com.jetbrains.jetpad.vclang.core.sort;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.core.expr.factory.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.core.expr.type.PiTypeOmega;
import com.jetbrains.jetpad.vclang.core.expr.type.PiUniverseType;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeMax;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ToAbstractVisitor;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.Collections;

public class SortMax {
  private LevelMax myPLevel;
  private LevelMax myHLevel;

  public static SortMax OMEGA = new SortMax(LevelMax.INFINITY, LevelMax.INFINITY);

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

  public boolean isOmega() {
    return myPLevel.isInfinity() && myHLevel.toLevel() != null;
  }

  public SortMax max(SortMax sort) {
    return new SortMax(myPLevel.max(sort.getPLevel()), myHLevel.max(sort.getHLevel()));
  }

  public SortMax max(Sort sort) {
    return new SortMax(myPLevel.max(sort.getPLevel()), myHLevel.max(sort.getHLevel()));
  }

  public Sort toSort() {
    if (myHLevel.isMinimum()) {
      return Sort.PROP;
    }
    Level pLevel = myPLevel.toLevel();
    if (pLevel == null) {
      return null;
    }
    Level hLevel = myHLevel.toLevel();
    if (hLevel == null) {
      return null;
    }
    return new Sort(pLevel, hLevel);
  }

  public void add(SortMax sorts) {
    myPLevel.add(sorts.myPLevel);
    myHLevel.add(sorts.myHLevel);
  }

  public void add(Sort sort) {
    myPLevel.add(sort.getPLevel());
    myHLevel.add(sort.getHLevel());
  }

  public TypeMax toType() {
    if (isOmega()) {
      return new PiTypeOmega(EmptyDependentLink.getInstance(), myHLevel.toLevel());
    }
    if (toSort() != null) {
      return new UniverseExpression(toSort());
    }
    return new PiUniverseType(EmptyDependentLink.getInstance(), this);
  }

  public SortMax subst(LevelSubstitution subst) {
    if (subst.isEmpty()) {
      return this;
    }
    return new SortMax(myPLevel.subst(subst), myHLevel.subst(subst));
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
    return myPLevel.isLessOrEquals(sort.getPLevel()) && myHLevel.isLessOrEquals(sort.getHLevel());
  }

  public boolean isLessOrEquals(Sort sort, Equations equations, Abstract.SourceNode sourceNode) {
    if (myHLevel.isMinimum()) {
      return true;
    }
    return myPLevel.isLessOrEquals(sort.getPLevel(), equations, sourceNode) && myHLevel.isLessOrEquals(sort.getHLevel(), equations, sourceNode);
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
