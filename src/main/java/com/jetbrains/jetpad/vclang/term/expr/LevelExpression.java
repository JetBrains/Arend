package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.*;

public class LevelExpression extends Expression {
  private int myConstant = 0;
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
    myConstant = constant;
    myConv = conv;
    myIsInfinity = false;
  }

  public LevelExpression(Binding var, int numSucs, Converter conv) {
    myNumSucsOfVars.put(var, numSucs);
    myConv = conv;
    myIsInfinity = false;
  }

  public LevelExpression(Map<Binding, Integer> numSucsOfVars, int constant, Converter conv) {
    myNumSucsOfVars = new HashMap<>(numSucsOfVars);
    myConstant = constant;
    myConv = conv;
    myIsInfinity = false;
  }

  public LevelExpression(LevelExpression level) {
    myNumSucsOfVars = new HashMap<>(level.myNumSucsOfVars);
    myConstant = level.myConstant;
    myConv = level.myConv;
    myIsInfinity = level.myIsInfinity;
  }

  public LevelExpression max(LevelExpression other) {
    if (other.isInfinity()) return new LevelExpression(other);
    if (isInfinity()) return new LevelExpression(this);

    LevelExpression result = new LevelExpression(other.myNumSucsOfVars, Math.max(myConstant, other.myConstant), other.myConv);

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
    if (isInfinity()) return new LevelExpression(myConv);
    LevelExpression result = new LevelExpression(myNumSucsOfVars, myConstant, myConv);
    Integer sucs = myNumSucsOfVars.get(var);
    if (sucs == null) {
      return result;
    }
    if (level.isInfinity()) return new LevelExpression(myConv);
    result.myNumSucsOfVars.remove(var);
    result.myConstant += level.myConstant;
    for (Map.Entry<Binding, Integer> var_ : level.myNumSucsOfVars.entrySet()) {
      result = result.max(new LevelExpression(var_.getKey(), var_.getValue() + sucs, myConv));
    }
    if (!result.myNumSucsOfVars.isEmpty() && result.extractOuterSucs() == result.myConstant) {
      result.myConstant = 0;
    }
    return result; /**/
  }

  public List<LevelExpression> toListOfMaxArgs() {
    if (isInfinity()) return Collections.singletonList(new LevelExpression(myConv));
    ArrayList<LevelExpression> list = new ArrayList<>();
    list.add(new LevelExpression(myConstant, myConv));
    for (Map.Entry<Binding, Integer> var : myNumSucsOfVars.entrySet()) {
      list.add(new LevelExpression(var.getKey(), var.getValue(), myConv));
    }
    return list;
  }

  public Converter getConverter() { return myConv; }

  public boolean isZero() {
    return !myIsInfinity && myConstant == 0 && myNumSucsOfVars.isEmpty();
  }

  public boolean isClosed() {
    return myIsInfinity || myNumSucsOfVars.isEmpty();
  }

  public boolean isUnit() {
    return isClosed() || (myNumSucsOfVars.size() == 1 && (myConstant == 0));
  }

  public int getUnitSucs() {
    if (isClosed() || !isUnit()) {
      return myConstant;
    }
    return myNumSucsOfVars.entrySet().iterator().next().getValue();
  }

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

  public Expression getExpr() {
    if (myIsInfinity) return myConv.convert();
    Expression result = myConstant > 0 ? myConv.convert(myConstant) : null;
    for (Map.Entry<Binding, Integer> var : myNumSucsOfVars.entrySet()) {
      Expression expr = myConv.convert(var.getKey(), var.getValue());
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
    return subtract(-1);
  }

  public boolean isInfinity() { return myIsInfinity; }

  public int extractOuterSucs() {
    if (myNumSucsOfVars.isEmpty()) {
      return myConstant;
    }
    return myConstant > 0 ? Math.min(myConstant, Collections.min(myNumSucsOfVars.values())) : Collections.min(myNumSucsOfVars.values());
  }

 // @Override
  //public boolean equals(Object other) {
  //  if (!(other instanceof Expression) || ((Expression) other).toLevel() == null) return false;
  //  return this.compare((LevelExpression) other) == CMP.EQUAL;
 // }

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
