package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LevelSubstitution {
  private Map<Binding, LevelExpression> mySubstExprs = new HashMap<>();

  public LevelSubstitution() {}

  public Set<Binding> getDomain() {
    return mySubstExprs.keySet();
  }

  public LevelExpression get(Binding binding)  {
    return mySubstExprs.get(binding);
  }

  public void add(Binding binding, LevelExpression expr) {
    mySubstExprs.put(binding, expr);
  }

  public void subst(Binding binding, LevelExpression expr) {
    for (Map.Entry<Binding, LevelExpression> var : mySubstExprs.entrySet()) {
      var.setValue(var.getValue().subst(binding, expr));
    }
  }
}
