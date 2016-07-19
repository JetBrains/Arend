package com.jetbrains.jetpad.vclang.term.expr.sort;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.factory.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.term.expr.visitor.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ToAbstractVisitor;

import java.util.*;

public class LevelMax implements PrettyPrintable {
  private Map<Binding, Integer> myVars;

  public static final LevelMax INFINITY = new LevelMax((Map<Binding, Integer>) null);

  public LevelMax() {
    myVars = new HashMap<>();
  }

  public LevelMax(Level level) {
    if (level.isInfinity()) {
      myVars = null;
    } else {
      myVars = new HashMap<>();
      myVars.put(level.getVar(), level.getConstant());
    }
  }

  private LevelMax(Map<Binding, Integer> vars) {
    myVars = vars;
  }

  public boolean isMinimum() {
    return myVars != null && myVars.isEmpty();
  }

  public boolean isInfinity() {
    return myVars == null;
  }

  public LevelMax max(LevelMax other) {
    if (other.isInfinity() || isInfinity()) {
      return INFINITY;
    }

    Map<Binding, Integer> result = new HashMap<>(myVars);
    for (Map.Entry<Binding, Integer> entry : other.myVars.entrySet()) {
      add(result, entry.getKey(), entry.getValue());
    }

    return new LevelMax(result);
  }

  public void add(Level level) {
    add(myVars, level.getVar(), level.getConstant());
  }

  private void add(Map<Binding, Integer> result, Binding var, int constant) {
    Integer sucs = myVars.get(var);
    if (sucs != null) {
      result.put(var, Math.max(constant, sucs));
    } else {
      result.put(var, constant);
    }
  }

  public boolean isLessOrEquals(Level level) {
    for (Map.Entry<Binding, Integer> entry : myVars.entrySet()) {
      if (!new Level(entry.getKey(), entry.getValue()).isLessOrEquals(level)) {
        return false;
      }
    }
    return true;
  }

  public boolean isLessOrEquals(LevelMax levels) {
    loop:
    for (Map.Entry<Binding, Integer> entry : myVars.entrySet()) {
      for (Map.Entry<Binding, Integer> entry1 : levels.myVars.entrySet()) {
        if (new Level(entry.getKey(), entry.getValue()).isLessOrEquals(new Level(entry1.getKey(), entry1.getValue()))) {
          continue loop;
        }
      }
      return false;
    }
    return true;
  }

  // TODO [sorts]
  @Deprecated
  public Level toLevel() {
    if (isInfinity()) {
      return Level.INFINITY;
    }
    if (myVars.isEmpty()) {
      return new Level(0);
    }
    Map.Entry<Binding, Integer> entry = myVars.entrySet().iterator().next();
    return new Level(entry.getKey(), entry.getValue());
  }

  public List<Level> toListOfLevels() {
    if (isInfinity()) {
      return Collections.singletonList(Level.INFINITY);
    }

    List<Level> result = new ArrayList<>(myVars.size());
    for (Map.Entry<Binding, Integer> var : myVars.entrySet()) {
      result.add(new Level(var.getKey(), var.getValue()));
    }
    return result;
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec, int indent) {
    new ToAbstractVisitor(new ConcreteExpressionFactory(), names).visitLevelMax(this).accept(new PrettyPrintVisitor(builder, indent), prec);
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

/*
  public LevelMax() { }

  public LevelMax(int constant) {
    myConstant = constant;
    myIsInfinity = false;
  }

  public LevelMax(Binding var, int numSucs) {
    myVars.put(var, numSucs);
    myIsInfinity = false;
  }

  public LevelMax(Binding var) {
    myVars.put(var, 0);
    myIsInfinity = false;
  }

  public LevelMax(Map<Binding, Integer> numSucsOfVars, int constant) {
    myVars = new HashMap<>(numSucsOfVars);
    myConstant = constant;
    myIsInfinity = false;
  }

  public LevelMax(LevelMax level) {
    myVars = new HashMap<>(level.myVars);
    myConstant = level.myConstant;
    myIsInfinity = level.myIsInfinity;
  }

  public LevelMax subst(Binding var, LevelMax level) {
    if (isInfinity()) return new LevelMax();
    LevelMax result = new LevelMax(myVars, myConstant);
    Integer sucs = myVars.get(var);
    if (sucs == null) {
      return result;
    }
    if (level.isInfinity()) return new LevelMax();
    result.myVars.remove(var);
    result.myConstant += level.myConstant;
    for (Map.Entry<Binding, Integer> var_ : level.myVars.entrySet()) {
      result = result.max(new LevelMax(var_.getKey(), var_.getValue() + sucs));
    }
    if (!result.myVars.isEmpty() && result.extractOuterSucs() == result.myConstant) {
      result.myConstant = 0;
    }
    return result;
  }

  public LevelMax subst(LevelSubstitution subst) {
    LevelMax result = this;
    for (Binding var : subst.getDomain()) {
      result = result.subst(var, subst.get(var));
    }
    return result;
  }

  public int getNumOfMaxArgs() {
    if (myConstant == 0) return Math.max(myVars.size(), 1);
    return myVars.size() + 1;
  }

  public Integer getNumSucs(Binding binding) {
    if (myVars.containsKey(binding)) {
      return myVars.get(binding);
    }
    return null;
  }

  public Set<Binding> getAllBindings() {
    return myVars.keySet();
  }

  public boolean isZero() {
    return !myIsInfinity && myConstant == 0 && myVars.isEmpty();
  }

  public boolean isUnit() {
    return isClosed() || (myVars.size() == 1 && (myConstant == 0));
  }

  public boolean isBinding() { return isUnit() && (!isClosed()) && getUnitSucs() == 0; }

  public int getUnitSucs() {
    if (isClosed() || !isUnit()) {
      return myConstant;
    }
    return myVars.entrySet().iterator().next().getValue();
  }

  public boolean findBinding(Binding binding) { return myVars.containsKey(binding); }

  public Binding getUnitBinding() {
    if (myVars.size() != 1) return null;
    return myVars.entrySet().iterator().next().getKey();
  }

  public LevelMax subtract(int val) {
    LevelMax result = new LevelMax(this);
    if (isClosed() || myConstant != 0) {
      result.myConstant = Math.max(myConstant - val, 0);
    }
    for (Map.Entry<Binding, Integer> var : result.myVars.entrySet()) {
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
    if (myVars.isEmpty()) {
      return myConstant;
    }
    return myConstant > 0 ? Math.min(myConstant, Collections.min(myVars.values())) : Collections.min(myVars.values());
  }
  */
}
