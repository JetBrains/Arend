package com.jetbrains.jetpad.vclang.term.expr.sort;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.factory.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ToAbstractVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.Collections;

public class Sort {
  private final Level myPLevel;
  private final Level myHLevel;

  public static final int ANY_LEVEL = -10;
  public static final int NOT_TRUNCATED = -10;
  public static final Sort PROP = new Sort(0, -1);
  public static final Sort SET = new Sort(ANY_LEVEL, 0);

  public static Sort SetOfLevel(int level) {
    return new Sort(level, 0);
  }

  public static Sort SetOfLevel(Level level) {
    return new Sort(level, SET.getHLevel());
  }

  public Sort(int plevel, int hlevel) {
    if (hlevel == -1) {
      plevel = 0;
    }
    if (plevel != ANY_LEVEL) {
      myPLevel = new Level(plevel);
    } else {
      myPLevel = Level.INFINITY;
    }
    if (hlevel != NOT_TRUNCATED)
      myHLevel = new Level(hlevel + 1);
    else {
      myHLevel = Level.INFINITY;
    }
  }

  public Sort(Level plevel, Level hlevel) {
    myPLevel = hlevel.isZero() ? new Level(0) : plevel;
    myHLevel = hlevel;
  }

  public Level getPLevel() {
    return myPLevel;
  }

  public Level getHLevel() {
    return myHLevel;
  }

  public Sort succ() {
    return isProp() ? SetOfLevel(0) : new Sort(getPLevel().add(1), getHLevel().add(1));
  }

  public boolean isProp() {
    return myHLevel.equals(PROP.getHLevel());
  }

  public static boolean compare(Sort sort1, Sort sort2, Equations.CMP cmp, Equations equations, Abstract.SourceNode sourceNode) {
    if (sort1.isProp()) {
      return cmp == Equations.CMP.LE || sort2.isProp();
    }
    if (sort2.isProp()) {
      return cmp == Equations.CMP.GE;
    }
    return Level.compare(sort1.getPLevel(), sort2.getPLevel(), cmp, equations, sourceNode) && Level.compare(sort1.getHLevel(), sort2.getHLevel(), cmp, equations, sourceNode);
  }

  public boolean isLessOrEquals(Sort other) {
    return compare(this, other, Equations.CMP.LE, DummyEquations.getInstance(), null);
  }

  public Sort subst(LevelSubstitution subst) {
    return myPLevel.isClosed() && myHLevel.isClosed() ? this : new Sort(myPLevel.subst(subst), myHLevel.subst(subst));
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof Sort && compare(this, (Sort) other, Equations.CMP.EQ, DummyEquations.getInstance(), null);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    new ToAbstractVisitor(new ConcreteExpressionFactory(), Collections.<String>emptyList()).visitSort(this).accept(new PrettyPrintVisitor(builder, 0), Abstract.Expression.PREC);
    return builder.toString();
  }
}
