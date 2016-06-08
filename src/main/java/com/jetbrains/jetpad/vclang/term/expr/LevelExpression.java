package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.*;

public class LevelExpression implements PrettyPrintable {
  private int myConstant = 0;
  private Map<Binding, Integer> myNumSucsOfVars = new HashMap<>();
  private boolean myIsInfinity = true;

  public LevelExpression() { }

  public LevelExpression(int constant) {
    myConstant = constant;
    myIsInfinity = false;
  }

  public LevelExpression(Binding var, int numSucs) {
    myNumSucsOfVars.put(var, numSucs);
    myIsInfinity = false;
  }

  public LevelExpression(Map<Binding, Integer> numSucsOfVars, int constant) {
    myNumSucsOfVars = new HashMap<>(numSucsOfVars);
    myConstant = constant;
    myIsInfinity = false;
  }

  public LevelExpression(LevelExpression level) {
    myNumSucsOfVars = new HashMap<>(level.myNumSucsOfVars);
    myConstant = level.myConstant;
    myIsInfinity = level.myIsInfinity;
  }

  public LevelExpression max(LevelExpression other) {
    if (other.isInfinity()) return new LevelExpression(other);
    if (isInfinity()) return new LevelExpression(this);

    LevelExpression result = new LevelExpression(other.myNumSucsOfVars, Math.max(myConstant, other.myConstant));

    for (Map.Entry<Binding, Integer> var : myNumSucsOfVars.entrySet()) {
      Integer sucs = result.myNumSucsOfVars.get(var.getKey());
      if (sucs != null) {
        result.myNumSucsOfVars.put(var.getKey(), Math.max(var.getValue(), sucs));
      } else {
        result.myNumSucsOfVars.put(var.getKey(), var.getValue());
      }
    }

    return result;
  }

  public LevelExpression subst(Binding var, LevelExpression level) {
    if (isInfinity()) return new LevelExpression();
    LevelExpression result = new LevelExpression(myNumSucsOfVars, myConstant);
    Integer sucs = myNumSucsOfVars.get(var);
    if (sucs == null) {
      return result;
    }
    if (level.isInfinity()) return new LevelExpression();
    result.myNumSucsOfVars.remove(var);
    result.myConstant += level.myConstant;
    for (Map.Entry<Binding, Integer> var_ : level.myNumSucsOfVars.entrySet()) {
      result = result.max(new LevelExpression(var_.getKey(), var_.getValue() + sucs));
    }
    if (!result.myNumSucsOfVars.isEmpty() && result.extractOuterSucs() == result.myConstant) {
      result.myConstant = 0;
    }
    return result; /**/
  }

  public LevelExpression subst(LevelSubstitution subst) {
    LevelExpression result = this;
    for (Binding var : subst.getDomain()) {
      result = result.subst(var, subst.get(var));
    }
    return result;
  }

  public List<LevelExpression> toListOfMaxArgs() {
    if (isInfinity()) return Collections.singletonList(new LevelExpression());
    ArrayList<LevelExpression> list = new ArrayList<>();
    list.add(new LevelExpression(myConstant));
    for (Map.Entry<Binding, Integer> var : myNumSucsOfVars.entrySet()) {
      list.add(new LevelExpression(var.getKey(), var.getValue()));
    }
    return list;
  }

  public boolean isZero() {
    return !myIsInfinity && myConstant == 0 && myNumSucsOfVars.isEmpty();
  }

  public boolean isClosed() {
    return myIsInfinity || myNumSucsOfVars.isEmpty();
  }

  public boolean isUnit() {
    return isClosed() || (myNumSucsOfVars.size() == 1 && (myConstant == 0));
  }

  public boolean isBinding() { return isUnit() && (!isClosed()) && getUnitSucs() == 0; }

  public int getUnitSucs() {
    if (isClosed() || !isUnit()) {
      return myConstant;
    }
    return myNumSucsOfVars.entrySet().iterator().next().getValue();
  }

  public boolean findBinding(Binding binding) { return myNumSucsOfVars.containsKey(binding); }

  public Binding getUnitBinding() {
    if (myNumSucsOfVars.size() != 1) return null;
    return myNumSucsOfVars.entrySet().iterator().next().getKey();
  }

  public LevelExpression subtract(int val) {
    LevelExpression result = new LevelExpression(this);
    result.myConstant = Math.max(myConstant - val, 0);
    for (Map.Entry<Binding, Integer> var : result.myNumSucsOfVars.entrySet()) {
      var.setValue(Math.max(var.getValue() - val, 0));
    }
    return result;
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec, int indent) {

  }

  public enum CMP { LESS, GREATER, EQUAL, NOT_COMPARABLE }

  public static boolean compare(LevelExpression expr1, LevelExpression expr2, Equations.CMP cmp) {
    return compare(expr1, expr2, cmp, DummyEquations.getInstance());
  }

  public static boolean compare(LevelExpression expr1, LevelExpression expr2, Equations.CMP cmp, Equations equations) {

  }

  public CMP compare(LevelExpression other) {
     if (compare(this, other, Equations.CMP.EQ)) {
      return CMP.EQUAL;
    }

    if (compare(this, other, Equations.CMP.GE)) {
      return CMP.GREATER;
    }

    if (compare(this, other, Equations.CMP.LE)) {
      return CMP.LESS;
    }

    return CMP.NOT_COMPARABLE;
  }

  public LevelExpression succ() {
    return subtract(-1);
  }

  public boolean isInfinity() { return myIsInfinity; }

  public int extractOuterSucs() {
    if (myNumSucsOfVars.isEmpty()) {
      return myConstant;
    }
    return myConstant > 0 ? Math.min(myConstant, Collections.min(myNumSucsOfVars.values())) : Collections.min(myNumSucsOfVars.values());
  }

}
