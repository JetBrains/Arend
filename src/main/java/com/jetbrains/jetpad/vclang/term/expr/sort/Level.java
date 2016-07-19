package com.jetbrains.jetpad.vclang.term.expr.sort;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.factory.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ToAbstractVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.Collections;
import java.util.List;

public class Level implements PrettyPrintable {
  private final int myConstant;
  private final Binding myVar;

  public static final Level INFINITY = new Level(null, -1);

  public Level(Binding var, int constant) {
    myConstant = constant;
    myVar = var;
  }

  public Level(int constant) {
    this(null, constant);
  }

  public Binding getVar() {
    return myVar;
  }

  public int getConstant() {
    return myConstant;
  }

  public boolean isInfinity() {
    return myConstant == -1;
  }

  public boolean isZero() {
    return myConstant == 0 && myVar == null;
  }

  public boolean isClosed() {
    return myVar == null;
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

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec, int indent) {
    new ToAbstractVisitor(new ConcreteExpressionFactory(), names).visitLevel(this).accept(new PrettyPrintVisitor(builder, indent), prec);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    prettyPrint(builder, Collections.<String>emptyList(), Abstract.Expression.PREC, 0);
    return builder.toString();
  }

  public static boolean compare(Level level1, Level level2, Equations.CMP cmp, Equations equations, Abstract.SourceNode sourceNode) {
    if (level1.isClosed() && level2.isClosed()) {
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

  /*
  public Level subst(Binding var, Level level) {
    if (isInfinity()) return new Level();
    Level result = new Level(myNumSucsOfVars, myConstant);
    Integer sucs = myNumSucsOfVars.get(var);
    if (sucs == null) {
      return result;
    }
    if (level.isInfinity()) return new Level();
    result.myNumSucsOfVars.remove(var);
    result.myConstant += level.myConstant;
    for (Map.Entry<Binding, Integer> var_ : level.myNumSucsOfVars.entrySet()) {
      result = result.max(new Level(var_.getKey(), var_.getValue() + sucs));
    }
    if (!result.myNumSucsOfVars.isEmpty() && result.extractOuterSucs() == result.myConstant) {
      result.myConstant = 0;
    }
    return result;
  }

  public int getNumOfMaxArgs() {
    if (myConstant == 0) return Math.max(myNumSucsOfVars.size(), 1);
    return myNumSucsOfVars.size() + 1;
  }

  public Integer getNumSucs(Binding binding) {
    if (myNumSucsOfVars.containsKey(binding)) {
      return myNumSucsOfVars.get(binding);
    }
    return null;
  }

  public Set<Binding> getAllBindings() {
    return myNumSucsOfVars.keySet();
  }

  public boolean isBinding() { return !isClosed() && getUnitSucs() == 0; }

  public int getUnitSucs() {
    if (isClosed()) {
      return myConstant;
    }
    return myNumSucsOfVars.entrySet().iterator().next().getValue();
  }

  public boolean findBinding(Binding binding) { return myNumSucsOfVars.containsKey(binding); }

  public Binding getUnitBinding() {
    if (myNumSucsOfVars.size() != 1) return null;
    return myNumSucsOfVars.entrySet().iterator().next().getKey();
  }

  public Level subtract(int val) {
    Level result = new Level(this);
    if (isClosed() || myConstant != 0) {
      result.myConstant = Math.max(myConstant - val, 0);
    }
    for (Map.Entry<Binding, Integer> var : result.myNumSucsOfVars.entrySet()) {
      var.setValue(Math.max(var.getValue() - val, 0));
    }
    return result;
  }

  private boolean containsInferVar() {
    for (Binding bnd : getAllBindings()) {
      if (bnd instanceof InferenceBinding) {
        return true;
      }
    }
    return false;
  }

  public int extractOuterSucs() {
    if (myNumSucsOfVars.isEmpty()) {
      return myConstant;
    }
    return myConstant > 0 ? Math.min(myConstant, Collections.min(myNumSucsOfVars.values())) : Collections.min(myNumSucsOfVars.values());
  }
  */
}
