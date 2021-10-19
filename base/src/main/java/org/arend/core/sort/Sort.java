package org.arend.core.sort;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.core.expr.UniverseExpression;
import org.arend.ext.core.level.LevelSubstitution;
import org.arend.ext.core.level.CoreSort;
import org.arend.ext.core.ops.CMP;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.jetbrains.annotations.NotNull;

public class Sort implements CoreSort {
  private final Level myPLevel;
  private final Level myHLevel;

  public static final Sort PROP = new Sort(new Level(0), new Level(-1));
  public static final Sort SET0 = new Sort(new Level(0), new Level(0));
  public static final Sort STD = new Sort(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR));

  public static Sort SetOfLevel(int pLevel) {
    return new Sort(pLevel, 0);
  }

  public static Sort SetOfLevel(Level pLevel) {
    return new Sort(pLevel, new Level(0));
  }

  public static Sort TypeOfLevel(int pLevel) {
    return new Sort(new Level(pLevel), Level.INFINITY);
  }

  public Sort(int pLevel, int hLevel) {
    assert pLevel >= 0;
    assert hLevel >= 0;
    myPLevel = new Level(pLevel);
    myHLevel = new Level(hLevel);
  }

  public Sort(Level pLevel, Level hLevel) {
    myPLevel = pLevel;
    myHLevel = hLevel;
  }

  @NotNull
  @Override
  public Level getPLevel() {
    return myPLevel;
  }

  @NotNull
  @Override
  public Level getHLevel() {
    return myHLevel;
  }

  public boolean isOmega() {
    return myPLevel.isInfinity();
  }

  public Sort succ() {
    return isProp() ? SET0 : new Sort(getPLevel().add(1), getHLevel().add(1));
  }

  public Sort max(Sort sort) {
    if (isProp()) return sort;
    if (sort.isProp()) return this;
    Level pLevel = myPLevel.max(sort.myPLevel);
    Level hLevel = myHLevel.max(sort.myHLevel);
    return pLevel == null || hLevel == null ? null : new Sort(pLevel, hLevel);
  }

  @Override
  public boolean isProp() {
    return myHLevel.isProp();
  }

  @Override
  public boolean isSet() {
    return myHLevel.isClosed() && myHLevel.getConstant() == 0;
  }

  public boolean isStd() {
    return myPLevel.isVarOnly() && myPLevel.getVar().equals(LevelVariable.PVAR) && myHLevel.isVarOnly() && myHLevel.getVar().equals(LevelVariable.HVAR);
  }

  private static boolean compareProp(Sort sort, Equations equations, Concrete.SourceNode sourceNode) {
    if (sort.isProp()) {
      return true;
    }
    if (!(sort.getHLevel().getVar() instanceof InferenceLevelVariable) || sort.getHLevel().getMaxConstant() > -1 || sort.getHLevel().getConstant() > 0) {
      return false;
    }
    if (equations == null) {
      return true;
    }
    return equations.addEquation(new Level(sort.getHLevel().getVar()), new Level(sort.getHLevel().getConstant() == 0 ? -1 : 0), CMP.LE, sourceNode);
  }

  public static boolean compare(Sort sort1, Sort sort2, CMP cmp, Equations equations, Concrete.SourceNode sourceNode) {
    if (sort1.isProp()) {
      if (cmp == CMP.LE || sort2.isProp()) {
        return true;
      }
      return compareProp(sort2, equations, sourceNode);
    }
    if (sort2.isProp()) {
      if (cmp == CMP.GE) {
        return true;
      }
      return compareProp(sort1, equations, sourceNode);
    }
    return Level.compare(sort1.getPLevel(), sort2.getPLevel(), cmp, equations, sourceNode) && Level.compare(sort1.getHLevel(), sort2.getHLevel(), cmp, equations, sourceNode);
  }

  public boolean isLessOrEquals(Sort other) {
    return compare(this, other, CMP.LE, DummyEquations.getInstance(), null);
  }

  public Sort subst(LevelSubstitution subst) {
    return subst.isEmpty() || myPLevel.isClosed() && myHLevel.isClosed() ? this : new Sort(myPLevel.subst(subst), myHLevel.subst(subst));
  }

  public static Sort generateInferVars(Equations equations, boolean isUniverseLike, Concrete.SourceNode sourceNode) {
    InferenceLevelVariable pl = new InferenceLevelVariable(LevelVariable.LvlType.PLVL, isUniverseLike, sourceNode);
    InferenceLevelVariable hl = new InferenceLevelVariable(LevelVariable.LvlType.HLVL, isUniverseLike, sourceNode);
    equations.addVariable(pl);
    equations.addVariable(hl);
    return new Sort(new Level(pl), new Level(hl));
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof Sort && compare(this, (Sort) other, CMP.EQ, DummyEquations.getInstance(), null);
  }

  @Override
  public String toString() {
    return new UniverseExpression(this).toString();
  }
}
