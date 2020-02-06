package org.arend.core.sort;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.core.expr.visitor.ToAbstractVisitor;
import org.arend.core.subst.LevelSubstitution;
import org.arend.ext.core.level.CoreLevel;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.reference.Precedence;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrintVisitor;
import org.arend.typechecking.implicitargs.equations.Equations;

public class Level implements CoreLevel {
  private final int myConstant;
  private final LevelVariable myVar;
  private final int myMaxConstant;

  public static final Level INFINITY = new Level();

  private Level() {
    myVar = null;
    myConstant = Integer.MAX_VALUE;
    myMaxConstant = 0;
  }

  // max(var, maxConstant) + constant
  public Level(LevelVariable var, int constant, int maxConstant) {
    assert maxConstant + constant >= -1 && (var == null || constant >= 0 && (var.getType() != LevelVariable.LvlType.PLVL || maxConstant + constant >= 0));
    myVar = var;
    myConstant = var == null ? constant + maxConstant : constant;
    myMaxConstant = var == null ? 0 : maxConstant;
  }

  public Level(LevelVariable var, int constant) {
    assert constant >= 0 || var == null && constant == -1;
    myConstant = constant;
    myVar = var;
    myMaxConstant = var != null && var.getType() == LevelVariable.LvlType.HLVL ? -1 : 0;
  }

  public Level(LevelVariable var) {
    myConstant = 0;
    myVar = var;
    myMaxConstant = var != null && var.getType() == LevelVariable.LvlType.HLVL ? -1 : 0;
  }

  public Level(int constant) {
    assert constant >= -1;
    myConstant = constant;
    myVar = null;
    myMaxConstant = 0;
  }

  public LevelVariable getVar() {
    return myVar;
  }

  @Override
  public int getConstant() {
    return myConstant;
  }

  @Override
  public int getMaxConstant() {
    return myMaxConstant;
  }

  public int getMaxAddedConstant() {
    return myConstant + myMaxConstant;
  }

  @Override
  public boolean isInfinity() {
    return this == INFINITY;
  }

  @Override
  public boolean isClosed() {
    return myVar == null;
  }

  public boolean isProp() {
    return isClosed() && myConstant == -1;
  }

  public boolean withMaxConstant() {
    return myVar != null && (myMaxConstant > 0 || myVar.getType() == LevelVariable.LvlType.HLVL && myMaxConstant == 0);
  }

  public boolean isVarOnly() {
    return myVar != null && myConstant == 0 && !withMaxConstant();
  }

  public Level add(int constant) {
    return constant == 0 || isInfinity() ? this : new Level(myVar, myConstant + constant, myMaxConstant);
  }

  public Level max(Level level) {
    if (isInfinity() || level.isInfinity()) {
      return INFINITY;
    }

    if (myVar != null && level.myVar != null) {
      if (myVar == level.myVar) {
        int constant = Math.max(myConstant, level.myConstant);
        return new Level(myVar, constant, Math.max(myConstant + myMaxConstant, level.myConstant + level.myMaxConstant) - constant);
      } else {
        return null;
      }
    }

    if (myVar == null && level.myVar == null) {
      return new Level(Math.max(myConstant, level.myConstant));
    }

    int constant = myVar == null ? myConstant : level.myConstant;
    Level lvl = myVar == null ? level : this;
    return constant <= lvl.getMaxAddedConstant() ? lvl : new Level(lvl.myVar, lvl.myConstant, Math.max(lvl.myMaxConstant, constant - lvl.myConstant));
  }

  public Level subst(LevelSubstitution subst) {
    if (myVar == null || this == INFINITY) {
      return this;
    }
    Level level = subst.get(myVar);
    if (level == null) {
      return this;
    }
    if (level == INFINITY || isVarOnly()) {
      return level;
    }

    if (level.myVar != null) {
      return new Level(level.myVar, level.myConstant + myConstant, Math.max(level.myMaxConstant, myMaxConstant - level.myConstant));
    } else {
      return new Level(Math.max(level.myConstant, myMaxConstant) + myConstant);
    }
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    ToAbstractVisitor.convert(this).accept(new PrettyPrintVisitor(builder, 0), new Precedence(Concrete.Expression.PREC));
    return builder.toString();
  }

  public static boolean compare(Level level1, Level level2, CMP cmp, Equations equations, Concrete.SourceNode sourceNode) {
    if (cmp == CMP.GE) {
      return compare(level2, level1, CMP.LE, equations, sourceNode);
    }

    if (level1.isInfinity()) {
      return level2.isInfinity() || !level2.isClosed() && (equations == null || equations.addEquation(INFINITY, level2, CMP.LE, sourceNode));
    }
    if (level2.isInfinity()) {
      return cmp == CMP.LE || !level1.isClosed() && (equations == null || equations.addEquation(INFINITY, level1, CMP.LE, sourceNode));
    }

    if (level2.getVar() == null && level1.getVar() != null && !(level1.getVar() instanceof InferenceLevelVariable)) {
      return false;
    }

    if (level1.getVar() == null && cmp == CMP.LE) {
      if (level1.myConstant <= level2.myConstant + level2.myMaxConstant) {
        return true;
      }
    }

    if (level1.getVar() == level2.getVar()) {
      if (cmp == CMP.LE) {
        return level1.myConstant <= level2.myConstant && level1.getMaxAddedConstant() <= level2.getMaxAddedConstant();
      } else {
        return level1.myConstant == level2.myConstant && level1.getMaxConstant() == level2.getMaxConstant();
      }
    } else {
      if (equations == null) {
        return level1.getVar() instanceof InferenceLevelVariable || level2.getVar() instanceof InferenceLevelVariable;
      } else {
        return equations.addEquation(level1, level2, cmp, sourceNode);
      }
    }
  }
}
