package com.jetbrains.jetpad.vclang.term.expr.sort;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.context.binding.Variable;
import com.jetbrains.jetpad.vclang.term.expr.factory.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ToAbstractVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.*;

public class LevelMax implements PrettyPrintable {
  private Map<Variable, Integer> myVars;

  public static final LevelMax INFINITY = new LevelMax((Map<Variable, Integer>) null);

  public LevelMax() {
    myVars = new HashMap<>();
  }

  public LevelMax(Level level) {
    if (level.isInfinity()) {
      myVars = null;
    } else {
      myVars = new HashMap<>();
      add(level);
    }
  }

  private LevelMax(Map<Variable, Integer> vars) {
    myVars = vars;
  }

  public boolean isMinimum() {
    return myVars != null && myVars.isEmpty();
  }

  public boolean isInfinity() {
    return myVars == null;
  }

  public LevelMax max(LevelMax level) {
    if (level.isInfinity() || isInfinity()) {
      return INFINITY;
    }

    LevelMax result = new LevelMax(new HashMap<>(myVars));
    result.add(level);
    return result;
  }

  public LevelMax max(Level level) {
    if (level.isMinimum()) {
      return this;
    }
    if (level.isInfinity() || isInfinity()) {
      return INFINITY;
    }

    LevelMax result = new LevelMax(new HashMap<>(myVars));
    add(result.myVars, level.getVar(), level.getConstant());
    return result;
  }

  public void add(LevelMax level) {
    if (level.isInfinity() || isInfinity()) {
      myVars = null;
    } else {
      for (Map.Entry<Variable, Integer> entry : level.myVars.entrySet()) {
        add(myVars, entry.getKey(), entry.getValue());
      }
    }
  }

  public void add(Level level) {
    if (level.isMinimum()) {
      return;
    }
    if (level.isInfinity() || isInfinity()) {
      myVars = null;
    } else {
      add(myVars, level.getVar(), level.getConstant());
    }
  }

  private void add(Map<Variable, Integer> result, Variable var, int constant) {
    Integer sucs = myVars.get(var);
    if (sucs != null) {
      result.put(var, Math.max(constant, sucs));
    } else {
      result.put(var, constant);
    }
  }

  public boolean isLessOrEquals(Level level) {
    if (level.isInfinity()) {
      return true;
    }
    if (isInfinity()) {
      return false;
    }

    for (Map.Entry<Variable, Integer> entry : myVars.entrySet()) {
      if (!new Level(entry.getKey(), entry.getValue()).isLessOrEquals(level)) {
        return false;
      }
    }
    return true;
  }

  public boolean isLessOrEquals(LevelMax levels) {
    if (levels.isInfinity()) {
      return true;
    }
    if (isInfinity()) {
      return false;
    }

    loop:
    for (Map.Entry<Variable, Integer> entry : myVars.entrySet()) {
      for (Map.Entry<Variable, Integer> entry1 : levels.myVars.entrySet()) {
        if (new Level(entry.getKey(), entry.getValue()).isLessOrEquals(new Level(entry1.getKey(), entry1.getValue()))) {
          continue loop;
        }
      }
      return false;
    }
    return true;
  }

  public boolean isLessOrEquals(Level level, Equations equations, Abstract.SourceNode sourceNode) {
    if (level.isInfinity()) {
      return true;
    }
    if (isInfinity()) {
      return !level.isClosed() && equations.add(level, Level.INFINITY, Equations.CMP.EQ, sourceNode);
    }

    for (Map.Entry<Variable, Integer> entry : myVars.entrySet()) {
      if (!Level.compare(new Level(entry.getKey(), entry.getValue()), level, Equations.CMP.LE, equations, sourceNode)) {
        return false;
      }
    }
    return true;
  }

  public LevelMax subst(LevelSubstitution subst) {
    if (myVars == null || myVars.isEmpty() || subst.isEmpty()) {
      return this;
    }

    Map<Variable, Integer> vars = new HashMap<>();
    for (Map.Entry<Variable, Integer> entry : myVars.entrySet()) {
      Level level = subst.get(entry.getKey());
      if (level == null) {
        vars.put(entry.getKey(), entry.getValue());
      } else {
        add(vars, level.getVar(), level.getConstant());
      }
    }
    return new LevelMax(vars);
  }

  public Level toLevel() {
    if (isInfinity()) {
      return Level.INFINITY;
    }
    if (isMinimum()) {
      return new Level(0);
    }
    if (myVars.size() == 1) {
      Map.Entry<Variable, Integer> entry = myVars.entrySet().iterator().next();
      return new Level(entry.getKey(), entry.getValue());
    }
    return null;
  }

  public List<Level> toListOfLevels() {
    if (isInfinity()) {
      return Collections.singletonList(Level.INFINITY);
    }

    List<Level> result = new ArrayList<>(myVars.size());
    for (Map.Entry<Variable, Integer> var : myVars.entrySet()) {
      result.add(new Level(var.getKey(), var.getValue()));
    }
    return result;
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec, int indent) {
    new ToAbstractVisitor(new ConcreteExpressionFactory(), names).visitLevelMax(this, 0).accept(new PrettyPrintVisitor(builder, indent), prec);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    prettyPrint(builder, Collections.<String>emptyList(), Abstract.Expression.PREC, 0);
    return builder.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    LevelMax levelMax = (LevelMax) o;

    return myVars != null ? myVars.equals(levelMax.myVars) : levelMax.myVars == null;
  }

  @Override
  public int hashCode() {
    return myVars != null ? myVars.hashCode() : 0;
  }
}
