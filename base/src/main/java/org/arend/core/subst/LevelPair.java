package org.arend.core.subst;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.core.definition.Definition;
import org.arend.ext.core.level.LevelSubstitution;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.variable.Variable;
import org.arend.core.sort.Level;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class LevelPair implements LevelSubstitution, Levels {
  public static final LevelPair STD = new LevelPair(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR));
  public static final LevelPair PROP = new LevelPair(new Level(0), new Level(-1));
  public static final LevelPair SET0 = new LevelPair(new Level(0), new Level(0));

  private final Level myPLevel;
  private final Level myHLevel;

  public LevelPair(Level pLevel, Level hLevel) {
    this.myPLevel = pLevel;
    this.myHLevel = hLevel;
  }

  @Override
  public boolean isEmpty() {
    return isSTD();
  }

  @Override
  public boolean isClosed() {
    return myPLevel.isClosed() && myHLevel.isClosed();
  }

  public boolean isSTD() {
    return myPLevel.isVarOnly() && myPLevel.getVar().equals(LevelVariable.PVAR) && myHLevel.isVarOnly() && myHLevel.getVar().equals(LevelVariable.HVAR);
  }

  @Override
  public Level get(Variable variable) {
    return LevelVariable.PVAR.equals(variable) ? myPLevel : LevelVariable.HVAR.equals(variable) ? myHLevel : null;
  }

  @Override
  public LevelPair subst(LevelSubstitution substitution) {
    return new LevelPair(myPLevel.subst(substitution), myHLevel.subst(substitution));
  }

  @Override
  public boolean compare(Levels other, CMP cmp, Equations equations, Concrete.SourceNode sourceNode) {
    return other instanceof LevelPair && Level.compare(myPLevel, ((LevelPair) other).myPLevel, cmp, equations, sourceNode) && Level.compare(myHLevel, ((LevelPair) other).myHLevel, cmp, equations, sourceNode);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof Levels && compare((Levels) other, CMP.EQ, DummyEquations.getInstance(), null);
  }

  public static LevelPair generateInferVars(Equations equations, boolean isUniverseLike, Concrete.SourceNode sourceNode) {
    InferenceLevelVariable pl = new InferenceLevelVariable(LevelVariable.LvlType.PLVL, isUniverseLike, sourceNode);
    InferenceLevelVariable hl = new InferenceLevelVariable(LevelVariable.LvlType.HLVL, isUniverseLike, sourceNode);
    equations.addVariable(pl);
    equations.addVariable(hl);
    return new LevelPair(new Level(pl), new Level(hl));
  }

  @Override
  public LevelSubstitution makeSubstitution(@NotNull Definition definition) {
    return this;
  }

  @Override
  public LevelPair toLevelPair() {
    return this;
  }

  @Override
  public List<? extends Level> toList() {
    return Arrays.asList(myPLevel, myHLevel);
  }
}
