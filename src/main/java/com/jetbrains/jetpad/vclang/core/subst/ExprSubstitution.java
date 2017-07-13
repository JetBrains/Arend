package com.jetbrains.jetpad.vclang.core.subst;

import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.expr.Expression;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ExprSubstitution {
  private Map<Variable, Expression> mySubstExprs;

  public ExprSubstitution() {
    mySubstExprs = Collections.emptyMap();
  }

  public ExprSubstitution(Variable from, Expression to) {
    mySubstExprs = new HashMap<>();
    add(from, to);
  }

  public Set<Map.Entry<Variable, Expression>> getEntries() {
    return mySubstExprs.entrySet();
  }

  public boolean isEmpty() {
    return mySubstExprs.isEmpty();
  }

  public Expression get(Variable binding)  {
    return mySubstExprs.get(binding);
  }

  public void clear() {
    mySubstExprs.clear();
  }

  public void remove(Variable variable) {
    mySubstExprs.remove(variable);
  }

  public void add(Variable binding, Expression expression) {
    if (mySubstExprs.isEmpty()) {
      mySubstExprs = new HashMap<>();
    }
    mySubstExprs.put(binding, expression);
  }

  public void addAll(ExprSubstitution substitution) {
    if (!substitution.mySubstExprs.isEmpty()) {
      if (mySubstExprs.isEmpty()) {
        mySubstExprs = new HashMap<>();
      }
      mySubstExprs.putAll(substitution.mySubstExprs);
    }
  }

  public void subst(ExprSubstitution subst) {
    for (Map.Entry<Variable, Expression> entry : mySubstExprs.entrySet()) {
      entry.setValue(entry.getValue().subst(subst));
    }
  }

  public String toString() {
    return mySubstExprs.toString();
  }
}