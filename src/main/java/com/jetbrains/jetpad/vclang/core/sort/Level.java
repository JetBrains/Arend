package com.jetbrains.jetpad.vclang.core.sort;

import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.expr.factory.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ToAbstractVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Level implements PrettyPrintable {
  private final int myConstant;
  private final LevelVariable myVar;

  public static final Level INFINITY = new Level(null, -1);

  public Level(LevelVariable var, int constant) {
    myConstant = constant;
    myVar = var;
  }

  public Level(LevelVariable var) {
    myConstant = 0;
    myVar = var;
  }

  public Level(int constant) {
    myConstant = constant;
    myVar = null;
  }

  public LevelVariable getVar() {
    return myVar;
  }

  public int getConstant() {
    return myConstant;
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
    return isInfinity() ? this : new Level(myVar, myConstant + constant);
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
    new ToAbstractVisitor(new ConcreteExpressionFactory(), names).visitLevel(this, 0).accept(new PrettyPrintVisitor(builder, indent), prec);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    prettyPrint(builder, Collections.<String>emptyList(), Abstract.Expression.PREC, 0);
    return builder.toString();
  }

  public static boolean compare(Level level1, Level level2, Equations.CMP cmp, Equations equations, Abstract.SourceNode sourceNode) {
    if (level1.isInfinity()) {
      if (level2.isInfinity() || cmp == Equations.CMP.GE) {
        return true;
      } else {
        return !level2.isClosed() && equations.add(level2, INFINITY, Equations.CMP.EQ, sourceNode);
      }
    }
    if (level2.isInfinity()) {
      if (cmp == Equations.CMP.LE) {
        return true;
      } else {
        return !level1.isClosed() && equations.add(level1, INFINITY, Equations.CMP.EQ, sourceNode);
      }
    }

    if (level1.getVar() == null && cmp == Equations.CMP.LE) {
      if (level1.getConstant() <= level2.getConstant()) {
        return true;
      }
    }

    if (level2.getVar() == null && cmp == Equations.CMP.GE) {
      if (level1.getConstant() >= level2.getConstant()) {
        return true;
      }
    }

    if (level1.getVar() == level2.getVar()) {
      if (cmp == Equations.CMP.LE) {
        return level1.getConstant() <= level2.getConstant();
      }
      if (cmp == Equations.CMP.GE) {
        return level1.getConstant() >= level2.getConstant();
      }
      return level1.getConstant() == level2.getConstant();
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
