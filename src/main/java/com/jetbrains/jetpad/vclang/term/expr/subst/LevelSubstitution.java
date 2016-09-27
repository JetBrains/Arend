package com.jetbrains.jetpad.vclang.term.expr.subst;

import com.jetbrains.jetpad.vclang.term.context.binding.Variable;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

  public LevelSubstitution subst(LevelSubstitution subst) {
    if (subst.getDomain().isEmpty()) {
      return this;
    }

    LevelSubstitution result = new LevelSubstitution();
    result.mySubstExprs = new HashMap<>(mySubstExprs);
    for (Map.Entry<Variable, Level> entry : result.mySubstExprs.entrySet()) {
      entry.setValue(entry.getValue().subst(subst));
    }
    return result;
  }
}
