package org.arend.core.sort;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.core.definition.UniverseKind;
import org.arend.core.expr.UniverseExpression;
import org.arend.core.subst.LevelSubstitution;
import org.arend.core.subst.StdLevelSubstitution;
import org.arend.ext.core.level.CoreSort;
import org.arend.ext.core.ops.CMP;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.arend.typechecking.implicitargs.equations.Equations;

import javax.annotation.Nonnull;

public class Sort implements CoreSort {
  private final Level myPLevel;
  private final Level myHLevel;

  public static final Sort PROP = new Sort(new Level(0), new Level(-1));
  public static final Sort SET0 = new Sort(new Level(0), new Level(0));
  public static final Sort STD = new Sort(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR)) {
    @Override
    public LevelSubstitution toLevelSubstitution() {
      return LevelSubstitution.EMPTY;
    }
  };

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

  @Nonnull
  @Override
  public Level getPLevel() {
    return myPLevel;
  }

  @Nonnull
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
    if (isProp()) {
      return sort;
    }
    if (sort.isProp()) {
      return this;
    }
    if (myPLevel.getVar() != null && sort.myPLevel.getVar() != null && myPLevel.getVar() != sort.myPLevel.getVar() ||
        myHLevel.getVar() != null && sort.myHLevel.getVar() != null && myHLevel.getVar() != sort.myHLevel.getVar()) {
      return null;
    } else {
      return new Sort(myPLevel.max(sort.myPLevel), myHLevel.max(sort.myHLevel));
    }
  }

  @Override
  public boolean isProp() {
    return myHLevel.isProp();
  }

  @Override
  public boolean isSet() {
    return myHLevel.isClosed() && myHLevel.getConstant() == 0;
  }

  public LevelSubstitution toLevelSubstitution() {
    return new StdLevelSubstitution(myPLevel, myHLevel);
  }

  public static boolean compareProp(Sort sort, Equations equations, Concrete.SourceNode sourceNode) {
    if (sort.isProp()) {
      return true;
    }
    if (!(sort.getHLevel().getVar() instanceof InferenceLevelVariable) || sort.getHLevel().getMaxAddedConstant() > -1) {
      return false;
    }
    if (equations == null) {
      return true;
    }
    return equations.addEquation(new Level(sort.getHLevel().getVar()), new Level(-1), CMP.LE, sourceNode);
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

  public static Sort generateInferVars(Equations equations, boolean isPLevelBased, boolean isHLevelBased, Concrete.SourceNode sourceNode) {
    InferenceLevelVariable pl = new InferenceLevelVariable(LevelVariable.LvlType.PLVL, isPLevelBased, sourceNode);
    InferenceLevelVariable hl = new InferenceLevelVariable(LevelVariable.LvlType.HLVL, isHLevelBased, sourceNode);
    equations.addVariable(pl);
    equations.addVariable(hl);
    return new Sort(new Level(pl), new Level(hl));
  }

  public static Sort generateInferVars(Equations equations, UniverseKind pLevelKind, UniverseKind hLevelKind, Concrete.SourceNode sourceNode) {
    return generateInferVars(equations, pLevelKind != UniverseKind.NO_UNIVERSES, hLevelKind != UniverseKind.NO_UNIVERSES, sourceNode);
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
