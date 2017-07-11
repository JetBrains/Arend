package com.jetbrains.jetpad.vclang.core.sort;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.expr.factory.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ToAbstractVisitor;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintable;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.Collections;
import java.util.List;

public class Level implements PrettyPrintable {
  private final int myConstant;
  private final LevelVariable myVar;
  private final int myMaxConstant;

  public static final Level INFINITY = new Level();

  private Level() {
    myVar = null;
    myConstant = -10;
    myMaxConstant = 0;
  }

  // max(var, maxConstant) + constant
  public Level(LevelVariable var, int constant, int maxConstant) {
    assert constant >= -1;
    assert maxConstant >= 0;
    myVar = var;
    myConstant = var == null ? constant + maxConstant : constant;
    myMaxConstant = var == null ? 0 : maxConstant;
  }

  public Level(LevelVariable var, int constant) {
    assert constant >= -1;
    myConstant = constant;
    myVar = var;
    myMaxConstant = 0;
  }

  public Level(LevelVariable var) {
    myConstant = 0;
    myVar = var;
    myMaxConstant = 0;
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

  public int getConstant() {
    return myConstant;
  }

  public int getMaxConstant() {
    return myMaxConstant;
  }

  public int getMaxAddedConstant() {
    return myVar == null || myMaxConstant == 0 ? 0 : myConstant + myMaxConstant;
  }

  public boolean isInfinity() {
    return this == INFINITY;
  }

  public boolean isClosed() {
    return myVar == null;
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
      return new Level(null, Math.max(myConstant, level.myConstant));
    }

    int constant = myVar == null ? myConstant : level.myConstant;
    Level lvl = myVar == null ? level : this;
    return constant <= lvl.myConstant ? lvl : new Level(lvl.myVar, lvl.myConstant, Math.max(lvl.myMaxConstant, constant - lvl.myConstant));
  }

  public Level subst(LevelSubstitution subst) {
    if (myVar == null || this == INFINITY) {
      return this;
    }
    Level level = subst.get(myVar);
    if (level == null) {
      return this;
    }

    if (level.myVar != null) {
      int constant = level.myConstant + myConstant;
      if (constant < -1) {
        constant = -1;
      }
      return new Level(level.myVar, constant, Math.max(level.myMaxConstant, myMaxConstant - level.myConstant));
    } else {
      return new Level(Math.max(level.myConstant, myMaxConstant) + myConstant);
    }
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec, int indent) {
    ToAbstractVisitor toAbsVisitor = new ToAbstractVisitor(new ConcreteExpressionFactory(), names);
    toAbsVisitor.visitLevel(this).accept(new PrettyPrintVisitor(builder, indent), prec);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    prettyPrint(builder, Collections.emptyList(), Abstract.Expression.PREC, 0);
    return builder.toString();
  }

  public static boolean compare(Level level1, Level level2, Equations.CMP cmp, Equations equations, Abstract.SourceNode sourceNode) {
    if (cmp == Equations.CMP.GE) {
      return compare(level2, level1, Equations.CMP.LE, equations, sourceNode);
    }

    if (level1.isInfinity()) {
      return level2.isInfinity() || !level2.isClosed() && equations.add(INFINITY, level2, Equations.CMP.LE, sourceNode);
    }
    if (level2.isInfinity()) {
      return cmp == Equations.CMP.LE || !level1.isClosed() && equations.add(INFINITY, level1, Equations.CMP.LE, sourceNode);
    }

    if (level1.getVar() == null && cmp == Equations.CMP.LE) {
      if (level1.myConstant <= level2.myConstant + level2.myMaxConstant) {
        return true;
      }
    }

    if (level1.getVar() == level2.getVar()) {
      if (cmp == Equations.CMP.LE) {
        return level1.myConstant <= level2.myConstant && level1.myMaxConstant <= level2.myMaxConstant;
      } else {
        return level1.myConstant == level2.myConstant && level1.myMaxConstant == level2.myMaxConstant;
      }
    } else {
      return equations.add(level1, level2, cmp, sourceNode);
    }
  }

  public boolean isLessOrEquals(Level level) {
    return compare(this, level, Equations.CMP.LE, DummyEquations.getInstance(), null);
  }
}
