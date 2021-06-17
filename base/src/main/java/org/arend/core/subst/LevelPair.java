package org.arend.core.subst;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.core.definition.UniverseKind;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.variable.Variable;
import org.arend.core.sort.Level;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.arend.typechecking.implicitargs.equations.Equations;

public class LevelPair implements LevelSubstitution {
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

  public boolean isSTD() {
    return myPLevel.isVarOnly() && myPLevel.getVar() == LevelVariable.PVAR && myHLevel.isVarOnly() && myHLevel.getVar() == LevelVariable.HVAR;
  }

  @Override
  public Level get(Variable variable) {
    return variable == LevelVariable.PVAR ? myPLevel : variable == LevelVariable.HVAR ? myHLevel : null;
  }

  @Override
  public LevelPair subst(LevelSubstitution substitution) {
    return new LevelPair(myPLevel.subst(substitution), myHLevel.subst(substitution));
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof LevelPair && compare(this, (LevelPair) other, CMP.EQ, DummyEquations.getInstance(), null);
  }

  public static boolean compare(LevelPair levels1, LevelPair levels2, CMP cmp, Equations equations, Concrete.SourceNode sourceNode) {
    return Level.compare(levels1.myPLevel, levels2.myPLevel, cmp, equations, sourceNode) && Level.compare(levels1.myHLevel, levels2.myHLevel, cmp, equations, sourceNode);
  }

  public static LevelPair generateInferVars(Equations equations, boolean isUniverseLike, Concrete.SourceNode sourceNode) {
    InferenceLevelVariable pl = new InferenceLevelVariable(LevelVariable.LvlType.PLVL, isUniverseLike, sourceNode);
    InferenceLevelVariable hl = new InferenceLevelVariable(LevelVariable.LvlType.HLVL, isUniverseLike, sourceNode);
    equations.addVariable(pl);
    equations.addVariable(hl);
    return new LevelPair(new Level(pl), new Level(hl));
  }

  public static LevelPair generateInferVars(Equations equations, UniverseKind universeKind, Concrete.SourceNode sourceNode) {
    return generateInferVars(equations, universeKind != UniverseKind.NO_UNIVERSES, sourceNode);
  }
}
