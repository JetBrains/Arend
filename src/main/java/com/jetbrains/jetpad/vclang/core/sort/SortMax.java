package com.jetbrains.jetpad.vclang.core.sort;

import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.core.expr.type.PiUniverseType;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeMax;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

public class SortMax {
  private final LevelMax myPLevel;
  private final LevelMax myHLevel;

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

  public void addPLevel(LevelMax pLevel) {
    myPLevel.add(pLevel);
  }

  public void addHLevel(LevelMax hLevel) {
    myHLevel.add(hLevel);
  }

  public SortMax succ() {
    Sort sort = toSort();
    if (sort != null) {
      return new SortMax(sort.succ());
    }

    LevelMax sucP = new LevelMax(myPLevel);
    LevelMax sucH = new LevelMax(myHLevel);
    sucP.add(new Level(1));
    sucH.add(new Level(1));
    return new SortMax(sucP, sucH);
  }

  public void add(Sort sort) {
    myPLevel.add(sort.getPLevel());
    myHLevel.add(sort.getHLevel());
  }

  public TypeMax toType() {
    Sort sort = toSort();
    if (sort != null) {
      return new UniverseExpression(sort);
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
    Sort sort = toSort();
    if (sort != null && sort.isProp()) {
      return "\\Prop";
    }
    StringBuilder builder = new StringBuilder();
    boolean hlevelIsConstant = !myHLevel.isInfinity() && myHLevel.toLevel() != null && myHLevel.toLevel().isClosed();
    if (hlevelIsConstant) {
      if (myHLevel.toLevel().getConstant() == 1) {
        builder.append("\\Set");
      } else {
        builder.append("\\").append(myHLevel).append("-Type");
      }
    } else {
      builder.append("\\Type");
    }

    if (hlevelIsConstant) {
      if (myPLevel.toLevel() == null || !myPLevel.toLevel().isClosed()) {
        builder.append(" ");
      }
      builder.append(myPLevel);
    } else {
      builder.append(" (").append(myPLevel).append(", ").append(myHLevel).append(")");
    }

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
