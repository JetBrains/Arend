package com.jetbrains.jetpad.vclang.term.expr.subst;

import com.jetbrains.jetpad.vclang.term.context.binding.Variable;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;

import java.util.*;

public class LevelSubstitution {
  private Map<Variable, Level> mySubstExprs;

  public LevelSubstitution() {
    mySubstExprs = Collections.emptyMap();
  }

  public LevelSubstitution(Variable l, Level expr) {
    mySubstExprs = new HashMap<>();
    mySubstExprs.put(l, expr);
  }

  public LevelSubstitution(Variable lp, Level lpExpr, Variable lh, Level lhExpr) {
    mySubstExprs = new HashMap<>();
    mySubstExprs.put(lp, lpExpr);
    mySubstExprs.put(lh, lhExpr);
  }

  public LevelSubstitution(Variable lp, Variable lpNew, Variable lh, Variable lhNew) {
    mySubstExprs = new HashMap<>();
    mySubstExprs.put(lp, new Level(lpNew));
    mySubstExprs.put(lh, new Level(lhNew));
  }

  public Set<Variable> getDomain() {
    return mySubstExprs.keySet();
  }

  public Level get(Variable var)  {
    return mySubstExprs.get(var);
  }

  public void add(Variable var, Level expr) {
    if (mySubstExprs.isEmpty()) {
      mySubstExprs = new HashMap<>();
    }
    mySubstExprs.put(var, expr);
  }

  public void add(LevelSubstitution subst) {
    if (mySubstExprs.isEmpty()) {
      mySubstExprs = new HashMap<>();
    }
    mySubstExprs.putAll(subst.mySubstExprs);
  }

  public void subst(Variable var, Level expr) {
    for (Map.Entry<Variable, Level> entry : mySubstExprs.entrySet()) {
      entry.setValue(entry.getValue().subst(var, expr));
    }
  }

  public LevelSubstitution compose(LevelSubstitution subst, Collection<? extends Variable> params) {
    LevelSubstitution result = new LevelSubstitution();
    result.add(this);

    loop:
    for (Variable var : subst.getDomain()) {
      if (mySubstExprs.containsKey(var)) {
        result.add(var, subst.get(var));
        continue;
      }

      for (Map.Entry<Variable, Level> substExpr : mySubstExprs.entrySet()) {
        if (substExpr.getValue().getVar() == var) {
          result.add(substExpr.getKey(), substExpr.getValue().subst(subst));
          continue loop;
        }
      }

      for (Variable var1 : getDomain()) {
        if (var1.getType().toDataCall().getDefinition() == var.getType().toDefCall().getDefinition()) {
          continue loop;
        }
      }

      if (params.contains(var)) {
        result.add(var, subst.get(var));
      }
    }
    return result;
  }
}
