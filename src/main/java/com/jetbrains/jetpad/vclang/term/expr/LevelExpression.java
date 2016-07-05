package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.InferenceBinding;
import com.jetbrains.jetpad.vclang.term.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.term.expr.visitor.PrettyPrintVisitor;
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

  public LevelExpression(Binding var) {
    myNumSucsOfVars.put(var, 0);
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

  // TODO: more clever max s.t. max(suc x, 1) => suc x

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
    if (myConstant != 0 || myNumSucsOfVars.isEmpty()) {
      list.add(new LevelExpression(myConstant));
    }
    for (Map.Entry<Binding, Integer> var : myNumSucsOfVars.entrySet()) {
      list.add(new LevelExpression(var.getKey(), var.getValue()));
    }
    return list;
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

  public int getConstant() {
    return myConstant;
  }

  public Set<Binding> getAllBindings() {
    return myNumSucsOfVars.keySet();
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
    if (isClosed() || myConstant != 0) {
      result.myConstant = Math.max(myConstant - val, 0);
    }
    for (Map.Entry<Binding, Integer> var : result.myNumSucsOfVars.entrySet()) {
      var.setValue(Math.max(var.getValue() - val, 0));
    }
    return result;
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec, int indent) {
    toAbstract().accept(new PrettyPrintVisitor(builder, names, indent), prec);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC, 0);
    return builder.toString();
  }

  public Abstract.Expression toAbstract() {
    if (isInfinity()) {
      return ConcreteExpressionFactory.cVar("inf");
    }

    Concrete.DefCallExpression suc = ConcreteExpressionFactory.cVar("suc");
    List<Concrete.Expression> maxArgExprList = new ArrayList<>();

    for (LevelExpression maxArg : toListOfMaxArgs()) {
      if (maxArg.isClosed()) {
        maxArgExprList.add(ConcreteExpressionFactory.cNum(maxArg.myConstant));
      } else {
        Concrete.Expression argExpr = ConcreteExpressionFactory.cVar(maxArg.getUnitBinding().getName());
        for (int i = 0; i < maxArg.getUnitSucs(); ++i) {
          argExpr = ConcreteExpressionFactory.cApps(suc, argExpr);
        }
        maxArgExprList.add(argExpr);
      }
    }

    if (maxArgExprList.size() == 1) {
      return maxArgExprList.get(0);
    }

    Concrete.DefCallExpression max = ConcreteExpressionFactory.cVar("max");
    Concrete.Expression result = maxArgExprList.get(0);

    for (int i = 1; i < maxArgExprList.size(); ++i) {
      result = ConcreteExpressionFactory.cApps(max, maxArgExprList.get(i), result);
    }

    return result;
  }

  public enum CMP { LESS, GREATER, EQUAL, NOT_COMPARABLE }

  public static boolean compare(LevelExpression expr1, LevelExpression expr2, Equations.CMP cmp) {
    return compare(expr1, expr2, cmp, DummyEquations.getInstance());
  }

  private boolean containsInferVar() {
    for (Binding bnd : getAllBindings()) {
      if (bnd instanceof InferenceBinding) {
        return true;
      }
    }
    return false;
  }

  public static boolean compare(LevelExpression expr1, LevelExpression expr2, Equations.CMP cmp, Equations equations) {
    LevelExpression level1 = cmp == Equations.CMP.GE ? expr1 : expr2;
    LevelExpression level2 = cmp == Equations.CMP.GE ? expr2 : expr1;
    boolean leftContainsInferenceBnd = level1.containsInferVar();
    boolean rightContainsInferenceBnd = level2.containsInferVar();

    if (level1.isInfinity()) {
      if (rightContainsInferenceBnd) {
        equations.add(level2.subtract(level2.extractOuterSucs()), level1, cmp == Equations.CMP.EQ ? Equations.CMP.EQ : Equations.CMP.LE, null);
        return true;
      }
      if (cmp != Equations.CMP.EQ || level2.isInfinity()) {
        return true;
      }
      return false;
    }

    if (level2.isInfinity()) {
      if (leftContainsInferenceBnd && !(equations instanceof DummyEquations)) {
        equations.add(level1, level2.subtract(level2.extractOuterSucs()), cmp == Equations.CMP.EQ ? Equations.CMP.EQ : Equations.CMP.GE, null);
        return true;
      }
      return false;
    }

    if (cmp == Equations.CMP.EQ) {
      if (level1.isClosed() && level2.isClosed()) {
        return level1.getConstant() == level2.getConstant();
      }
      boolean equal = true;
      if (level1.getNumOfMaxArgs() != level2.getNumOfMaxArgs()) {
        equal = false;
      } else {
        for (LevelExpression rightMaxArg : level2.toListOfMaxArgs()) {
          if (rightMaxArg.isClosed()) {
            if (level1.getConstant() != rightMaxArg.getUnitSucs()) {
              equal = false;
              break;
            }
          } else if (!level1.findBinding(rightMaxArg.getUnitBinding()) || level1.getNumSucs(rightMaxArg.getUnitBinding()) != rightMaxArg.getUnitSucs()) {
            equal = false;
            break;
          }
        }
      }
      //if (equal) {
      //  return true;
     // }
      if ((!rightContainsInferenceBnd && !leftContainsInferenceBnd) || (equations instanceof DummyEquations)) {
        return equal;
      }

      int externalSucs = Math.min(level1.extractOuterSucs(), level2.extractOuterSucs());

      equations.add(level1.subtract(externalSucs), level2.subtract(externalSucs), Equations.CMP.EQ, null);
      return true;
    }

    int leftSucs = level1.extractOuterSucs();
    LevelExpression leftLevel = level1.subtract(leftSucs);

    for (LevelExpression rightMaxArg : level2.toListOfMaxArgs()) {
      int rightSucs = rightMaxArg.getUnitSucs();
      int sucsToRemove = Math.min(leftSucs, rightSucs);

      if (rightMaxArg.isClosed()) {
        if (leftLevel.isClosed() && leftSucs >= rightSucs) {
          continue;
        }
        if (!leftContainsInferenceBnd || (equations instanceof DummyEquations)) {
          return false;
        }
        equations.add(level1.subtract(sucsToRemove), rightMaxArg.subtract(sucsToRemove), Equations.CMP.GE, null);
      } else if (leftLevel.findBinding(rightMaxArg.getUnitBinding())) {
        if (leftLevel.getNumSucs(rightMaxArg.getUnitBinding()) + leftSucs < rightSucs) {
          return false;
        }
      } else {
        if ((leftContainsInferenceBnd || (rightMaxArg.getUnitBinding() instanceof InferenceBinding)) && !(equations instanceof DummyEquations)) {
          equations.add(level1.subtract(sucsToRemove), rightMaxArg.subtract(sucsToRemove), Equations.CMP.GE, null);
          continue;
        }
        return false;
      }
    }

    return true;
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

  @Override
  public boolean equals(Object other) {
    return other instanceof LevelExpression && compare((LevelExpression)other) == CMP.EQUAL;
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
