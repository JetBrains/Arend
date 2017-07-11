package com.jetbrains.jetpad.vclang.core.subst;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ReferenceExpression;

import java.util.*;

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

  public void removeAll(Collection<? extends Variable> variables) {
    mySubstExprs.keySet().removeAll(variables);
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

  public ExprSubstitution compose(ExprSubstitution subst) {
    ExprSubstitution result = new ExprSubstitution();
    for (Map.Entry<Variable, Expression> entry : subst.mySubstExprs.entrySet()) {
      result.add(entry.getKey(), entry.getValue().subst(this));
    }
    return result;
  }

  public List<TypedBinding> extendBy(List<Binding> context) {
    List<TypedBinding> result = new ArrayList<>();
    for (Binding binding : context) {
      result.add(new TypedBinding(binding.getName(), binding.getType().subst(this, LevelSubstitution.EMPTY)));
      add(binding, new ReferenceExpression(result.get(result.size() - 1)));
    }
    return result;
  }

  public static ExprSubstitution getIdentity(List<? extends Binding> bindings) {
    ExprSubstitution result = new ExprSubstitution();
    for (Binding binding : bindings) {
      result.add(binding, new ReferenceExpression(binding));
    }
    return result;
  }

  public String toString() {
    return mySubstExprs.toString();
  }
}