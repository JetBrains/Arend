package com.jetbrains.jetpad.vclang.term.expr.sort;

import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

public class Sort {
  private Level myPLevel;
  private Level myHLevel;

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
      myPLevel = new Level();
    }
    if (hlevel != NOT_TRUNCATED)
      myHLevel = new Level(hlevel + 1);
    else {
      myHLevel = new Level();
    }
  }

  public Sort(Level plevel, Level hlevel) {
    myPLevel = hlevel.isZero() ? new Level(0) : plevel;
    myHLevel = hlevel;
  }

  public Sort(Sort universe) {
    myPLevel = new Level(universe.getPLevel());
    myHLevel = new Level(universe.getHLevel());
  }

  public Level getPLevel() {
    return myPLevel;
  }

  public Level getHLevel() {
    return myHLevel;
  }

  public void setPLevel(Level level) {
    myPLevel = level;
  }

  public void setHLevel(Level level) {
    myHLevel = level;
  }

  public Sort max(Sort other) {
    return new Sort(myPLevel.max(other.getPLevel()), myHLevel.max(other.getHLevel()));
  }

  public static Level intToPLevel(int plevel) {
    return new Level(plevel);
  }

  public static Level intToHLevel(int hlevel) {
    if (hlevel == NOT_TRUNCATED) return new Level();
    return new Level(hlevel + 1);
  }

  public Sort succ() {
    return isProp() ? SetOfLevel(0) : new Sort(getPLevel().succ(), getHLevel().succ());
  }

  public boolean isProp() {
    return myHLevel.equals(PROP.getHLevel());
  }

  public boolean isLessOrEquals(Sort other) {
    if (equals(other)) return true;
    UniverseExpression uni1 = new UniverseExpression(this);
    UniverseExpression uni2 = new UniverseExpression(other);
    return Expression.compare(uni1, uni2, Equations.CMP.LE);
  }

  public Sort subst(LevelSubstitution subst) {
    Level plevel = myPLevel;
    Level hlevel = myHLevel;
    for (Binding var : subst.getDomain()) {
      if (var.getType().toDefCall().getDefinition() == Preprelude.LVL) {
        plevel = plevel.subst(var, subst.get(var));
      } else if (var.getType().toDefCall().getDefinition() == Preprelude.CNAT) {
        hlevel = hlevel.subst(var, subst.get(var));
      }
    }
    return new Sort(plevel, hlevel);
  }

  public static boolean compare(Sort uni1, Sort uni2, Equations.CMP cmp, Equations equations) {
    if (uni1.getHLevel().isZero() || uni2.getHLevel().isZero()) {
      Level.compare(uni1.getPLevel(), new Level(0), Equations.CMP.GE, equations);
      Level.compare(uni2.getPLevel(), new Level(0), Equations.CMP.GE, equations);
      return Level.compare(uni1.getHLevel(), uni2.getHLevel(), cmp, equations);
    }
    return Level.compare(uni1.getPLevel(), uni2.getPLevel(), cmp, equations) && Level.compare(uni1.getHLevel(), uni2.getHLevel(), cmp, equations);
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Sort)) return false;
    if (isProp() || ((Sort) other).isProp()) return myHLevel.equals(((Sort) other).getHLevel());
    if (myPLevel.isInfinity() || ((Sort) other).getPLevel().isInfinity()) return myHLevel.equals(((Sort) other).getHLevel());
    return myPLevel.equals(((Sort) other).getPLevel()) && myHLevel.equals(((Sort) other).getHLevel());
  }

  @Override
  public String toString() {
    return "\\Type (" + myPLevel + "," + myHLevel + ")";
  }
}
