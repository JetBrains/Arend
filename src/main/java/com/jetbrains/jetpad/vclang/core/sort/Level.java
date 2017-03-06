package com.jetbrains.jetpad.vclang.core.sort;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.expr.factory.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ToAbstractVisitor;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintable;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Level implements PrettyPrintable {
  private final int myConstant;
  private final LevelVariable myVar;
  private final int myMaxConstant;

  public static final Level INFINITY = new Level(null, -1);

  // max(var + constant, maxConstant)
  public Level(LevelVariable var, int constant, int maxConstant) {
    myVar = var;
    myConstant = var == null ? Math.max(constant, maxConstant) : constant;
    myMaxConstant = var == null || maxConstant <= constant ? 0 : maxConstant;
  }

  public Level(LevelVariable var, int constant) {
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

  public boolean isInfinity() {
    return myConstant == -1;
  }

  public boolean isClosed() {
    return myVar == null;
  }

  public boolean isMinimum() {
    return isClosed() && myConstant == 0;
  }

  public Level add(int constant) {
    return constant == 0 || isInfinity() ? this : new Level(myVar, myConstant + constant, myMaxConstant + constant);
  }

  public Level max(Level level) {
    if (isInfinity() || level.isInfinity()) {
      return INFINITY;
    }

    int constant;
    if (myVar == null && level.myVar != null) {
      constant = level.myConstant;
    } else
    if (myVar != null && level.myVar == null){
      constant = myConstant;
    } else {
      constant = Math.max(myConstant, level.myConstant);
    }

    int maxConstant = Math.max(myMaxConstant, level.myMaxConstant);
    if (myVar == null) {
      maxConstant = Math.max(maxConstant, myConstant);
    }
    if (level.myVar == null) {
      maxConstant = Math.max(maxConstant, level.myConstant);
    }

    return new Level(myVar != null ? myVar : level.myVar != null ? level.myVar : null, constant, maxConstant);
  }

  public Level subst(LevelSubstitution subst) {
    if (myVar == null) {
      return this;
    }
    Level level = subst.get(myVar);
    if (level == null) {
      return this;
    }
    return level.add(myConstant);
  }

  public Level subst(Variable binding, Level subst) {
    return myVar != binding ? this : subst.add(myConstant);
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec, int indent) {
    ToAbstractVisitor toAbsVisitor = new ToAbstractVisitor(new ConcreteExpressionFactory(), names);
    new PrettyPrintVisitor(builder, indent).prettyPrintLevelExpression(toAbsVisitor.visitLevel(this, 0), prec);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    prettyPrint(builder, Collections.<String>emptyList(), Abstract.Expression.PREC, 0);
    return builder.toString();
  }

  public static boolean compare(Level level1, Level level2, Equations.CMP cmp, Equations equations, Abstract.SourceNode sourceNode) {
    if (cmp == Equations.CMP.GE) {
      return compare(level2, level1, Equations.CMP.LE, equations, sourceNode);
    }

    if (level1.isInfinity()) {
      return level2.isInfinity() || !level2.isClosed() && equations.add(level2, INFINITY, Equations.CMP.EQ, sourceNode);
    }
    if (level2.isInfinity()) {
      return cmp == Equations.CMP.LE || !level1.isClosed() && equations.add(level1, INFINITY, Equations.CMP.EQ, sourceNode);
    }

    if (level1.getVar() == null && cmp == Equations.CMP.LE) {
      if (level1.getConstant() <= level2.getConstant() || level1.getConstant() <= level2.getMaxConstant()) {
        return true;
      }
    }

    if (level1.getVar() == level2.getVar()) {
      if (cmp == Equations.CMP.LE) {
        return level1.getConstant() <= level2.getConstant() && level1.getMaxConstant() <= level2.getMaxConstant();
      }
      return level1.getConstant() == level2.getConstant() && level1.getMaxConstant() == level2.getMaxConstant();
    } else {
      return equations.add(level1, level2, cmp, sourceNode);
    }
  }

  public boolean isLessOrEquals(Level level) {
    return compare(this, level, Equations.CMP.LE, DummyEquations.getInstance(), null);
  }

  public static List<Level> map(List<? extends LevelVariable> variables) {
    List<Level> levels = new ArrayList<>();
    for (LevelVariable var : variables) {
      levels.add(new Level(var));
    }
    return levels;
  }
}
