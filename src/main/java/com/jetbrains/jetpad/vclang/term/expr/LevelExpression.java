package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.*;

public class LevelExpression extends Expression {
  private int myConstant = 0;
  private int myOuterSucs = 0;
  private Map<Binding, Integer> myNumSucsOfVars = new HashMap<>();
  private Converter myConv;
  private boolean myIsInfinity = true;

  public interface Converter {
    Expression getType();
    Expression convert(Binding var, int sucs);
    Expression convert(int sucs);
    Expression convert();
    LevelExpression convert(Expression expr);
    Expression max(Expression expr1, Expression expr2);
  }

  public LevelExpression(Converter conv) { myConv = conv; }

  public LevelExpression(int constant, Converter conv) {
    myOuterSucs = constant;
    myConv = conv;
    myIsInfinity = false;
  }

  public LevelExpression(Binding var, int numSucs, Converter conv) {
    myNumSucsOfVars.put(var, numSucs);
    myConv = conv;
    myIsInfinity = false;
  }

  public LevelExpression(Map<Binding, Integer> numSucsOfVars, int constant, int outerSucs, Converter conv) {
    myNumSucsOfVars = new HashMap<>(numSucsOfVars);
    myConstant = constant;
    myOuterSucs = outerSucs;
    myConv = conv;
    myIsInfinity = false;
  }

  public LevelExpression(LevelExpression level) {
    myNumSucsOfVars = new HashMap<>(level.myNumSucsOfVars);
    myConstant = level.myConstant;
    myOuterSucs = level.myOuterSucs;
    myConv = level.myConv;
    myIsInfinity = level.myIsInfinity;
  }

  public LevelExpression max(LevelExpression other) {
    if (other.isInfinity()) return new LevelExpression(other);
    if (isInfinity()) return new LevelExpression(this);
    LevelExpression result = new LevelExpression(other.myNumSucsOfVars, Math.max(myConstant, other.myConstant), Math.min(myOuterSucs, other.myOuterSucs), other.myConv);
    int thisSucDiff = Math.max(myOuterSucs - other.myOuterSucs, 0);
    int otherSucDiff = Math.max(other.myOuterSucs - myOuterSucs, 0);

    for (Map.Entry<Binding, Integer> var : myNumSucsOfVars.entrySet()) {
      Integer sucs = result.myNumSucsOfVars.get(var.getKey());
      if (sucs != null) {
        result.myNumSucsOfVars.put(var.getKey(), Math.max(var.getValue() + thisSucDiff, sucs + otherSucDiff));
      } else {
        result.myNumSucsOfVars.put(var.getKey(), var.getValue() + thisSucDiff);
      }
    }

    result.extractOuterSucs();
    return result;
  }

  public LevelExpression subst(Binding var, LevelExpression level) {
    if (isInfinity()) return new LevelExpression(myConv);
    LevelExpression result = new LevelExpression(myNumSucsOfVars, myConstant, myOuterSucs, myConv);
    Integer sucs = myNumSucsOfVars.get(var);
    if (sucs == null) {
      return result;
    }
    if (level.isInfinity()) return new LevelExpression(myConv);
    result.myNumSucsOfVars.remove(var);
    result.myConstant += level.myConstant + level.myOuterSucs;
    for (Map.Entry<Binding, Integer> var_ : level.myNumSucsOfVars.entrySet()) {
      result = result.max(new LevelExpression(var_.getKey(), var_.getValue() + sucs + level.myOuterSucs, myConv));
    }
    result.extractOuterSucs();
    return result; /**/
    /*if (isInfinity()) return this;
    Integer sucs = myNumSucsOfVars.get(var);
    if (sucs == null) {
      return this;
    }
    if (level.isInfinity()) return new LevelExpression(myConv);
    myNumSucsOfVars.remove(var);
    myConstant += level.myConstant;
    for (Map.Entry<Binding, Integer> var_ : level.myNumSucsOfVars.entrySet()) {
      Integer thisSucs = myNumSucsOfVars.get(var_.getKey());
      if (thisSucs == null) {
        myNumSucsOfVars.put(var_.getKey(), var_.getValue() + sucs);
      } else {
        myNumSucsOfVars.put(var_.getKey(), Integer.max(var_.getValue() + sucs, thisSucs));
      }
    }
    extractOuterSucs();
    return this; /**/
  }

  public List<LevelExpression> toListOfMaxArgs() {
    if (isInfinity()) return Collections.singletonList(new LevelExpression(myConv));
    ArrayList<LevelExpression> list = new ArrayList<>();
    list.add(new LevelExpression(myConstant + myOuterSucs, myConv));
    for (Map.Entry<Binding, Integer> var : myNumSucsOfVars.entrySet()) {
      list.add(new LevelExpression(var.getKey(), var.getValue() + myOuterSucs, myConv));
    }
    return list;
  }

  public int getOuterSucs() {
    return myOuterSucs;
  }

  public Converter getConverter() { return myConv; }

  public boolean isZero() {
    return !myIsInfinity && myOuterSucs == 0 && myConstant == 0 && myNumSucsOfVars.isEmpty();
  }

  public boolean isClosed() {
    return myIsInfinity || myNumSucsOfVars.isEmpty();
  }

  public boolean isUnit() {
    return isClosed() || (myNumSucsOfVars.size() == 1 && (myConstant == 0 && myOuterSucs == 0));
  }

  public int getUnitSucs() {
    if (isClosed() || !isUnit()) {
      return myOuterSucs;
    }
    return myNumSucsOfVars.entrySet().iterator().next().getValue();
  }

  public Binding getUnitBinding() {
    if (myNumSucsOfVars.size() != 1) return null;
    return myNumSucsOfVars.entrySet().iterator().next().getKey();
  }

  public LevelExpression subtract(int val) {
    LevelExpression result = new LevelExpression(this);
    if (myOuterSucs >= val) {
      result.myOuterSucs -= val;
      return result;
    }
    val -= myOuterSucs;
    for (Map.Entry<Binding, Integer> var : result.myNumSucsOfVars.entrySet()) {
      var.setValue(var.getValue() - val);
    }
    return result;
  }

  public Expression getExpr(int subtract) {
    if (myIsInfinity) return myConv.convert();
    Expression result = myConstant + myOuterSucs - subtract > 0 ? myConv.convert(myConstant + myOuterSucs - subtract) : null;
    for (Map.Entry<Binding, Integer> var : myNumSucsOfVars.entrySet()) {
      Expression expr = myConv.convert(var.getKey(), var.getValue() + myOuterSucs - subtract);
      if (result == null) {
        result = expr;
      } else {
        result = myConv.max(expr, result);
      }
    }
    return result != null ? result : myConv.convert(0);
  }

  public enum CMP { LESS, GREATER, EQUAL, NOT_COMPARABLE }

  public CMP compare(LevelExpression other) {
    if (Expression.compare(this, other, Equations.CMP.EQ)) {
      return CMP.EQUAL;
    }

    if (Expression.compare(this, other, Equations.CMP.GE)) {
      return CMP.GREATER;
    }

    if (Expression.compare(this, other, Equations.CMP.LE)) {
      return CMP.LESS;
    }

    return CMP.NOT_COMPARABLE;
  }

  public LevelExpression succ() {
    return new LevelExpression(myNumSucsOfVars, myConstant, myOuterSucs + 1, myConv);
  }

  public boolean isInfinity() { return myIsInfinity; }

  private void extractOuterSucs() {
    if (myNumSucsOfVars.isEmpty()) {
      myOuterSucs += myConstant;
      myConstant = 0;
      return;
    }
    int minSucs = Math.min(myConstant, Collections.min(myNumSucsOfVars.values()));
    if (minSucs > 0) {
      myOuterSucs += minSucs;
      myConstant -= minSucs;
      for (Map.Entry<Binding, Integer> var : myNumSucsOfVars.entrySet()) {
        var.setValue(var.getValue() - minSucs);
      }
    }
  }

  @Override
  public boolean equals(Object other) {
    return (other instanceof LevelExpression) && this.compare((LevelExpression) other) == CMP.EQUAL;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitLevel(this, params);
  }

  @Override
  public Expression getType() {
    return myConv.getType();
  }

  @Override
  public LevelExpression toLevel() { return this; }

  @Override
  public ReferenceExpression toReference() {
    if (isUnit() && !isClosed() && myNumSucsOfVars.entrySet().iterator().next().getValue() == 0) {
      return ExpressionFactory.Reference(myNumSucsOfVars.entrySet().iterator().next().getKey());
    }
    return null;
  }
}
