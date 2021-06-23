package org.arend.core.sort;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.inference.InferenceLevelVariable;
import org.arend.ext.core.context.CoreInferenceVariable;
import org.arend.term.prettyprint.ToAbstractVisitor;
import org.arend.ext.core.level.LevelSubstitution;
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

  // max(var + constant, maxConstant)
  public Level(LevelVariable var, int constant, int maxConstant) {
    assert constant >= (var == null ? -1 : var.getMinValue());
    myVar = var;
    myConstant = constant;
    myMaxConstant = var == null
      ? (constant == -1 ? -1 : 0)
      : maxConstant > constant + var.getMinValue()
        ? maxConstant
        : var.getMinValue();
  }

  public Level(LevelVariable var, int constant) {
    this(var, constant, -1);
  }

  public Level(LevelVariable var) {
    this(var, 0);
  }

  public Level(int constant) {
    this(null, constant);
  }

  public LevelVariable getVar() {
    return myVar;
  }

  @Override
  public int getConstant() {
    return myConstant;
  }

  public int getMaxConstant() {
    return myMaxConstant;
  }

  @Override
  public boolean isInfinity() {
    return this == INFINITY;
  }

  @Override
  public boolean isClosed() {
    return myVar == null;
  }

  @Override
  public boolean hasInferenceVar() {
    return myVar instanceof CoreInferenceVariable;
  }

  public boolean isProp() {
    return isClosed() && myConstant == -1;
  }

  public boolean withMaxConstant() {
    return myVar != null && myMaxConstant > myVar.getMinValue();
  }

  public boolean isVarOnly() {
    return myVar != null && myConstant == 0 && !withMaxConstant();
  }

  public Level add(int constant) {
    assert constant >= 0;
    return constant == 0 || isInfinity() ? this : new Level(myVar, myConstant + constant, myMaxConstant + constant);
  }

  public Level max(Level level) {
    if (isInfinity() || level.isInfinity()) {
      return INFINITY;
    }

    if (myVar != null && level.myVar != null) {
      LevelVariable var = myVar.max(level.myVar);
      if (var != null) {
        return new Level(var, Math.max(myConstant, level.myConstant), Math.max(myMaxConstant, level.myMaxConstant));
      } else {
        return null;
      }
    }

    if (myVar == null && level.myVar == null) {
      return new Level(Math.max(myConstant, level.myConstant));
    }

    int constant = myVar == null ? myConstant : level.myConstant;
    Level lvl = myVar == null ? level : this;
    return new Level(lvl.myVar, lvl.myConstant, Math.max(constant, lvl.myMaxConstant));
  }

  public Level subst(LevelSubstitution subst) {
    if (myVar == null || this == INFINITY) {
      return this;
    }
    Level level = (Level) subst.get(myVar);
    if (level == null) {
      return this;
    }
    if (level == INFINITY || isVarOnly()) {
      return level;
    }

    int constant = myConstant == -1 && level.myConstant == -1 ? -1 : level.myConstant + myConstant;
    if (level.myVar != null) {
      return new Level(level.myVar, constant, Math.max(level.myMaxConstant + myConstant, myMaxConstant));
    } else {
      return new Level(Math.max(constant, myMaxConstant));
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

    if (level2.myVar == null && level1.myVar != null && !(level1.myVar instanceof InferenceLevelVariable)) {
      return false;
    }

    if (level1.myVar == null) {
      if (level2.myVar == null) {
        return cmp == CMP.LE ? level1.myConstant <= level2.myConstant : level1.myConstant == level2.myConstant;
      }
      if (cmp == CMP.EQ) {
        // c == max(l + c', m') can be true only if l is an inference var and c >= m' and c >= c' + l.minValue
        if (!(level2.myVar instanceof InferenceLevelVariable) || level1.myConstant < level2.myMaxConstant || level1.myConstant < level2.myConstant + level2.myVar.getMinValue()) {
          return false;
        }
      } else {
        // c <= max(l + c', m') always can be true if l is an inference var
        if (!(level2.myVar instanceof InferenceLevelVariable)) {
          // If l is not an inference var, then c <= max(l + c', m') is true if and only if either c <= m' or c <= c' + l.minValue
          return level1.myConstant <= level2.myMaxConstant || level1.myConstant <= level2.myConstant + level2.myVar.getMinValue();
        }
      }
    }

    if (level1.myVar != null && level2.myVar != null && level1.myVar.compare(level2.myVar, cmp)) {
      if (cmp == CMP.EQ && level1.myConstant != level2.myConstant || cmp == CMP.LE && level1.myConstant > level2.myConstant) {
        return false;
      }
      if (level1.myMaxConstant == level2.myMaxConstant || cmp == CMP.LE && (level1.myMaxConstant <= level2.myMaxConstant || level1.myMaxConstant <= level2.myConstant + level2.myVar.getMinValue())) {
        return true;
      }
      if (!(level1.myVar instanceof InferenceLevelVariable)) {
        return false;
      }
    }

    if (equations == null) {
      return level1.myVar instanceof InferenceLevelVariable || level2.myVar instanceof InferenceLevelVariable;
    } else {
      return equations.addEquation(level1, level2, cmp, sourceNode);
    }
  }
}
