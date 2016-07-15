package com.jetbrains.jetpad.vclang.term.expr.subst;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LevelSubstitution {
  private Map<Binding, Level> mySubstExprs = new HashMap<>();

  public LevelSubstitution() {}

  public LevelSubstitution(Binding l, Level expr) {
    mySubstExprs.put(l, expr);
  }

  public LevelSubstitution(Binding lp, Level lp_expr, Binding lh, Level lh_expr) {
    mySubstExprs.put(lp, lp_expr);
    mySubstExprs.put(lh, lh_expr);
  }

  public LevelSubstitution(Binding lp, Binding lp_new, Binding lh, Binding lh_new) {
    mySubstExprs.put(lp, new Level(lp_new));
    mySubstExprs.put(lh, new Level(lh_new));
  }

  public Set<Binding> getDomain() {
    return mySubstExprs.keySet();
  }

  public Level get(Binding binding)  {
    return mySubstExprs.get(binding);
  }

  public void add(Binding binding, Level expr) {
    mySubstExprs.put(binding, expr);
  }

  public void add(LevelSubstitution subst) {
    mySubstExprs.putAll(subst.mySubstExprs);
  }

  public void subst(Binding binding, Level expr) {
    for (Map.Entry<Binding, Level> var : mySubstExprs.entrySet()) {
      var.setValue(var.getValue().subst(binding, expr));
    }
  }

  public LevelSubstitution filter(List<Binding> params) {
    LevelSubstitution result = new LevelSubstitution();
    for (Binding param : params) {
      if (mySubstExprs.containsKey(param)) {
        result.add(param, mySubstExprs.get(param));
      }
    }
    return result;
  }

  public LevelSubstitution compose(LevelSubstitution subst, Set<Binding> params) {
    LevelSubstitution result = new LevelSubstitution();
    result.add(this);
    for (Binding binding : subst.getDomain()) {
      if (mySubstExprs.containsKey(binding)) {
        result.add(binding, subst.get(binding));
        continue;
      }
      boolean foundInExprs = false;
      for (Map.Entry<Binding, Level> substExpr : mySubstExprs.entrySet()) {
        if (substExpr.getValue().findBinding(binding)) {
          result.add(substExpr.getKey(), substExpr.getValue().subst(subst));
          foundInExprs = true;
        }
      }
      if (!foundInExprs) {
        boolean exists = false;
        for (Binding myBinding : getDomain()) {
          if (myBinding.getType().toDataCall().getDefinition() == binding.getType().toDefCall().getDefinition()) {
            exists = true;
            break;
          }
        }
        if (!exists && params.contains(binding)) {
          result.add(binding, subst.get(binding));
        }
      }
    }
    return result;
  }
}
