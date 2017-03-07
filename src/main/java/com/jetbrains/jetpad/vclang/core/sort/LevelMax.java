package com.jetbrains.jetpad.vclang.core.sort;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.expr.factory.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ToAbstractVisitor;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintable;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.*;

public class LevelMax implements PrettyPrintable {
  private Map<LevelVariable, Integer> myVars;

  public static final LevelMax INFINITY = new LevelMax((Map<LevelVariable, Integer>) null);

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

  public LevelMax(LevelMax level) {
    myVars = new HashMap<>();
    myVars.putAll(level.myVars);
  }

  private LevelMax(Map<LevelVariable, Integer> vars) {
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
    if (level.getMaxConstant() != 0) {
      add(result.myVars, null, level.getMaxConstant());
    }
    return result;
  }

  public void add(LevelMax level) {
    if (level.isInfinity() || isInfinity()) {
      myVars = null;
    } else {
      for (Map.Entry<LevelVariable, Integer> entry : level.myVars.entrySet()) {
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
      if (level.getMaxConstant() != 0) {
        add(myVars, null, level.getMaxConstant());
      }
    }
  }

  private void add(Map<LevelVariable, Integer> result, LevelVariable var, int constant) {
    Integer sucs = myVars.get(var);
    if (sucs != null) {
      constant = Math.max(constant, sucs);
    }
    if (var != null || constant != 0) {
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

    for (Map.Entry<LevelVariable, Integer> entry : myVars.entrySet()) {
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
    for (Map.Entry<LevelVariable, Integer> entry : myVars.entrySet()) {
      for (Map.Entry<LevelVariable, Integer> entry1 : levels.myVars.entrySet()) {
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

    for (Map.Entry<LevelVariable, Integer> entry : myVars.entrySet()) {
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

    Map<LevelVariable, Integer> vars = new HashMap<>();
    for (Map.Entry<LevelVariable, Integer> entry : myVars.entrySet()) {
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
      Map.Entry<LevelVariable, Integer> entry = myVars.entrySet().iterator().next();
      return new Level(entry.getKey(), entry.getValue());
    }
    return null;
  }

  public List<Level> toListOfLevels() {
    if (isInfinity()) {
      return Collections.singletonList(Level.INFINITY);
    }

    List<Level> result = new ArrayList<>(myVars.size());
    for (Map.Entry<LevelVariable, Integer> var : myVars.entrySet()) {
      result.add(new Level(var.getKey(), var.getValue()));
    }
    return result;
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec, int indent) {
    ToAbstractVisitor toAbsVisitor = new ToAbstractVisitor(new ConcreteExpressionFactory(), names);
    toAbsVisitor.visitLevelMax(this, 0).accept(new PrettyPrintVisitor(builder, indent), prec);
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
